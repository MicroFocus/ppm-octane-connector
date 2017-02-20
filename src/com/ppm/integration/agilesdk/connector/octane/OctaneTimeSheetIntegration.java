package com.ppm.integration.agilesdk.connector.octane;

import com.hp.ppm.tm.model.TimeSheet;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.Client;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.TimesheetItem;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.tm.ExternalWorkItem;
import com.ppm.integration.agilesdk.tm.ExternalWorkItemEffortBreakdown;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegration;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegrationContext;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.PasswordText;
import com.ppm.integration.agilesdk.ui.PlainText;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientRuntimeException;
import org.json.JSONException;

public class OctaneTimeSheetIntegration extends TimeSheetIntegration {
    private final Logger logger = Logger.getLogger(this.getClass());

    protected synchronized String convertDate(Date date) {

        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.format(date);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return "";

    }

    @Override public List<Field> getMappingConfigurationFields(ValueSet paramValueSet) {
        return Arrays.asList(new Field[] {new PlainText(OctaneConstants.KEY_USERNAME, "USERNAME", "", true),
                new PasswordText(OctaneConstants.KEY_PASSWORD, "PASSWORD", "", true)});
    }

    @Override public List<ExternalWorkItem> getExternalWorkItems(TimeSheetIntegrationContext context,
            final ValueSet values)
    {

        List<ExternalWorkItem> items = null;
        try {
            items = getExternalWorkItemsByTasks(context, values);
        } catch (ParseException e) {
            logger.error(e.getMessage());
        }

        return items;
    }

    public List<ExternalWorkItem> getExternalWorkItemsByTasks(TimeSheetIntegrationContext context,
            final ValueSet values) throws ParseException
    {
        final List<ExternalWorkItem> items = Collections.synchronizedList(new LinkedList<ExternalWorkItem>());
        try {

            boolean passAuth = false;
            final Client simpleClient =
                    OctaneFunctionIntegration.setupClient(new Client(values.get(OctaneConstants.KEY_BASE_URL)), values);
            if (simpleClient.getCookies() != null || simpleClient.getCookies().equals("")) {
                passAuth = true;
            }
            //the auth is in this method: setupClient(new Client(values.get(AgmConstants.KEY_BASE_URL)),values);

            TimeSheet currentTimeSheet = context.currentTimeSheet();
            final String startDate = convertDate(currentTimeSheet.getPeriodStartDate().toGregorianCalendar().getTime());
            final String endDate = convertDate(currentTimeSheet.getPeriodEndDate().toGregorianCalendar().getTime());

            ClientPublicAPI clientP = new ClientPublicAPI(values.get(OctaneConstants.KEY_BASE_URL));

            String clientid = values.get(OctaneConstants.APP_CLIENT_ID);
            String clientScrete = values.get(OctaneConstants.APP_CLIENT_SECRET);
            String proxyHost = values.get(OctaneConstants.KEY_PROXY_HOST);
            String proxyPort = values.get(OctaneConstants.KEY_PROXY_PORT);

            if (!proxyHost.equals("")) {
                clientP.setProxy(proxyHost, Integer.parseInt(proxyPort));
            }
            if (clientP.getAccessTokenWithFormFormat(clientid, clientScrete) && passAuth) {
                List<SharedSpace> shareSpaces = clientP.getSharedSpaces();
                List<WorkSpace> workspacesAll = new ArrayList<WorkSpace>();
                for (SharedSpace shareSpace : shareSpaces) {
                    List<WorkSpace> workspaces = clientP.getWorkSpaces(Integer.parseInt(shareSpace.id));
                    workspacesAll.addAll(workspaces);
                    for (WorkSpace workSpace : workspacesAll) {
                        List<TimesheetItem> timeSheets = clientP.getTimeSheetData(Integer.parseInt(shareSpace.id),
                                values.get(OctaneConstants.KEY_USERNAME), startDate.toString(), endDate.toString(),
                                Integer.parseInt(workSpace.id));
                        Map<String, ArrayList<TimesheetItem>> releaseTimesheet =
                                new HashMap<String, ArrayList<TimesheetItem>>();

                        //this is used to generate Map<"release",List<TimesheetItem>>
                        for (TimesheetItem timeItem : timeSheets) {
                            if (!releaseTimesheet.containsKey(timeItem.getReleaseName())) {
                                releaseTimesheet.put(timeItem.getReleaseName(), new ArrayList<TimesheetItem>());
                            }
                            releaseTimesheet.get(timeItem.getReleaseName()).add(timeItem);
                        }

                        //this is used to add one IExternalWorkItem as one line for one specific release under a workspace
                        Iterator iter = releaseTimesheet.entrySet().iterator();
                        while (iter.hasNext()) {
                            Map.Entry entry = (Map.Entry)iter.next();
                            Object releaseName = entry.getKey();
                            ArrayList<TimesheetItem> oneReleaseTimeItems = (ArrayList<TimesheetItem>)entry.getValue();

                            items.add(new OctaneExternalWorkItem(workSpace.name, releaseName.toString(),
                                    oneReleaseTimeItems, values, startDate, endDate));

                        }
                    }
                }
            }
        } catch (ClientRuntimeException | OctaneClientException e) {
            logger.error("", e);
            new OctaneConnectivityExceptionHandler()
                    .uncaughtException(Thread.currentThread(), e, OctaneTimeSheetIntegration.class);
        } catch (RuntimeException e) {
            logger.error("", e);
            new OctaneConnectivityExceptionHandler()
                    .uncaughtException(Thread.currentThread(), e, OctaneTimeSheetIntegration.class);
        } catch (JSONException e) {
            logger.error("", e);
            new OctaneConnectivityExceptionHandler()
                    .uncaughtException(Thread.currentThread(), e, OctaneTimeSheetIntegration.class);
        } catch (IOException e) {
            logger.error("", e);
            new OctaneConnectivityExceptionHandler()
                    .uncaughtException(Thread.currentThread(), e, OctaneTimeSheetIntegration.class);
        }

        return items;
    }

    private class OctaneExternalWorkItem extends ExternalWorkItem {

        static final String SEP = ">";

        String workSpace;

        String releaseName;

        double totalEffort = 0;

        String errorMessage = null;

        Date startDate;

        Date finishDate;

        Hashtable<String, Long> effortList = new Hashtable<>();

        public OctaneExternalWorkItem(String workSpace, String releaseName, List<TimesheetItem> timeSheets,
                ValueSet values, String startDate, String finishDate)
        {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            this.workSpace = workSpace;
            this.releaseName = releaseName;
            try {
                this.startDate = sdf.parse(startDate);
                this.finishDate = sdf.parse(finishDate);
            } catch (ParseException e) {

            }
            for (TimesheetItem timeSheet : timeSheets) {
                totalEffort += timeSheet.getInvested();
                Long effort = effortList.get(timeSheet.getDate());
                if (effort == null)
                    effort = 0L;
                effortList.put(timeSheet.getDate(), timeSheet.getInvested() + effort);
            }
        }

        @Override public String getName() {
            return this.workSpace + SEP + this.releaseName;
        }

        
        @Override
        public Double getTotalEffort() {
            return totalEffort;
        }

        @Override
        public ExternalWorkItemEffortBreakdown getEffortBreakDown() {

        	ExternalWorkItemEffortBreakdown effortBreakdown = new ExternalWorkItemEffortBreakdown();

            int numOfWorkDays = getDaysDiffNumber(startDate, finishDate);

            if (numOfWorkDays > 0) {
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(startDate);

                for(int i = 0; i < numOfWorkDays; i++) {
                    Long effort = effortList.get(convertDate(calendar.getTime()));
                    if(effort == null) effort = 0L;
                    effortBreakdown.addEffort(calendar.getTime(), effort.doubleValue());
                    // move to next day
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
            }

            return effortBreakdown;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    private int getDaysDiffNumber(Date startDate, Date endDate) {
        Calendar start = new GregorianCalendar();
        start.setTime(startDate);

        Calendar end = new GregorianCalendar();
        end.setTime(endDate);
        //move to last millisecond
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        Calendar dayDiff = Calendar.getInstance();
        dayDiff.setTime(startDate);
        int diffNumber = 0;
        while (dayDiff.before(end)) {
            diffNumber++;
            dayDiff.add(Calendar.DAY_OF_MONTH, 1);
        }
        return diffNumber;
    }
    
}
