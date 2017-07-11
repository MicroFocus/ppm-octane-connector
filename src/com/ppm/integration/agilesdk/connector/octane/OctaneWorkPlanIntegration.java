package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.pm.LinkedTaskAgileEntityInfo;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemEpic;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemFeature;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemStory;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTask.TaskStatus;
import com.ppm.integration.agilesdk.pm.ExternalWorkPlan;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegration;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegrationContext;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.LineBreaker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.json.JSONException;

public class OctaneWorkPlanIntegration extends WorkPlanIntegration implements FunctionIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    protected ClientPublicAPI getClient(ValueSet values) {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);
        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        try {
            if (!client.getAccessTokenWithFormFormat(clientId, clientSecret)) {
                throw new OctaneClientException("AGM_APP", "error in access token retrieve.");
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (JSONException e) {
            logger.error(e.getMessage());
        }
        return client;
    }

    @Override public List<Field> getMappingConfigurationFields(WorkPlanIntegrationContext context, ValueSet values) {

        return Arrays.asList(new Field[] {
                new OctaneEntityDropdown(OctaneConstants.KEY_SHAREDSPACEID, "OCTANE_SHARESPACE", "block", true) {
                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] {OctaneConstants.KEY_BASE_URL, OctaneConstants.KEY_PROXY_HOST,
                                OctaneConstants.KEY_PROXY_PORT, OctaneConstants.APP_CLIENT_ID,
                                OctaneConstants.APP_CLIENT_SECRET,});
                    }

                    @Override public List<Option> fetchDynamicalOptions(ValueSet values) {
                        ClientPublicAPI client = getClient(values);
                        if (!values.isAllSet(OctaneConstants.APP_CLIENT_ID, OctaneConstants.APP_CLIENT_SECRET,
                                OctaneConstants.KEY_BASE_URL)) {
                            return null;
                        }
                        List<SharedSpace> sharedSpaces;
                        try {
                            sharedSpaces = client.getSharedSpaces();
                            List<Option> options = new ArrayList<Option>(sharedSpaces.size());
                            for (SharedSpace sd : sharedSpaces) {
                                options.add(new Option(sd.id, sd.name));
                            }
                            return options;
                        } catch (IOException e) {
                            logger.error(e.getMessage());
                        }
                        return null;
                    }
                }, new OctaneEntityDropdown(OctaneConstants.KEY_WORKSPACEID, "OCTANE_WORKSPACE", "block", true) {
            @Override public List<String> getDependencies() {
                return Arrays.asList(new String[] {OctaneConstants.KEY_BASE_URL, OctaneConstants.APP_CLIENT_ID,
                        OctaneConstants.APP_CLIENT_SECRET, OctaneConstants.KEY_SHAREDSPACEID});
            }

            @Override public List<Option> fetchDynamicalOptions(ValueSet values) {

                ClientPublicAPI client = getClient(values);
                if (!values.isAllSet(OctaneConstants.KEY_SHAREDSPACEID)) {
                    return null;
                }
                List<WorkSpace> workSpaces;
                try {
                    workSpaces = client.getWorkSpaces(Integer.parseInt(values.get(OctaneConstants.KEY_SHAREDSPACEID)));
                    List<Option> options = new ArrayList<Option>(workSpaces.size());
                    for (WorkSpace w : workSpaces) {
                        options.add(new Option(w.id, w.name));
                    }
                    return options;
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage());
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }//();
                return null;

            }
        },

                new OctaneEntityDropdown(OctaneConstants.KEY_RELEASEID, "RELEASE", "block", true) {

                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] {OctaneConstants.KEY_BASE_URL, OctaneConstants.APP_CLIENT_ID,
                                OctaneConstants.APP_CLIENT_SECRET, OctaneConstants.KEY_SHAREDSPACEID,
                                OctaneConstants.KEY_WORKSPACEID});
                    }

                    @Override public List<Option> fetchDynamicalOptions(ValueSet values) {

                        ClientPublicAPI client = getClient(values);
                        if (!values.isAllSet(OctaneConstants.KEY_SHAREDSPACEID, OctaneConstants.KEY_WORKSPACEID)) {
                            return null;
                        }
                        List<Release> releases;
                        try {
                            releases =
                                    client.getReleases(Integer.parseInt(values.get(OctaneConstants.KEY_SHAREDSPACEID)),
                                            Integer.parseInt((values.get(OctaneConstants.KEY_WORKSPACEID))));
                            List<Option> options = new ArrayList<Option>(releases.size());
                            for (Release r : releases) {
                                options.add(new Option(r.id, r.name));
                            }
                            return options;

                        } catch (NumberFormatException | IOException e) {
                            logger.error(e.getMessage());
                        }
                        return null;
                    }
                }, new LineBreaker(),});

    }

    public boolean linkTaskWithExternal(WorkPlanIntegrationContext context, ValueSet values) {
        return false;
    }

    @Override
    public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, ValueSet values) {
        final List<ExternalTask> sprintTasks = new ArrayList<>();
        ClientPublicAPI client = getClient(values);
        try {
            final Release release = client.getRelease(Integer.parseInt(values.get(OctaneConstants.KEY_SHAREDSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_WORKSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_RELEASEID)));
            if (release == null || release.id == null) {
                //release dont exist anymore
                return null;
            }
            List<Sprint> sprints = client.getSprints(Integer.parseInt(values.get(OctaneConstants.KEY_SHAREDSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_WORKSPACEID)));

            WorkItemRoot workItemRoot = client.getWorkItemRoot(Integer.parseInt(values.get(OctaneConstants.KEY_SHAREDSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_WORKSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_RELEASEID)));

            if (sprints != null && sprints.size() > 0) {
                for (Sprint spt: sprints) {
                    if(release.id.equals(spt.releaseId)) {
                        OctaneSprintIExternalTask octaneSprint =
                                createOctaneIExternalTask(release, spt);
                        //user story?
                        if(workItemRoot != null && workItemRoot.workItemStories.size() > 0) {
                            for(WorkItemStory us : workItemRoot.workItemStories) {
                                if(spt.id.equals(us.sprintId) && OctaneConstants.SUB_TYPE_STORY.equals(us.subType)) {
                                    OctaneUSIExternalTask octaneUS =
                                            createOctaneIExternalTask(release, us);
                                    octaneSprint.getChildren().add(octaneUS);
                                }
                            }
                            //sort User Story by Id
                            Collections.sort(octaneSprint.getChildren(), new Comparator<ExternalTask>() {
                                @Override public int compare(ExternalTask us1, ExternalTask us2)
                                {
                                    return us1.getId().compareTo(us2.getId());
                                }
                            });
                        }
                        sprintTasks.add(octaneSprint);
                    }
                }
            }

            return new ExternalWorkPlan() {
                @Override public List<ExternalTask> getRootTasks() {
                    if (sprintTasks.size() == 0) {
                        return new ArrayList<ExternalTask>();
                    }
                    return sprintTasks;
                }
            };

        } catch (NumberFormatException | IOException e) {
            logger.error(e.getMessage());
        }
        return null;

    }

    public boolean unlinkTaskWithExternal(WorkPlanIntegrationContext context, ValueSet values) {
        return false;
    }

    public OctaneSprintIExternalTask createOctaneIExternalTask(Release release, Sprint spt)
    {
        OctaneTaskData tempData = new OctaneTaskData();
        tempData.AddDataToFieldDict("id", spt.id);
        tempData.AddDataToFieldDict("name", spt.name);
        tempData.AddDataToFieldDict("subtype", spt.type);
        //TODO format to yyyy-MM-dd
        tempData.AddDataToFieldDict("releaseStartDate", release.startDate);
        tempData.AddDataToFieldDict("releaseEndDate", release.endDate);
        tempData.AddDataToFieldDict("sprintStartDate", spt.sprintStartDate);
        tempData.AddDataToFieldDict("sprintEndDate", spt.sprintEndDate);

        return new OctaneSprintIExternalTask(tempData);
    }

    public OctaneUSIExternalTask createOctaneIExternalTask(Release release, WorkItemStory tempStory)
    {
        OctaneTaskData tempStoryTask = new OctaneTaskData();
        tempStoryTask.AddDataToFieldDict("id", tempStory.id);
        tempStoryTask.AddDataToFieldDict("name", tempStory.name);
        tempStoryTask.AddDataToFieldDict("subType", tempStory.subType);
        tempStoryTask.AddDataToFieldDict("ownerId", tempStory.ownerId);
        tempStoryTask.AddDataToFieldDict("ownerName", tempStory.ownerName);
        tempStoryTask.AddDataToFieldDict("releaseId", tempStory.releaseId);
        //TODO format to yyyy-MM-dd
        tempStoryTask.AddDataToFieldDict("sprintStartDate", tempStory.sprintStartDate);
        tempStoryTask.AddDataToFieldDict("sprintEndDate", tempStory.sprintEndDate);
        tempStoryTask.AddDataToFieldDict("releaseStartDate", release.startDate);
        tempStoryTask.AddDataToFieldDict("releaseEndDate", release.endDate);
        tempStoryTask.AddDataToFieldDict("creationTime", tempStory.creationTime);
        tempStoryTask.AddDataToFieldDict("lastModifiedTime", tempStory.lastModifiedTime);

        tempStoryTask.AddDataToFieldDict("investedHours", String.valueOf(tempStory.investedHours));
        tempStoryTask.AddDataToFieldDict("remainingHours", String.valueOf(tempStory.remainingHours));
        tempStoryTask.AddDataToFieldDict("estimatedHours", String.valueOf(tempStory.estimatedHours));
        TaskStatus status = TaskStatus.READY;
        switch (tempStory.status) {
            case "New":
                status = TaskStatus.READY;
                break;
            case "In Progress":
            case "In Testing":
                status = TaskStatus.IN_PROGRESS;
                break;
            case "Done":
                status = TaskStatus.COMPLETED;
                break;
        }

        OctaneUSIExternalTask octaneStory = new OctaneUSIExternalTask(tempStoryTask, status);
        return octaneStory;
    }

    @Override public String getCustomDetailPage() {
        return null;
    }

    public LinkedTaskAgileEntityInfo getAgileEntityInfoFromMappingConfiguration(ValueSet values) {
        LinkedTaskAgileEntityInfo info = new LinkedTaskAgileEntityInfo();
        if (values != null) {
            String releaseId = (String)(values.get("releaseId"));
            String projectId = (String)(values.get("workSpaceId"));
            info.setReleaseId(releaseId);
            info.setProjectId(projectId);
        }
        return info;
    }
}
