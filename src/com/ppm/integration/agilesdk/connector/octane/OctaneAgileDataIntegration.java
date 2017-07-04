package com.ppm.integration.agilesdk.connector.octane;

import com.hp.ppm.integration.model.Workspace;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.*;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.*;
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
            backlogItem.setBacklogType(tempWorkItem.subType);

            backlogItem.setRank(0);
            backlogItem.setName(tempWorkItem.name);
            backlogItem.setAssignTo(tempWorkItem.ownerName);
            backlogItem.setStoryPoints(tempWorkItem.storyPoints);

            backlogItem.setStatus(tempWorkItem.status);

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
            backlogItem.setPriority(tempWorkItem.priority);
            backlogItem.setLastModified(tempWorkItem.lastModifiedTime);
            backlogItem.setNumberOfTasks(0);
            backlogItem.setDefectStatus(tempWorkItem.defectStatus);
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
                 feature.setStatus(tempFeature.status);
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
                        feature.setStatus(tempFeature.status);
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

    public List<AgileDataBacklogConfig> getAgileDataBacklogConfig() {
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

    public List<AgileDataBacklogSeverity> getAgileDataBacklogSeverity() {
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

        return list;
    }

}