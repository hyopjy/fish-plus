package fish.plus.mirai.plugin.manager;
import cn.chahuyun.hibernateplus.HibernateFactory;
import fish.plus.mirai.plugin.JavaPluginMain;
import fish.plus.mirai.plugin.entity.rodeo.GroupInfo;
import fish.plus.mirai.plugin.entity.rodeo.GroupUser;
import fish.plus.mirai.plugin.mqtt.MqttClientStart;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class GroupManagerRunner implements Runnable {
    private volatile boolean running = true; // 使用volatile确保可见性
    private volatile boolean initialized = false; // 添加初始化标志
    private final List<Long> pendingSubscriptions = new ArrayList<>(); // 待订阅的主题列表

    public void loadGroup() {
        List<Long> initGroupList = new ArrayList<>(2);
        initGroupList.add(758085692L);
        initGroupList.add(835186488L);
        initGroupList.add(227265762L);
        initGroupList.forEach(g -> {
            Group group = JavaPluginMain.INSTANCE.getBotInstance().getGroup(g);
            if (Objects.nonNull(group)) {
                GroupInfo groupInfo = new GroupInfo();
                groupInfo.setGroupId(g);
                groupInfo.setGroupName(group.getName());
                groupInfo.saveOrUpdate();
                removeGroupUser(g);
                List<GroupUser> userList = new ArrayList<>();
                group.getMembers().stream().forEach(me -> {
                    GroupUser user = new GroupUser();
                    user.setUserNick(StringUtils.isBlank(me.getNameCard()) ? me.getNick() : me.getNameCard());
                    user.setUserId(me.getId());
                    user.setGroupId(g);
                    userList.add(user);
                });
                userList.forEach(GroupUser::saveOrUpdate);
                System.out.println("添加待订阅主题: topic/" + g);
            }
            // 将主题添加到待订阅列表，而不是直接订阅
            pendingSubscriptions.add(g);
        });
        initialized = true; // 标记已初始化
        
        // 设置MQTT重连成功回调
        MqttClientStart.getInstance().setOnReconnectSuccessCallback(() -> {
            System.out.println("MQTT重连成功，重新尝试订阅主题...");
            // 将已订阅的主题重新加入待订阅列表
            pendingSubscriptions.clear();
            initGroupList.forEach(g -> {
                System.out.println("----》" + g);
                pendingSubscriptions.add(g);
                System.out.println("重新添加待订阅主题: topic/" + g);
            });
        });
    }

    /**
     * 尝试订阅所有待订阅的主题
     */
    private void attemptSubscribeTopics() {
        if (pendingSubscriptions.isEmpty()) {
            return;
        }

        MqttClientStart mqttClient = MqttClientStart.getInstance();
        if (!mqttClient.isConnected()) {
            System.out.println("MQTT未连接，等待连接后订阅主题...");
            return;
        }
        
        // 检查连接质量
        if (!mqttClient.isConnectionHealthy()) {
            System.out.println("MQTT连接质量不佳，等待连接稳定后订阅主题...");
            return;
        }
        
        System.out.println("MQTT已连接且连接健康，开始订阅主题...");
        List<Long> subscribedGroups = new ArrayList<>();
        
        for (Long groupId : pendingSubscriptions) {
            subscribedGroups(groupId, mqttClient, subscribedGroups);
        }


        // 从待订阅列表中移除已订阅的主题
        pendingSubscriptions.removeAll(subscribedGroups);

        if (!pendingSubscriptions.isEmpty()) {
            System.out.println("还有 " + pendingSubscriptions.size() + " 个主题待订阅");
        } else {
            System.out.println("所有主题订阅完成");
            // 输出详细的订阅状态信息
            System.out.println(mqttClient.getSubscriptionStatus());
        }
    }

    private static void subscribedGroups(Long groupId, MqttClientStart mqttClient, List<Long> subscribedGroups) {
        try {
            System.out.println("待订阅的主题： " + groupId);
            String topic = "topic/" + groupId;
            mqttClient.subscribeTopic(topic);
            subscribedGroups.add(groupId);
            System.out.println("成功订阅主题: " + topic);

            // 验证订阅是否成功
            if (mqttClient.isTopicSubscribed(topic)) {
                System.out.println("验证订阅成功: " + topic);
            } else {
                System.err.println("订阅验证失败: " + topic);
            }

            // 订阅成功后立即发送一条测试消息验证订阅
//                mqttClient.publishMessage(topic, "GroupManagerRunner订阅测试消息");


        } catch (Exception e) {
            System.err.println("订阅主题失败: topic/" + groupId + ", 错误: " + e.getMessage());
        }
    }

    public void removeGroupUser(Long groupId) {
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        List<GroupUser> groupUsers = HibernateFactory.selectList(GroupUser.class, params);
        groupUsers.forEach(GroupUser::remove);
    }

    @Override
    public void run() {
        while (running) { // 仅依赖 running 标志
            Bot bot = JavaPluginMain.INSTANCE.getBotInstance();
            if (bot == null) {
                // 处理 bot 未就绪的情况 (如延迟重试)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    if (!running) {
                        break; // 如果是正常停止，退出循环
                    }
                    throw new RuntimeException(e);
                }
                continue;
            } else {
                // 加载群组信息（只执行一次）
                if (!initialized) {
                    loadGroup();
                }
                
                // 尝试订阅主题
                attemptSubscribeTopics();
                
                // 如果还有待订阅的主题，继续等待
                if (!pendingSubscriptions.isEmpty()) {
                    try {
                        Thread.sleep(2000); // 减少等待时间到2秒
                        continue;
                    } catch (InterruptedException e) {
                        if (!running) {
                            break;
                        }
                    }
                }
                
                // 初始化完成且所有主题订阅完成，线程可以退出
                if (initialized && pendingSubscriptions.isEmpty()) {
                    System.out.println("GroupManagerRunner初始化完成，所有主题订阅成功");
                    break;
                }
            }
        }
    }

    // 提供停止方法
    public void stop() {
        running = false;
        Thread.currentThread().interrupt(); // 中断线程
    }

 }