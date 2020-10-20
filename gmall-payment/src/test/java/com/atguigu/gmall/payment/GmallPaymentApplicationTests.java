package com.atguigu.gmall.payment;

import lombok.SneakyThrows;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

    @Test
    public void producer() throws JMSException {
        //创建连接工厂
        //获取连接
        //打开连接
        //获取session
        //创建queue
        //创建生产者
        //创建消息
        //发送消息
        //关闭连接
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.10.128:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue queue = session.createQueue("XULINGYUN-Transaction");
        MessageProducer producer = session.createProducer(queue);
        ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
        textMessage.setText("hello world！！");
        producer.send(textMessage);
        session.commit();

        producer.close();
        session.close();
        connection.close();
    }

    @Test
    public void consumer() throws JMSException {
        //创建连接工厂
        //获取连接
        //打开连接
        //获取session
        //创建queue
        //创建消费者
        //获取消息
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.10.128:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue queue = session.createQueue("XULINGYUN-Transaction");
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(new MessageListener() {
            @SneakyThrows
            @Override
            public void onMessage(Message message) {
                if (message instanceof TextMessage){
                    String text = ((TextMessage) message).getText();
                    System.out.println("text = " + text);
                }
            }
        });

    }
}
