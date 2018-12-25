package com.ppm.integration.agilesdk.connector.octane;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wink.client.ClientRuntimeException;

import com.hp.ppm.tm.model.TimeSheet;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientHelper;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneConnectivityExceptionHandler;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.TimesheetItem;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.tm.ExternalWorkItem;
import com.ppm.integration.agilesdk.tm.ExternalWorkItemEffortBreakdown;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegration;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegrationContext;
import com.ppm.integration.agilesdk.tm.TimeSheetLineAgileEntityInfo;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.LineBreaker;
import com.ppm.integration.agilesdk.ui.Link;
import com.ppm.integration.agilesdk.ui.SelectList;

public class OctaneTimeSheetIntegration extends TimeSheetIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    static final String SEP = ">";

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
        return Arrays.asList(new Field[] {
                new Link("auth", "AUTHENTICATION_LINK", "defaultValue", "true", true, "javascript:void(0);",
                        "openSSOLink()"),
                new LineBreaker(),
                new SelectList(OctaneConstants.TS_GROUP_BY,"TS_GROUP_BY",OctaneConstants.TS_GROUP_BY_RELEASE,true)
                        .addLevel(OctaneConstants.TS_GROUP_BY, "TS_GROUP_BY")
                        .addOption(new SelectList.Option(OctaneConstants.TS_GROUP_BY_WORKSPACE,"TS_GROUP_BY_WORKSPACE"))
                        .addOption(new SelectList.Option(OctaneConstants.TS_GROUP_BY_RELEASE,"TS_GROUP_BY_RELEASE"))
                        .addOption(new SelectList.Option(OctaneConstants.TS_GROUP_BY_BACKLOG_ITEM,"TS_GROUP_BY_BACKLOG_ITEM"))

        });
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

    @Override
    public String getSSOurl(ValueSet values) {
        ClientPublicAPI clientP = OctaneClientHelper.setupClientPublicAPI(values);
        return clientP.getSSOURL();
    }

    public List<ExternalWorkItem> getExternalWorkItemsByTasks(TimeSheetIntegrationContext context,
            final ValueSet values) throws ParseException
    {
        final List<ExternalWorkItem> timesheetLines = Collections.synchronizedList(new LinkedList<ExternalWorkItem>());

        try {

            TimeSheet currentTimeSheet = context.currentTimeSheet();
            final String startDate = convertDate(currentTimeSheet.getPeriodStartDate().toGregorianCalendar().getTime());
            final String endDate = convertDate(currentTimeSheet.getPeriodEndDate().toGregorianCalendar().getTime());

            ClientPublicAPI clientP = OctaneClientHelper.setupClientPublicAPI(values);
            
            String identifier = values.get(OctaneConstants.SSO_IDENTIFIER);
            if (identifier == null) {
                throw new OctaneClientException("OCTANE_APP", "SSO identifier lose");
            }
            String userName = clientP.getSSOAuthentication(identifier);
            if (userName == null) {
                throw new OctaneClientException("OCTANE_APP", "You need to authenticate");
            }

            String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
            String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
            String groupBy = values.get(OctaneConstants.TS_GROUP_BY);


            if (clientP.getAccessTokenWithFormFormat(clientId, clientSecret)) {
                List<SharedSpace> shareSpaces = clientP.getSharedSpaces();
                List<WorkSpace> workspacesAll = new ArrayList<WorkSpace>();
                for (SharedSpace shareSpace : shareSpaces) {
                    List<WorkSpace> workspaces = clientP.getWorkSpaces(Integer.parseInt(shareSpace.id));
                    workspacesAll.addAll(workspaces);
                    for (WorkSpace workSpace : workspacesAll) {
                        List<TimesheetItem> timeSheets = clientP.getTimeSheetData(Integer.parseInt(shareSpace.id),
                                userName, startDate.toString(), endDate.toString(),
                                Integer.parseInt(workSpace.id));

                        if (timeSheets == null || timeSheets.isEmpty()) {
                            continue;
                        }

                        if (OctaneConstants.TS_GROUP_BY_WORKSPACE.equals(groupBy)) {
                            // Group by workspace - We aggregate all items into one line for the current workspace
                            timesheetLines.add(new OctaneExternalWorkItem(workSpace.name,
                                    timeSheets, startDate, endDate, workSpace.id, null, null));

                        } else if (OctaneConstants.TS_GROUP_BY_BACKLOG_ITEM.equals(groupBy)) {
                            // Group by Backlog items - One item = one line
                            for (TimesheetItem item : timeSheets) {
                                timesheetLines.add(new OctaneExternalWorkItem(workSpace.name + SEP + item.getEntityName(),
                                        Arrays.asList(new TimesheetItem[] {item}), startDate, endDate, workSpace.id, item.getReleaseId() == 0 ? null : String.valueOf(item.getReleaseId()), String.valueOf(item.getEntityId())));
                            }

                        } else {
                            // group by Releases
                            Map<Integer, List<TimesheetItem>> releaseTimesheet = new HashMap<Integer, List<TimesheetItem>>();
                            Map<Integer, String> releaseIdName = new HashMap<>();
                            for (TimesheetItem timeItem : timeSheets) {
                                if (!releaseTimesheet.containsKey(timeItem.getReleaseId())) {
                                    releaseTimesheet.put(timeItem.getReleaseId(), new ArrayList<TimesheetItem>());
                                }
                                releaseTimesheet.get(timeItem.getReleaseId()).add(timeItem);
                                releaseIdName.put(timeItem.getReleaseId(), timeItem.getReleaseName());
                            }

                            for (Map.Entry<Integer, List<TimesheetItem>> entry : releaseTimesheet.entrySet()) {
                                int releaseIdInt = entry.getKey();

                                String releaseId = null;
                                String releaseName = null;
                                if (releaseIdInt == 0) {
                                    releaseId = null;
                                    releaseName = Providers.getLocalizationProvider(OctaneIntegrationConnector.class).getConnectorText("TS_NO_RELEASE_LABEL");
                                } else {
                                    releaseId = String.valueOf(releaseIdInt);
                                    releaseName = releaseIdName.get(releaseIdInt);
                                }



                                ArrayList<TimesheetItem> oneReleaseTimeItems = (ArrayList<TimesheetItem>)entry.getValue();

                                timesheetLines.add(new OctaneExternalWorkItem(workSpace.name + SEP + releaseName,
                                        oneReleaseTimeItems, startDate, endDate, workSpace.id, releaseId, null));
                            }
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
        }

        return timesheetLines;
    }

    private class OctaneExternalWorkItem extends ExternalWorkItem {

        String workspaceId;

        String releaseId;

        String backlogItemId;

        String timesheetLineName;

        double totalEffort = 0;

        String errorMessage = null;

        Date startDate;

        Date finishDate;

        Hashtable<String, Long> effortList = new Hashtable<>();

        public OctaneExternalWorkItem(String timesheetLineName, List<TimesheetItem> timeSheets,
                String startDate, String finishDate, String workspaceId, String releaseId, String backlogItemId)
        {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            this.timesheetLineName = timesheetLineName;
            this.workspaceId = workspaceId;
            this.releaseId = releaseId;
            this.backlogItemId = backlogItemId;

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
            return this.timesheetLineName;
        }

        
        @Override
        public Double getTotalEffort() {
            return totalEffort;
        }

        @Override
        public TimeSheetLineAgileEntityInfo getLineAgileEntityInfo() {
            TimeSheetLineAgileEntityInfo lineInfo = new TimeSheetLineAgileEntityInfo(workspaceId);
            lineInfo.setBacklogItemId(this.backlogItemId);
            lineInfo.setReleaseId(this.releaseId);
            return lineInfo;
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
