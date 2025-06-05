package fish.plus.mirai.plugin.strategy.impl;


import fish.plus.mirai.plugin.JavaPluginMain;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.strategy.RodeoFactory;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import fish.plus.mirai.plugin.util.Log;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.MessageEvent;

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

    //    决斗
    // 决斗 groupId 场次名称 2024-08-23 15:18-14:38 934415751,952746839 5
    // 轮盘 groupId 场次名称 2024-08-23 15:18-14:38 934415751,952746839,123456,788522
    // 大乱斗 groupId 场次名称 2024-08-23 15:18-14:38 934415751,952746839,123456,788522
    @Override
    public Rodeo checkOrderAndGetRodeo(MessageEvent event, String[] messageArr) {
        Contact subject = event.getSubject();
        // 玩法（决斗、轮盘、大乱斗）
        String playingMethod= messageArr[0];
        if(RodeoFactory.DUEL.equals(playingMethod)){
            if(messageArr.length != 7 ){
                subject.sendMessage("决斗格式不正确" );
                return null;
            }
        }else {
            if(messageArr.length != 6 ){
                subject.sendMessage("轮盘、大乱斗格式不正确" );
                return null;
            }
        }
        Long groupId = Long.parseLong(messageArr[1]);
        String venue = messageArr[2];
        String day = messageArr[3];
        String[] timeArr = messageArr[4].split(Constant.SPILT);
        String startTime = timeArr[0] + ":00";
        String endTime = timeArr[1] + ":00";
        String players = messageArr[5];

        int round = 0;
        if(RodeoFactory.DUEL.equals(playingMethod)){
            round = Integer.parseInt(timeArr[6]);
        }
        Rodeo rodeo = new Rodeo(groupId, venue, day, startTime, endTime, players, round, playingMethod);
        // 时间是否有交叉
        if (!RodeoManager.checkDateAndTime(rodeo.getDay(), rodeo.getStartTime(), rodeo.getEndTime())) {
            subject.sendMessage("时间段有交叉 请重新配置" );
            return null;
        }
        rodeo.saveOrUpdate();
        // 启动定时任务
        RodeoManager.runTask(rodeo);
        return rodeo;
    }

}
