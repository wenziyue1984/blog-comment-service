package com.wenziyue.blog.comment.web.schedule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wenziyue.blog.comment.common.base.AppInstanceId;
import com.wenziyue.blog.comment.common.enums.NotifyOutboxStatusEnum;
import com.wenziyue.blog.comment.dal.dto.NotifyOutboxMqDTO;
import com.wenziyue.blog.comment.dal.entity.NotifyOutboxEntity;
import com.wenziyue.blog.comment.dal.service.NotifyOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.wenziyue.blog.comment.common.constants.RocketMqTopic.NotifyOutboxTopic;

/**
 * @author wenziyue
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyOutboxSchedule {

    private final NotifyOutboxService notifyOutboxService;
    private final RocketMQTemplate rocketMQTemplate;
    private final AppInstanceId appInstanceId;
    private final Environment env;

    @Scheduled(fixedDelay = 2000)
    public void notifyOutbox() {

        if (env.acceptsProfiles(Profiles.of("dev", "test"))) {
            log.info("当前环境为开发环境，每五分钟处理一次本地消息表，要不然控制台全是sql日志，很烦");
            try {
                Thread.sleep(5*60*1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // TODO: 2025/11/4 处理发送失败和发送超时的消息，暂时先不处理，后续再说

        // 先对一批数据做标记
        String ownerToken = appInstanceId.newOwnerToken();
        int batchSize = 200;
        int count = notifyOutboxService.batchSetOwner(ownerToken, batchSize);
        if (count == 0) return;
        List<NotifyOutboxEntity> notifyOutboxEntityList = notifyOutboxService.list(Wrappers.<NotifyOutboxEntity>lambdaQuery()
                .eq(NotifyOutboxEntity::getOwner, ownerToken)
                .eq(NotifyOutboxEntity::getStatus, NotifyOutboxStatusEnum.SENDING.getCode()));
        if (notifyOutboxEntityList.isEmpty()) return;
        log.info("开始处理本地消息表");

        // 发送给mq
        for (NotifyOutboxEntity notifyOutboxEntity : notifyOutboxEntityList) {
            try {
                // 发送给mq
                val dto = new NotifyOutboxMqDTO(notifyOutboxEntity);
                var msg = MessageBuilder.withPayload(dto)
                        .setHeader(org.apache.rocketmq.spring.support.RocketMQHeaders.KEYS, String.valueOf(notifyOutboxEntity.getId()))
                        .build();
                var sendResult = rocketMQTemplate.syncSendOrderly(NotifyOutboxTopic, msg, notifyOutboxEntity.getRecipientUserId().toString());
//                val sendResult = rocketMQTemplate.syncSendOrderly(NotifyOutboxTopic, dto, notifyOutboxEntity.getRecipientUserId().toString());
                if (sendResult == null || !sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
                    log.error("NotifyOutboxTopic加入mq失败:{}", sendResult);
                    // 修改通知状态为发送失败
                    notifyOutboxService.update(Wrappers.<NotifyOutboxEntity>lambdaUpdate()
                            .set(NotifyOutboxEntity::getStatus, NotifyOutboxStatusEnum.FAILED.getCode())
                            .eq(NotifyOutboxEntity::getId, notifyOutboxEntity.getId()));
                } else {
                    // 修改通知状态为发送成功
                    notifyOutboxService.update(Wrappers.<NotifyOutboxEntity>lambdaUpdate()
                            .set(NotifyOutboxEntity::getStatus, NotifyOutboxStatusEnum.SENT.getCode())
                            .eq(NotifyOutboxEntity::getId, notifyOutboxEntity.getId()));
                }
            } catch (Exception e) {
                log.error("处理本地消息表异常:{}", notifyOutboxEntity, e);
            }
        }
        log.info("本轮处理本地消息表结束");
    }

}
