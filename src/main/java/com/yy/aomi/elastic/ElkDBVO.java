package com.yy.aomi.elastic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href= "mailto:909074682@yy.com" style="color:##E0E;">zhangzhibin</a>
 * @version V1.0
 * @date 2017年4月13日下午6:27:33
 */
public class ElkDBVO {
    private static Logger logger = LoggerFactory.getLogger(ElkDBVO.class);

    public String index;
    public String type;
    public String timePattern;
    public static final String YYYYMMDD = "yyyy.MM.dd";
    public static final String YYYYMM = "yyyy.MM";
    public static final String YYYY = "yyyy";

    public ElkDBVO() {

    }

    public ElkDBVO(String index, String type, String timePattern) {
        super();
        this.index = index;
        this.type = type;
        this.timePattern = timePattern;
    }

    public String getTimeIndex(Date now) {
    	String timeIndex = index + DateUtils.formatUTC(now, timePattern);
        return timeIndex;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimePattern() {
        return timePattern;
    }

    public void setTimePattern(String timePattern) {
        this.timePattern = timePattern;
    }


    public String[] getIndexRand(Date start, Date end) {
        if (this.timePattern == null) {
            return new String[]{this.index};
        }
        Set<String> dbRand = new HashSet<>();
        Date temp = start;
        String startIndex = index + DateUtils.formatUTC(temp, timePattern);
        String endIndex = index + DateUtils.formatUTC(end, timePattern);
        dbRand.add(endIndex);
        while (temp.before(end)) {
            dbRand.add(startIndex);
            temp = getNextIndexsTime(temp, 1);
            startIndex = index + DateUtils.formatUTC(temp, timePattern);
        }
        String[] result = new String[dbRand.size()];
        dbRand.toArray(result);
        String indexs = "";
        for (String index : result) {
            indexs += index + ",";
        }
        logger.info("indexs={}", indexs);
        return result;
    }


    /**
     * 获n=1，表示下一个数据库的时间，以此类推
     *
     * @param now
     * @param n
     * @return
     */
    public Date getNextIndexsTime(Date now, int n) {
        Date temp = now;
        switch (timePattern) {
            case YYYYMMDD:
                temp = DateUtils.add(temp, Calendar.DAY_OF_MONTH, n);
                break;
            case YYYYMM:
                temp = DateUtils.add(temp, Calendar.MONTH, n);
                break;
            case YYYY:
                temp = DateUtils.add(temp, Calendar.YEAR, n);
                break;
            default:
                temp = DateUtils.add(temp, Calendar.DAY_OF_MONTH, n);
                break;
        }
        return temp;
    }

    /**
     * 获取指定时间后的数据库名，如db-2017.04，前后数据库名是db-2017.05,db-2017.06,
     *
     * @param from
     * @return
     */
    public String[] getNextIndexs(Date from) {
        Date to = getNextIndexsTime(from, 1);
        Date now = new Date();
        if(to.getTime() < now.getTime()){
        	to= now;
        }
        return getIndexRand(now, to);
    }

    @Override
    public String toString() {
        return "ElkDBVO{" +
                "index='" + index + '\'' +
                ", type='" + type + '\'' +
                ", timePattern='" + timePattern + '\'' +
                '}';
    }

}
