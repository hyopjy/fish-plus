package fish.plus.mirai.plugin.strategy;


import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;


public interface RodeoStrategy {

    Double DEFAULT_PENALTY = -99999.00;

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
     *  结束播报
     *  取消权限
     *  删除定时
     *  删除游戏
     */
    public void endGame(Rodeo rodeo);

    /**
     *  取消权限
     *  删除定时
     *  删除游戏
     */
    public void cancelGame(Rodeo rodeo);

//     取消授权
//     删除定时
    public void cancelPermissionAndDeleteCronTask(Rodeo rodeo);

    void grantPermission(Rodeo rodeo);

    void cancelPermission(Rodeo rodeo);

}
