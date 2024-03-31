package com.learn.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Test
    public void testKafka() {
        kafkaProducer.sendMessage("test", "你好");
        kafkaProducer.sendMessage("test", "在吗");

        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

@Component //由spring容器管理
class KafkaProducer {

    @Autowired //注入spring容器中的kafka模板
    private KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String content) { //发送到的主题以及消息内容（主动发）
        kafkaTemplate.send(topic, content);
    }

}

@Component
class KafkaConsumer {

    @KafkaListener(topics = {"test"}) //说明这个方法监听kafka中“test”主题的消息
    //此时这个线程会一直阻塞在此读取"test"的消息，收到消息就会自动调用这个方法去处理消息（listener）（被动收）
    public void handleMessage(ConsumerRecord record) { //“test”的消息会通过ConsumerRecord封装后传输过来
        System.out.println(record.value());
    }


}