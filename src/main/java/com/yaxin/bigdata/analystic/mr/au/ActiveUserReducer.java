package com.yaxin.bigdata.analystic.mr.au;

import com.yaxin.bigdata.Util.TimeUtil;
import com.yaxin.bigdata.analystic.model.StatsUserDimension;
import com.yaxin.bigdata.analystic.model.value.map.TimeOutputValue;
import com.yaxin.bigdata.analystic.model.value.reduce.OutputWritable;
import com.yaxin.bigdata.common.DateEnum;
import com.yaxin.bigdata.common.KpiType;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;


public class ActiveUserReducer extends Reducer<StatsUserDimension, TimeOutputValue, StatsUserDimension, OutputWritable> {
    private static final Logger logger = Logger.getLogger(ActiveUserReducer.class);
    private OutputWritable v = new OutputWritable();
    private Set<String> unique = new HashSet<>();//用于去重，利用hashse
    private MapWritable map = new MapWritable();
    // 小时的统计
    private Map<Integer, Set<String>> hourlyMap = new HashMap<Integer, Set<String>>();
    private MapWritable hourlyWritable = new MapWritable();

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        //初始化按小时的容器
        for (int i = 0; i < 24; i++) {
            this.hourlyMap.put(i, new HashSet<String>());
            this.hourlyWritable.put(new IntWritable(i), new IntWritable(0));
        }
    }

    @Override
    protected void reduce(StatsUserDimension key, Iterable<TimeOutputValue> values, Context context) throws IOException, InterruptedException {
        KpiType kpi = KpiType.valueOfKpiName(key.getStatsCommonDimension().getKpiDimension().getKpiName());
        if (kpi.equals(KpiType.HOURLY_ACTIVE_USER)) {
            try {
                //循环
                for (TimeOutputValue tv : values) {
                    int hour = TimeUtil.getDateInfo(tv.getTime(), DateEnum.HOUR);
                    this.hourlyMap.get(hour).add(tv.getId());//循环将uuid添加在set中
                }
                //构建输出
                this.v.setKpi(KpiType.HOURLY_ACTIVE_USER);
                //循环
                for (Map.Entry<Integer, Set<String>> en : this.hourlyMap.entrySet()) {
                    this.hourlyWritable.put(new IntWritable(en.getKey()), new IntWritable(en.getValue().size()));
                }
                this.v.setValue(hourlyWritable);
//                System.out.println(hourlyWritable.keySet());
//                Set set =hourlyWritable.keySet();
//                Iterator iterator = set.iterator();
//                while (iterator.hasNext()){
//                    Object next = iterator.next();
//                    System.out.println(hourlyWritable.get(next));
//                }
                context.write(key, this.v);
            } finally {
                this.hourlyWritable.clear();
                this.hourlyMap.clear();
                for (int i = 0; i < 24; i++) {
                    this.hourlyMap.put(i, new HashSet<String>());
                    this.hourlyWritable.put(new IntWritable(i), new IntWritable(0));
                }
            }
        } else {
            map.clear();//清空map，因为map是在外面定义的，每一个key都需要调用一次reduce方法，也就是说上次操作会保留map中的key-value
            for (TimeOutputValue tv : values) {
                this.unique.add(tv.getId());//将uuid取出添加到set中进行去重
            }
            //构造输出value
            //根据kpi别名获取kpi类型（比较灵活） ------第一种方法
            this.v.setKpi(KpiType.valueOfKpiName(key.getStatsCommonDimension().getKpiDimension().getKpiName()));
            //这样写比较死，对于每一个kpi都需要进行判断
//        if(key.getStatsCommonDimension().getKpiDimension().getKpiName().equals(KpiType.NEW_USER.kpiName))
//        {
//            this.v.setKpi(KpiType.ACTIVE_USER);
//        }else if(key.getStatsCommonDimension().getKpiDimension().getKpiName().equals(KpiType.BROWSER_NEW_USER.kpiName))
//        {
//            this.v.setKpi(KpiType.BROWSER_ACTIVE_USER);
//        }
            //通过集合的size统计新增用户UUID的个数，前面的key可以随便设置，就是用来标识新增用户个数的（比较难理解）
            this.map.put(new IntWritable(-1), new IntWritable(this.unique.size()));
            this.v.setValue(this.map);
            //输出
            context.write(key, this.v);
            this.unique.clear();//清空操作
        }


        /**
         * 注意点：
         * 如果只是输出到文件系统中，则不需要kpi，不需要声明集合map
         * value只需要uuid的个数，这就不要封装对象了
         */

    }
}
