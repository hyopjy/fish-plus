package fish.plus.mirai.plugin.strategy.impl;


import cn.hutool.cron.CronUtil;
import fish.plus.mirai.plugin.JavaPluginMain;
import fish.plus.mirai.plugin.commonEvent.UserWinEvent;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
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

    public void removeEndTask(Rodeo rodeo){
        String endCronKey = rodeo.getGroupId() + Constant.SPILT + rodeo.getDay() + Constant.SPILT + rodeo.getEndTime();
        CronUtil.remove(endCronKey);
    }

    public void publishPropEvent(Long groupId, List<Long> userIds, String propCode){
        CompletableFuture.runAsync(() -> {
            UserWinEvent event = new UserWinEvent("GIVE_PROP", groupId, userIds, propCode);
            String finalAction = EventKt.broadcast(event).getAction();
            System.out.println("action = " + finalAction);
        });

    }

}
