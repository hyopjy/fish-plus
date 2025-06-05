package fish.plus.mirai.plugin.manager;


import cn.hutool.cron.task.Task;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.strategy.RodeoFactory;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;

import java.util.Objects;

public class RodeoEndTask implements Task {

    private String cronKey;

    private Rodeo rodeo;

    public RodeoEndTask(String cronKey, Rodeo rodeo) {
        this.cronKey = cronKey;
        this.rodeo = rodeo;
    }

    @Override
    public void execute() {
        // 获取比赛结果
        RodeoManager.CURRENT_SPORTS.remove(cronKey, rodeo);
        RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(rodeo.getPlayingMethod());
        if(Objects.isNull(strategy)){
            return;
        }
        // todo 取消权限
        strategy.endGame(rodeo);
    }
}
