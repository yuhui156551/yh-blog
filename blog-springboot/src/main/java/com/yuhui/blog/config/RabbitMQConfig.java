package com.yuhui.blog.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.yuhui.blog.constant.MQPrefixConst.*;

/**
 * Rabbitmq配置类
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 文章队列
     *
     * durable – 如果我们声明一个持久队列，则为 true（该队列将在服务器重新启动后幸存下来）
     */
    @Bean
    public Queue articleQueue() {
        return new Queue(MAXWELL_QUEUE, true);
    }

    /**
     * maxwell交换机（fanout）
     */
    @Bean
    public FanoutExchange maxWellExchange() {
        return new FanoutExchange(MAXWELL_EXCHANGE, true, false);
    }

    /**
     * 绑定文章队列和maxwell交换机
     */
    @Bean
    public Binding bindingArticleDirect() {
        return BindingBuilder.bind(articleQueue()).to(maxWellExchange());
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public FanoutExchange emailExchange() {
        return new FanoutExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public Binding bindingEmailDirect() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange());
    }

}
