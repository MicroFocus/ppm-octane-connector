package com.ppm.integration.agilesdk.connector.octane;

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
import com.ppm.integration.agilesdk.release.ReleaseBacklogItem;
import com.ppm.integration.agilesdk.release.ReleaseFeature;
import com.ppm.integration.agilesdk.release.ReleaseIntegration;
import com.ppm.integration.agilesdk.release.ReleaseRelease;
import com.ppm.integration.agilesdk.release.ReleaseReleaseTeam;
import com.ppm.integration.agilesdk.release.ReleaseSprint;
import com.ppm.integration.agilesdk.release.ReleaseTheme;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.json.JSONException;

/**
 * Created by lutian on 2017/1/13.
 */
public class OctaneReleaseIntegration extends ReleaseIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    ClientPublicAPI client = null;

    String currentInstanceId = "";

    List<ReleaseBacklogItem> releaseBacklogItems = new LinkedList<>();

    List<ReleaseFeature> releaseFeatures = new LinkedList<>();

    List<ReleaseReleaseTeam> releaseReleaseTeams = new LinkedList<>();

    List<ReleaseRelease> releaseReleases = new LinkedList<>();

    List<ReleaseSprint> releaseSprints = new LinkedList<>();

    List<com.ppm.integration.agilesdk.release.ReleaseTeam> releaseTeams = new LinkedList<>();

    List<ReleaseTheme> releaseThemes = new LinkedList<>();

    Map<Integer, List<WorkSpace>> workSpacesWithSharedSpaceMap = null;

    protected ClientPublicAPI getClient(ValueSet paramValueSet) {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(paramValueSet);
        String clientId = paramValueSet.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = paramValueSet.get(OctaneConstants.APP_CLIENT_SECRET);
        currentInstanceId = paramValueSet.get(OctaneConstants.KEY_INSTANCE_ID);
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

    protected void SetUpSharedSpacesAndWorkSpaces(ValueSet paramValueSet) {
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
                    int workSpaceId = Integer.parseInt(strWorkSpaceId);
                    SetUpReleaseTeams(client, sharedSpaceId, workSpaceId);
                    SetUpReleases(client, sharedSpaceId, workSpaceId);
                    SetUpSprints(client, sharedSpaceId, workSpaceId);
                    SetUpTeams(client, sharedSpaceId, workSpaceId);
                    SetUpThemeFeatureBacklogItems(client, sharedSpaceId, workSpaceId);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private List<ReleaseBacklogItem> pareseBacklogItem(List<WorkItemStory> tempWorkItems) {

        List<ReleaseBacklogItem> backlogItems = new LinkedList<ReleaseBacklogItem>();
        for (int i = 0, size = tempWorkItems.size(); i < size; i++) {
            WorkItemStory tempWorkItem = tempWorkItems.get(i);
            ReleaseBacklogItem backlogItem = new ReleaseBacklogItem();
            backlogItem.setBacklogItemId(Integer.parseInt(tempWorkItem.id));
            if ("story".equals(tempWorkItem.subType)) {
                backlogItem.setBacklogType(ReleaseBacklogItem.BACKLOG_TYPE.USER_STOTY);
            } else {
                backlogItem.setBacklogType(ReleaseBacklogItem.BACKLOG_TYPE.DEFECT);
            }
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
            backlogItem.setInstanceId(currentInstanceId);
            backlogItems.add(backlogItem);
        }

        return backlogItems;
    }

    protected void SetUpThemeFeatureBacklogItems(ClientPublicAPI client, int sharedSpaceId, int workSpaceId)
            throws IOException
    {
        WorkItemRoot workItemRoot = client.getWorkItemRoot(sharedSpaceId, workSpaceId);
        List<WorkItemStory> itemBacklogs = workItemRoot.storyList;
        releaseBacklogItems.addAll(this.pareseBacklogItem(itemBacklogs));
        Map<String, WorkItemEpic> itemEpicMap = workItemRoot.epicList;
        if (itemEpicMap != null && itemEpicMap.size() > 0) {
            Set<String> keySetT = itemEpicMap.keySet();
            for (String keyT : keySetT) {
                WorkItemEpic tempEpic = itemEpicMap.get(keyT);
                ReleaseTheme theme = new ReleaseTheme();
                theme.setThemeId(Integer.parseInt(tempEpic.id));
                theme.setName(tempEpic.name);
                theme.setAggFeatureStoryPoints(tempEpic.aggFeatureStoryPoints);
                theme.setPlanedStoryPoints(tempEpic.planedStoryPoints);
                theme.setTotalStoryPoints(tempEpic.totalStoryPoints);
                theme.setAuthor(tempEpic.author);
                theme.setInstanceId(currentInstanceId);
                releaseThemes.add(theme);//for epic
                Map<String, WorkItemFeature> itemFeatureMap = tempEpic.featureList;
                if (itemFeatureMap != null && itemFeatureMap.size() > 0) {
                    Set<String> keySetF = itemFeatureMap.keySet();
                    for (String keyF : keySetF) {
                        WorkItemFeature tempFeature = itemFeatureMap.get(keyF);
                        ReleaseFeature feature = new ReleaseFeature();
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
                        feature.setInstanceId(currentInstanceId);
                        releaseFeatures.add(feature);//for feature
                        List<WorkItemStory> tempItemBacklog = tempFeature.storyList;//for story
                        releaseBacklogItems.addAll(this.pareseBacklogItem(itemBacklogs));
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
                ReleaseReleaseTeam tempReleaseReleaseTeam = new ReleaseReleaseTeam();
                tempReleaseReleaseTeam.setInstanceId(currentInstanceId);
                tempReleaseReleaseTeam.setReleaseId(Integer.parseInt(entity.releaseId));
                tempReleaseReleaseTeam.setTeamId(Integer.parseInt(entity.teamId));
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
                ReleaseRelease tempRelease = new ReleaseRelease();
                tempRelease.setInstanceId(currentInstanceId);
                tempRelease.setReleaseId(Integer.parseInt(entity.id));
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
                ReleaseSprint tempSprint = new ReleaseSprint();
                tempSprint.setInstanceId(currentInstanceId);
                tempSprint.setSprintId(Integer.parseInt(entity.id));
                tempSprint.setName(entity.name);
                tempSprint.setReleaseId(Integer.parseInt(entity.releaseId));
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
                com.ppm.integration.agilesdk.release.ReleaseTeam tempTeam =
                        new com.ppm.integration.agilesdk.release.ReleaseTeam();
                tempTeam.setTeamId(Integer.parseInt(entity.id));
                tempTeam.setName(entity.name);
                tempTeam.setTeamLeader(entity.teamLeader);
                tempTeam.setMembersCapacity(entity.membersCapacity);
                tempTeam.setNumOfMembers(entity.numOfMembers);
                tempTeam.setEstimatedVelocity(entity.estimatedVelocity);
                tempTeam.setInstanceId(currentInstanceId);
                this.releaseTeams.add(tempTeam);
            }
        }
    }

    @Override public List<ReleaseBacklogItem> getBacklogItems(ValueSet paramValueSet) {
        if (releaseBacklogItems.isEmpty()) {
            SetUpSharedSpacesAndWorkSpaces(paramValueSet);
        }
        return releaseBacklogItems;
    }

    @Override public List<ReleaseFeature> getFeatures(ValueSet paramValueSet) {

        if (releaseFeatures.isEmpty()) {
            SetUpSharedSpacesAndWorkSpaces(paramValueSet);
        }
        return releaseFeatures;
    }

    @Override public List<ReleaseReleaseTeam> getReleaseTeams(ValueSet paramValueSet) {

        if (releaseReleaseTeams.isEmpty()) {
            SetUpSharedSpacesAndWorkSpaces(paramValueSet);
        }
        return releaseReleaseTeams;
    }

    @Override public List<ReleaseRelease> getReleases(ValueSet paramValueSet) {

        if (releaseReleases.isEmpty()) {
            SetUpSharedSpacesAndWorkSpaces(paramValueSet);
        }
        return releaseReleases;
    }

    @Override public List<ReleaseSprint> getSprints(ValueSet paramValueSet) {

        if (releaseSprints.isEmpty()) {
            SetUpSharedSpacesAndWorkSpaces(paramValueSet);
        }
        return releaseSprints;
    }

    @Override public List<com.ppm.integration.agilesdk.release.ReleaseTeam> getTeams(ValueSet paramValueSet) {
        if (releaseTeams.isEmpty()) {
            SetUpSharedSpacesAndWorkSpaces(paramValueSet);
        }

        return releaseTeams;
    }

    @Override public List<ReleaseTheme> getThemes(ValueSet paramValueSet) {

        if (releaseThemes.isEmpty()) {
            SetUpSharedSpacesAndWorkSpaces(paramValueSet);
        }
        return releaseThemes;
    }

}
