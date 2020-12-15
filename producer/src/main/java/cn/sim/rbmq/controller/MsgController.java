package cn.sim.rbmq.controller;

import cn.sim.rbmq.config.CommonSmsSendRabbitConfig;
import cn.sim.rbmq.config.RabbitMQClusterConfig;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class MsgController {
    @Autowired
    AmqpTemplate amqpTemplate;
    @Autowired
    RabbitTemplate rt;
    @Autowired
    private ConnectionFactory connectionFactory;

    @RequestMapping("/send")
    public String sendMsg(String msg) {
        // 发布持久化消息
        MessageProperties props = MessagePropertiesBuilder.newInstance().setDeliveryMode(MessageDeliveryMode.fromInt(2))
                .build();
        Message message = new Message(msg == null ? "".getBytes() : msg.getBytes(), props);
//        amqpTemplate.convertAndSend(RabbitMQClusterConfig.EXCHANGE_NAME, "", message);
        CommonSmsJobPayload cc = new CommonSmsJobPayload();
        cc.setAddress("广州");
        cc.setAge("26");
        cc.setMobile("13650291566");
        cc.setName("yaofet");
        send(cc);

        return "发送消息【" + msg + "】成功！";
    }

    @RequestMapping("/get")
    public String getMsg() throws UnsupportedEncodingException {
        // 发布持久化消息
//        Object message = amqpTemplate.receiveAndConvert(RabbitMQClusterConfig.QUEUE_NAME,2000l);
//        if(message==null) {
//            return "没有任何数据";
//        }
//        String msg = null;
//        if (message instanceof Message) {
//            msg = new String(((Message) message).getBody(), "utf-8");
//        }
//        msg = new String((byte[])message, "utf-8");
//        Date date = new Date(System.currentTimeMillis());
//        SimpleDateFormat sd = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
          consume();
//        String format = sd.format(date);
        return "接收消息【喜哦】成功！";
    }



    public void send(CommonSmsJobPayload payload) {
        String msg = "";
        try {
            msg = JSON.toJSONString(payload);
            String msgId = UUID.randomUUID().toString();
            rt.convertAndSend(CommonSmsSendRabbitConfig.COMMON_SMS_SEND_EXCHANGE,
                    CommonSmsSendRabbitConfig.COMMON_SMS_SEND_KEY_PREFIX + Long.parseLong(payload.getMobile()) % 32, msg,
                    message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    }, new CorrelationData(msgId));
        } catch (Exception e) {
            System.out.println("失败了");
        }
    }



    private void consume() {
        Map<String, Object> dxlArg = new HashMap<>();
        dxlArg.put(CommonSmsSendRabbitConfig.DEAD_LETTER_EXCHANGE_KEY, CommonSmsSendRabbitConfig.DXL_COMMON_SMS_SEND_EXCHANGE);
        dxlArg.put(CommonSmsSendRabbitConfig.DEAD_LETTER_ROUTING_KEY, CommonSmsSendRabbitConfig.DXL_COMMON_SMS_SEND_KEY);
        for (int i = 0; i < 32; i++) {
            try {
                // 配置信道
                Channel channel = connectionFactory.createConnection().createChannel(false);
                channel.basicQos(32);

                // 声明队列
                String queueName = CommonSmsSendRabbitConfig.COMMON_SMS_SEND_QUEUE_PREFIX + i;
                channel.queueDeclare(queueName, true, false, false, dxlArg);

                // 定义消费者
                Consumer consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {
                        try {
                            String message = new String(body, StandardCharsets.UTF_8);
                            CommonSmsJobPayload payload = JSON.parseObject(message,CommonSmsJobPayload.class);
                            System.out.println("============================================");
                            System.out.println(message);
                            // 确认消息
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (Exception e) {
                            channel.basicNack(envelope.getDeliveryTag(), false, false);
                        }
                    }
                };

                channel.basicConsume(queueName, false, consumer);
            } catch (Exception e) {
            }
        }

    }

}