package fish.plus.mirai.plugin.strategy.impl;


import com.alibaba.fastjson2.JSONObject;
import fish.plus.mirai.plugin.JavaPluginMain;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.mqtt.MqttClientStart;
import fish.plus.mirai.plugin.obj.dto.MessageContentUserWinEventDTO;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import fish.plus.mirai.plugin.util.Log;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.EventKt;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public abstract class RodeoAbstractStrategy implements RodeoStrategy {

    public Group getBotGroup(Long groupId){
        Bot bot = JavaPluginMain.INSTANCE.getBotInstance();
        if(Objects.isNull(bot)){
            Log.info("RodeoAbstractStrategy bot 为空");
            return null ;
        }
        return bot.getGroup(groupId);
    }

    public static void publishPropEvent(Long groupId, List<Long> userIds, String propCode){
        CompletableFuture.runAsync(() -> {
            MessageContentUserWinEventDTO dto = new MessageContentUserWinEventDTO();
            dto.setMessageType("USER_WIN_EVENT");
            dto.setGroupId(groupId);
            dto.setUserIds(userIds);
            dto.setPropCode(propCode);
            MqttClientStart.getInstance().sendMessage("economy/" + groupId, JSONObject.toJSONString(dto));
        });

    }

    public void cancelPermissionAndDeleteCronTask(Rodeo rodeo){
        this.cancelPermission(rodeo);
        RodeoManager.removeCron(rodeo);
        rodeo.setRunning(0);
        rodeo.saveOrUpdate();
    }

}
