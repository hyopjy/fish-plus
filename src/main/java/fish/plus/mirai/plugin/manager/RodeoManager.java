package fish.plus.mirai.plugin.manager;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.cron.CronUtil;
import fish.plus.mirai.plugin.JavaPluginMain;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.entity.rodeo.RodeoRecord;
import fish.plus.mirai.plugin.strategy.RodeoFactory;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Group;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RodeoManager {
//
//
//    决斗
//1.分配决斗[双方][规定时间段]内的比赛[局数]（按局数给权限）
//            【
//    东风吹，战鼓擂，决斗场上怕过谁！
//    新的🏟[比赛场次名]已确定于[14:00-17:00]开战！
//            [@A ]与[@B ]正式展开决斗的巅峰对决！⚔[N]局比赛，谁将笑傲鱼塘🤺，谁又将菜然神伤🥬？
//            】
//            2.该场比赛结束后，统计双方的得分和总被禁言时长
//【
//        [比赛场次名]结束，恭喜胜者@B以[3:1]把对手@A鸡哔！🔫
//    @B共被禁言[秒]
//    @A共被禁言[秒]
//    菜！就！多！练！
//            】
//
//    轮盘
//1.分配轮盘[多方][时间段]（10分钟左右，手动配置）内的比赛（按时间段给权限）
//            【
//    东风吹，战鼓擂，轮盘赛上怕过谁！
//    新的🏟[比赛场次名]正式开战！比赛时长[10分钟]，参赛选手有：@A@B@C@D
//    轮盘比赛正式打响！🔫[10分钟]的比赛，谁将笑傲鱼塘🤺，谁又将菜然神伤🥬？
//            】
//            2.该场比赛结束后，统计多方的得分和总被禁言时长
//【
//        [比赛场次名]结束，得分表如下：
//    B-3
//    C-2
//    D-1
//    A-0
//    @A共被禁言[秒]
//    @B共被禁言[秒]
//    @C共被禁言[秒]
//    @D共被禁言[秒]
//            】
//
//    大乱斗（多人决斗，逻辑同轮盘）
//            1.分配决斗[多方][时间段]（10分钟左右，手动配置）内的比赛（按时间段给权限）
//            【
//    东风吹，战鼓擂，决斗场上怕过谁！
//    新的🏟[比赛场次名]正式开战！比赛时长[10分钟]，参赛选手有：@A@B@C@D
//    大乱斗比赛正式打响！🔫[10分钟]的比赛，谁将笑傲鱼塘🤺，谁又将菜然神伤🥬？
//            】
//            2.该场比赛结束后，统计多方的得分和总被禁言时长
//【
//        [比赛场次名]结束，得分表如下：
//    B-3
//    C-2
//    D-1
//    A-0
//    @A共被禁言[秒]
//    @B共被禁言[秒]
//    @C共被禁言[秒]
//    @D共被禁言[秒]】

    public static Rodeo getCurrent(long groupId, Set<Long> atUser) {
        // 获取当前日期和时间
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String todayStr = today.format(Constant.FORMATTER_YYYY_MM_DD);
        String nowStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        return HibernateFactory.getSession().fromSession(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Rodeo> query = builder.createQuery(Rodeo.class);
            Root<Rodeo> root = query.from(Rodeo.class);

            // 创建主要条件列表
            List<Predicate> predicates = new ArrayList<>();

            // 基本条件：匹配群组ID和当天日期
            predicates.add(builder.equal(root.get("groupId"), groupId));
            predicates.add(builder.equal(root.get("day"), todayStr));
            predicates.add(builder.equal(root.get("running"), 1)); // 运行状态为1
            predicates.add(builder.lessThanOrEqualTo(root.get("startTime"), nowStr));
            predicates.add(builder.greaterThanOrEqualTo(root.get("endTime"), nowStr));

            // 玩家匹配条件（利用存储格式优势）
            if (atUser != null && !atUser.isEmpty()) {
                Expression<String> playersExpr = root.get("players");

                for (Long userId : atUser) {
                    // 直接匹配格式化的用户ID字符串
                    String pattern = "%," + userId + ",%";
                    predicates.add(builder.like(playersExpr, pattern));
                }
            }

            // 组合所有条件
            query.where(builder.and(predicates.toArray(new Predicate[0])));

            // 按开始时间倒序排序（获取最近的比赛）
            query.orderBy(builder.desc(root.get("startTime")));

            // 执行查询，只获取第一个结果
            return session.createQuery(query)
                    .setMaxResults(1)
                    .uniqueResult();
        });
    }

    /**
     * 判断决斗的胜负是否已经分出
     *
     * @param rodeo
     */
    public static boolean isDuelOver(Rodeo rodeo) {
        if(!RodeoFactory.DUEL.equals(rodeo.getPlayingMethod())){
            return false;
        }

        Long id = rodeo.getId();
        List<RodeoRecord> records = RodeoRecordManager.getRecordsByRodeoId(id);
        if(CollectionUtil.isEmpty(records)){
            return false;
        }

        // 从记录中提取局数并找出最大局数
        Integer maxTurns = records.stream()
                .map(RodeoRecord::getTurns)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(0);
        // 决斗存入赢+输的场次
        if (maxTurns.equals(rodeo.getRound())) {
            return true;
        }

        // 如果打5局 ， 每一局都会有赢家和胜利者
        // 只要记录胜利次数为 5 / 2 + 1   = 2 说明已经胜利

        // 查询最大场次
        List<String> winnerPlayers = new ArrayList<String>();
        List<String> losePlayers = new ArrayList<String>();
        // 局数
        Map<Integer, List<RodeoRecord>> recordsByTurns = records.stream()
                .collect(Collectors.groupingBy(RodeoRecord::getTurns));
        recordsByTurns.forEach((turns, recordList) -> {
            Optional<RodeoRecord> winnerOptional = recordList.stream().filter(r-> Objects.isNull(r.getForbiddenSpeech()) || r.getForbiddenSpeech().equals(0)).findAny();
            winnerOptional.ifPresent(rodeoRecord -> winnerPlayers.add(rodeoRecord.getPlayer()));

            Optional<RodeoRecord> loseOptional = recordList.stream().filter(r->  r.getForbiddenSpeech() > 0).findAny();
            loseOptional.ifPresent(rodeoRecord -> losePlayers.add(rodeoRecord.getPlayer()));
        });
        // 3 0
        // 每一局赢的人
        Map<String, List<String>> winnerMap = winnerPlayers.stream()
                .collect(Collectors.groupingBy(s -> s));
        // 记录的是赢的次数
        int roundWinCount = (rodeo.getRound() / 2) + 1;
        return winnerMap.entrySet().stream()
                .anyMatch(entry -> entry.getValue().size() >= roundWinCount);
    }

    public static void init(){
        // 删除结束时间小于当前时间的数据
        removeExpRodeoList();
        // 启动有效的任务
        List<Rodeo> list = getRodeoList(1);
        list.forEach(rodeo->{
            RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(rodeo.getPlayingMethod());
            strategy.cancelPermissionAndDeleteCronTask(rodeo);
            RodeoManager.runTask(rodeo);
        });
    }

    public static void runRodeoId(Long id){
        // 启动有效的任务
        Rodeo rodeo = getRodeoById(id);
        if(Objects.isNull(rodeo)){
            return;
        }
        //
        RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(rodeo.getPlayingMethod());
        String endTime = rodeo.getDay() + " " +rodeo.getEndTime();
        // 17.05
        LocalDateTime end = LocalDateTime.parse(endTime, Constant.FORMATTER);
        if (end.isBefore(LocalDateTime.now())) {
            strategy.cancelGame(rodeo);
            return;
        }
        strategy.cancelPermissionAndDeleteCronTask(rodeo);
        runTask(rodeo);
    }


    public static void removeExpRodeoList() {
        LocalDateTime now = LocalDateTime.now();
        List<Rodeo> list = getRodeoList(null);
        List<Rodeo> expRodeo = list.stream().map(l -> {
            String endTime = l.getDay() + " " + l.getEndTime();
            // 17.05
            LocalDateTime end = LocalDateTime.parse(endTime, Constant.FORMATTER);
            if (end.isBefore(now)) {
                return l;
            }
            return null;
        }).filter(Objects::nonNull).toList();

        List<Long> rodeoIds = expRodeo.stream().map(Rodeo::getId).collect(Collectors.toList());
        List<RodeoRecord> records = RodeoRecordManager.getRodeoRecordByRodeoIds(rodeoIds);
        records.forEach(RodeoRecord::remove);

//           *  取消权限
//           *  删除定时
//           *  删除游戏
        expRodeo.forEach(rodeo->{
            RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(rodeo.getPlayingMethod());
            strategy.cancelGame(rodeo);
        });

    }

    public static void removeCron(Rodeo rodeo){
        String startCronKey = rodeo.getGroupId() + "_" + rodeo.getId() + Constant.SPILT +  rodeo.getDay() + Constant.SPILT + rodeo.getStartTime();
        String endCronKey = rodeo.getGroupId() + "_" + rodeo.getId() + Constant.SPILT +  rodeo.getDay() + Constant.SPILT + rodeo.getEndTime();
        CronUtil.remove(startCronKey);
        CronUtil.remove(endCronKey);
    }


    public static void removeEndRodeo(Rodeo rodeo) {
        List<Long> rodeoIds = Collections.singletonList(rodeo.getId());
        List<RodeoRecord> records = RodeoRecordManager.getRodeoRecordByRodeoIds(rodeoIds);

        records.forEach(RodeoRecord::remove);
        rodeo.remove();
    }


//    public static List<Rodeo> getRodeoList(Integer running){
//        return HibernateFactory.selectList(Rodeo.class);
//    }
    /**
     * 根据运行状态查询比赛列表
     * @param running 运行状态 (1=运行中, 0=未运行, null=查询所有)
     * @return 匹配的比赛列表
     */
    public static List<Rodeo> getRodeoList(Integer running) {
        return HibernateFactory.getSession().fromSession(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Rodeo> query = builder.createQuery(Rodeo.class);
            Root<Rodeo> root = query.from(Rodeo.class);

            // 添加条件（如果running不为null）
            if (running != null) {
                query.where(builder.equal(root.get("running"), running));
            }

            // 添加排序（按开始时间倒序）
            query.orderBy(
                    builder.desc(root.get("day")),
                    builder.desc(root.get("startTime"))
            );

            return session.createQuery(query).list();
        });
    }

    public static Rodeo getRodeoById(Long Id){
      return HibernateFactory.selectOne(Rodeo.class, Id);
    }

//    public static Optional<RodeoRecord> getRodeoById(Long id) {
//        return HibernateFactory.getSession().fromSession(session -> {
//            CriteriaBuilder builder = session.getCriteriaBuilder();
//            CriteriaQuery<RodeoRecord> query = builder.createQuery(RodeoRecord.class);
//            Root<RodeoRecord> root = query.from(RodeoRecord.class);
//
//            query.where(builder.equal(root.get("rodeoId"), id));
//
//            RodeoRecord result = session.createQuery(query)
//                    .setMaxResults(1)
//                    .uniqueResult();
//
//            return Optional.ofNullable(result);
//        });
//    }


    /**
     * 取消授权
     * 删除定时
     * @param rodeo
     */

//    public static void cancelPermissonAndDeletCronTask(Rodeo rodeo){
//        if(Objects.isNull(rodeo)){
//            return;
//        }
//        // 取消授权
//        // 删除定时
//        RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(rodeo.getPlayingMethod());
//        strategy.cancelPermission(rodeo);
//
//        removeCron(rodeo);
//        rodeo.setRunning(0);
//        rodeo.saveOrUpdate();
//    }


    public static void runTask(Rodeo rodeo) {
        if(Objects.isNull(rodeo)){
            return;
        }
        // String date = "2024-08-06";
        // String startTime = "12:00:00";
        // String endTime = "13:00:00";
        String startCronExpression = getCronByDateAndTime(rodeo.getDay(), rodeo.getStartTime());
        String endCronExpression = getCronByDateAndTime(rodeo.getDay(), rodeo.getEndTime());

        // 开始任务
        String startCronKey = rodeo.getGroupId() + "_" + rodeo.getId() + Constant.SPILT + rodeo.getDay() + Constant.SPILT + rodeo.getStartTime();
        CronUtil.remove(startCronKey);

        RodeoOpenTask startTask = new RodeoOpenTask(rodeo);
        CronUtil.schedule(startCronKey, startCronExpression, startTask);

        // 结束任务
        String endCronKey = rodeo.getGroupId() + "_"+ rodeo.getId() + Constant.SPILT + rodeo.getDay() + Constant.SPILT + rodeo.getEndTime();
        CronUtil.remove(endCronKey);
        RodeoEndTask endTask = new RodeoEndTask(rodeo);
        CronUtil.schedule(endCronKey, endCronExpression, endTask);

        LocalDateTime dateTime = LocalDateTime.parse(rodeo.getDay() + " " + rodeo.getStartTime(), Constant.FORMATTER);
        if(dateTime.isBefore(LocalDateTime.now())){
            dateTime = LocalDateTime.now().plusSeconds(60L);
        }
        String startTime = dateTime.format(Constant.FORMATTER);

        if(Objects.nonNull(JavaPluginMain.INSTANCE.getBotInstance())){
            Group group = JavaPluginMain.INSTANCE.getBotInstance().getGroup(rodeo.getGroupId());
            if(Objects.nonNull(group)){
                group.sendMessage(rodeo.getVenue()+"("+ rodeo.getPlayingMethod() +")将在[ "+startTime+" ]开始⚡️⚡️");
            }
        }
        rodeo.setRunning(1);
        rodeo.saveOrUpdate();
    }

    public static String getCronByDateAndTime(String date, String time) {
        LocalDateTime dateTime = LocalDateTime.parse(date + " " + time, Constant.FORMATTER);
        if(dateTime.isBefore(LocalDateTime.now())){
            long random = RandomUtil.randomLong(50, 61);
            dateTime = LocalDateTime.now().plusSeconds(random);
        }
        int seconds = dateTime.getSecond();
        int minutes = dateTime.getMinute();
        int hour = dateTime.getHour();
        int dayOfMonth = dateTime.getDayOfMonth();
        int month = dateTime.getMonthValue();

        Set<Integer> hourList = new TreeSet<>();
        Set<Integer> dayList = new TreeSet<>();
        Set<Integer> monthList = new TreeSet<>();

        hourList.add(hour);
        dayList.add(dayOfMonth);
        monthList.add(month);

        String hourStr = CollUtil.join(hourList, ",");
        String dayStr = CollUtil.join(dayList, ",");
        String monthStr = CollUtil.join(monthList, ",");

        // [秒] [分] [时] [日] [月] [周] [年]
        return seconds + " " + minutes + " " + hourStr + " " + dayStr + " " + monthStr + " ?";
    }


    /**
     * 根据日期查赛场
     * @param day
     * @return
     */
    public static List<Rodeo> getRodeoByDay(String day) {
        Map<String, Object> map = new HashMap<>();
        map.put("day", day);
        return HibernateFactory.selectList(Rodeo.class, map);
    }

    public static void stopGame(Long rodeoId) {
        Rodeo rodeo = getRodeoById(rodeoId);
        if(Objects.isNull(rodeo)){
            return;
        }
        RodeoStrategy strategy = RodeoFactory.createRodeoDuelStrategy(rodeo.getPlayingMethod());
        strategy.cancelGame(rodeo);
        if(Objects.nonNull(JavaPluginMain.INSTANCE.getBotInstance())){
            Group group = JavaPluginMain.INSTANCE.getBotInstance().getGroup(rodeo.getGroupId());
            if(Objects.nonNull(group)){
                group.sendMessage(rodeo.getVenue()+"("+ rodeo.getPlayingMethod() +") 结束 🎮🔚");
            }
        }

    }
}