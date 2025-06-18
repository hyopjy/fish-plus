//package fish.plus.config;
//
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.EventLoopGroup;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class NettyRunner implements CommandLineRunner {
//
//    @Autowired
//    private ServerBootstrap serverBootstrap;
//
//    @Autowired
//    @Qualifier("bossGroup")
//    private EventLoopGroup bossGroup;
//
//    @Autowired
//    @Qualifier("workerGroup")
//    private EventLoopGroup workerGroup;
//
//    @Value("${netty.port:1883}")
//    private int port;
//
//    @Override
//    public void run(String... args) throws Exception {
//        try {
//            ChannelFuture future = serverBootstrap.bind(port).sync();
//            future.addListener(f -> {
//                if(f.isSuccess()) {
//                    System.out.println("MQTT服务启动成功，端口：" + port);
//                }
//            });
//            future.channel().closeFuture().sync();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
//    }
//}
