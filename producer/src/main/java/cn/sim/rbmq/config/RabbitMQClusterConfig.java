package cn.sim.rbmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQClusterConfig {
    /*
     * @Autowired RabbitTemplate rabbitTemplate;
     *
     * @Autowired AmqpTemplate amqpTemplate;
     */

    public static final String QUEUE_NAME = "node3Queue";

    public static final String EXCHANGE_NAME="testExchange";

    // 创建持久化的node3Queue
    @Bean
    Queue queue() {
        return  QueueBuilder.durable(QUEUE_NAME).build();
    }

    // 创建持久化的testExchange
    @Bean
    org.springframework.amqp.core.Exchange exchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME).durable(true).build();
    }

    // 创建绑定
    @Bean
    Binding bindings(){
        return BindingBuilder.bind(queue()).to(exchange()).with("").and(null);
    }

}