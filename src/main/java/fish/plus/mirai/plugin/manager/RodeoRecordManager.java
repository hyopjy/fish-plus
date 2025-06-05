package fish.plus.mirai.plugin.manager;


import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.collection.CollectionUtil;
import fish.plus.mirai.plugin.entity.rodeo.RodeoRecord;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;


import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RodeoRecordManager {
    public static List<RodeoRecord> getRecordsByRodeoId(Long rodeoId) {
        Map<String, Object> params = new HashMap<>();
        params.put("rodeoId", rodeoId);
        return HibernateFactory.selectList(RodeoRecord.class, params);

    }

    public static int getMaxTurnsByRodeoId(Long rodeoId) {
        List<RodeoRecord> records = RodeoRecordManager.getRecordsByRodeoId(rodeoId);
        if (CollectionUtil.isEmpty(records)){
            return 0;
        }

        // 从记录中提取局数并找出最大局数
       return records.stream()
                .map(RodeoRecord::getTurns)
                .max(Comparator.naturalOrder()).orElse(0);
    }

    public static List<RodeoRecord> getRodeoRecordByRodeoIds(List<Long> rodeoIds) {

//        Map<String, Object> params = new HashMap<>();
//        params.put("rodeoId", rodeoIds);
//        return HibernateFactory.selectList(RodeoRecord.class, params);

        return HibernateFactory.getSession().fromSession(session -> {
            HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
            JpaCriteriaQuery<RodeoRecord> query = builder.createQuery(RodeoRecord.class);
            JpaRoot<RodeoRecord> from = query.from(RodeoRecord.class);
            query.where(builder.in(from.get("rodeoId"), rodeoIds));
            query.select(from);
            return session.createQuery(query).list();
        });
    }
}
