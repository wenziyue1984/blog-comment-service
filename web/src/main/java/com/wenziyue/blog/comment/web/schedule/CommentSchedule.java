package com.wenziyue.blog.comment.web.schedule;

import com.wenziyue.blog.comment.common.utils.BlogUtils;
import com.wenziyue.blog.comment.dal.dto.CommentLikeDeltaDTO;
import com.wenziyue.blog.comment.dal.service.CommentService;
import com.wenziyue.redis.lock.DistLock;
import com.wenziyue.redis.lock.DistLockFactory;
import com.wenziyue.redis.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.wenziyue.blog.comment.common.constants.RedisConstant.*;
import static com.wenziyue.blog.comment.common.constants.RedisConstant.COMMENT_LIKE_SYNC_LOCK_KEY;

/**
 * @author wenziyue
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentSchedule {

    private final CommentService commentService;
    private final RedisUtils redisUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> createCf;
    private final RedisScript<Long> changeHashKey;
    private final DistLockFactory distLockFactory;

    private final AtomicBoolean stopping = new AtomicBoolean(false);

    @PreDestroy
    public void onShutdown() {
        stopping.set(true);
    }

    /**
     * 每天23:50创建明天的cf
     */
    @Scheduled(cron = "0 50 23 * * ?")
    public void createTomorrowCf() {
        log.info("开始创建cf");
        // 不存在cf就创建，为了避免多机器一直执行造成并发问题，用lua脚本原子执行
        List<String> keys = Collections.singletonList(BlogUtils.getTomorrowCfKey());
        List<String> args = Arrays.asList("1000000", "2", "20", "1", Long.toString(3L * 24 * 3600)); //三天过期
        stringRedisTemplate.execute(createCf, keys, args.toArray());
    }

    /**
     * 同步评论点赞数，不断执行，间隔2s
     */
    @Scheduled(fixedDelay = 2000)
    public void syncCommentLikeCount() {
        // 正在停机，直接跳过本轮（其实没啥用，定时任务优雅停机靠配置文件中的:spring:task:scheduling:shutdown:await-termination: true）
        if (stopping.get()) return;

        // 看门狗分布式锁：用唯一值占位，释放时比对，避免“删了别人的锁”
        try (DistLock lock = distLockFactory.create(COMMENT_LIKE_SYNC_LOCK_KEY, 60, TimeUnit.SECONDS)) {
            if (!lock.tryLock()) {
                return;
            }
//            log.info("开始同步评论点赞数");

            // 先处理“上次失败遗留”的 drainKey（每片最多一个），防丢
            for (int shard = 0; shard < 64; shard++) {
                String pattern = BlogUtils.getCommentLikeHashPatternKey(shard);
                scanAndDrain(pattern);
            }

            // 再旋转当前增量并处理
            for (int shard = 0; shard < 64; shard++) {
                String base = BlogUtils.shardKey(shard);
                String drainKey = BlogUtils.getCommentLikeHashDrainKey(shard);

                // 直接旋转；不存在就跳过。多机并发安全在 Lua 原子里。
                val rotated = stringRedisTemplate.execute(changeHashKey, Arrays.asList(base, drainKey));
                if (rotated == null || rotated == 0L) continue;

                drainOnce(drainKey, /*setTtlOnDrain*/ true);
            }

//            log.info("结束同步评论点赞数");
        }
    }

    /**
     * 扫描所有符合 pattern 的 drainKey 并处理（用于上次失败的重试），用 SCAN 而非 KEYS
     */
    private void scanAndDrain(String pattern) {
        ScanOptions so = ScanOptions.scanOptions().match(pattern).count(16).build();
        try (Cursor<byte[]> kc = Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection().scan(so)) {
            while (kc.hasNext()) {
                String drainKey = new String(kc.next(), java.nio.charset.StandardCharsets.UTF_8);
                log.info("重新处理 drainKey={}", drainKey);
                drainOnce(drainKey, /*setTtlOnDrain*/ false);
            }
        }
    }

    /**
     * 处理一个 drainKey：HSCAN → 批量 SQL；成功后 UNLINK；失败则保留并设一个兜底 TTL
     */
    private void drainOnce(String drainKey, boolean setTtlOnDrain) {
        boolean success = false;
        // 可选：给 drainKey 设个兜底 TTL，避免进程崩溃后永久遗留（例如 30 分钟）
        if (setTtlOnDrain) stringRedisTemplate.expire(drainKey, java.time.Duration.ofMinutes(30));

        List<CommentLikeDeltaDTO> batch = new ArrayList<>(1000);
        ScanOptions scan = ScanOptions.scanOptions().count(500).build();
        try (Cursor<Map.Entry<Object, Object>> cur = stringRedisTemplate.opsForHash().scan(drainKey, scan)) {

            while (cur.hasNext()) {
                Map.Entry<Object, Object> e = cur.next();
                long commentId = Long.parseLong(String.valueOf(e.getKey()));
                int delta = Integer.parseInt(String.valueOf(e.getValue()));
                if (delta == 0) continue;

                batch.add(CommentLikeDeltaDTO.builder()
                        .commentId(commentId)
                        .delta(delta)
                        .build());

                if (batch.size() >= 1000) {
                    flushBatchAndDelete(drainKey, batch);
                }
            }
            if (!batch.isEmpty()) {
                flushBatchAndDelete(drainKey, batch);
            }
            success = true;
        } catch (Exception ex) {
            log.error("处理 drainKey={} 失败，将保留以便下次重试", drainKey, ex);
        } finally {
            if (success) {
                // 只在成功时删除；失败保留，下一轮 scanAndDrain 会再处理
                stringRedisTemplate.unlink(drainKey);
            }
        }
    }

    private void flushBatchAndDelete(String drainKey, List<CommentLikeDeltaDTO> batch) {
        if (batch.isEmpty()) return;
        try {
            commentService.batchApplyLikeDeltas(batch);
            // 写库成功，再删除本批字段，避免下次重复执行
            Object[] fields = batch.stream()
                    .map(it -> String.valueOf(it.getCommentId()))
                    .toArray(Object[]::new);
            hDelChunked(drainKey, fields, 400);
        } catch (Exception ex) {
            // 失败抛出给上层，让 drainOnce 保留 drainKey 以便重试
            throw ex;
        } finally {
            batch.clear();
        }
    }

    /**
     * 批量删除字段，分批执行，防止单次删除太多字段耗时太久
     */
    private void hDelChunked(String key, Object[] fields, int chunk) {
        for (int i = 0; i < fields.length; i += chunk) {
            int to = Math.min(i + chunk, fields.length);
//            redisUtils.hDel(key, Arrays.copyOfRange(fields, i, to));
            // 因为redisUtils.hDel()用的是redisTemplate，而写入的是stringRedisTemplate，所以这里用stringRedisTemplate执行删除
            stringRedisTemplate.opsForHash().delete(key,  Arrays.copyOfRange(fields, i, to));
        }
    }
}
