package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneEntityDropdown;
import com.ppm.integration.agilesdk.connector.octane.model.*;
import com.ppm.integration.agilesdk.connector.octane.model.workplan.*;
import com.ppm.integration.agilesdk.pm.LinkedTaskAgileEntityInfo;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalWorkPlan;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegration;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegrationContext;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.ui.*;

import java.util.*;

import org.apache.log4j.Logger;

public class OctaneWorkPlanIntegration extends WorkPlanIntegration implements FunctionIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    @Override public List<Field> getMappingConfigurationFields(WorkPlanIntegrationContext context, ValueSet values) {

        return Arrays.asList(new Field[] {
                new OctaneEntityDropdown(OctaneConstants.KEY_SHAREDSPACEID, "OCTANE_SHARESPACE", "block", true) {
                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] {OctaneConstants.KEY_BASE_URL, OctaneConstants.KEY_PROXY_HOST,
                                OctaneConstants.KEY_PROXY_PORT, OctaneConstants.APP_CLIENT_ID,
                                OctaneConstants.APP_CLIENT_SECRET,});
                    }

                    @Override public List<Option> fetchDynamicalOptions(ValueSet values) {
                        ClientPublicAPI client = ClientPublicAPI.getClient(values);
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
                        } catch (Exception e) {
                            logger.error("Error occured when getting Mapping config fields, returning null", e);
                        }
                        return null;
                    }
                },
                new OctaneEntityDropdown(OctaneConstants.KEY_WORKSPACEID, "OCTANE_WORKSPACE", "block", true) {
                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] {OctaneConstants.KEY_BASE_URL, OctaneConstants.APP_CLIENT_ID,
                                OctaneConstants.APP_CLIENT_SECRET, OctaneConstants.KEY_SHAREDSPACEID});
                    }

                    @Override public List<Option> fetchDynamicalOptions(ValueSet values) {

                        ClientPublicAPI client = ClientPublicAPI.getClient(values);
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
                        } catch (Exception e) {
                            logger.error("Error occured when getting Mapping config fields, returning null", e);
                        }
                        return null;

                    }
                },

                new LineBreaker(),
                new LineBreaker(),


                new DynamicDropdown(OctaneConstants.KEY_IMPORT_SELECTION, "IMPORT_SELECTION",
                        OctaneConstants.IMPORT_SELECTION_RELEASE, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(OctaneConstants.IMPORT_SELECTION_EPIC, "One Epic");
                        Option option2 = new Option(OctaneConstants.IMPORT_SELECTION_RELEASE, "One Release");

                        optionList.add(option1);
                        optionList.add(option2);

                        return optionList;
                    }

                },
                new DynamicDropdown(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS, "IMPORT_SELECTION_DETAILS", "", true) {

                    @Override
                    public List<String> getDependencies() {

                        return Arrays.asList(
                                new String[] {OctaneConstants.KEY_SHAREDSPACEID, OctaneConstants.KEY_WORKSPACEID, OctaneConstants.KEY_IMPORT_SELECTION});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        String importSelection = values.get(OctaneConstants.KEY_IMPORT_SELECTION);

                        ClientPublicAPI client = ClientPublicAPI.getClient(values);

                        List<Option> options = new ArrayList<>();
                        switch (importSelection) {
                            case OctaneConstants.IMPORT_SELECTION_EPIC:
                                List<EpicAttr> epics =
                                        client.getAllEpics();
                                options = new ArrayList<Option>(epics.size());
                                for (EpicAttr epic : epics) {
                                    options.add(new Option(epic.getId(), epic.getName()));
                                }
                                return options;
                            case OctaneConstants.IMPORT_SELECTION_RELEASE:
                                List<Release> releases =
                                        client.getAllReleases();
                                options = new ArrayList<Option>(releases.size());
                                for (Release r : releases) {
                                    options.add(new Option(r.id, r.name));
                                }
                                return options;
                        }
                        return options;
                    }
                },
                new LineBreaker(),
                new LineBreaker(),

                new DynamicDropdown(OctaneConstants.KEY_IMPORT_GROUPS, "IMPORT_GROUPS",
                        OctaneConstants.GROUP_RELEASE, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(OctaneConstants.GROUP_RELEASE, "Release / Sprint");
                        Option option2 = new Option(OctaneConstants.GROUP_BACKLOG_STRUCTURE, "Backlog / Epic / Feature");

                        optionList.add(option1);
                        optionList.add(option2);

                        return optionList;
                    }

                },
                new DynamicDropdown(OctaneConstants.KEY_PERCENT_COMPLETE, "PERCENT_COMPLETE_CHOICE",
                        OctaneConstants.PERCENT_COMPLETE_STORY_POINTS, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(OctaneConstants.PERCENT_COMPLETE_WORK, "% Work Complete");
                        Option option2 = new Option(OctaneConstants.PERCENT_COMPLETE_STORY_POINTS, "% Story Points Done");

                        optionList.add(option1);
                        optionList.add(option2);

                        return optionList;
                    }

                },
                new LineBreaker()
        });
    }

    @Override
    public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext wpiContext, ValueSet values) {

        ClientPublicAPI client = ClientPublicAPI.getClient(values);

        WorkplanContext wpContext = new WorkplanContext();

        wpContext.wpiContext = wpiContext;

        wpContext.percentComplete = values.get(OctaneConstants.KEY_PERCENT_COMPLETE);

        wpContext.phases = client.getAllPhases();

        wpContext.usersEmails = client.getAllWorkspaceUsers();


        // Get the backlog data. It's either one Epic or one Release

        final List<GenericWorkItem> workItems = new ArrayList<>();

        switch(values.get(OctaneConstants.KEY_IMPORT_SELECTION)) {

            case OctaneConstants.IMPORT_SELECTION_RELEASE:
                workItems.addAll(client.getReleaseWorkItems(Integer.parseInt(values.get(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS))));
                break;

            case OctaneConstants.IMPORT_SELECTION_EPIC:
                workItems.addAll(client.getEpicWorkItems(Integer.parseInt(values.get(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS))));
                break;
        }


        final List<ExternalTask> rootTasks = new ArrayList<>();

        List<Sprint> sprints = client.getAllSprints();

        wpContext.setSprints(sprints);

        switch(values.get(OctaneConstants.KEY_IMPORT_GROUPS)) {
            case OctaneConstants.GROUP_RELEASE:

                // Group by Release / Sprint / Type

                List<Release> releases = client.getAllReleases();

                Map<String,Release> releasesMap = new HashMap<>();

                for (Release release : releases) {
                    releasesMap.put(release.getId(), release);
                }

                Map<String,Sprint> sprintsMap = new HashMap<>();

                for (Sprint sprint : sprints) {
                    sprintsMap.put(sprint.getId(), sprint);
                }

                // We're building a data structure that will mimic work plan, minus work item types (we'll do that later).
                final Map<String, Map<String, List<GenericWorkItem>>> workItemsPerReleaseIdAndSprintId = new HashMap<>();

                for (GenericWorkItem wi : workItems) {
                    String releaseId = wi.getReleaseId();
                    String sprintId = wi.getSprintId();

                    Map<String, List<GenericWorkItem>> sprintWorkItems = workItemsPerReleaseIdAndSprintId.get(releaseId);

                    if (sprintWorkItems == null) {
                        sprintWorkItems = new HashMap<>();
                        workItemsPerReleaseIdAndSprintId.put(releaseId, sprintWorkItems);
                    }

                    List<GenericWorkItem> itemsInSprint = sprintWorkItems.get(sprintId);

                    if (itemsInSprint == null) {
                        itemsInSprint = new ArrayList<>();
                        sprintWorkItems.put(sprintId, itemsInSprint);
                    }

                    itemsInSprint.add(wi);
                }

                // First level is Release
                List <Release> sortedReleases = getSortedReleases(workItemsPerReleaseIdAndSprintId.keySet(), releasesMap);

                for (Release release : sortedReleases) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneReleaseExternalTask(release, workItemsPerReleaseIdAndSprintId.get(release.getId()), wpContext)));
                }

                break;
            case OctaneConstants.GROUP_BACKLOG_STRUCTURE:

                // Group by Backlog / Epic / Feature / Type

                // We need to first retrieve the missing Epics & Features, if any.
                Set<String> retrievedIds = new HashSet<>();
                for (GenericWorkItem wi : workItems) {
                    retrievedIds.add(wi.getId());
                }

                Set<String> missingIds = new HashSet<>();
                for (GenericWorkItem wi : workItems) {
                    if (!retrievedIds.contains(wi.getParentId()) && !wi.isInBacklog()) {
                        missingIds.add(wi.getParentId());
                    }
                }

                workItems.addAll(client.getWorkItemsByIds(missingIds));

                Map<String, List<GenericWorkItem>> epicsFeatures = new HashMap<>();

                Map<String, List<GenericWorkItem>> featuresItems = new HashMap<>();

                List<GenericWorkItem> featuresInBacklog = new ArrayList<>();

                List<GenericWorkItem> itemsInBacklog = new ArrayList<>();

                List<GenericWorkItem> epics = new ArrayList<>();

                for (GenericWorkItem wi : workItems) {
                    if (wi.isEpic()) {
                        epics.add(wi);
                    } else if (wi.isFeature()) {
                        if (wi.isInBacklog()) {
                            featuresInBacklog.add(wi);
                        } else {
                            List<GenericWorkItem> features = epicsFeatures.get(wi.getParentId());
                            if (features == null) {
                                features = new ArrayList<>();
                                epicsFeatures.put(wi.getParentId(), features);
                            }
                            features.add(wi);
                        }
                    } else {
                        // Backlog Item
                        if (wi.isInBacklog()) {
                            itemsInBacklog.add(wi);
                        } else {
                            List<GenericWorkItem> items = featuresItems.get(wi.getParentId());
                            if (items == null) {
                                items = new ArrayList<>();
                                featuresItems.put(wi.getParentId(), items);
                            }
                            items.add(wi);
                        }
                    }
                }

                // We always start with Backlog tasks
                if (!featuresInBacklog.isEmpty() || !itemsInBacklog.isEmpty()) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneRootBacklogExternalTask(featuresInBacklog, itemsInBacklog, featuresItems, wpContext)));
                }

                // Then the Epics / Features / Backlog Items hierarchy.

                OctaneUtils.sortWorkItemsByName(epics);

                for (GenericWorkItem epic : epics) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneEpicExternalTask(epic, epicsFeatures.get(epic.getId()), featuresItems, wpContext)));
                }

                break;
        }



        return new ExternalWorkPlan() {

            @Override
            public List<ExternalTask> getRootTasks() {
                return rootTasks;
            }
        };

    }

    private List<Release> getSortedReleases(Set<String> releaseIds, Map<String, Release> releasesMap) {
        List<Release> releases = new LinkedList<>();

        boolean needsToCreateNullRelease = false;

        for (String releaseId : releaseIds) {

            Release release = releasesMap.get(releaseId);

            if (release == null) {
                needsToCreateNullRelease = true;
            } else {
                releases.add(release);
            }
        }

        if (needsToCreateNullRelease) {
            Release nullRelease = new Release();
            nullRelease.setName(Providers.getLocalizationProvider(OctaneIntegrationConnector.class).getConnectorText("WORKPLAN_NO_RELEASE_DEFINED_TASK_NAME"));
            releases.add(0, nullRelease);
        }

        OctaneUtils.sortReleases(releases);

        return releases;
    }

    public LinkedTaskAgileEntityInfo getAgileEntityInfoFromMappingConfiguration(ValueSet values) {
        LinkedTaskAgileEntityInfo info = new LinkedTaskAgileEntityInfo();

        if (values != null) {

            info.setProjectId(values.get(OctaneConstants.KEY_WORKSPACEID));

            String importSelection = values.get(OctaneConstants.KEY_IMPORT_SELECTION);
            String importSelectionDetails = values.get(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS);
            switch (importSelection) {
                case OctaneConstants.IMPORT_SELECTION_EPIC:
                    info.setEpicId(importSelectionDetails);
                    break;
                case OctaneConstants.IMPORT_SELECTION_RELEASE:
                    info.setReleaseId(importSelectionDetails);
                    break;
            }
        }

        return info;
    }
}
