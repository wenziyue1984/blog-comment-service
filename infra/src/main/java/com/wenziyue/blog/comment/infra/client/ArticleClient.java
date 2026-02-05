package com.wenziyue.blog.comment.infra.client;

import com.wenziyue.blog.comment.infra.dto.ArticleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author wenziyue
 */

@FeignClient(
        name = "blog-article-service",
//        url = "${blog.service-url.article}",
        path = "/internal/articles"
)
public interface ArticleClient {

    @GetMapping("/getArticleById/{id}")
    ArticleDTO getArticleById(@PathVariable("id") Long id);
}
