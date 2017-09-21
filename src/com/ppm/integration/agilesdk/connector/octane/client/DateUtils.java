package com.ppm.integration.agilesdk.connector.octane.client;

import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by canaud on 8/3/2017.
 */
public class DateUtils {

    protected static final Logger logger = LogManager.getLogger(DateUtils.class);

    public static final String dateFormat =  "yyyy-MM-dd'T'HH:mm:ss";

    public static Date convertDateTime(String dateStr) {
        try {
            synchronized (dateFormat) {
                return new SimpleDateFormat(dateFormat).parse(dateStr);
            }
        } catch (ParseException e) {
            logger.error("Exception when parsing date string '" + dateStr + "', returning new date()", e);
            return new Date();
        }
    }
}
