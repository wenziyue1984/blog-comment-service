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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.wenziyue.blog.comment.common.constants.RocketMqTopic.CommentLikeTopic;

/**
 * 这里还有个问题是关于批量处理，这里用的是阻塞队列，但是阻塞队列的容量是有限的，如果消息处理速度比消息产生速度要快，那么就会导致阻塞队列满，
 * 并且如果宕机，那么就会导致队列中的消息丢失，内存中的队列并不是可靠的，所以需要能持久化的队列，
 *
 * 提高性能的关键就是数据库，一切都为了减少它的压力，尽量用批量处理，大大减少操作数据库次数。所做的一切都是围绕这点。
 *
 * @author wenziyue
 */
@Slf4j
@RequiredArgsConstructor
@Component
@RocketMQMessageListener(topic = CommentLikeTopic, consumerGroup = "comment-like-consumer-group",
        consumeMode = ConsumeMode.ORDERLY, // 消费模式，默认并发模式，消费顺序消息使用顺序模式
        maxReconsumeTimes = 2, consumeThreadNumber = 4, // 设置线程数为4,因为是顺序消费，所以和topic的读队列数对应，它默认为4
        enableMsgTrace = true)
public class CommentLikeListener implements RocketMQListener<CommentLikeMqDTO>, InitializingBean, RocketMQPushConsumerLifecycleListener {

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

    private final BlockingQueue<CommentLikeMqDTO> queue = new LinkedBlockingQueue<>(5000);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread worker;
    private volatile DefaultMQPushConsumer consumer;

    @Override
    public void prepareStart(DefaultMQPushConsumer consumer) {
        this.consumer = consumer; // 保存引用，之后可暂停/关闭
    }

    @PostConstruct
    public void init() {
        // 获取当天的日期YYYYMMDD
        List<String> keys = Collections.singletonList(BlogUtils.getTodayCfKey());
        List<String> args = Arrays.asList("1000000", "2", "20", "1", Long.toString(3L * 24 * 3600)); //三天过期
        stringRedisTemplate.execute(createCf, keys, args.toArray());
    }

    @Override
    public void afterPropertiesSet() {
        // 启动后台处理线程
        worker = new Thread(this::processLoop);
        worker.setName("comment-like-worker-" + UUID.randomUUID());
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 停机：发停机信号，打断等待，等工作线程把 buffer 自己刷完退出；兜底再 drain 一轮
     */
    @PreDestroy
    public void shutdown() throws InterruptedException {
        // 1 先停消费：不再向 onMessage 投递新消息
        if (consumer != null) {
            try {
                consumer.suspend(); // 有的版本叫 pause / suspend
                // 若没有 suspend，可直接 consumer.shutdown()（会更“狠”）
            } catch (Throwable ignore) {}
        }

        // 2 再关本地 worker：优雅刷完队列
        running.set(false);
        if (worker == null) {
            return;
        }
        worker.interrupt(); // 打断 queue.poll 的阻塞
        worker.join(5000); // 主线程等最多5s
        // 兜底：再把队列里剩余的怼一把
        if (!worker.isAlive()) {
            // 仅当 worker 已退出时再兜底
            drainAndFlushLeftovers();
        } else {
            log.warn("worker didn't stop within 5s, skip leftover drain to avoid race.");
        }
    }
    
    private void drainAndFlushLeftovers() {
        log.info("开始处理剩余的点赞行为");
        List<CommentLikeMqDTO> last = new ArrayList<>();
        queue.drainTo(last);
        if (last.isEmpty()) {
            return;
        }
        val split = reduceAndSplit(last);
        batchInsertAndDelete(split.getLikes(), split.getCancels());
        log.info("剩余的点赞行为处理完毕");
    }

    @Override
    public void onMessage(CommentLikeMqDTO dto) {
        if (dto == null || dto.getCommentId() == null || dto.getUserId() == null || dto.getType() == null) {
            log.warn("参数错误:{}", dto);
            return;
        }
        if (dto.getType().equals(LikeTypeEnum.LIKE.getCode())) {
            like(dto);
        } else if (dto.getType().equals(LikeTypeEnum.CANCEL_LIKE.getCode())) {
            cancelLike(dto);
        } else {
            log.warn("未知的点赞类型:{}", dto);
        }

    }

    private void like(CommentLikeMqDTO dto) {
        // 先判断昨天和今天的cf中是否存在该用户对该评论的点赞行为
        if (redisUtils.cfExists(BlogUtils.getTodayCfKey(), dto.getCommentId()+":"+dto.getUserId())
                || redisUtils.cfExists(BlogUtils.getYesterdayCfKey(), dto.getCommentId()+":"+dto.getUserId())) {
            // 如果存在，要确定是否为cf的假阳性
            // 先判断闸门中是否存在该用户对该评论的点赞行为
            val sluiceGate = redisUtils.hasKey(BlogUtils.getSluiceGateKey(dto.getCommentId().toString(), dto.getUserId().toString()));
            // 如果闸门存在，说明cf不是假阳性，则直接返回
            if (sluiceGate) {
                return;
            }
            // 查表中是否存在该用户对该评论的点赞行为
            val count = commentLikeService.count(Wrappers.<CommentLikeEntity>lambdaQuery()
                    .eq(CommentLikeEntity::getCommentId, dto.getCommentId())
                    .eq(CommentLikeEntity::getUserId, dto.getUserId()));
            // 如果表里有点赞记录，说明cf不是假阳性，则直接返回
            if (count > 0) {
                return;
            }
        }

        // 确实没有点赞过则进行点赞
        // lua 原子执行redis操作：添加进cf，点赞数+1，记录闸门
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

        // 点赞行为添加进queue中，用于批量处理
        if (!queue.offer(dto)) {
            log.error("queue offer failed:{}", dto);
        }
    }
    private void cancelLike(CommentLikeMqDTO dto) {
        // 先判断昨天和今天的cf中是否存在该用户对该评论的点赞行为
        String todayCfKey = BlogUtils.getTodayCfKey(); // 当天cf的key
        String yesterdayCfKey = BlogUtils.getYesterdayCfKey(); // 昨天的cf的key
        final String item = dto.getCommentId() + ":" + dto.getUserId(); // cf中的item
        final String field = dto.getCommentId().toString(); // hash中的field
        final String gateKey = BlogUtils.getSluiceGateKey(dto.getCommentId().toString(), dto.getUserId().toString()); // 闸门的key
        final String hashKey = BlogUtils.getCommentLikeCountHashKey(dto.getCommentId().toString()); // hash的key
        if (redisUtils.cfExists(todayCfKey, dto.getCommentId()+":"+dto.getUserId())
                || redisUtils.cfExists(yesterdayCfKey, dto.getCommentId()+":"+dto.getUserId())) {
            // 如果存在，要确定是否为cf的假阳性
            // 先判断闸门中是否存在该用户对该评论的点赞行为
            if (redisUtils.hasKey(gateKey)) {
                // 如果有的话则说明确实点赞过了，需要取消点赞并且清理cf和闸门
                // lua执行：取消点赞、清理cf、清理闸门
                List<String> keys = Arrays.asList(todayCfKey, yesterdayCfKey, hashKey, gateKey);
                List<String> argv = Arrays.asList(item, field, "-1", "1");
                stringRedisTemplate.execute(cancelCommentLike, keys, argv.toArray());
                // 取消点赞行为添加进queue中，用于批量处理
                if (!queue.offer(dto)) {
                    log.error("queue offer error:{}", dto);
                }
                return;
            }

            // 查表中是否存在该用户对该评论的点赞行为。如果不存在，说明只是cf的假阳性，那么删除掉cf即可；如果存在，则说明真的点赞过，那么不仅要删除cf，还要进行取消点赞
            val count = commentLikeService.count(Wrappers.<CommentLikeEntity>lambdaQuery()
                    .eq(CommentLikeEntity::getCommentId, dto.getCommentId())
                    .eq(CommentLikeEntity::getUserId, dto.getUserId()));
            // lua执行：清理cf、清理闸门、根据count决定点赞数是否减1
            List<String> keys = Arrays.asList(todayCfKey, yesterdayCfKey, hashKey, gateKey);
            List<String> argv = Arrays.asList(item, field, "-1", /*根据count决定是否要点赞数-1*/count > 0 ? "1" : "0");
            stringRedisTemplate.execute(cancelCommentLike, keys, argv.toArray());
            // 取消点赞行为添加进queue中，用于批量处理
            if (!queue.offer(dto)) {
                log.error("queue offer error:{}", dto);
            }
            return;
        }

        // 两天的cf中没有的话，说明起码这两天没有点赞过，那么如果点赞过也肯定已经落库，后面再不用考虑维护cf和闸门中的数据
        // 查表中是否存在该用户对该评论的点赞行为
        val count = commentLikeService.count(Wrappers.<CommentLikeEntity>lambdaQuery()
                .eq(CommentLikeEntity::getCommentId, dto.getCommentId())
                .eq(CommentLikeEntity::getUserId, dto.getUserId()));
        // 没有点赞过，直接返回
        if (count == 0) {
            return;
        }
        // 存在，则进行取消点赞
        // 进行redis操作：点赞数-1。因为redisUtils.hIncrBy()使用的模板是redisTemplate，为了避免和定时任务中模板不统一，所以这里统一使用stringRedisTemplate
        stringRedisTemplate.opsForHash().increment(hashKey, dto.getCommentId().toString(), -1);
        // 取消点赞行为添加进queue中，用于批量处理
        if (!queue.offer(dto)) {
            log.error("queue offer error:{}", dto);
        }
    }

    private void processLoop() {
        List<CommentLikeMqDTO> buffer = new ArrayList<>(1500);
        long FLUSH_INTERVAL_MS = 2000;
        long lastFlushTime = System.currentTimeMillis();

        while (running.get()) {
            try {
                // 每次取一条，等待最多 1 秒
                CommentLikeMqDTO dto = queue.poll(1, TimeUnit.SECONDS);
                if (dto != null) {
                    buffer.add(dto);
                }
                // 满足批量写入条件（满1000条 或 刷新时间大于2秒）
                if (buffer.size() >= 1000 || (!buffer.isEmpty() && System.currentTimeMillis() - lastFlushTime >= FLUSH_INTERVAL_MS)) {
                    try {
                        val split = reduceAndSplit(buffer);
                        // 批量操作
                        batchInsertAndDelete(split.getLikes(), split.getCancels());
                    } finally {
                        // 落库后清空buffer
                        buffer.clear();
                        lastFlushTime = System.currentTimeMillis();
                    }
                }
            } catch (InterruptedException ie) {
                // 被 shutdown() 打断
                if (!running.get()) break;   // 退出循环，进入最后收尾
                // 下面这行代码的意思是重新标记中断状态，但是我们的业务中不需要处理中断，直接继续while循环即可，所以这里注释掉。没有删掉这行的目的是提醒自己这里还可以标记中断状态
//                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            }
        }

        // 最后一刷：把队列里还有的 + buffer 里剩的都刷掉
        try {
            List<CommentLikeMqDTO> rest = new ArrayList<>(buffer);
            buffer.clear();
            queue.drainTo(rest);
            if (rest.isEmpty()) {
                return;
            }
            val split = reduceAndSplit(rest);
            batchInsertAndDelete(split.getLikes(), split.getCancels());
        } catch (Exception e) {
            log.error("final flush on shutdown failed", e);
        }
    }

    private void batchInsertAndDelete(List<CommentLikeMqDTO> likeList, List<CommentLikeMqDTO> cancelList) {
        // 批量写库
        try {
            if (likeList != null && !likeList.isEmpty()) {
                // 写入前先查询一下，去掉重复数据
                List<CommentLikeEntity> existingPairs = commentLikeService.selectExistingPairs(likeList);
                if (!existingPairs.isEmpty()) {
                    removeDuplicateAndRepairData(likeList, existingPairs);
                }
                // 批量查询一下评论对应的用户，本地消息表用
                val commentEntityList = commentService.list(Wrappers.<CommentEntity>lambdaQuery()
                        .select(CommentEntity::getUserId, CommentEntity::getId)
                        .in(CommentEntity::getId, likeList.stream().map(CommentLikeMqDTO::getCommentId).collect(Collectors.toList())));
                Map<Long, Long> commentAuthorMap = commentEntityList.stream()
                        .collect(Collectors.toMap(CommentEntity::getId, CommentEntity::getUserId, (a,b)->a));
                // 执行插入
                transactionTemplate.executeWithoutResult(status -> {
                    commentLikeService.insertIgnoreBatch(likeList);
                    // 本地消息表
                    List<NotifyOutboxEntity> rows = likeList.stream()
                            .map(it -> {
                                val rcpt = commentAuthorMap.get(it.getCommentId());
                                if (rcpt == null) return null; // 评论不存在就丢弃
                                long newId;
                                try {
                                    newId = IdUtils.getID(idGen);
                                } catch (Exception e) {
                                    log.error("获取id异常", e);
                                    return null;
                                }
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
                    if (!rows.isEmpty()) {
                        notifyOutboxService.saveBatch(rows);
                    }
                });
            }
        } catch (Exception e) {
            log.error("点赞批量写库失败:{}", likeList, e);
        }
        // 批量删除
        try {
            if (cancelList != null && !cancelList.isEmpty()) {
                commentLikeService.deleteBatch(cancelList);
            }
        } catch (Exception e) {
            log.error("取消点赞批量写库失败:{}", cancelList, e);
        }
    }

    /**
     * 去除likeList中在existingPairs中的数据，并修复redis中点赞数据
     * @param likeList 原dto列表
     * @param existingPairs 已存在的数据
     */
    private void removeDuplicateAndRepairData(List<CommentLikeMqDTO> likeList, List<CommentLikeEntity> existingPairs) {
        if (likeList == null || likeList.isEmpty() || existingPairs == null || existingPairs.isEmpty()) {
            return;
        }
        // 已存在 pair -> Set，提高匹配效率
        Set<String> existed = existingPairs.stream()
                .map(p -> p.getCommentId() + ":" + p.getUserId())
                .collect(Collectors.toSet());
        likeList.removeIf(item -> existed.contains(item.getCommentId() + ":" + item.getUserId()));
        // 也可以用下面的方式，但是复杂度是O(N*M)，相对与上面的O(N)性能更差
//        likeList.removeIf(item -> existingPairs.stream().anyMatch(pair -> pair.getCommentId().equals(item.getCommentId()) && pair.getUserId().equals(item.getUserId())));

        // 修复redis中点赞数据（评论点赞数-1）
        for (CommentLikeEntity existingPair : existingPairs) {
            String hashKey = BlogUtils.getCommentLikeCountHashKey(existingPair.getCommentId().toString());
            stringRedisTemplate.opsForHash().increment(hashKey, existingPair.getCommentId().toString(), -1);
        }
    }

    /**
     * 抵消并拆分：
     * 对每个 (commentId, userId) 计算 diff = #like - #cancel；
     * 按净效应保留至多一条
     */
    private Split reduceAndSplit(List<CommentLikeMqDTO> list) {
        if (list == null || list.isEmpty()) {
            return new Split(Collections.emptyList(), Collections.emptyList());
        }

        // 1) 汇总净效应
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

        // 2) 生成结果
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

    /** 键：同一对 (commentId, userId) 归为一组 */
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
}
