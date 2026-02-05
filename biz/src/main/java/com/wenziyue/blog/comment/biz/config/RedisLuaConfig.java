package com.wenziyue.blog.comment.biz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * @author wenziyue
 */
@Configuration
public class RedisLuaConfig {
    @Bean
    public RedisScript<Long> clearAllTokensScript() {
        return RedisScript.of(
                new ClassPathResource("scripts/clearAllUserTokens.lua"),
                Long.class
        );
    }

    @Bean("likeArticleScript")
    public RedisScript<Boolean> likeArticleScript() {
        return RedisScript.of(
                new ClassPathResource("scripts/likeArticle.lua"),
                Boolean.class
        );
    }

    @Bean("cancelLikeArticleScript")
    public RedisScript<Boolean> cancelLikeArticleScript() {
        return RedisScript.of(new ClassPathResource("scripts/cancelLikeArticle.lua"), Boolean.class);
    }

    @Bean("countPV")
    public DefaultRedisScript<Long> countPV() {
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setLocation(new ClassPathResource("scripts/CountPV.lua"));
        s.setResultType(Long.class);
        return s;
    }

    @Bean("createCf")
    public RedisScript<Long> createCf() {
        return RedisScript.of(new ClassPathResource("scripts/createCf.lua"), Long.class);
    }

    @Bean("changeHashKey")
    public RedisScript<Long> changeHashKey() {
        return RedisScript.of(new ClassPathResource("scripts/changeHashKey.lua"), Long.class);
    }

    @Bean("commentLike")
    public RedisScript<List> commentLike() {
        return RedisScript.of(new ClassPathResource("scripts/commentLike.lua"), List.class);
    }

    @Bean("cancelCommentLike")
    public RedisScript<List> cancelCommentLike() {
        return RedisScript.of(new ClassPathResource("scripts/cancelCommentLike.lua"), List.class);
    }

    @Bean("getNextSeqIfPresent")
    public RedisScript<Long> getNextSeqIfPresent() {
        return RedisScript.of(new ClassPathResource("scripts/getNextSeqIfPresent.lua"), Long.class);
    }

    @Bean("nextOrElectRebuild")
    public RedisScript<List> nextOrElectRebuild() {
        return RedisScript.of(new ClassPathResource("scripts/nextOrElectRebuild.lua"), List.class);
    }

    @Bean("initFromBaseAndNext")
    public RedisScript<Long> initFromBaseAndNext() {
        return RedisScript.of(new ClassPathResource("scripts/initFromBaseAndNext.lua"), Long.class);
    }

    @Bean("lastOrElectRebuild")
    public RedisScript<List> lastOrElectRebuild() {
        return RedisScript.of(new ClassPathResource("scripts/lastOrElectRebuild.lua"), List.class);
    }

    @Bean("initFromBase")
    public RedisScript<Long> initFromBase() {
        return RedisScript.of(new ClassPathResource("scripts/initFromBase.lua"), Long.class);
    }

}
