package com.ppm.integration.agilesdk.connector.octane;

import com.hp.ppm.tm.model.TimeSheet;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegrationContext;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class TestTimeSheetIntegrationContext implements TimeSheetIntegrationContext {

    @Override public TimeSheet currentTimeSheet() {
        TimeSheet timeSheet = new TimeSheet();
        Calendar start = new GregorianCalendar();

        start.set(Calendar.MONTH, Calendar.NOVEMBER);
        start.set(Calendar.DAY_OF_MONTH, 1);

        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);

        Calendar end = new GregorianCalendar();
        end.set(Calendar.MONTH, Calendar.DECEMBER);
        end.set(Calendar.DAY_OF_MONTH, 30);

        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);

        System.out.println(start.getTime());
        System.out.println(end.getTime());
        timeSheet.setPeriodStartDate(convertToXMLGregorianCalendar(start.getTime()));
        timeSheet.setPeriodEndDate(convertToXMLGregorianCalendar(end.getTime()));
        return timeSheet;
    }

    public XMLGregorianCalendar convertToXMLGregorianCalendar(Date date) {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        XMLGregorianCalendar gc = null;
        try {
            gc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (Exception e) {

            e.printStackTrace();
        }
        return gc;
    }

    public Date convertToDate(XMLGregorianCalendar cal) throws Exception {
        GregorianCalendar ca = cal.toGregorianCalendar();
        return ca.getTime();
    }

}
