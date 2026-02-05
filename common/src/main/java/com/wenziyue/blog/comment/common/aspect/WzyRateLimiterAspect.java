package com.wenziyue.blog.comment.common.aspect;

import com.wenziyue.blog.comment.common.annotation.WzyRateLimiter;
import com.wenziyue.framework.exception.ApiException;
import com.wenziyue.redis.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.wenziyue.blog.comment.common.constants.RedisConstant.LIMIT_RATE_PREFIX;
import static com.wenziyue.blog.comment.common.constants.RedisConstant.LIMIT_RATE_PREFIX;
import static com.wenziyue.blog.comment.common.exception.BlogResultCode.GLOBAL_RATE_LIMITED;

/**
 * @author wenziyue
 */
@Aspect
@Component
@RequiredArgsConstructor
public class WzyRateLimiterAspect {

    private final RedisUtils redisUtils;

    @Pointcut("@annotation(com.wenziyue.blog.comment.common.annotation.WzyRateLimiter)")
    public void rateLimitPointcut() {}

    @Around("rateLimitPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        WzyRateLimiter rateLimiter = method.getAnnotation(WzyRateLimiter.class);

        Object[] args = joinPoint.getArgs();
        String realKey = parseSpEL(rateLimiter.key(), joinPoint);
        String key = LIMIT_RATE_PREFIX + realKey;

        long window = rateLimiter.window();
        int maxCount = rateLimiter.maxCount();
        String message = rateLimiter.message();

        // 滑动窗口限流判断
        long now = System.currentTimeMillis();
        redisUtils.zRemoveRangeByScore(key, 0, now - window);
        Set<Object> members = redisUtils.zRange(key, 0, -1);
        if (members.size() >= maxCount) {
            throw new ApiException(GLOBAL_RATE_LIMITED.getCode(), message);
        }
        // 使用 now + UUID 保证唯一
        String uniqueValue = now + "-" + UUID.randomUUID();
        redisUtils.zAdd(key, uniqueValue, now, window, TimeUnit.MILLISECONDS);

        return joinPoint.proceed();
    }

    private String parseSpEL(String spEL, ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            Object[] args = joinPoint.getArgs();
            String[] paramNames = signature.getParameterNames();

            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            Expression expression = new SpelExpressionParser().parseExpression(spEL);
            return expression.getValue(context, String.class);
        } catch (Exception e) {
            throw new RuntimeException("解析SpEL表达式出错: " + spEL, e);
        }
    }
}
