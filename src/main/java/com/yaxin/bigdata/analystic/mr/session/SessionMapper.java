package com.yaxin.bigdata.analystic.mr.session;

import com.yaxin.bigdata.analystic.model.StatsCommonDimension;
import com.yaxin.bigdata.analystic.model.StatsUserDimension;
import com.yaxin.bigdata.analystic.model.base.BrowserDimension;
import com.yaxin.bigdata.analystic.model.base.DateDimension;
import com.yaxin.bigdata.analystic.model.base.KpiDimension;
import com.yaxin.bigdata.analystic.model.base.PlatformDimension;
import com.yaxin.bigdata.analystic.model.value.map.TimeOutputValue;
import com.yaxin.bigdata.common.DateEnum;
import com.yaxin.bigdata.common.KpiType;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;

import java.io.IOException;


public class SessionMapper extends Mapper<LongWritable, Text, StatsUserDimension, TimeOutputValue> {
    private static final Logger logger = Logger.getLogger(SessionMapper.class);
    StatsUserDimension k = new StatsUserDimension();
    TimeOutputValue v = new TimeOutputValue();
    private KpiDimension sessionKpi = new KpiDimension(KpiType.SESSION.kpiName);
    private KpiDimension browserSessionKpi = new KpiDimension(KpiType.BROWSER_SESSION.kpiName);

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String lines = value.toString();
        if (StringUtils.isEmpty(lines)) {
            return;
        }
        String[] fileds = lines.split("\u0001");
        String serverTime = fileds[1];
        String platform = fileds[13];
        String sessions = fileds[5];
        String browserName = fileds[24];
        String browserVersion = fileds[25];
        if (StringUtils.isEmpty(serverTime)) {
            logger.info("serverTime & sessions is null serverTime:" + serverTime + "sessions" + sessions);
            return;
        }

        Long stime = Long.valueOf(serverTime);

        PlatformDimension platformDimension = PlatformDimension.getInstance(platform);

        DateDimension dateDimension = DateDimension.buildDate(stime, DateEnum.DAY);

        StatsCommonDimension statsCommonDimension = this.k.getStatsCommonDimension();

        //为StatsCommonDimension设值
        statsCommonDimension.setDateDimension(dateDimension);
        statsCommonDimension.setPlatformDimension(platformDimension);
        //用户模块新增用户is.k.setStatsCommonDimension(statsCommonDimension);
        //////            context.write(this.k,this.v);//输出
        //设置默认的浏览器对象（新增用户不一定需要浏览器维度，设置为空）
        BrowserDimension defaultbrowserDimension = new BrowserDimension("", "");
        statsCommonDimension.setKpiDimension(sessionKpi);
        this.k.setBrowserDimension(defaultbrowserDimension);
        this.k.setStatsCommonDimension(statsCommonDimension);
        this.v.setId(sessions);
        this.v.setTime(stime);
        context.write(this.k, this.v);
        //浏览器模块新增用户
        statsCommonDimension.setKpiDimension(browserSessionKpi);
        BrowserDimension browserDimension = new BrowserDimension(browserName, browserVersion);
        this.k.setBrowserDimension(browserDimension);
        this.k.setStatsCommonDimension(statsCommonDimension);
        context.write(this.k, this.v);
    }
}
