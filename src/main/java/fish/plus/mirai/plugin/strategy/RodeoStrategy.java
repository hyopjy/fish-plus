package fish.plus.mirai.plugin.strategy;


import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
import net.mamoe.mirai.event.events.MessageEvent;

public interface RodeoStrategy {

    /**
     * 开始
     */
    public void startGame(Rodeo rodeo);

    /**
     * 记录
     */
    public void record(Rodeo rodeo, RodeoRecordGameInfoDto dto);

    /**
     * 结算
     */
    public void endGame(Rodeo rodeo);

    Rodeo checkOrderAndGetRodeo(MessageEvent event, String[] messageArr);

    /**
     * 解析消息
     *
     * @param message
     * @return
     */
    RodeoRecordGameInfoDto analyzeMessage(String message);
}
