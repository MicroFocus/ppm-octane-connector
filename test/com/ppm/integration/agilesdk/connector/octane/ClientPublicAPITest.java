package com.ppm.integration.agilesdk.connector.octane;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hp.ppm.tm.model.TimeSheet;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientHelper;
import com.ppm.integration.agilesdk.connector.octane.model.EpicAttr;
import com.ppm.integration.agilesdk.connector.octane.model.EpicCreateEntity;
import com.ppm.integration.agilesdk.connector.octane.model.EpicEntity;
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
import com.ppm.integration.agilesdk.model.AgileEntity;
import com.ppm.integration.agilesdk.model.AgileEntityFieldValue;

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

        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
    }

    @Test public void testGetTimeSheetData() throws Exception {
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);
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
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

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
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

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
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

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
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<Release> releases = client.getAllReleases();
        Assert.assertNotNull(releases);
        Assert.assertTrue(releases.size() > 0);
        for (Release d : releases) {
            System.out.println("name=" + d.name + ", id=" + d.id);
        }
    }

    @Test public void testGetReleaseTeams() throws Exception {
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<ReleaseTeam> releaseTeams =
                client.getReleaseTeams();
        Assert.assertNotNull(releaseTeams);
        Assert.assertTrue(releaseTeams.size() > 0);
        for (ReleaseTeam d : releaseTeams) {
            System.out.println(d.teamId);
        }
    }

    @Test public void testGetTeams() throws Exception {
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);
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
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);
        List<Sprint> sprints = client.getAllSprints();
        Assert.assertNotNull(sprints);
        Assert.assertTrue(sprints.size() > 0);
        for (Sprint d : sprints) {
            System.out.println(d.name);
        }
    }

    @Test public void testGetWorkItemRoot() throws Exception {
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

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


    @Test public void testGetEpicDoneStoryPoints() throws
            Exception{
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);

        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);

        String[] doneStatusIDs = client.getDoneDefinitionOfUserStoryAndDefect(
                values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002));
        Assert.assertNotNull(doneStatusIDs);
        System.out.println("doneStatusIDs------------------------" + doneStatusIDs.length);
        String epicId = "114032";

        WorkItemEpic epic1= client.getEpicActualStoryPointsAndPath(
                values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002), epicId);
        Assert.assertNotNull(epic1);
        System.out.println("epic.path=" + epic1.path);
        System.out.println("epic.name=" + epic1.name);
        System.out.println("epic.totalStoryPoints=" + epic1.totalStoryPoints);

        WorkItemEpic epic = client.getEpicDoneStoryPoints(values.getInteger(OctaneConstants.KEY_SHAREDSPACEID, 1001),
                values.getInteger(OctaneConstants.KEY_WORKSPACEID, 1002), epic1.path, doneStatusIDs);
        Assert.assertNotNull(doneStatusIDs);
        System.out.println("epic.doneStoryPoints=" + epic.doneStoryPoints);
    }

    
    @Test public void testCreateEpicInWorkspace() throws Exception {
    	ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);
    	String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
    	String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
    	boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
    	Assert.assertTrue(isGetAccess);
    	List<EpicEntity> result = null;
    	EpicCreateEntity epicCreateEntity = new EpicCreateEntity();
    	List<EpicEntity> data = new ArrayList<>();
    	EpicEntity epicEntity=new EpicEntity();
    	epicEntity.setName(values.get(OctaneConstants.KEY_EPIC_ENTITY_NAME));
    	epicEntity.setType(values.get(OctaneConstants.KEY_EPIC_ENTITY_TYPE));
    	EpicAttr epicAttrPhase=new EpicAttr();
    	epicAttrPhase.setId(values.get(OctaneConstants.KEY_PHASE_LOGICNAME_ID));
    	epicAttrPhase.setType(values.get(OctaneConstants.KEY_PHASE_LOGICNAME_TYPE));
    	EpicAttr epicAttrParent=new EpicAttr();
    	epicAttrParent.setId(values.get(OctaneConstants.KEY_WORKITEM_PARENT_ID));
    	epicAttrParent.setType(values.get(OctaneConstants.KEY_WORKITEM_PARENT_TYPE));
    	epicEntity.setPhase(epicAttrPhase);
    	epicEntity.setParent(epicAttrParent);
    	data.add(epicEntity);
    	epicCreateEntity.setData(data);
    	result= client.createEpicInWorkspace(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), epicCreateEntity);
    	Assert.assertNotNull(result);
    	for(EpicEntity e : result){
    		 System.out.println("id:"+e.getId()+"   name:"+e.getName()+"   type:"+e.getType()+
    				 "  parent:"+e.getParent().name+ "  phase:"+e.getPhase().getName());
    	}
    }
    
    @Test public void testGetEpicPhase() throws Exception {
    	ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);
    	String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
    	String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
    	boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
    	Assert.assertTrue(isGetAccess);
    	List<EpicAttr> result = client.getEpicPhase(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), values.get(OctaneConstants.KEY_PHASE_LOGICNAME));
    	Assert.assertNotNull(result);
    	for(EpicAttr e : result){
    		System.out.println("id: " + e.getId() +"  name:"+ e.getName() + "  type:"+ e.getType());
    	}
    }
    
    @Test public void testGetEpicParent() throws Exception {
    	ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);
    	String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
    	String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
    	boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
    	Assert.assertTrue(isGetAccess);
    	List<EpicAttr> result = client.getEpicParent(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), values.get(OctaneConstants.KEY_WORKITEM_SUBTYPE));
    	Assert.assertNotNull(result);
    	for(EpicAttr e : result){
    		System.out.println("id: " + e.getId() +"  name:"+ e.getName() + "  type:"+ e.getType());
    	}
    }

    @Test
    public void testGetUserStoriesAfterDate() throws Exception {
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);
        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);

        Set<String> ids = new HashSet<String>();
        ids.add("111111");
        ids.add("222222");
        ids.add("103014");
        ids.add("225053");
        ids.add("226013");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date updateDate = cal.getTime();

        List<AgileEntity> result = client.getUserStoriesAfterDate(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), null, null);
        List<AgileEntity> result1 = client.getUserStoriesAfterDate(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), ids, null);
        List<AgileEntity> result2 = client.getUserStoriesAfterDate(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), null, updateDate);
        List<AgileEntity> result3 = client.getUserStoriesAfterDate(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), ids, updateDate);

        Assert.assertNull(result);
        Assert.assertNotNull(result1);
        Assert.assertNotNull(result2);
        Assert.assertNotNull(result3);

        printAgileEntityList(result1);
        printAgileEntityList(result2);
        printAgileEntityList(result3);
    }

    @Test
    public void testGetFeaturesAfterDate() throws Exception {
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);
        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        boolean isGetAccess = client.getAccessTokenWithFormFormat(clientId, clientSecret);
        Assert.assertTrue(isGetAccess);

        Set<String> ids = new HashSet<String>();
        ids.add("111111");
        ids.add("222222");
        ids.add("221011");
        ids.add("222001");
        ids.add("229001");



        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date updateDate = cal.getTime();

        List<AgileEntity> result = client.getFeaturesAfterDate(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), null, null);
        List<AgileEntity> result1 = client.getFeaturesAfterDate(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), ids, null);
        List<AgileEntity> result2 = client.getFeaturesAfterDate(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), null, updateDate);
        List<AgileEntity> result3 = client.getFeaturesAfterDate(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), ids, updateDate);

        Assert.assertNull(result);
        Assert.assertNotNull(result1);
        Assert.assertNotNull(result2);
        Assert.assertNotNull(result3);

        printAgileEntityList(result1);
        printAgileEntityList(result2);
        printAgileEntityList(result3);
    }

    private void printAgileEntityList(List<AgileEntity> agileEntityList) {
        for (AgileEntity agileEntity : agileEntityList) {

            Iterator<Entry<String, List<AgileEntityFieldValue>>> iterator = agileEntity.getAllFields();
            System.out.println("\t{");
            System.out.println("\tID: " + agileEntity.getId());
            System.out.println("\tlast update time: " + agileEntity.getLastUpdateTime());
            while (iterator.hasNext()) {

                Entry<String, List<AgileEntityFieldValue>> entry = iterator.next();
                System.out.print("\t\t" + entry.getKey() + ": ");
                List<AgileEntityFieldValue> value = entry.getValue();
                for (AgileEntityFieldValue v : value) {
                    System.out.println(v.getValue() + ", \t\t referenceValue:" + v.getReferenceValue() + ", ");
                }

            }
            System.out.println("\t}");
        }
        System.out.println("===========================================");
    }

}

