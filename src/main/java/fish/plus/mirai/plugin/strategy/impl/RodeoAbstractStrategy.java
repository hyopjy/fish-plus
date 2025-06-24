package fish.plus.mirai.plugin.strategy.impl;


import cn.hutool.cron.CronUtil;
import fish.plus.mirai.plugin.JavaPluginMain;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import fish.plus.mirai.plugin.util.Log;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;

import java.util.Objects;

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


}
