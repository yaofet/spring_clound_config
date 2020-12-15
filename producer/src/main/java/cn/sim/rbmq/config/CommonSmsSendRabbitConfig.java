package cn.sim.rbmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author wuyingtan
 * @create time 2020/1/3 10:12
 */
@Configuration
public class CommonSmsSendRabbitConfig {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    public static final Map<String, Object> COMMON_ROUTING_FAIL_EXCHANGE_ARGS = new HashMap<String, Object>() {{
        put(ALTERNATE_EXCHANGE_KEY, COMMON_ROUTING_FAIL_EXCHANGE);
    }};
    /**
     * 用于接收路由失败的消息的交换机的标识符
     */
    public static final String ALTERNATE_EXCHANGE_KEY = "alternate-exchange";
    // 路由失败消息的交换机，类型为fanout
    public static final String COMMON_ROUTING_FAIL_EXCHANGE = "common_routing_fail_exchange";

    /**
     * 死信队列标识符
     */
    public static final String DEAD_LETTER_EXCHANGE_KEY = "x-dead-letter-exchange";
    /**
     * 死信队列绑定标识符
     */
    public static final String DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";


    public static final String DXL_COMMON_SMS_SEND_EXCHANGE = "dxl_common_sms_send_exchange";
    public static final String DXL_COMMON_SMS_SEND_KEY = "dxl_common_sms_send_key";
    public static final String DXL_COMMON_SMS_SEND_QUEUE = "dxl_common_sms_send_queue";

    public static final String COMMON_SMS_SEND_QUEUE_PREFIX = "common_sms_send_queue_";
    public static final String COMMON_SMS_SEND_EXCHANGE = "common_sms_send_exchange";
    public static final String COMMON_SMS_SEND_KEY_PREFIX = "common_sms_send_key_";


    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory){
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return  rabbitAdmin;

    }

    @Bean(COMMON_SMS_SEND_EXCHANGE)
    public Exchange commonSmsSendExchange() {
        return ExchangeBuilder.topicExchange(COMMON_SMS_SEND_EXCHANGE).durable(true)
                .withArguments(COMMON_ROUTING_FAIL_EXCHANGE_ARGS).build();
    }

    @PostConstruct
    public void init() {
        registerQueueAndBindToExchange();
    }

    private void registerQueueAndBindToExchange() {
        rabbitAdmin.declareExchange(commonSmsSendExchange());

        Map<String, Object> args = new HashMap<>();
        args.put(DEAD_LETTER_EXCHANGE_KEY, DXL_COMMON_SMS_SEND_EXCHANGE);
        args.put(DEAD_LETTER_ROUTING_KEY, DXL_COMMON_SMS_SEND_KEY);

        IntStream.range(0,32).forEach(index -> {
            String queueName = COMMON_SMS_SEND_QUEUE_PREFIX + index;
            rabbitAdmin.declareQueue(new Queue(queueName, true, false, false, args));
            rabbitAdmin.declareBinding(
                    BindingBuilder.bind(new Queue(queueName, true, false, false, args)).to(commonSmsSendExchange())
                            .with(COMMON_SMS_SEND_KEY_PREFIX + index).noargs());
        });
    }

    @Bean(DXL_COMMON_SMS_SEND_EXCHANGE)
    public Exchange dxlCommonSmsSendExchange() {
        return ExchangeBuilder.directExchange(DXL_COMMON_SMS_SEND_EXCHANGE).durable(true)
                .withArguments(COMMON_ROUTING_FAIL_EXCHANGE_ARGS).build();
    }

    @Bean(DXL_COMMON_SMS_SEND_QUEUE)
    public Queue dxlCommonSmsSendQueue() {
        return new Queue(DXL_COMMON_SMS_SEND_QUEUE, true, false, false);
    }

    @Bean
    public Binding dxlCommonSmsSendBinding(@Qualifier(DXL_COMMON_SMS_SEND_EXCHANGE) Exchange exchange,
                                           @Qualifier(DXL_COMMON_SMS_SEND_QUEUE) Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(DXL_COMMON_SMS_SEND_KEY).noargs();
    }
}