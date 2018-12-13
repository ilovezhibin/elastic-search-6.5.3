package com.yy.aomi.elastic;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class DateUtils
{  
	// "yyyy-MM-dd" "yyyy-MM-dd HH:mm:ss
    //private static String defaultDatePattern = "yyyy-MM-dd";  
    
    public static final String yyyyMMddHHmmss = "yyyy-MM-dd HH:mm:ss";
	public static final String yyyyMMdd = "yyyy-MM-dd";
	public static final String UTC_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	public static final long DAY = 1000*60*60*24;
	public static final long HOUR = 1000*60*60;
	public static final long MINUTE = 1000*60;
	
  
    /** 
     * 获得默认的 date pattern 
     */  
    public static String getDatePattern()  
    {  
        return yyyyMMdd;  
    }  
  
    /** 
     * 返回预设Format的当前日期字符串 
     */  
    public static String getCurDate()  
    {  
        return getCurTime(yyyyMMdd);  
    }  
    /** 
     * 返回当前时间,使用参数Format格式化Date成字符串 
     */  
    public static String getCurTime(String pattenrn)  
    {  
        Date today = new Date();  
        return format(today, pattenrn);  
    }
    
    /** yyyy-MM-dd
     * 使用预设Format格式化Date成字符串 
     */  
    public static String format(Date date)  
    {  
        return date == null ? "" : format(date, getDatePattern());  
    }
    
    /** 测试使用，要删除
     * 使用参数Format格式化Date成字符串 
     */  
    public static String testFormat(Date date)  
    {  
        return format(date, UTC_TIME_PATTERN);
    } 
  
    /** 
     * 使用参数Format格式化Date成字符串 
     */  
    public static String format(Date date, String pattern)  
    {  
    	SimpleDateFormat sdf = new SimpleDateFormat(pattern); 
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); 
    	
        return date == null ? "" : sdf.format(date);  
    }  
    public static String formatUTC(Date date, String pattern){
    	SimpleDateFormat sdf = new SimpleDateFormat(pattern); 
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT")); 
    	return date == null ? "" : sdf.format(date);  
        
    }

    public static String formatUTCByUTCPattern(long time){
        return formatUTC(new Date(time),UTC_TIME_PATTERN);
    }

    public static String formatUTCByUTCPattern(Date date){
        return formatUTC(date,UTC_TIME_PATTERN);
    }
    
    public static String formatUTC2(DateTime date, String pattern){
        DateTimeZone zone = DateTimeZone.forID("GMT");
        DateTime time = date.withZone(zone);
        return time == null ? "" : time.toString(pattern);
    }
    
    public static Date parseUTC(String strDate, String pattern) throws ParseException{
    	SimpleDateFormat sdf = new SimpleDateFormat(pattern); 
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT")); 
        return StringUtils.isBlank(strDate) ? null : sdf.parse(strDate);
    }
    
    public static Date parseUTCByUTCPattern(String strDate) throws ParseException{
    	SimpleDateFormat sdf = new SimpleDateFormat(UTC_TIME_PATTERN); 
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT")); 
        return StringUtils.isBlank(strDate) ? null : sdf.parse(strDate);
    }

    /** 
     * 使用预设格式将字符串转为Date 
     */  
    public static Date parse(String strDate) throws ParseException  
    {  
        return StringUtils.isBlank(strDate) ? null : parse(strDate,  
                getDatePattern());  
    }  
  
    /** 
     * 使用参数Format将字符串转为Date 
     */  
    public static Date parse(String strDate, String pattern)  
            throws ParseException  
    {  
    	SimpleDateFormat sdf = new SimpleDateFormat(pattern); 
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); 
        return StringUtils.isBlank(strDate) ? null : sdf.parse(strDate);  
    }  
  

    /** 
     * 在日期上增加数个整月 
     */  
    public static Date addMonth(Date date, int n)  
    {  
        Calendar cal = Calendar.getInstance();  
        cal.setTime(date);  
        cal.add(Calendar.MONTH, n);  
        return cal.getTime();  
    }  
  
    public static Date addDay(Date date, int n)  
    {  
        Calendar cal = Calendar.getInstance();  
        cal.setTime(date);  
        cal.add(Calendar.DATE, n);  
        return cal.getTime();  
    }  
    public static Date addMinute(Date date, int n)  
    {  
        Calendar cal = Calendar.getInstance();  
        cal.setTime(date);  
        cal.add(Calendar.MINUTE, n);  
        return cal.getTime();  
    } 
    public static Date addSecond(Date date, int n)  
    {  
        Calendar cal = Calendar.getInstance();  
        cal.setTime(date);  
        cal.add(Calendar.SECOND, n);  
        return cal.getTime();  
    }
    public static String getLastDayOfMonth(String year, String month)  
    {  
        Calendar cal = Calendar.getInstance();  
        // 年  
        cal.set(Calendar.YEAR, Integer.parseInt(year));  
        // 月，因为Calendar里的月是从0开始，所以要-1  
        // cal.set(Calendar.MONTH, Integer.parseInt(month) - 1);  
        // 日，设为一号  
        cal.set(Calendar.DATE, 1);  
        // 月份加一，得到下个月的一号  
        cal.add(Calendar.MONTH, 1);  
        // 下一个月减一为本月最后一天  
        cal.add(Calendar.DATE, -1);  
        return String.valueOf(cal.get(Calendar.DAY_OF_MONTH));// 获得月末是几号  
    }  
  
    public static Date getDate(String year, String month, String day)  
            throws ParseException  
    {  
        String result = year + "- "  
                + (month.length() == 1 ? ("0 " + month) : month) + "- "  
                + (day.length() == 1 ? ("0 " + day) : day);  
        return parse(result);  
    }  
    
    // 第二天的0点
    public static Date nextDate0(Date now) throws ParseException{
    	String yymmdd = format(addDay(now,1), yyyyMMdd);
    	return parse(yymmdd, yyyyMMdd);

    }
    
    public static int getHour(Date now){
    	Calendar cal=Calendar.getInstance();
    	cal.setTime(now);
    	return cal.get(Calendar.HOUR_OF_DAY);
    }
    
    public static int getDay(Date now){
    	Calendar cal=Calendar.getInstance();
    	cal.setTime(now);
    	return cal.get(Calendar.DAY_OF_MONTH);
    }
    
    /**把格式化日期转成date
	 * @param source
	 * @param pattern
	 * @return
	 */
	public static Date getDateFromString(String source,String pattern){
		if(source == null) return null;
		if(pattern==null){
			if(source.length()==10){
				pattern = yyyyMMdd;
			}else if(source.length()==19)
			pattern = yyyyMMddHHmmss;
		}
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		Date date = null;
		try {
			date = format.parse(source);
		} catch (ParseException e) {
			//e.printStackTrace();
		}
		return date;
		
	}
	
	/**把格式化日期yyyy-MM-dd HH:mm:ss或yyyy-MM-dd转成date
	 * @param source
	 * @return
	 */
	public static Date getDateFromString(String source){
		return getDateFromString(source,null);
	}
	
	public static String formatDate(Date date,String pattern){
		if(pattern==null){
			pattern = yyyyMMddHHmmss;
		}
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		String result = format.format(date);
		return result;
	}
	
	/**把日期格式化为yyyy-MM-dd HH:mm:ss
	 * @param date
	 * @return
	 */
	public static String formatDate(Date date){
		return formatDate(date,null);
	}
	
	/**把日期的时分钞毫秒置0
	 * @param time
	 * @return
	 */
	public static Date timeToDate(Date time){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}
	
	/**把日期设置成当天最大的时间
	 * @param time
	 * @return
	 */
	public static Date timeToMaxDate(Date time){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}
	
	/**日期加减
	 * @param date
	 * @param field
	 * @param amount
	 * @return
	 */
	public static Date add(Date date,int field,int amount){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(field, amount);
		return calendar.getTime();
	}
	
	/**日期加减,且格式化成yyyy-MM-dd HH:mm:ss
	 * @param date
	 * @param field
	 * @param amount
	 * @return
	 */
	public static String addAndFormat(Date date,int field,int amount){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(field, amount);
		return formatDate(calendar.getTime());
	}
	
	public static long subDay(Date st,Date ed){
		return (ed.getTime() - st.getTime())/DAY;
	}
	
	/**把格式为"yyyy-MM-dd - yyyy-MM-dd"字符串转成2个date类型
	 * @param source
	 * @return
	 */
	public static Date[] String2Date(String source){
		Date[] result = new Date[]{null,null};
		if(source == null || source.length() != 23){
			return result;
		}else{
			String startTime = source.substring(0, 10);
			String endTime = source.substring(13, 23);
			Date sDate = DateUtils.getDateFromString(startTime);
			Date eDate = DateUtils.getDateFromString(endTime);
			result = new Date[]{sDate,eDate};
			return result;
		}
	}
}  