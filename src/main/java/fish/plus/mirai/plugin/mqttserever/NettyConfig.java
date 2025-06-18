//package fish.plus.config;
//
//
//import fish.plus.handler.MqttServerHandler;
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.ChannelPipeline;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import io.netty.handler.codec.mqtt.MqttDecoder;
//import io.netty.handler.codec.mqtt.MqttEncoder;
//import io.netty.handler.timeout.IdleStateHandler;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.concurrent.TimeUnit;
//
//@Configuration
//public class NettyConfig {
//    // 主线程组（接收连接）
//    @Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
//    public EventLoopGroup bossGroup() {
//        // 1个线程足够处理连接请求
//        return new NioEventLoopGroup(1);
//    }
//
//    // 工作线程组（处理I/O）
//    @Bean(name = "workerGroup", destroyMethod = "shutdownGracefully")
//    public EventLoopGroup workerGroup() {
//        // 默认线程数 = CPU核心数 * 2
//        return new NioEventLoopGroup();
//    }
//
//    // 协议初始化器
//    @Bean
//    public ChannelInitializer<SocketChannel> channelInitializer() {
//        return new ChannelInitializer<>() {
//            @Override
//            protected void initChannel(SocketChannel ch) {
//                ChannelPipeline pipeline = ch.pipeline();
//                pipeline.addLast("decoder", new MqttDecoder(1024 * 1024)); // 设置最大消息长度
//                pipeline.addLast("encoder", MqttEncoder.INSTANCE);
//                pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS)); // 心跳检测：60秒读空闲超时
//                pipeline.addLast(new MqttServerHandler());
//
//            }
//        };
//    }
//
//    // 服务端引导配置
//    @Bean
//    public ServerBootstrap serverBootstrap(
//            @Qualifier("bossGroup") EventLoopGroup bossGroup,
//            @Qualifier("workerGroup") EventLoopGroup workerGroup) {
//
//        return new ServerBootstrap()
//                .group(bossGroup, workerGroup)
//                .channel(NioServerSocketChannel.class)
//                .option(ChannelOption.SO_BACKLOG, 1024) // 连接队列大小[8][9]
//                .childOption(ChannelOption.TCP_NODELAY, true) // 禁用Nagle算法[6]
//                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保持长连接[9]
//                .childHandler(channelInitializer());
//
//    }
//
//}