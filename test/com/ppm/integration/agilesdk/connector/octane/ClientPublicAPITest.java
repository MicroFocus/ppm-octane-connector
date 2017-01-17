package com.ppm.integration.agilesdk.connector.octane;

import com.hp.ppm.tm.model.TimeSheet;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.model.ReleaseTeam;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.connector.octane.model.Team;
import com.ppm.integration.agilesdk.connector.octane.model.TimesheetItem;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemEpic;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemFeature;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemStory;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientPublicAPITest {
    ValueSet values = new ValueSet();

    @Before public void setUp() throws Exception {
        values = CommonParameters.getDefaultValueSet();
    }

    public String convertDate(Date date) {

        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.format(date);
        } catch (Exception e) {

        }
        return "";

    }

    @Test public void testGetAccessTokenWithFormFormat() throws Exception {

        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
    }

    @Test public void testGetTimeSheetData() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);
        TestTimeSheetIntegrationContext context = new TestTimeSheetIntegrationContext();
        TimeSheet timeSheet = context.currentTimeSheet();

        final Date startDate = timeSheet.getPeriodStartDate().toGregorianCalendar().getTime();
        final Date endDate = timeSheet.getPeriodEndDate().toGregorianCalendar().getTime();
        final String startDateStr = convertDate(startDate);
        final String endDateStr = convertDate(endDate);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<SharedSpace> shareSpaces = client.getSharedSpaces();
        List<WorkSpace> workspacesAll = new ArrayList<WorkSpace>();
        for (SharedSpace shareSpace : shareSpaces) {
            List<WorkSpace> workspaces = client.getWorkSpaces(Integer.parseInt(shareSpace.id));
            workspacesAll.addAll(workspaces);
            for (WorkSpace workSpace : workspacesAll) {
                List<TimesheetItem> timeSheets = client.getTimeSheetData(Integer.parseInt(shareSpace.id),
                        values.get(OctaneConstants.KEY_USERNAME), startDateStr, endDateStr,
                        Integer.parseInt(workSpace.id));
                Map<String, ArrayList<TimesheetItem>> releaseTimesheet =
                        new HashMap<String, ArrayList<TimesheetItem>>();
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
                    Map<String, ArrayList<TimesheetItem>> sprintTimesheet =
                            new HashMap<String, ArrayList<TimesheetItem>>();
                    //this is used to generate Map<"sprint",List<TimesheetItem>>
                    for (TimesheetItem timeItem : oneReleaseTimeItems) {
                        if (!sprintTimesheet.containsKey(timeItem.getReleaseName())) {
                            sprintTimesheet.put(timeItem.getSprintName(), new ArrayList<TimesheetItem>());
                        }
                        sprintTimesheet.get(timeItem.getSprintName()).add(timeItem);
                    }
                    Iterator iterSprint = sprintTimesheet.entrySet().iterator();
                    while (iterSprint.hasNext()) {
                        Map.Entry entrySprint = (Map.Entry)iterSprint.next();
                        Object sprintName = entry.getKey();
                        System.out.println("---------------------------------sprint");
                        System.out.println("sprint:" + sprintName.toString());
                        ArrayList<TimesheetItem> oneSprintTimeItems = (ArrayList<TimesheetItem>)entrySprint.getValue();
                        for (TimesheetItem t : oneSprintTimeItems) {
                            System.out.println("---------------------------------task");
                            System.out.println("task:" + t.toString());
                        }
                    }
                }
            }
        }

    }

    @Test public void testGetSharedSpaces() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<SharedSpace> sharedSpaces = client.getSharedSpaces();
        Assert.assertNotNull(sharedSpaces);
        Assert.assertTrue(sharedSpaces.size() > 0);
        for (SharedSpace d : sharedSpaces) {
            System.out.println(d.name);
        }
    }

    @Test public void testGetWorkSpaces() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<WorkSpace> workSpace = client.getWorkSpaces(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001));
        Assert.assertNotNull(workSpace);
        Assert.assertTrue(workSpace.size() > 0);
        for (WorkSpace d : workSpace) {
            System.out.println(d.name);
        }
    }

    @Test public void testGetRelease() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        Release release = client.getRelease(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002),
                values.getInteger(OctaneConstants.KEY_RELEASEID, 1001));
        Assert.assertNotNull(release);
        System.out.println("start_date" + release.startDate + ", end_date=" + release.endDate);
    }

    @Test public void testGetReleases() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<Release> releases = client.getReleases(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002));
        Assert.assertNotNull(releases);
        Assert.assertTrue(releases.size() > 0);
        for (Release d : releases) {
            System.out.println("name=" + d.name + ", id=" + d.id);
        }
    }

    @Test public void testGetReleaseTeams() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<ReleaseTeam> releaseTeams =
                client.getReleaseTeams(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                        values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002));
        Assert.assertNotNull(releaseTeams);
        Assert.assertTrue(releaseTeams.size() > 0);
        for (ReleaseTeam d : releaseTeams) {
            System.out.println(d.teamId);
        }
    }

    @Test public void testGetTeams() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);
        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<Team> teams = client.getTeams(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002));
        Assert.assertNotNull(teams);
        Assert.assertTrue(teams.size() > 0);
        for (Team d : teams) {
            System.out.println("name=" + d.name + ", id=" + d.id + ", membersCapacity=" + d.membersCapacity);
        }
    }

    @Test public void testGetSprints() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<Sprint> sprints = client.getSprints(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002));
        Assert.assertNotNull(sprints);
        Assert.assertTrue(sprints.size() > 0);
        for (Sprint d : sprints) {
            System.out.println(d.name);
        }
    }

    @Test public void testGetWorkItemRoot() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        WorkItemRoot workItemRoot = client.getWorkItemRoot(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002));
        Assert.assertNotNull(workItemRoot);
        for (WorkItemStory d : workItemRoot.storyList) {
            System.out.println("name=" + d.name + ", id=" + d.id + ", type=" + d.subType);
        }
        Set<String> keySet = workItemRoot.epicList.keySet();
        for (String key : keySet) {
            WorkItemEpic tempEpic = workItemRoot.epicList.get(key);
            System.out.println("                        ");
            System.out.println("name=" + tempEpic.name + ", id=" + tempEpic.id + ", type=" + tempEpic.subType);
            System.out.println("------------------------");
            Set<String> keySetFeature = tempEpic.featureList.keySet();
            for (String keyFeature : keySetFeature) {
                WorkItemFeature tempFeature = tempEpic.featureList.get(keyFeature);
                System.out.println(
                        "name=" + tempFeature.name + ", id=" + tempFeature.id + ", type=" + tempFeature.subType);
                System.out.println("--------------");
                for (WorkItemStory d : tempFeature.storyList) {
                    System.out.println("name=" + d.name + ", id=" + d.id + ", type=" + d.subType);
                }
            }
        }
    }

    @Test public void testGetWorkItemRootWithReleaseId() throws Exception {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        WorkItemRoot workItemRoot = client.getWorkItemRoot(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002),
                values.getInteger(OctaneConstants.KEY_RELEASEID, 1001));
        Assert.assertNotNull(workItemRoot);
        System.out.println("backlog------------------------");
        for (WorkItemStory d : workItemRoot.storyList) {
            System.out.println("name=" + d.name + ", id=" + d.id + ", type=" + d.subType);
        }
        Set<String> keySet = workItemRoot.epicList.keySet();
        for (String key : keySet) {
            WorkItemEpic tempEpic = workItemRoot.epicList.get(key);
            System.out.println("                        ");
            System.out.println("name=" + tempEpic.name + ", id=" + tempEpic.id + ", type=" + tempEpic.subType);
            System.out.println("------------------------");
            Set<String> keySetFeature = tempEpic.featureList.keySet();
            for (String keyFeature : keySetFeature) {
                WorkItemFeature tempFeature = tempEpic.featureList.get(keyFeature);
                System.out.println(
                        "name=" + tempFeature.name + ", id=" + tempFeature.id + ", type=" + tempFeature.subType);
                System.out.println("--------------");
                for (WorkItemStory d : tempFeature.storyList) {
                    System.out.println("name=" + d.name + ", id=" + d.id + ", type=" + d.subType);
                }
            }
        }
    }

}
