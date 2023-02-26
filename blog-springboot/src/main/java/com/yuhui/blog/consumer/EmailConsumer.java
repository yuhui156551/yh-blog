package com.yuhui.blog.consumer;

import com.alibaba.fastjson.JSON;
import com.yuhui.blog.dto.EmailDTO;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;


import static com.yuhui.blog.constant.MQPrefixConst.EMAIL_QUEUE;

/**
 * 邮箱消息监听
 *
 * @since 1.0.0
 **/
@Component
@RabbitListener(queues = EMAIL_QUEUE)// @RabbitListener 标注在类上面表示当有收到消息的时候，就交给 @RabbitHandler 的方法处理
public class EmailConsumer {

    /**
     * 邮箱号
     */
    @Value("${spring.mail.username}")
    private String email;

    @Autowired
    private JavaMailSender javaMailSender;

    @RabbitHandler
    public void process(byte[] data) {
        // 1、获取消息
        EmailDTO emailDTO = JSON.parseObject(new String(data), EmailDTO.class);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(email);
        message.setTo(emailDTO.getEmail());
        message.setSubject(emailDTO.getSubject());
        message.setText(emailDTO.getContent());
        // 2、发送消息
        javaMailSender.send(message);
    }

}
