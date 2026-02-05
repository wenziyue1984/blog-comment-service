package com.wenziyue.blog.comment.common.base;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.UUID;

/**
 * @author wenziyue
 */
@Component
public class AppInstanceId {

    private final String instanceId;

    public AppInstanceId(Environment env) {
        String app  = env.getProperty("spring.application.name", "app");
        String pod  = System.getenv("HOSTNAME"); // K8s / Docker 推荐
        String host = safeHost();
        String pid  = jdk8Pid();
        String port = env.getProperty("server.port", "?");

        String node = (pod != null && !pod.isEmpty()) ? pod : host + "-" + pid;
        this.instanceId = app + ":" + node + ":" + port;
    }

    public String get() {
        return instanceId;
    }

    private static String safeHost() {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "unknown-host"; }
    }

    private static String jdk8Pid() {
        try {
            String jvm = ManagementFactory.getRuntimeMXBean().getName(); // e.g. "12345@myhost"
            int i = jvm.indexOf('@');
            return (i > 0) ? jvm.substring(0, i) : jvm; // 拿到 "12345"
        } catch (Exception e) {
            return "unknown-pid";
        }
    }

    /** 生成一次性的 ownerToken：instanceId + 线程 + uuid */
    public String newOwnerToken() {
        Thread t = Thread.currentThread();
        return instanceId
                + ":" + t.getName() + "-" + t.getId()
                + ":" + UUID.randomUUID().toString().replace("-", "");
    }
}
