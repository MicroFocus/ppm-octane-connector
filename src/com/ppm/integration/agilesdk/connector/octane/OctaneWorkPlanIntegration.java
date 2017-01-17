package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
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
                return null;
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
                new OctaneEntityDropdown(OctaneConstants.KEY_SHAREDSPACEID, "SHAREDSPACE", "block", true) {
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
                }, new OctaneEntityDropdown(OctaneConstants.KEY_WORKSPACEID, "WORKSPACE", "block", true) {
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

    public OctaneIExternalTask createOctaneIExternalTaskWithAttribute(String id, String name, String type) {
        OctaneTaskData tempData = new OctaneTaskData();
        tempData.AddDataToFieldDict("id", id);
        tempData.AddDataToFieldDict("name", name);
        tempData.AddDataToFieldDict("subtype", type);
        return new OctaneIExternalTask(tempData);
    }

    public OctaneIExternalTask createOctaneIExternalTaskWithAttributeAndReleaseData(String id, String name, String type,
            Release releaseIdDate)
    {
        OctaneTaskData tempData = new OctaneTaskData();
        tempData.AddDataToFieldDict("id", id);
        tempData.AddDataToFieldDict("name", name);
        tempData.AddDataToFieldDict("subtype", type);
        tempData.AddDataToFieldDict("releaseStartDate", releaseIdDate.startDate);
        tempData.AddDataToFieldDict("releaseEndDate", releaseIdDate.endDate);
        return new OctaneIExternalTask(tempData);
    }

    @Override public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, ValueSet values) {

        ClientPublicAPI client = getClient(values);
        WorkItemRoot itemRoot;
        Release releaseIdDate;
        try {
            itemRoot = client.getWorkItemRoot(Integer.parseInt(values.get(OctaneConstants.KEY_SHAREDSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_WORKSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_RELEASEID)));

            releaseIdDate = client.getRelease(Integer.parseInt(values.get(OctaneConstants.KEY_SHAREDSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_WORKSPACEID)),
                    Integer.parseInt(values.get(OctaneConstants.KEY_RELEASEID)));

            final List<ExternalWorkPlan> rootList = new ArrayList<ExternalWorkPlan>();
            //if US is exits in backlog, create OctaneIExternalTask backlog and add US to it
            if (itemRoot.storyList.size() > 0) {
                OctaneIExternalTask octaneWorkPlan =
                        createOctaneIExternalTaskWithAttribute(itemRoot.id, itemRoot.name, itemRoot.type);

                //rootList.add(getOctaneIExternalTask(octaneWorkPlan,itemRoot.storyList,releaseIdDate));
            }

            //get us in feature
            Set<String> keyEpicSet = itemRoot.epicList.keySet();
            for (String keyEpic : keyEpicSet) {
                WorkItemEpic tempEpic = itemRoot.epicList.get(keyEpic);// one epic
                Set<String> keyFeatureSet = tempEpic.featureList.keySet();
                if (keyFeatureSet.size() == 0) {
                    //null epic
                    continue;
                }
                //if there is feature in epic, new an epicOctaneIExternalTask

                OctaneIExternalTask epicOctaneIExternalTask =
                        createOctaneIExternalTaskWithAttribute(tempEpic.id, tempEpic.name, tempEpic.subType);
                for (String keyFeature : keyFeatureSet) {
                    WorkItemFeature tempFeature = tempEpic.featureList.get(keyFeature);// one feature

                    OctaneIExternalTask
                            tempFeatureTask;//=createOctaneIExternalTaskWithAttribute(tempFeature.id, tempFeature.name, tempFeature.subType);
                    if (tempFeature.storyList.size() == 0) {
                        //the feature has no story
                        tempFeatureTask =
                                createOctaneIExternalTaskWithAttributeAndReleaseData(tempFeature.id, tempFeature.name,
                                        tempFeature.subType, releaseIdDate);
                        epicOctaneIExternalTask.octaneIExternaltask.add(tempFeatureTask);
                        continue;
                    }
                    //if exits US in feature, add all us to tempFeatureTask,
                    //and add tempFeatureTask to epicOctaneIExternalTask' child
                    tempFeatureTask = createOctaneIExternalTaskWithAttribute(tempFeature.id, tempFeature.name,
                            tempFeature.subType);
                    epicOctaneIExternalTask.octaneIExternaltask
                            .add(getOctaneIExternalTask(tempFeatureTask, tempFeature.storyList, releaseIdDate));
                }
                //rootList.add(epicOctaneIExternalTask);
            }
            //

            return new ExternalWorkPlan() {
                @Override public List<ExternalTask> getRootTasks() {
                    if (rootList.size() == 0) {
                        return null;
                    }
                    //return rootList;
                    return null;
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

    public OctaneIExternalTask getOctaneIExternalTask(OctaneIExternalTask octaneWorkPlan, List<WorkItemStory> storyList,
            Release releaseIdDate)
    {
        int storySize = storyList.size();
        if (storySize == 0) {
            return octaneWorkPlan;
        }
        Iterator<WorkItemStory> iterator = storyList.iterator();
        while (iterator.hasNext()) {
            WorkItemStory tempStory = iterator.next();
            OctaneTaskData tempStoryTask = new OctaneTaskData();
            tempStoryTask.AddDataToFieldDict("id", tempStory.id);
            tempStoryTask.AddDataToFieldDict("name", tempStory.name);
            tempStoryTask.AddDataToFieldDict("subType", tempStory.subType);
            tempStoryTask.AddDataToFieldDict("ownerId", tempStory.ownerId);
            tempStoryTask.AddDataToFieldDict("ownerName", tempStory.ownerName);
            tempStoryTask.AddDataToFieldDict("releaseId", tempStory.releaseId);
            tempStoryTask.AddDataToFieldDict("sprintStartDate", tempStory.sprintStartDate);
            tempStoryTask.AddDataToFieldDict("sprintEndDate", tempStory.sprintEndDate);
            tempStoryTask.AddDataToFieldDict("releaseStartDate", releaseIdDate.startDate);
            tempStoryTask.AddDataToFieldDict("releaseEndDate", releaseIdDate.endDate);
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
            OctaneIExternalTask octaneStory = new OctaneIExternalTask(tempStoryTask, status);
            octaneWorkPlan.octaneIExternaltask.add(octaneStory);
        }
        return octaneWorkPlan;
    }

    @Override public String getCustomDetailPage() {
        return "/itg/integrationcenter/agm-connector-impl-web/agm-graphs.jsp";
    }

}
