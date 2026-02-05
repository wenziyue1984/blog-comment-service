package com.wenziyue.blog.comment.biz.utils;

import com.wenziyue.uid.common.Status;
import com.wenziyue.uid.core.IdGen;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * id工具类
 *
 * @author wenziyue
 */
@Slf4j
public class IdUtils {

    public static long getID(IdGen idGen) {
        val nextId = idGen.nextId();
        if (nextId.getStatus().equals(Status.EXCEPTION)) {
            log.info("googleLogin 获取的id异常:{}", nextId);
            throw new RuntimeException("获取id异常");
        }
        return nextId.getId();
    }
}
