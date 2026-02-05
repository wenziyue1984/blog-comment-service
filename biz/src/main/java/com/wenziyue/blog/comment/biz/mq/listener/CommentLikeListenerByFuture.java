package com.wenziyue.blog.comment.biz.mq.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wenziyue.blog.comment.biz.utils.IdUtils;
import com.wenziyue.blog.comment.common.enums.LikeTypeEnum;
import com.wenziyue.blog.comment.common.enums.NotifyOutboxStatusEnum;
import com.wenziyue.blog.comment.common.utils.BlogUtils;
import com.wenziyue.blog.comment.dal.dto.CommentLikeMqDTO;
import com.wenziyue.blog.comment.dal.entity.CommentEntity;
import com.wenziyue.blog.comment.dal.entity.CommentLikeEntity;
import com.wenziyue.blog.comment.dal.entity.NotifyOutboxEntity;
import com.wenziyue.blog.comment.dal.service.CommentLikeService;
import com.wenziyue.blog.comment.dal.service.CommentService;
import com.wenziyue.blog.comment.dal.service.NotifyOutboxService;
import com.wenziyue.redis.utils.RedisUtils;
import com.wenziyue.uid.core.IdGen;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQPushConsumerLifecycleListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.wenziyue.blog.comment.common.constants.RocketMqTopic.CommentLikeTopic;

/**
 * 此消费者不可用，只是为了说明问题而写入这里留存
 *
 * 点赞消费者，gpt修改版本，引入了CompletableFuture，让点赞入库完成才给mq返回ack，避免了消息丢失。
 * 但是实际这个有很大的问题就是mq消费模式是顺序消费，consumeMode = ConsumeMode.ORDERLY,在顺序消费模式下只有当一条消息ack了才会消费下一条消息，
 * 所以在当前代码下queue中永远只会有一条消息，然后等待到FLUSH_INTERVAL_MS后才落库
 * 不可能三角：顺序 + 批量落库 + 等落库后再 ACK
 */
@Slf4j
@RequiredArgsConstructor
//下面两个注解注释掉，不让它真正被使用
//@Component
//@RocketMQMessageListener(
//        topic = CommentLikeTopic,
//        consumerGroup = "comment-like-consumer-group",
//        consumeMode = ConsumeMode.ORDERLY,
//        maxReconsumeTimes = 2,
//        consumeThreadNumber = 4,
//        enableMsgTrace = true
//)
public class CommentLikeListenerByFuture implements RocketMQListener<CommentLikeMqDTO>, RocketMQPushConsumerLifecycleListener {

    private final CommentLikeService commentLikeService;
    private final RedisUtils redisUtils;
    private final RedisScript<Long> createCf;
    private final RedisScript<List> commentLike;
    private final RedisScript<List> cancelCommentLike;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final NotifyOutboxService notifyOutboxService;
    private final CommentService commentService;
    private final IdGen idGen;

    /**
     * 关键：队列里放的是“待确认”的消息包装。
     * onMessage 会等待 future 完成后才返回，从而把 ack 边界推到“已落库”之后。
     */
    private final BlockingQueue<Pending> queue = new LinkedBlockingQueue<>(5000);

    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread worker;
    private volatile DefaultMQPushConsumer consumer;

    // 你可以按吞吐调这几个参数
    private static final int BATCH_SIZE = 1000;
    private static final long FLUSH_INTERVAL_MS = 2000;
    private static final long OFFER_TIMEOUT_MS = 50;
    private static final long ACK_WAIT_TIMEOUT_MS = 8000;

    @Override
    public void prepareStart(DefaultMQPushConsumer consumer) {
        this.consumer = consumer;
    }

    @PostConstruct
    public void init() {
        List<String> keys = Collections.singletonList(BlogUtils.getTodayCfKey());
        List<String> args = Arrays.asList("1000000", "2", "20", "1", Long.toString(3L * 24 * 3600));
        stringRedisTemplate.execute(createCf, keys, args.toArray());

        worker = new Thread(this::processLoop);
        worker.setName("comment-like-worker-" + UUID.randomUUID());
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        // 先停消费，避免继续进来新消息
        if (consumer != null) {
            try { consumer.suspend(); } catch (Throwable ignore) {}
        }

        running.set(false);

        if (worker != null) {
            worker.interrupt();
            worker.join(5000);
        }

        // 兜底：把剩余消息刷掉（不保证所有都能成功；失败就让 future 失败，消息会重投）
        drainAndFlushLeftovers();
    }

    private void drainAndFlushLeftovers() {
        List<Pending> left = new ArrayList<>();
        queue.drainTo(left);
        if (left.isEmpty()) return;

        try {
            flushBatch(left);
            left.forEach(p -> p.future.complete(null));
        } catch (Exception e) {
            left.forEach(p -> p.future.completeExceptionally(e));
        }
    }

    @Override
    public void onMessage(CommentLikeMqDTO dto) {
        if (dto == null || dto.getCommentId() == null || dto.getUserId() == null || dto.getType() == null) {
            log.warn("参数错误:{}", dto);
            return;
        }

        // 你原来的 redis 快速路径仍然保留：cf + 闸门 + lua 原子维护点赞数
        if (dto.getType().equals(LikeTypeEnum.LIKE.getCode())) {
            like(dto);
        } else if (dto.getType().equals(LikeTypeEnum.CANCEL_LIKE.getCode())) {
            cancelLike(dto);
        } else {
            log.warn("未知的点赞类型:{}", dto);
            return;
        }

        // 关键：把“需要落库的动作”放进队列，然后等待落库完成再返回（决定 ack）
        Pending pending = new Pending(dto);
        boolean recover = false;
        try {
            boolean offered = queue.offer(pending, OFFER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!offered) {
                // 需要把redis点赞数-1
                recover = true;
                // 队列满：直接抛异常让 MQ 过一会重投（相当于背压）
                throw new RuntimeException("like buffer full");
            }

            // 等待这一条所属的批次完成落库
            pending.future.get(ACK_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        } catch (TimeoutException te) {
            recover = true;
            // 等太久：让 MQ 重投；你已有幂等（唯一索引/去重修复/闸门），重投不会把数打爆
            throw new RuntimeException("wait flush timeout", te);
        } catch (ExecutionException ee) {
            recover = true;
            // 落库失败：抛异常让 MQ 重投
            throw new RuntimeException("flush failed", ee.getCause());
        } catch (InterruptedException ie) {
            recover = true;
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", ie);
        } finally {
            if (recover) {
                // todo 恢复redis中的数据
                if (dto.getType().equals(LikeTypeEnum.LIKE.getCode())) {
                    // todo 将cf和闸门删掉，点赞数-1，lua原子操作
                } else if (dto.getType().equals(LikeTypeEnum.CANCEL_LIKE.getCode())) {
                    // todo 点赞数+1
                }
            }
        }
    }

    private void processLoop() {
        List<Pending> buffer = new ArrayList<>(BATCH_SIZE + 100);

        long lastFlushTime = System.currentTimeMillis();

        while (running.get()) {
            try {
                Pending p = queue.poll(1, TimeUnit.SECONDS);
                if (p != null) buffer.add(p);

                boolean shouldFlush = buffer.size() >= BATCH_SIZE
                        || (!buffer.isEmpty() && System.currentTimeMillis() - lastFlushTime >= FLUSH_INTERVAL_MS);

                if (!shouldFlush) continue;

                try {
                    flushBatch(buffer);
                    buffer.forEach(x -> x.future.complete(null));
                } catch (Exception e) {
                    // 失败：让这一批都失败，onMessage 会抛异常，MQ 会重投
                    buffer.forEach(x -> x.future.completeExceptionally(e));
                } finally {
                    buffer.clear();
                    lastFlushTime = System.currentTimeMillis();
                }

            } catch (InterruptedException ie) {
                if (!running.get()) break;
            } catch (Exception e) {
                log.error("processLoop error", e);
            }
        }

        // 退出前最后刷一次
        if (!buffer.isEmpty()) {
            try {
                flushBatch(buffer);
                buffer.forEach(x -> x.future.complete(null));
            } catch (Exception e) {
                buffer.forEach(x -> x.future.completeExceptionally(e));
            } finally {
                buffer.clear();
            }
        }
    }

    /**
     * 一次 flush 的最小闭环：抵消 -> 批量落库（或删除）-> 必要的修复
     * 这里不要吞异常，吞了就会“落库失败也 ack”，风险比你现在更大。
     */
    private void flushBatch(List<Pending> pendings) {
        if (pendings == null || pendings.isEmpty()) return;

        List<CommentLikeMqDTO> list = pendings.stream()
                .map(p -> p.dto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        val split = reduceAndSplit(list);
        batchInsertAndDelete(split.getLikes(), split.getCancels());
    }

    /**
     * 下面 like/cancelLike、reduceAndSplit、removeDuplicateAndRepairData 基本沿用你现在的实现即可。
     * 我这里只保留你原来的签名，把核心逻辑直接粘过来（你也可以保持原样）。
     */
    private void like(CommentLikeMqDTO dto) {
        if (redisUtils.cfExists(BlogUtils.getTodayCfKey(), dto.getCommentId()+":"+dto.getUserId())
                || redisUtils.cfExists(BlogUtils.getYesterdayCfKey(), dto.getCommentId()+":"+dto.getUserId())) {
            val sluiceGate = redisUtils.hasKey(BlogUtils.getSluiceGateKey(dto.getCommentId().toString(), dto.getUserId().toString()));
            if (sluiceGate) return;

            val count = commentLikeService.count(Wrappers.<CommentLikeEntity>lambdaQuery()
                    .eq(CommentLikeEntity::getCommentId, dto.getCommentId())
                    .eq(CommentLikeEntity::getUserId, dto.getUserId()));
            if (count > 0) return;
        }

        String todayCf = BlogUtils.getTodayCfKey();
        String shardKey = BlogUtils.getCommentLikeCountHashKey(dto.getCommentId().toString());
        String gateKey = BlogUtils.getSluiceGateKey(dto.getCommentId().toString(), dto.getUserId().toString());

        String cfItem = dto.getCommentId() + ":" + dto.getUserId();
        String field = dto.getCommentId().toString();
        String delta = "1";
        String gateVal = "1";
        String gateTtl = Long.toString(TimeUnit.MINUTES.toSeconds(2));

        List<String> keys = Arrays.asList(todayCf, shardKey, gateKey);
        List<String> argv = Arrays.asList(cfItem, field, delta, gateVal, gateTtl);

        stringRedisTemplate.execute(commentLike, keys, argv.toArray());
    }

    private void cancelLike(CommentLikeMqDTO dto) {
        String todayCfKey = BlogUtils.getTodayCfKey();
        String yesterdayCfKey = BlogUtils.getYesterdayCfKey();
        final String item = dto.getCommentId() + ":" + dto.getUserId();
        final String field = dto.getCommentId().toString();
        final String gateKey = BlogUtils.getSluiceGateKey(dto.getCommentId().toString(), dto.getUserId().toString());
        final String hashKey = BlogUtils.getCommentLikeCountHashKey(dto.getCommentId().toString());

        if (redisUtils.cfExists(todayCfKey, dto.getCommentId()+":"+dto.getUserId())
                || redisUtils.cfExists(yesterdayCfKey, dto.getCommentId()+":"+dto.getUserId())) {

            if (redisUtils.hasKey(gateKey)) {
                List<String> keys = Arrays.asList(todayCfKey, yesterdayCfKey, hashKey, gateKey);
                List<String> argv = Arrays.asList(item, field, "-1", "1");
                stringRedisTemplate.execute(cancelCommentLike, keys, argv.toArray());
                return;
            }

            val count = commentLikeService.count(Wrappers.<CommentLikeEntity>lambdaQuery()
                    .eq(CommentLikeEntity::getCommentId, dto.getCommentId())
                    .eq(CommentLikeEntity::getUserId, dto.getUserId()));

            List<String> keys = Arrays.asList(todayCfKey, yesterdayCfKey, hashKey, gateKey);
            List<String> argv = Arrays.asList(item, field, "-1", count > 0 ? "1" : "0");
            stringRedisTemplate.execute(cancelCommentLike, keys, argv.toArray());
            return;
        }

        val count = commentLikeService.count(Wrappers.<CommentLikeEntity>lambdaQuery()
                .eq(CommentLikeEntity::getCommentId, dto.getCommentId())
                .eq(CommentLikeEntity::getUserId, dto.getUserId()));
        if (count == 0) return;

        stringRedisTemplate.opsForHash().increment(hashKey, dto.getCommentId().toString(), -1);
    }

    private void batchInsertAndDelete(List<CommentLikeMqDTO> likeList, List<CommentLikeMqDTO> cancelList) {
        // 这里的语义改成：失败就抛，让上层决定重试
        if (likeList != null && !likeList.isEmpty()) {
            List<CommentLikeEntity> existingPairs = commentLikeService.selectExistingPairs(likeList);
            if (!existingPairs.isEmpty()) {
                removeDuplicateAndRepairData(likeList, existingPairs);
            }

            val commentEntityList = commentService.list(Wrappers.<CommentEntity>lambdaQuery()
                    .select(CommentEntity::getUserId, CommentEntity::getId)
                    .in(CommentEntity::getId, likeList.stream().map(CommentLikeMqDTO::getCommentId).collect(Collectors.toList())));

            Map<Long, Long> commentAuthorMap = commentEntityList.stream()
                    .collect(Collectors.toMap(CommentEntity::getId, CommentEntity::getUserId, (a,b)->a));

            transactionTemplate.executeWithoutResult(status -> {
                // 1) 点赞表
                commentLikeService.insertIgnoreBatch(likeList);

                // 2) 本地消息表
                List<NotifyOutboxEntity> rows = likeList.stream()
                        .map(it -> {
                            val rcpt = commentAuthorMap.get(it.getCommentId());
                            if (rcpt == null) return null;
                            long newId = IdUtils.getID(idGen);
                            return NotifyOutboxEntity.builder()
                                    .id(newId)
                                    .commentId(it.getCommentId())
                                    .userId(it.getUserId())
                                    .recipientUserId(rcpt)
                                    .status(NotifyOutboxStatusEnum.NEW)
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (!rows.isEmpty()) notifyOutboxService.saveBatch(rows);
            });
        }

        if (cancelList != null && !cancelList.isEmpty()) {
            // 删除失败也应该抛，让 MQ 重投；你的 deleteBatch 自己要保证幂等
            commentLikeService.deleteBatch(cancelList);
        }
    }

    private void removeDuplicateAndRepairData(List<CommentLikeMqDTO> likeList, List<CommentLikeEntity> existingPairs) {
        if (likeList == null || likeList.isEmpty() || existingPairs == null || existingPairs.isEmpty()) return;

        Set<String> existed = existingPairs.stream()
                .map(p -> p.getCommentId() + ":" + p.getUserId())
                .collect(Collectors.toSet());

        likeList.removeIf(item -> existed.contains(item.getCommentId() + ":" + item.getUserId()));

        for (CommentLikeEntity existingPair : existingPairs) {
            String hashKey = BlogUtils.getCommentLikeCountHashKey(existingPair.getCommentId().toString());
            stringRedisTemplate.opsForHash().increment(hashKey, existingPair.getCommentId().toString(), -1);
        }
    }

    private Split reduceAndSplit(List<CommentLikeMqDTO> list) {
        if (list == null || list.isEmpty()) {
            return new Split(Collections.emptyList(), Collections.emptyList());
        }

        Map<Key, Integer> diffMap = new HashMap<>(list.size() * 2);
        for (CommentLikeMqDTO dto : list) {
            if (dto == null) continue;
            Long cid = dto.getCommentId(), uid = dto.getUserId();
            Integer type = dto.getType();
            if (cid == null || uid == null || type == null) continue;
            if (!type.equals(LikeTypeEnum.LIKE.getCode()) && !type.equals(LikeTypeEnum.CANCEL_LIKE.getCode())) continue;

            Key k = new Key(cid, uid);
            int delta = (type == 0) ? 1 : -1;
            diffMap.merge(k, delta, Integer::sum);
        }

        List<CommentLikeMqDTO> likes = new ArrayList<>();
        List<CommentLikeMqDTO> cancels = new ArrayList<>();

        for (Map.Entry<Key, Integer> e : diffMap.entrySet()) {
            Key k = e.getKey();
            int diff = e.getValue();
            if (diff > 0) {
                likes.add(CommentLikeMqDTO.builder().commentId(k.commentId).userId(k.userId).type(0).build());
            } else if (diff < 0) {
                cancels.add(CommentLikeMqDTO.builder().commentId(k.commentId).userId(k.userId).type(1).build());
            }
        }
        return new Split(likes, cancels);
    }

    @Data
    @RequiredArgsConstructor
    public static class Split {
        private final List<CommentLikeMqDTO> likes;
        private final List<CommentLikeMqDTO> cancels;
    }

    private static final class Key {
        final Long commentId;
        final Long userId;
        Key(Long c, Long u) { this.commentId = c; this.userId = u; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return Objects.equals(commentId, k.commentId) && Objects.equals(userId, k.userId);
        }
        @Override public int hashCode() { return Objects.hash(commentId, userId); }
    }

    private static final class Pending {
        final CommentLikeMqDTO dto;
        final CompletableFuture<Void> future = new CompletableFuture<>();
        Pending(CommentLikeMqDTO dto) { this.dto = dto; }
    }
}