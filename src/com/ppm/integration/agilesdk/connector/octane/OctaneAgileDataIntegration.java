package com.ppm.integration.agilesdk.connector.octane;

import com.hp.ppm.integration.model.Workspace;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.model.ReleaseTeam;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.connector.octane.model.Team;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemEpic;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemFeature;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemStory;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.agiledata.AgileDataBacklogItem;
import com.ppm.integration.agilesdk.agiledata.AgileDataFeature;
import com.ppm.integration.agilesdk.agiledata.AgileDataIntegration;
import com.ppm.integration.agilesdk.agiledata.AgileDataProgram;
import com.ppm.integration.agilesdk.agiledata.AgileDataProgramProjectMapping;
import com.ppm.integration.agilesdk.agiledata.AgileDataProject;
import com.ppm.integration.agilesdk.agiledata.AgileDataRelease;
import com.ppm.integration.agilesdk.agiledata.AgileDataReleaseTeam;
import com.ppm.integration.agilesdk.agiledata.AgileDataSprint;
import com.ppm.integration.agilesdk.agiledata.AgileDataTheme;

import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by lutian on 2017/1/13.
 */
public class OctaneAgileDataIntegration extends AgileDataIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    ClientPublicAPI client = null;
    
    List<AgileDataProject> releaseProjects = null;
    
    List<AgileDataProgram> releasePrograms = null;
    
    List<AgileDataProgramProjectMapping> releaseProgramProjectMappings = null;

    List<AgileDataBacklogItem> releaseBacklogItems = null;

    List<AgileDataFeature> releaseFeatures = null;

    List<AgileDataReleaseTeam> releaseReleaseTeams = null;

    List<AgileDataRelease> releaseReleases = null;

    List<AgileDataSprint> releaseSprints = null;

    List<com.ppm.integration.agilesdk.agiledata.AgileDataTeam> releaseTeams = null;

    List<AgileDataTheme> releaseThemes = null;

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
    public void setUp(final Workspace wp, ValueSet paramValueSet) {
        releaseProjects = new LinkedList<>();
        releasePrograms = new LinkedList<>();
        releaseProgramProjectMappings = new LinkedList<>();
        releaseBacklogItems = new LinkedList<>();
        releaseFeatures = new LinkedList<>();
        releaseReleaseTeams = new LinkedList<>();
        releaseReleases = new LinkedList<>();
        releaseSprints = new LinkedList<>();
        releaseTeams = new LinkedList<>();
        releaseThemes = new LinkedList<>();

        Map<Integer, List<WorkSpace>> workSpacesWithSharedSpaceMap = null;
        try {
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
                    if(!paramValueSet.get(OctaneConstants.PROJECT_ID).equals(strWorkSpaceId)){
                    	continue;
                    }
                    int workSpaceId = Integer.parseInt(strWorkSpaceId);
                    AgileDataProject tempProject = new AgileDataProject();;
                    tempProject.setInstanceId(wp.getId());
                    tempProject.setProjectId(workSpaceId);
                    tempProject.setName(workSpaces.get(j).name);
                    this.releaseProjects.add(tempProject);
                    
                    AgileDataProgram tempProgram = new AgileDataProgram();;
                    tempProgram.setInstanceId(wp.getId());
                    tempProgram.setProgramId(sharedSpaceId);
                    tempProgram.setName(sharedSpacesList.get(i).name);
                    this.releasePrograms.add(tempProgram);
                    
                    SetUpProgramAndProject(wp,releaseProjects,releasePrograms);
                    SetUpReleaseTeams(wp, client, sharedSpaceId, workSpaceId);
                    SetUpReleases(wp, client, sharedSpaceId, workSpaceId);
                    SetUpSprints(wp, client, sharedSpaceId, workSpaceId);
                    SetUpTeams(wp, client, sharedSpaceId, workSpaceId);
                    SetUpThemeFeatureBacklogItems(wp, client, sharedSpaceId, workSpaceId);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    //defect or us
    private List<AgileDataBacklogItem> pareseBacklogItem(final Workspace wp, List<WorkItemStory> tempWorkItems, int workSpaceId) {

        List<AgileDataBacklogItem> backlogItems = new LinkedList<AgileDataBacklogItem>();
        for (int i = 0, size = tempWorkItems.size(); i < size; i++) {
            WorkItemStory tempWorkItem = tempWorkItems.get(i);
            AgileDataBacklogItem backlogItem = new AgileDataBacklogItem();
            backlogItem.setBacklogItemId(Integer.parseInt(tempWorkItem.id));
            if ("story".equals(tempWorkItem.subType)) {
                backlogItem.setBacklogType(AgileDataBacklogItem.BACKLOG_TYPE.USER_STORY);
            } else {
                backlogItem.setBacklogType(AgileDataBacklogItem.BACKLOG_TYPE.DEFECT);
            }
            backlogItem.setRank(0);
            backlogItem.setName(tempWorkItem.name);
            backlogItem.setAssignTo(tempWorkItem.ownerName);
            backlogItem.setStoryPoints(tempWorkItem.storyPoints);

            backlogItem.setStatus(AgileDataBacklogItem.BACKLOG_OR_DEFECT_STATUS.fromTypeName(tempWorkItem.status));

            if (!tempWorkItem.releaseId.equals("") && Integer.parseInt(tempWorkItem.releaseId) > 0) {
                backlogItem.setReleaseId(Integer.parseInt(tempWorkItem.releaseId));
            } else {
                backlogItem.setReleaseId(-1);
            }
            if (tempWorkItem.teamId > 0) {
                backlogItem.setTeamId((int)tempWorkItem.teamId);
            } else {
                backlogItem.setTeamId(-1);
            }
            if (tempWorkItem.themeId > 0) {
                backlogItem.setThemeId((int)tempWorkItem.themeId);
            } else {
                backlogItem.setThemeId(-1);
            }
            if (tempWorkItem.featureId > 0) {
                backlogItem.setFeatureId((int)tempWorkItem.featureId);
            } else {
                backlogItem.setFeatureId(-1);
            }
            if (tempWorkItem.sprintId > 0) {
                backlogItem.setSprintId((int)tempWorkItem.sprintId);
            } else {
                backlogItem.setSprintId(-1);
            }

            backlogItem.setAuthor(tempWorkItem.ownerName);
            backlogItem.setPriority(AgileDataBacklogItem.BACKLOG_PRIORITY.fromTypeName(tempWorkItem.priority));
            backlogItem.setLastModified(tempWorkItem.lastModifiedTime);
            backlogItem.setNumberOfTasks(0);
            backlogItem.setDefectStatus(AgileDataBacklogItem.BACKLOG_OR_DEFECT_STATUS.fromTypeName(tempWorkItem.defectStatus));
            backlogItem.setInstanceId(wp.getId());
            backlogItem.setProjectId(workSpaceId);
            backlogItems.add(backlogItem);
        }

        return backlogItems;
    }

    protected void SetUpThemeFeatureBacklogItems(final Workspace wp, ClientPublicAPI client, int sharedSpaceId, int workSpaceId)
            throws IOException
    {
        WorkItemRoot workItemRoot = client.getWorkItemRoot(sharedSpaceId, workSpaceId);
        List<WorkItemStory> itemBacklogs = workItemRoot.storyList;
        releaseBacklogItems.addAll(this.pareseBacklogItem(wp, itemBacklogs, workSpaceId));
        Map<String, WorkItemFeature> itemFeatureRootMap=workItemRoot.featureList;
        if(itemFeatureRootMap != null && itemFeatureRootMap.size() > 0) {
        	 Set<String> keySetF = itemFeatureRootMap.keySet();
             for (String keyF : keySetF) {
                 WorkItemFeature tempFeature = itemFeatureRootMap.get(keyF);
                 AgileDataFeature feature = new AgileDataFeature();
                 feature.setFeatureId(Integer.parseInt(tempFeature.id));
                 feature.setName(tempFeature.name);
                 feature.setStatus(AgileDataFeature.STATUS.fromTypeName(tempFeature.status));
                 feature.setFeaturePoints(tempFeature.featurePoints);
                 feature.setAggStoryPoints(tempFeature.aggStoryPoints);
                 feature.setNumOfUserStories(tempFeature.numOfStories);
                 feature.setNumOfDefects(tempFeature.numbOfDefects);
                 feature.setThemeId((int)tempFeature.themeId);
                 if (!tempFeature.releaseId.equals("") && Integer.parseInt(tempFeature.releaseId) > 0) {
                     feature.setReleaseId(Integer.parseInt(tempFeature.releaseId));
                 } else {
                     feature.setReleaseId(-1);
                 }
                 feature.setLastModified(tempFeature.lastModified);
                 feature.setSolution("");
                 feature.setInstanceId(wp.getId());
                 feature.setProjectId(workSpaceId);
                 releaseFeatures.add(feature);//for feature
                 List<WorkItemStory> tempItemBacklog = tempFeature.storyList;//for story
                 releaseBacklogItems.addAll(this.pareseBacklogItem(wp, tempItemBacklog, workSpaceId));
             }
        }
        Map<String, WorkItemEpic> itemEpicMap = workItemRoot.epicList;
        if (itemEpicMap != null && itemEpicMap.size() > 0) {
            Set<String> keySetT = itemEpicMap.keySet();
            for (String keyT : keySetT) {
                WorkItemEpic tempEpic = itemEpicMap.get(keyT);
                AgileDataTheme theme = new AgileDataTheme();
                theme.setThemeId(Integer.parseInt(tempEpic.id));
                theme.setName(tempEpic.name);
                theme.setAggFeatureStoryPoints(tempEpic.aggFeatureStoryPoints);
                theme.setPlanedStoryPoints(tempEpic.planedStoryPoints);
                theme.setTotalStoryPoints(tempEpic.totalStoryPoints);
                theme.setAuthor(tempEpic.author);
                theme.setInstanceId(wp.getId());
                theme.setProjectId(workSpaceId);
                releaseThemes.add(theme);//for epic
                Map<String, WorkItemFeature> itemFeatureMap = tempEpic.featureList;
                if (itemFeatureMap != null && itemFeatureMap.size() > 0) {
                    Set<String> keySetF = itemFeatureMap.keySet();
                    for (String keyF : keySetF) {
                        WorkItemFeature tempFeature = itemFeatureMap.get(keyF);
                        AgileDataFeature feature = new AgileDataFeature();
                        feature.setFeatureId(Integer.parseInt(tempFeature.id));
                        feature.setName(tempFeature.name);
                        feature.setStatus(AgileDataFeature.STATUS.fromTypeName(tempFeature.status));
                        feature.setFeaturePoints(tempFeature.featurePoints);
                        feature.setAggStoryPoints(tempFeature.aggStoryPoints);
                        feature.setNumOfUserStories(tempFeature.numOfStories);
                        feature.setNumOfDefects(tempFeature.numbOfDefects);
                        feature.setThemeId((int)tempFeature.themeId);
                        if (!tempFeature.releaseId.equals("") && Integer.parseInt(tempFeature.releaseId) > 0) {
                            feature.setReleaseId(Integer.parseInt(tempFeature.releaseId));
                        } else {
                            feature.setReleaseId(-1);
                        }
                        feature.setLastModified(tempFeature.lastModified);
                        feature.setSolution("");
                        feature.setInstanceId(wp.getId());
                        feature.setProjectId(workSpaceId);
                        releaseFeatures.add(feature);//for feature
                        List<WorkItemStory> tempItemBacklog = tempFeature.storyList;//for story
                        releaseBacklogItems.addAll(this.pareseBacklogItem(wp, tempItemBacklog, workSpaceId));
                    }
                }
            }
        }
    }

    protected void SetUpReleaseTeams(final Workspace wp, ClientPublicAPI client, int sharedSpaceId, int workSpaceId) throws IOException {
        List<ReleaseTeam> releaseTeams = client.getReleaseTeams(sharedSpaceId, workSpaceId);
        if (releaseTeams != null && releaseTeams.size() > 0) {
            Iterator<ReleaseTeam> iterator = releaseTeams.iterator();
            while (iterator.hasNext()) {
                ReleaseTeam entity = (ReleaseTeam)iterator.next();
                AgileDataReleaseTeam tempReleaseReleaseTeam = new AgileDataReleaseTeam();
                tempReleaseReleaseTeam.setInstanceId(wp.getId());
                tempReleaseReleaseTeam.setReleaseId(Integer.parseInt(entity.releaseId));
                tempReleaseReleaseTeam.setTeamId(Integer.parseInt(entity.teamId));
                tempReleaseReleaseTeam.setReleaseTeamId(entity.releaseTeamId);
                tempReleaseReleaseTeam.setProjectId(workSpaceId);
                this.releaseReleaseTeams.add(tempReleaseReleaseTeam);
            }
        }
    }

    protected void SetUpReleases(final Workspace wp, ClientPublicAPI client, int sharedSpaceId, int workSpaceId) throws IOException {
        List<Release> releases = client.getReleases(sharedSpaceId, workSpaceId);
        if (releases != null && releases.size() > 0) {
            Iterator<Release> iterator = releases.iterator();
            while (iterator.hasNext()) {
                Release entity = (Release)iterator.next();
                AgileDataRelease tempRelease = new AgileDataRelease();
                tempRelease.setInstanceId(wp.getId());
                tempRelease.setReleaseId(Integer.parseInt(entity.id));
                tempRelease.setName(entity.name);
                tempRelease.setProjectId(workSpaceId);
                this.releaseReleases.add(tempRelease);
            }
        }
    }
    
    protected void SetUpProgramAndProject(final Workspace wp,List<AgileDataProject> releaseProjects,  List<AgileDataProgram> releasePrograms) throws IOException {
    	for(AgileDataProject entityProject : releaseProjects){
    		for(AgileDataProgram entityProgram : releasePrograms){
    			AgileDataProgramProjectMapping tempMapping = new AgileDataProgramProjectMapping();
                tempMapping.setInstanceId(wp.getId());
                tempMapping.setProjectId(entityProject.getProjectId());
                tempMapping.setProgramId(entityProgram.getProgramId());
                this.releaseProgramProjectMappings.add(tempMapping);
    		}
    	}
    }

    protected void SetUpSprints(final Workspace wp, ClientPublicAPI client, int sharedSpaceId, int workSpaceId) throws IOException {
        List<Sprint> releases = client.getSprints(sharedSpaceId, workSpaceId);
        if (releases != null && releases.size() > 0) {
            Iterator<Sprint> iterator = releases.iterator();
            while (iterator.hasNext()) {
                Sprint entity = (Sprint)iterator.next();
                AgileDataSprint tempSprint = new AgileDataSprint();
                tempSprint.setInstanceId(wp.getId());
                tempSprint.setSprintId(Integer.parseInt(entity.id));
                tempSprint.setName(entity.name);
                tempSprint.setReleaseId(Integer.parseInt(entity.releaseId));
                tempSprint.setProjectId(workSpaceId);
                this.releaseSprints.add(tempSprint);
            }
        }
    }

    protected void SetUpTeams(final Workspace wp, ClientPublicAPI client, int sharedSpaceId, int workSpaceId) throws IOException {
        List<Team> teams = client.getTeams(sharedSpaceId, workSpaceId);
        if (teams != null && teams.size() > 0) {
            Iterator<Team> iterator = teams.iterator();
            while (iterator.hasNext()) {
                Team entity = (Team)iterator.next();
                com.ppm.integration.agilesdk.agiledata.AgileDataTeam tempTeam =
                        new com.ppm.integration.agilesdk.agiledata.AgileDataTeam();
                tempTeam.setTeamId(Integer.parseInt(entity.id));
                tempTeam.setName(entity.name);
                tempTeam.setTeamLeader(entity.teamLeader);
                tempTeam.setMembersCapacity(entity.membersCapacity);
                tempTeam.setNumOfMembers(entity.numOfMembers);
                tempTeam.setEstimatedVelocity(entity.estimatedVelocity);
                tempTeam.setInstanceId(wp.getId());
                tempTeam.setProjectId(workSpaceId);
                this.releaseTeams.add(tempTeam);
            }
        }
    }

    @Override public List<AgileDataProject> getProjects(final Workspace wp, final ValueSet paramValueSet){
        return releaseProjects;
    }
    @Override public List<AgileDataProgram> getPrograms(final Workspace wp, final ValueSet paramValueSet){
        return releasePrograms;
    }
    @Override public List<AgileDataProgramProjectMapping> getProgramProjectMappings(final Workspace wp, final ValueSet paramValueSet){
        return releaseProgramProjectMappings;
    }

    @Override public List<AgileDataBacklogItem> getBacklogItems(final Workspace wp, ValueSet paramValueSet) {
        return releaseBacklogItems;
    }

    @Override public List<AgileDataFeature> getFeatures(final Workspace wp, ValueSet paramValueSet) {
        return releaseFeatures;
    }

    @Override public List<AgileDataReleaseTeam> getReleaseTeams(final Workspace wp, ValueSet paramValueSet) {
        return releaseReleaseTeams;
    }

    @Override public List<AgileDataRelease> getReleases(final Workspace wp, ValueSet paramValueSet) {
        return releaseReleases;
    }

    @Override public List<AgileDataSprint> getSprints(final Workspace wp, ValueSet paramValueSet) {
        return releaseSprints;
    }

    @Override public List<com.ppm.integration.agilesdk.agiledata.AgileDataTeam> getTeams(final Workspace wp, ValueSet paramValueSet) {
        return releaseTeams;
    }

    @Override public List<AgileDataTheme> getThemes(final Workspace wp, ValueSet paramValueSet) {
        return releaseThemes;
    }

}