package fish.plus.mirai.plugin;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.cron.CronUtil;
import fish.plus.mirai.plugin.event.BotPostSendEventListener;
import fish.plus.mirai.plugin.event.GroupEventListener;
import fish.plus.mirai.plugin.manager.GroupManagerRunner;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.mqtt.MqttClientStart;
import fish.plus.mirai.plugin.util.HibernateUtil;
import fish.plus.mirai.plugin.util.Log;
import kotlin.Lazy;
import kotlin.LazyKt;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.permission.*;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;

import java.util.List;


/**
 * 使用 Java 请把
 * {@code /src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin}
 * 文件内容改成 {@code org.example.mirai.plugin.JavaPluginMain} <br/>
 * 也就是当前主类全类名
 * <p>
 * 使用 Java 可以把 kotlin 源集删除且不会对项目有影响
 * <p>
 * 在 {@code settings.gradle.kts} 里改构建的插件名称、依赖库和插件版本
 * <p>
 * 在该示例下的 {@link JvmPluginDescription} 修改插件名称，id 和版本等
 * <p>
 * 可以使用 {@code src/test/kotlin/RunMirai.kt} 在 IDE 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */

public final class JavaPluginMain extends JavaPlugin {
    public static final JavaPluginMain INSTANCE = new JavaPluginMain();

    private JavaPluginMain() {
        super(new JvmPluginDescriptionBuilder("fish.plus.mirai-plugin", "0.1.0")
                .info("EG")
                .dependsOn("com.evolvedghost.MuteGames", false)
                .dependsOn("cn.chahuyun.HuYanSession", true)
                .build());
    }

    @Override
    public void onDisable() {
        // 停止所有线程和任务
        if (groupManagerThread != null && groupManagerThread.isAlive()) {
            if (groupManagerRunner != null) {
                groupManagerRunner.stop();
            }
            groupManagerThread.interrupt();
            try {
                groupManagerThread.join(5000); // 等待最多5秒
            } catch (InterruptedException e) {
                Log.info("等待GroupManagerRunner线程停止时被中断");
            }
        }
        
        // 停止MQTT客户端
        MqttClientStart.getInstance().closed();
        
        // 停止定时任务
        CronUtil.stop();
        
        Log.info("插件已卸载!");
    }

    public Bot bot;
    private Thread groupManagerThread; // 保存线程引用
    private GroupManagerRunner groupManagerRunner; // 保存runner引用

    public Bot getBotInstance() {
        if (bot == null) {
            List<Bot> botList = Bot.getInstances();
            if (CollectionUtil.isEmpty(botList)) {
                Log.info("getBotInstance 获取bot为空");
                return bot;
            }
            return botList.get(0);
        }
        return bot;
    }

    @Override
    public void onEnable() {
        getLogger().info("日志");
        CronUtil.start();
        //初始化插件数据库
        HibernateUtil.init(this);

        EventChannel<Event> eventChannel = GlobalEventChannel.INSTANCE.parentScope(this);
        eventChannel.registerListenerHost(new BotPostSendEventListener());
        eventChannel.registerListenerHost(new GroupEventListener());
//        eventChannel.subscribeAlways(GroupMessageEvent.class, g -> {
//            //监听群消息
//            getLogger().info(g.getMessage().contentToString());
//            MqttClientStart.getInstance().subscribeTopic("test/topic");
//            MqttClientStart.getInstance().publishMessage("test/topic", g.getMessage().contentToString());
//
//        });
//        eventChannel.subscribeAlways(FriendMessageEvent.class, f -> {
//            //监听好友消息
//            getLogger().info(f.getMessage().contentToString());
//        });

        myCustomPermission.getValue(); // 注册权限
//        GlobalEventChannel.INSTANCE.subscribeAlways(BotGroupPermissionChangeEvent.class, event -> {
//            System.out.println("权限变更：" + event.getPermissionId() + " 状态：" + event.getType());
//        });

//        MqttClientStart mqttClientUtil = MqttClientStart.getInstance();
//        mqttClientUtil.subscribeTopic("test/topic");
//        mqttClientUtil.publishMessage("test/topic", "Hello MQTT!");

        // 初始化mqtt
        MqttClientStart mqttClient = MqttClientStart.getInstance();
        
        // 记录MQTT初始化状态
        Log.info("MQTT客户端初始化完成，状态: " + mqttClient.getConnectionStatus());
        
        // 等待MQTT连接建立（减少等待时间）
        int waitCount = 0;
        while (!mqttClient.isConnected() && waitCount < 15) { // 减少到最多等待15秒
            try {
                Thread.sleep(1000);
                waitCount++;
                if (waitCount % 3 == 0) { // 每3秒记录一次状态
                    Log.info("等待MQTT连接... (" + waitCount + "/15秒) 状态: " + mqttClient.getConnectionStatus());
                }
            } catch (InterruptedException e) {
                Log.info("等待MQTT连接时被中断");
                break;
            }
        }
        
        if (mqttClient.isConnected()) {
            Log.info("MQTT连接成功建立！");
        } else {
            Log.info("MQTT连接超时，但继续启动其他组件");
        }

        RodeoManager.init(null);
        
        // 启动GroupManagerRunner线程并保存引用
        groupManagerRunner = new GroupManagerRunner();
        groupManagerThread = new Thread(groupManagerRunner);
        groupManagerThread.setDaemon(true); // 设置为守护线程
        groupManagerThread.start();
        
        // 启动MQTT状态监控线程
        startMqttStatusMonitor();
    }

    /**
     * 启动MQTT状态监控线程
     */
    private void startMqttStatusMonitor() {
        Thread monitorThread = new Thread(() -> {
            MqttClientStart mqttClient = MqttClientStart.getInstance();
            int lastReconnectAttempts = 0;
            boolean lastMaxAttemptsReached = false;
            
            while (true) {
                try {
                    Thread.sleep(30000); // 每30秒检查一次
                    
                    // 检查重连次数变化
                    int currentAttempts = mqttClient.getReconnectAttempts();
                    if (currentAttempts != lastReconnectAttempts) {
                        Log.info("MQTT重连次数变化: " + lastReconnectAttempts + " -> " + currentAttempts);
                        lastReconnectAttempts = currentAttempts;
                    }
                    
                    // 检查最大尝试次数状态变化
                    boolean currentMaxAttemptsReached = mqttClient.isMaxAttemptsReached();
                    if (currentMaxAttemptsReached != lastMaxAttemptsReached) {
                        if (currentMaxAttemptsReached) {
                            Log.info("MQTT已达到最大重连次数，将等待60秒后重新尝试");
                        } else {
                            Log.info("MQTT重置重连计数，重新开始连接尝试");
                        }
                        lastMaxAttemptsReached = currentMaxAttemptsReached;
                    }
                    
                    // 如果重连次数较多但未达到最大次数，记录警告
                    if (currentAttempts >= mqttClient.getMaxReconnectAttempts() * 0.6 && !currentMaxAttemptsReached) {
                        Log.info("MQTT重连次数较多: " + currentAttempts + "/" + mqttClient.getMaxReconnectAttempts());
                    }
                    
                    // 记录当前连接状态
                    if (!mqttClient.isConnected()) {
                        Log.info("MQTT当前状态: " + mqttClient.getConnectionStatus());
                    }
                    
                } catch (InterruptedException e) {
                    Log.info("MQTT状态监控线程被中断");
                    break;
                } catch (Exception e) {
                    Log.info("MQTT状态监控异常: " + e.getMessage());
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("MQTT-Status-Monitor");
        monitorThread.start();
    }

    // region mirai-console 权限系统示例
    public static final Lazy<Permission> myCustomPermission = LazyKt.lazy(() -> {  // Lazy: Lazy 是必须的, console 不允许提前访问权限系统
        // 注册一条权限节点 org.example.mirai-example:my-permission
        // 并以 org.example.mirai-example:* 为父节点


        // @param: parent: 父权限
        //                 在 Console 内置权限系统中, 如果某人拥有父权限
        //                 那么意味着此人也拥有该权限 (org.example.mirai-example:my-permission)
        // @func: PermissionIdNamespace.permissionId: 根据插件 id 确定一条权限 id
        try {
            return PermissionService.getInstance().register(
                    INSTANCE.permissionId("my-permission"),
                    "一条自定义权限",
                    INSTANCE.getParentPermission()
            );
        } catch (PermissionRegistryConflictException e) {
            throw new RuntimeException(e);
        }
    });

    public static boolean hasCustomPermission(User usr) {
        PermitteeId pid;
        if (usr instanceof Member) {
            pid = new AbstractPermitteeId.ExactMember(((Member) usr).getGroup().getId(), usr.getId());
        } else {
            pid = new AbstractPermitteeId.ExactUser(usr.getId());
        }
        return PermissionService.hasPermission(pid, myCustomPermission.getValue());
    }
    // endregion
}
