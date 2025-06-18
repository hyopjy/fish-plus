//package fish.plus.utils;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.handler.codec.mqtt.*;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArraySet;
//
///**
// * MQTT服务端发送消息工具类
// */
//@Slf4j
//public class MqttMessageSender {
//
//    // 存储订阅者与主题的关系，支持一个主题有多个订阅者
//    private static final Map<String, Set<ChannelHandlerContext>> topicSubscribers = new ConcurrentHashMap<>();
//
//    /**
//     * 向指定主题发送消息
//     *
//     * @param topic   主题
//     * @param payload 消息内容
//     * @param qos     消息的服务质量级别
//     */
//    public static void sendMessage(String topic, String payload, MqttQoS qos) {
//        if (topicSubscribers.containsKey(topic)) {
//            Set<ChannelHandlerContext> contexts = topicSubscribers.get(topic);
//            MqttFixedHeader fixedHeader = new MqttFixedHeader(
//                    MqttMessageType.PUBLISH,
//                    false,
//                    qos, // 支持灵活的 QoS 设置
//                    false,
//                    0);
//
//            MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, 0);
//            ByteBuf byteBuf = null;
//
//            try {
//                byteBuf = contexts.iterator().next().alloc().buffer(); // 使用任意一个上下文分配缓冲区
//                byteBuf.writeBytes(payload.getBytes());
//                MqttPublishMessage publishMessage = new MqttPublishMessage(fixedHeader, variableHeader, byteBuf);
//
//                for (ChannelHandlerContext ctx : contexts) {
//                    ctx.writeAndFlush(publishMessage.copy()); // 每个上下文发送一份消息副本
//                }
//
//                log.info("MQTT消息已发送到主题: {}, 内容: {}, QoS: {}", topic, payload, qos.value());
//            } finally {
//                if (byteBuf != null) {
//                    byteBuf.release(); // 释放缓冲区资源
//                }
//            }
//        } else {
//            log.warn("没有客户端订阅主题: {}", topic);
//        }
//    }
//
//    /**
//     * 添加订阅关系
//     *
//     * @param topic   主题
//     * @param context 客户端上下文
//     */
//    public static void addSubscriber(String topic, ChannelHandlerContext context) {
//        topicSubscribers.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>()).add(context); // 确保线程安全
//        log.info("客户端已订阅主题: {}", topic);
//    }
//
//    /**
//     * 移除订阅关系
//     *
//     * @param topic   主题
//     * @param context 客户端上下文
//     */
//    public static void removeSubscriber(String topic, ChannelHandlerContext context) {
//        if (topicSubscribers.containsKey(topic)) {
//            Set<ChannelHandlerContext> contexts = topicSubscribers.get(topic);
//            contexts.remove(context); // 移除指定上下文
//            if (contexts.isEmpty()) {
//                topicSubscribers.remove(topic); // 如果该主题没有订阅者，则移除主题
//            }
//            log.info("客户端已取消订阅主题: {}", topic);
//        }
//    }
//
//    public static Set<ChannelHandlerContext> getTopic(String topic) {
//        return topicSubscribers.get(topic);
//    }
//
//
//    /**
//     * 主动向订阅指定主题的客户端发送消息
//     *
//     * @param topic   主题名称
//     * @param message 消息内容
//     */
//    public static void sendMessageToSubscribers(String topic, String message) {
//        MqttMessageSender.sendMessage(topic, message, MqttQoS.AT_MOST_ONCE);
//    }
//
//    /**
//     * 主动向订阅指定主题的客户端发送消息（支持自定义QoS）
//     *
//     * @param topic   主题名称
//     * @param message 消息内容
//     * @param qos     服务质量级别
//     */
//    public static void sendMessageToSubscribers(String topic, String message, MqttQoS qos) {
//        MqttMessageSender.sendMessage(topic, message, qos);
//    }
//
//    /**
//     * 获取指定主题的订阅者数量
//     *
//     * @param topic 主题名称
//     * @return 订阅者数量
//     */
//    public static int getSubscriberCount(String topic) {
//        Set<ChannelHandlerContext> subscribers = MqttMessageSender.getTopic(topic);
//        return subscribers != null ? subscribers.size() : 0;
//    }
//
//    /**
//     * 检查指定主题是否有订阅者
//     *
//     * @param topic 主题名称
//     * @return 是否有订阅者
//     */
//    public static boolean hasSubscribers(String topic) {
//        return getSubscriberCount(topic) > 0;
//    }
//
//    /**
//     * 清理指定客户端的所有订阅关系
//     *
//     * @param context 客户端上下文
//     */
//    public static void removeAllSubscriptions(ChannelHandlerContext context) {
//        topicSubscribers.entrySet().removeIf(entry -> {
//            Set<ChannelHandlerContext> contexts = entry.getValue();
//            boolean removed = contexts.remove(context);
//            if (contexts.isEmpty()) {
//                log.info("主题 {} 的所有订阅者已断开，移除主题", entry.getKey());
//            }
//            return removed && contexts.isEmpty();
//        });
//        log.info("已清理客户端的所有订阅关系: {}", context.channel().remoteAddress());
//    }
//
//    /**
//     * 获取所有订阅主题的统计信息
//     *
//     * @return 订阅统计信息
//     */
//    public static String getSubscriptionStats() {
//        StringBuilder stats = new StringBuilder();
//        stats.append("MQTT订阅统计:\n");
//        stats.append("- 总主题数: ").append(topicSubscribers.size()).append("\n");
//
//        for (Map.Entry<String, Set<ChannelHandlerContext>> entry : topicSubscribers.entrySet()) {
//            stats.append("- 主题: ").append(entry.getKey())
//                 .append(", 订阅者数: ").append(entry.getValue().size()).append("\n");
//        }
//
//        return stats.toString();
//    }
//}