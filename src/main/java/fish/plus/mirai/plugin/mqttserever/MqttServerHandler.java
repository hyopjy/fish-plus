//package fish.plus.handler;
//
//import fish.plus.utils.MqttMessageSender;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandler;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.SimpleChannelInboundHandler;
//import io.netty.handler.codec.mqtt.*;
//import lombok.extern.slf4j.Slf4j;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Set;
//
//@Slf4j
//@ChannelHandler.Sharable
//public class MqttServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
//
//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        log.info("客户端连接建立: {}", ctx.channel().remoteAddress());
//        super.channelActive(ctx);
//    }
//
//    @Override
//    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        log.info("客户端连接断开: {}", ctx.channel().remoteAddress());
//        // 清理该客户端的所有订阅
//        MqttMessageSender.removeAllSubscriptions(ctx);
//        super.channelInactive(ctx);
//    }
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        log.error("MQTT服务端异常: {}", cause.getMessage(), cause);
//        ctx.close();
//    }
//
//    @Override
//    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
//        if (msg instanceof MqttConnectMessage) {
//            handleConnect(ctx, (MqttConnectMessage) msg);
//            log.info("Connected");
//        } else if (msg instanceof MqttPublishMessage) {
//            handlePublish(ctx, (MqttPublishMessage) msg);
//            log.info("Published");
//        } else if (msg instanceof MqttSubscribeMessage) {
//            handleSubscribe(ctx, (MqttSubscribeMessage) msg);
//            log.info("Subscribe");
//        } else if (msg instanceof MqttUnsubscribeMessage) {
//            handleUnsubscribe(ctx, (MqttUnsubscribeMessage) msg);
//            log.info("Unsubscribe");
//        } else if (msg.fixedHeader().messageType() == MqttMessageType.PINGREQ) {
//            handlePingReq(ctx);
//            log.debug("PingReq received");
//        } else if (msg.fixedHeader().messageType() == MqttMessageType.DISCONNECT) {
//            handleDisconnect(ctx);
//            log.info("Disconnect");
//        }
//    }
//
//    private void handleConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
//        MqttFixedHeader header = new MqttFixedHeader(
//                MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
//        MqttConnAckVariableHeader ackHeader = new MqttConnAckVariableHeader(
//                MqttConnectReturnCode.CONNECTION_ACCEPTED, true);
//        ctx.writeAndFlush(new MqttConnAckMessage(header, ackHeader));
//    }
//
//    private void handlePublish(ChannelHandlerContext ctx, MqttPublishMessage msg) {
//        String topic = msg.variableHeader().topicName();
//        ByteBuf payload = msg.payload();
//        log.info("收到主题[{}]数据: {}", topic, payload.toString(StandardCharsets.UTF_8));
//
//        // 广播消息给所有订阅了该主题的客户端
//        broadcastMessage(topic, payload);
//    }
//
//    private void handleSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
//        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
//
//        // 使用 from 方法创建 MqttMessageIdVariableHeader 实例
//        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(msg.variableHeader().messageId());
//
//        MqttSubAckMessage subAckMessage = new MqttSubAckMessage(fixedHeader, variableHeader, new MqttSubAckPayload(new int[]{MqttQoS.AT_MOST_ONCE.value()}));
//        ctx.writeAndFlush(subAckMessage);
//
//        // 记录订阅关系
//        for (MqttTopicSubscription subscription : msg.payload().topicSubscriptions()) {
//            String topic = subscription.topicName();
//            MqttMessageSender.addSubscriber(topic, ctx);
//            log.info("客户端已订阅主题: {}", topic);
//        }
//    }
//
//    private void handleUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) {
//        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
//
//        // 使用 from 方法创建 MqttMessageIdVariableHeader 实例
//        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(msg.variableHeader().messageId());
//
//        MqttUnsubAckMessage unsubAckMessage = new MqttUnsubAckMessage(fixedHeader, variableHeader);
//        ctx.writeAndFlush(unsubAckMessage);
//
//        // 移除订阅关系
//        for (String topic : msg.payload().topics()) {
//            MqttMessageSender.removeSubscriber(topic, ctx);
//        }
//    }
//
//    private void broadcastMessage(String topic, ByteBuf payload) {
//        Set<ChannelHandlerContext> subscribers = MqttMessageSender.getTopic(topic);
//        if (subscribers != null) {
//            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, payload.readableBytes());
//            MqttPublishMessage publishMessage = new MqttPublishMessage(fixedHeader, new MqttPublishVariableHeader(topic, 0), payload.copy());
//
//            for (ChannelHandlerContext subscriberCtx : subscribers) {
//                subscriberCtx.writeAndFlush(publishMessage.retain());
//            }
//        }
//    }
//
//    private void handlePingReq(ChannelHandlerContext ctx) {
//        // 响应PINGREQ消息
//        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
//        MqttMessage pingResp = new MqttMessage(fixedHeader);
//        ctx.writeAndFlush(pingResp);
//        log.debug("PingResp sent");
//    }
//
//    private void handleDisconnect(ChannelHandlerContext ctx) {
//        // 处理断开连接
//        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
//        ctx.close();
//    }
//}