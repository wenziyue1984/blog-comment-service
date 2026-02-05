package com.wenziyue.blog.comment.infra.client;

import com.wenziyue.blog.comment.infra.dto.UserInfoFeignDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

/**
 * @author wenziyue
 */
@FeignClient(
        name = "blog-user-service",
//        url = "${blog.service-url.user}", # 注册中心获取，无须url
        path = "/internal/user"

)
public interface UserClient {

    @GetMapping("/userInfo/{id}")
    UserInfoFeignDTO userInfo(@PathVariable("id") Long id);

    @PostMapping("/getUserInfoList")
    List<UserInfoFeignDTO> getUserInfoList(@RequestBody Set<Long> ids);
}
