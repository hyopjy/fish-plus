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

    public void loadGroup() {
        List<Long> initGroupList = new ArrayList<>(2);
        initGroupList.add(835186488L);
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

                // 订阅主题
                MqttClientStart.getInstance().subscribeTopic("topic/"+ g);
            }
        });
        initialized = true; // 标记已初始化
        // 不要在这里调用stop()，让线程自然结束
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
                loadGroup();
                // 初始化完成后，线程可以退出
                if (initialized) {
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