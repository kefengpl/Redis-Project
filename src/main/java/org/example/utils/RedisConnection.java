package org.example.utils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

/**
 * @Author 3590
 * @Date 2024/1/24 0:39
 * @Description 设置 ssh 连接 redis
 */
@Component
public class RedisConnection {
    Session session;

    /**
     * 建立 SSH 连接
     * */
    @PostConstruct
    public void establishConnection() {
        try {
            JSch jSch = new JSch();
            session = jSch.getSession("root", "127.0.0.1", 2222);
            session.setPassword("1248");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            // 相当于配置了端口转发
            // 将请求转发到服务器并连接到数据库
            // 之所以可能 6379 被绑定且占用，是因为 IDEA 自带的 Redis 连接工具。直接把它删去就好
            session.setPortForwardingL(6379, "127.0.0.1", 6379);
            session.setTimeout(1000000);
            System.out.println("The ssh connection is OK.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭 SSH 连接
     * */
    @PreDestroy
    public void closeConnection() {
        session.disconnect();
        System.out.println("The ssh connection is destroyed");
    }
}
