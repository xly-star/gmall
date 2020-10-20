package com.atguigu.gmall.gmallorderservice.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.bean.enums.PaymentWay;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.gmallorderservice.mapper.OrderDetailMapper;
import com.atguigu.gmall.gmallorderservice.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

/**
 * @author xulingyun
 * @create 2020-10-16 10:50
 */
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Transactional
    @Override
    public String saveOrder(OrderInfo orderInfo) {
        orderInfo.setPaymentWay(PaymentWay.ONLINE);
        //设置一天过期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        // 生成第三方支付编号
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);

        orderInfo.setCreateTime(new Date());

        orderInfoMapper.insertSelective(orderInfo);
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }

        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString();
        Jedis jedis = redisUtil.getJedis();
        String tradeKey = "user:" + userId + ":tradeNo";
        jedis.setex(tradeKey, 10 * 60, tradeNo);
        jedis.close();
        return tradeNo;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeKey = "user:" + userId + ":tradeNo";
        String tradeNo = jedis.get(tradeKey);
        jedis.close();
        return tradeCodeNo.equals(tradeNo);
    }

    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeKey = "user:" + userId + ":tradeNo";
        jedis.del(tradeKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(paid);
        orderInfo.setOrderStatus(paid.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        try {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setId(orderId);
            Map<String, Object> map = initWareOrder(orderInfo);
            connection = activeMQUtil.getConnection();
            connection.start();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");
            producer = session.createProducer(queue);
            ActiveMQTextMessage message = new ActiveMQTextMessage();
            message.setText(JSON.toJSONString(map));
            producer.send(message);
            session.commit();
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                producer.close();
                session.close();
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        Example example = new Example(OrderInfo.class);
        //订单是未支付且过期时间小于当前时间，就是过期订单
        example.createCriteria().andEqualTo("processStatus", ProcessStatus.UNPAID)
                .andLessThan("expireTime", new Date());
        List<OrderInfo> orderInfoList = orderInfoMapper.selectByExample(example);
        return orderInfoList;
    }

    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        //过期订单状态改为已关闭，支付信息中状态也改为关闭
        updateOrderStatus(orderInfo.getId(), ProcessStatus.CLOSED);

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        paymentService.updatePaymentInfo(paymentInfo, orderInfo.getId());
    }

    @Override
    public Map<String, Object> initWareOrder(OrderInfo orderInfo1) {
        HashMap<String, Object> map = new HashMap<>();
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderInfo1.getId());
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderInfo1.getId());
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);

        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", "买卖手机");
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo1.getWareId());//仓库id
        ArrayList<Map<String, Object>> maps = new ArrayList<>();
        for (OrderDetail detail : orderDetailList) {
            HashMap<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId", detail.getSkuId());
            detailMap.put("skuName", detail.getSkuName());
            detailMap.put("skuNum", detail.getSkuNum());
            maps.add(detailMap);
        }
        map.put("details", maps);

        return map;
    }

    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        // 1 先查询原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        //序列化
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        //创建一个子订单集合
        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        //迭代maps [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        for (Map map : maps) {
            String wareId = (String) map.get("wareId");
            List<String> skuIdList = (List<String>) map.get("skuIds");

            OrderInfo subOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderInfoOrigin.getId());
            subOrderInfo.setWareId(wareId);

            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            List<OrderDetail> subOrderDetailList = new ArrayList<>();
            for (String skuId : skuIdList) {
                for (OrderDetail orderDetail : orderDetailList) {
                    if (skuId.equals(orderDetail.getSkuId())) {
                        orderDetail.setId(null);
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }
            subOrderInfo.setOrderDetailList(subOrderDetailList);
            subOrderInfo.sumTotalAmount();
            saveOrder(subOrderInfo);
            subOrderInfoList.add(subOrderInfo);
        }
        updateOrderStatus(orderId, ProcessStatus.SPLIT);

        return subOrderInfoList;
    }


}
