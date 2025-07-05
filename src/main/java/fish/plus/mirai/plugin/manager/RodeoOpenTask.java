package fish.plus.mirai.plugin.manager;


import cn.hutool.cron.task.Task;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.strategy.RodeoFactory;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;

import java.util.Objects;

public class RodeoOpenTask implements Task {


    private Rodeo rodeo;

    public RodeoOpenTask(Rodeo rodeo) {
        this.rodeo = rodeo;
    }
    @Override
    public void execute() {
        RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(rodeo.getPlayingMethod());
        if(Objects.isNull(strategy)){
            return;
        }
        //
        strategy.grantPermission(rodeo);
        strategy.startGame(rodeo);
    }
}
