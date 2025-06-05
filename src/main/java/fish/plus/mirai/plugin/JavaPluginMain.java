package fish.plus.mirai.plugin;

import cn.hutool.core.collection.CollectionUtil;
import fish.plus.mirai.plugin.event.BotPostSendEventListener;
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
                .build());
    }

    @Override
    public void onDisable() {
        MqttClientStart.getInstance().closed();
        Log.info("插件已卸载!");
    }

    public Bot bot;

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
        // 初始化mqtt
//        MqttClientStart.getInstance();
        //初始化插件数据库
        HibernateUtil.init(this);
        RodeoManager.init();

        EventChannel<Event> eventChannel = GlobalEventChannel.INSTANCE.parentScope(this);
        eventChannel.registerListenerHost(new BotPostSendEventListener());
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

//        MqttClientStart mqttClientUtil = MqttClientStart.getInstance();
//        mqttClientUtil.subscribeTopic("test/topic");
//        mqttClientUtil.publishMessage("test/topic", "Hello MQTT!");
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
