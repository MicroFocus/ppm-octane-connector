package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.*;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.*;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;
import java.util.*;

/**
 * Created by lutian on 2017/1/13.
 */
public class OctaneAgileDataIntegration extends AgileDataIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    ClientPublicAPI client = null;
    
    AgileDataProject releaseProject = null;
    
    List<AgileDataProgram> releasePrograms = null;
    
    List<AgileDataBacklogItem> releaseBacklogItems = null;

    List<AgileDataFeature> releaseFeatures = null;

    List<AgileDataReleaseTeam> releaseReleaseTeams = null;

    List<AgileDataRelease> releaseReleases = null;

    List<AgileDataSprint> releaseSprints = null;

    List<com.ppm.integration.agilesdk.agiledata.AgileDataTeam> releaseTeams = null;

    List<AgileDataEpic> releaseEpics = null;

    Map<Integer, List<WorkSpace>> workSpacesWithSharedSpaceMap = null;

    protected ClientPublicAPI getClient(ValueSet paramValueSet) {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(paramValueSet);
        String clientId = paramValueSet.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = paramValueSet.get(OctaneConstants.APP_CLIENT_SECRET);
        try {
            if (!client.getAccessTokenWithFormFormat(clientId, clientSecret)) {
                return null;
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (JSONException e) {
            logger.error(e.getMessage());
        }
        return client;
    }

    @Override
    public void setUp(ValueSet paramValueSet, String projectId) {

        if (projectId == null) {
            return;
        }

        releaseProject = null;
        releasePrograms = new LinkedList<>();
        releaseBacklogItems = new LinkedList<>();
        releaseFeatures = new LinkedList<>();
        releaseReleaseTeams = new LinkedList<>();
        releaseReleases = new LinkedList<>();
        releaseSprints = new LinkedList<>();
        releaseTeams = new LinkedList<>();
        releaseEpics = new LinkedList<>();

        Map<Integer, List<WorkSpace>> workSpacesWithSharedSpaceMap = null;
        try {
            boolean found = false;
            client = getClient(paramValueSet);
            workSpacesWithSharedSpaceMap = new HashMap<>();
            List<SharedSpace> sharedSpacesList = client.getSharedSpaces();
            for (int i = 0, sizei = sharedSpacesList.size(); i < sizei; i++) {
                //workspace
                String strSharedSpaceId = sharedSpacesList.get(i).id;
                int sharedSpaceId = Integer.parseInt(strSharedSpaceId);
                List<WorkSpace> workSpaces = client.getWorkSpaces(sharedSpaceId);
                workSpacesWithSharedSpaceMap.put(sharedSpaceId, workSpaces);
                for (int j = 0, sizej = workSpaces.size(); j < sizej; j++) {
                    String strWorkSpaceId = workSpaces.get(j).id;

                    if(!projectId.equals(strWorkSpaceId)){
                        // This is not the workspace we're looking for
                    	continue;
                    } else {

                        found = true;

                        int workSpaceId = Integer.parseInt(strWorkSpaceId);
                        AgileDataProject tempProject = new AgileDataProject();
                        tempProject.setProjectId(strWorkSpaceId);
                        tempProject.setName(workSpaces.get(j).name);
                        this.releaseProject = tempProject;

                        AgileDataProgram tempProgram = new AgileDataProgram();
                        tempProgram.setProgramId(strSharedSpaceId);
                        tempProgram.setName(sharedSpacesList.get(i).name);
                        this.releasePrograms.add(tempProgram);

                        SetUpReleaseTeams(client, sharedSpaceId, workSpaceId);
                        SetUpReleases(client, sharedSpaceId, workSpaceId);
                        SetUpSprints(client, sharedSpaceId, workSpaceId);
                        SetUpTeams(client, sharedSpaceId, workSpaceId);
                        SetUpEpicFeatureBacklogItems(client, sharedSpaceId, workSpaceId);

                        // workspace found and data loaded.
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("error when setting up & retrieving agile data", e);
        }
    }

    //defect or us
    private List<AgileDataBacklogItem> parseBacklogItem(List<WorkItemStory> tempWorkItems, int workSpaceId) {

        List<AgileDataBacklogItem> backlogItems = new LinkedList<AgileDataBacklogItem>();
        for (int i = 0, size = tempWorkItems.size(); i < size; i++) {
            WorkItemStory tempWorkItem = tempWorkItems.get(i);
            AgileDataBacklogItem backlogItem = new AgileDataBacklogItem();
            backlogItem.setBacklogItemId(tempWorkItem.id);
            backlogItem.setBacklogType(tempWorkItem.subType);

            backlogItem.setRank(0);
            backlogItem.setName(tempWorkItem.name);
            backlogItem.setAssignTo(tempWorkItem.ownerName);
            backlogItem.setStoryPoints(tempWorkItem.storyPoints);

            backlogItem.setStatus(tempWorkItem.status);

            if (!tempWorkItem.detectedInRelease.equals("") && Integer.parseInt(tempWorkItem.detectedInRelease) > 0) {
                backlogItem.setDetectedInReleaseId(tempWorkItem.detectedInRelease);
            } else {
                backlogItem.setDetectedInReleaseId(null);
            }
            
            if (!tempWorkItem.releaseId.equals("") && Integer.parseInt(tempWorkItem.releaseId) > 0) {
                backlogItem.setReleaseId(tempWorkItem.releaseId);
            } else {
                backlogItem.setReleaseId(null);
            }

                backlogItem.setTeamId(tempWorkItem.teamId);

                backlogItem.setEpicId(tempWorkItem.epicId);

                backlogItem.setFeatureId(tempWorkItem.featureId);

                backlogItem.setSprintId(tempWorkItem.sprintId);

            backlogItem.setAuthor(tempWorkItem.ownerName);
            backlogItem.setPriority(tempWorkItem.priority);
            backlogItem.setSeverity(tempWorkItem.severity);
            backlogItem.setLastModified(tempWorkItem.lastModifiedTime);
            backlogItem.setNumberOfTasks(0);
            backlogItem.setDefectStatus(tempWorkItem.defectStatus);
            backlogItems.add(backlogItem);
        }

        return backlogItems;
    }

    protected void SetUpEpicFeatureBacklogItems(ClientPublicAPI client, int sharedSpaceId, int workSpaceId)
            throws IOException
    {
        WorkItemRoot workItemRoot = client.getWorkItemRoot(sharedSpaceId, workSpaceId);
        List<WorkItemStory> itemBacklogs = workItemRoot.storyList;
        releaseBacklogItems.addAll(this.parseBacklogItem(itemBacklogs, workSpaceId));
        Map<String, WorkItemFeature> itemFeatureRootMap=workItemRoot.featureList;
        if(itemFeatureRootMap != null && itemFeatureRootMap.size() > 0) {
        	 Set<String> keySetF = itemFeatureRootMap.keySet();
             for (String keyF : keySetF) {
                 WorkItemFeature tempFeature = itemFeatureRootMap.get(keyF);
                 AgileDataFeature feature = new AgileDataFeature();
                 feature.setFeatureId(tempFeature.id);
                 feature.setName(tempFeature.name);
                 feature.setStatus(tempFeature.status);
                 feature.setFeaturePoints(tempFeature.featurePoints);
                 feature.setAggStoryPoints(tempFeature.aggStoryPoints);
                 feature.setNumOfUserStories(tempFeature.numOfStories);
                 feature.setNumOfDefects(tempFeature.numbOfDefects);
                 feature.setEpicId(tempFeature.epicId);
                 if (tempFeature.releaseId != null && !"".equals(tempFeature.releaseId) && Integer.parseInt(tempFeature.releaseId) > 0) {
                     feature.setReleaseId(tempFeature.releaseId);
                 } else {
                     feature.setReleaseId(null);
                 }
                 feature.setLastModified(tempFeature.lastModified);
                 feature.setSolution("");
                 releaseFeatures.add(feature);//for feature
                 List<WorkItemStory> tempItemBacklog = tempFeature.storyList;//for story
                 releaseBacklogItems.addAll(this.parseBacklogItem(tempItemBacklog, workSpaceId));
             }
        }
        Map<String, WorkItemEpic> itemEpicMap = workItemRoot.epicList;
        if (itemEpicMap != null && itemEpicMap.size() > 0) {
            Set<String> keySetT = itemEpicMap.keySet();
            for (String keyT : keySetT) {
                WorkItemEpic tempEpic = itemEpicMap.get(keyT);
                AgileDataEpic epic = new AgileDataEpic();
                epic.setEpicId(tempEpic.id);
                epic.setName(tempEpic.name);
                epic.setAggFeatureStoryPoints(tempEpic.aggFeatureStoryPoints);
                epic.setPlannedStoryPoints(tempEpic.plannedStoryPoints);
                epic.setTotalStoryPoints(tempEpic.totalStoryPoints);
                epic.setAuthor(tempEpic.author);
                releaseEpics.add(epic);//for epic
                Map<String, WorkItemFeature> itemFeatureMap = tempEpic.featureList;
                if (itemFeatureMap != null && itemFeatureMap.size() > 0) {
                    Set<String> keySetF = itemFeatureMap.keySet();
                    for (String keyF : keySetF) {
                        WorkItemFeature tempFeature = itemFeatureMap.get(keyF);
                        AgileDataFeature feature = new AgileDataFeature();
                        feature.setFeatureId(tempFeature.id);
                        feature.setName(tempFeature.name);
                        feature.setStatus(tempFeature.status);
                        feature.setFeaturePoints(tempFeature.featurePoints);
                        feature.setAggStoryPoints(tempFeature.aggStoryPoints);
                        feature.setNumOfUserStories(tempFeature.numOfStories);
                        feature.setNumOfDefects(tempFeature.numbOfDefects);
                        feature.setEpicId(tempFeature.epicId);
                        if (tempFeature.releaseId != null && !"".equals(tempFeature.releaseId) && Integer.parseInt(tempFeature.releaseId) > 0) {
                            feature.setReleaseId(tempFeature.releaseId);
                        } else {
                            feature.setReleaseId(null);
                        }
                        feature.setLastModified(tempFeature.lastModified);
                        feature.setSolution("");
                        releaseFeatures.add(feature);//for feature
                        List<WorkItemStory> tempItemBacklog = tempFeature.storyList;//for story
                        releaseBacklogItems.addAll(this.parseBacklogItem(tempItemBacklog, workSpaceId));
                    }
                }
            }
        }
    }

    protected void SetUpReleaseTeams(ClientPublicAPI client, int sharedSpaceId, int workSpaceId) throws IOException {
        List<ReleaseTeam> releaseTeams = client.getReleaseTeams(sharedSpaceId, workSpaceId);
        if (releaseTeams != null && releaseTeams.size() > 0) {
            Iterator<ReleaseTeam> iterator = releaseTeams.iterator();
            while (iterator.hasNext()) {
                ReleaseTeam entity = (ReleaseTeam)iterator.next();
                AgileDataReleaseTeam tempReleaseReleaseTeam = new AgileDataReleaseTeam();
                tempReleaseReleaseTeam.setReleaseId(entity.releaseId);
                tempReleaseReleaseTeam.setTeamId(entity.teamId);
                tempReleaseReleaseTeam.setReleaseTeamId(entity.releaseTeamId);
                this.releaseReleaseTeams.add(tempReleaseReleaseTeam);
            }
        }
    }

    protected void SetUpReleases(ClientPublicAPI client, int sharedSpaceId, int workSpaceId) throws IOException {
        List<Release> releases = client.getReleases(sharedSpaceId, workSpaceId);
        if (releases != null && releases.size() > 0) {
            Iterator<Release> iterator = releases.iterator();
            while (iterator.hasNext()) {
                Release entity = (Release)iterator.next();
                AgileDataRelease tempRelease = new AgileDataRelease();
                tempRelease.setReleaseId(entity.id);
                tempRelease.setName(entity.name);
                this.releaseReleases.add(tempRelease);
            }
        }
    }
    
    protected void SetUpSprints(ClientPublicAPI client, int sharedSpaceId, int workSpaceId) throws IOException {
        List<Sprint> releases = client.getSprints(sharedSpaceId, workSpaceId);
        if (releases != null && releases.size() > 0) {
            Iterator<Sprint> iterator = releases.iterator();
            while (iterator.hasNext()) {
                Sprint entity = (Sprint)iterator.next();
                AgileDataSprint tempSprint = new AgileDataSprint();
                tempSprint.setSprintId(entity.id);
                tempSprint.setName(entity.name);
                tempSprint.setReleaseId(entity.releaseId);
                tempSprint.setStartDate(entity.sprintStart);
                tempSprint.setFinishDate(entity.sprintEnd);
                this.releaseSprints.add(tempSprint);
            }
        }
    }

    protected void SetUpTeams(ClientPublicAPI client, int sharedSpaceId, int workSpaceId) throws IOException {
        List<Team> teams = client.getTeams(sharedSpaceId, workSpaceId);
        if (teams != null && teams.size() > 0) {
            Iterator<Team> iterator = teams.iterator();
            while (iterator.hasNext()) {
                Team entity = (Team)iterator.next();
                com.ppm.integration.agilesdk.agiledata.AgileDataTeam tempTeam =
                        new com.ppm.integration.agilesdk.agiledata.AgileDataTeam();
                tempTeam.setTeamId(entity.id);
                tempTeam.setName(entity.name);
                tempTeam.setTeamLeader(entity.teamLeader);
                tempTeam.setMembersCapacity(entity.membersCapacity);
                tempTeam.setNumOfMembers(entity.numOfMembers);
                tempTeam.setEstimatedVelocity(entity.estimatedVelocity);
                this.releaseTeams.add(tempTeam);
            }
        }
    }

    @Override public AgileDataProject getProject(){
        return releaseProject;
    }
    @Override public List<AgileDataProgram> getPrograms(){
        return releasePrograms;
    }

    @Override public List<AgileDataBacklogItem> getBacklogItems() {
        return releaseBacklogItems;
    }

    @Override public List<AgileDataFeature> getFeatures() {
        return releaseFeatures;
    }

    @Override public List<AgileDataReleaseTeam> getReleaseTeams() {
        return releaseReleaseTeams;
    }

    @Override public List<AgileDataRelease> getReleases() {
        return releaseReleases;
    }

    @Override public List<AgileDataSprint> getSprints() {
        return releaseSprints;
    }

    @Override public List<com.ppm.integration.agilesdk.agiledata.AgileDataTeam> getTeams() {
        return releaseTeams;
    }

    @Override public List<AgileDataEpic> getEpics() {
        return releaseEpics;
    }

    @Override
    public List<AgileDataBacklogConfig> getAgileDataBacklogConfig(ValueSet configuration) {
        List<AgileDataBacklogConfig> list = new ArrayList<AgileDataBacklogConfig>();
        AgileDataBacklogConfig config1 = new AgileDataBacklogConfig();
        config1.setBacklogStatus("Done");
        config1.setColor("black");
        config1.setBacklogType("story");
        config1.setIsFinishStatus(true);
        list.add(config1);

        AgileDataBacklogConfig config2 = new AgileDataBacklogConfig();
        config2.setBacklogStatus("Closed");
        config2.setColor("grey");
        config2.setBacklogType("defect");
        config2.setIsFinishStatus(true);
        list.add(config2);

        AgileDataBacklogConfig config3 = new AgileDataBacklogConfig();
        config3.setBacklogStatus("Duplicate");
        config3.setColor("orange");
        config3.setBacklogType("defect");
        config3.setIsFinishStatus(true);
        list.add(config3);

        AgileDataBacklogConfig config4 = new AgileDataBacklogConfig();
        config4.setBacklogStatus("Rejected");
        config4.setColor("red");
        config4.setBacklogType("defect");
        config4.setIsFinishStatus(true);
        list.add(config4);

        AgileDataBacklogConfig config5 = new AgileDataBacklogConfig();
        config5.setBacklogStatus("Deferred");
        config5.setColor("purple");
        config5.setBacklogType("defect");
        config5.setIsFinishStatus(false);
        list.add(config5);

        AgileDataBacklogConfig config6 = new AgileDataBacklogConfig();
        config6.setBacklogStatus("New");
        config6.setColor("pink");
        config6.setBacklogType("defect");
        config6.setIsFinishStatus(false);
        list.add(config6);

        AgileDataBacklogConfig config7 = new AgileDataBacklogConfig();
        config7.setBacklogStatus("Opened");
        config7.setColor("blue");
        config7.setBacklogType("defect");
        config7.setIsFinishStatus(false);
        list.add(config7);

        AgileDataBacklogConfig config8 = new AgileDataBacklogConfig();
        config8.setBacklogStatus("Fixed");
        config8.setColor("yellow");
        config8.setBacklogType("defect");
        config8.setIsFinishStatus(false);
        list.add(config8);

        AgileDataBacklogConfig config9 = new AgileDataBacklogConfig();
        config9.setBacklogStatus("Proposed Closed");
        config9.setColor("green");
        config9.setBacklogType("defect");
        config9.setIsFinishStatus(false);
        list.add(config9);
        return list;
    }

    @Override
    public List<AgileDataBacklogSeverity> getAgileDataBacklogSeverity(ValueSet configuration) {
        List<AgileDataBacklogSeverity> list = new ArrayList<AgileDataBacklogSeverity>();
        AgileDataBacklogSeverity severity1 = new AgileDataBacklogSeverity();
        severity1.setBacklogType("defect");
        severity1.setSeverity("1");
        list.add(severity1);

        AgileDataBacklogSeverity severity2 = new AgileDataBacklogSeverity();
        severity2.setBacklogType("defect");
        severity2.setSeverity("2");
        list.add(severity2);

        AgileDataBacklogSeverity severity3 = new AgileDataBacklogSeverity();
        severity3.setBacklogType("defect");
        severity3.setSeverity("3");
        list.add(severity3);

        AgileDataBacklogSeverity severity4 = new AgileDataBacklogSeverity();
        severity4.setBacklogType("defect");
        severity4.setSeverity("4");
        list.add(severity4);

        AgileDataBacklogSeverity severity5 = new AgileDataBacklogSeverity();
        severity5.setBacklogType("defect");
        severity5.setSeverity("5");
        list.add(severity5);

        return list;
    }

    @Override
    /**
     * We store both Shared Workspace ID and Workspace ID in the Agile Project value, but we only use workspace ID as "project ID" for Agile Data integration.
     */
    public String getProjectIdFromAgileProjectValue(String agileProjectValue) {

        JSONObject value = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        return value.getString("WORKSPACE_ID");
    }



}