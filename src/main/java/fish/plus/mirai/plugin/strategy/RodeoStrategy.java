package fish.plus.mirai.plugin.strategy;


import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;

public interface RodeoStrategy {

    Double DEFAULT_PENALTY = -99.00;

    int DEFAULT_SCORE = -99;

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

    public void cancelGame(Rodeo rodeo);


    /**
     * 解析消息
     *
     * @param message
     * @return
     */
    RodeoRecordGameInfoDto analyzeMessage(String message);

    void grantPermission(Rodeo rodeo);

    void cancelPermission(Rodeo rodeo);

    void removeEndTask(Rodeo redeo);
}
