package fish.plus.mirai.plugin.manager;


import cn.hutool.cron.task.Task;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.strategy.RodeoFactory;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;

import java.util.Objects;

public class RodeoOpenTask implements Task {

    private String cronKey;

    private Rodeo rodeo;

    public RodeoOpenTask(String cronKey, Rodeo rodeo) {
        this.cronKey = cronKey;
        this.rodeo = rodeo;
    }
    @Override
    public void execute() {
        RodeoManager.CURRENT_SPORTS.put(cronKey, rodeo);
        RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(rodeo.getPlayingMethod());
        if(Objects.isNull(strategy)){
            return;
        }
        // todo 授权
        strategy.startGame(rodeo);
    }
}
