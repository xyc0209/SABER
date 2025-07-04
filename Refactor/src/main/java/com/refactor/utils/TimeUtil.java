package com.refactor.utils;

/**
 * @description:
 * @author: xyc
 * @date: 2025-02-25 16:03
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    public static Date calculateStartTime(Date endTime, int intervalInMinutes) {
        return new Date(endTime.getTime() - intervalInMinutes * 60 * 1000);
    }

    public static Date parseStr2Date(String timeStr) throws ParseException {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time = dateTimeFormatter.parse(timeStr);
        return time;
    }
}