package fish.plus.mirai.plugin.strategy;


import fish.plus.mirai.plugin.strategy.impl.RodeoDuelStrategy;
import fish.plus.mirai.plugin.strategy.impl.RodeoRouletteStrategy;
import fish.plus.mirai.plugin.strategy.impl.RodeoSuperSmashBrothersStrategy;

public class RodeoFactory {
    public static final String DUEL = "决斗";
    public static final String ROULETTE = "轮盘";
    public static final String SUPER_SMASH_BROTHERS = "大乱斗";

    public static RodeoStrategy createRodeoDuelStrategy(String playingMethod) {
        if (DUEL.equals(playingMethod)) {
            return RodeoDuelStrategy.getInstance();
        }
        if (ROULETTE.equals(playingMethod)) {
            return RodeoRouletteStrategy.getInstance();
        }
        if (SUPER_SMASH_BROTHERS.equals(playingMethod)) {
            return RodeoSuperSmashBrothersStrategy.getInstance();
        }
        return null;
    }
}
