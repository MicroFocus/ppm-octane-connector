package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneUtils;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.pm.ExternalTask;

import java.util.*;

/**
 * Created by canaud on 9/9/2017.
 */
public class OctaneReleaseExternalTask extends BaseOctaneExternalTask {

    private Release release;

    private List<ExternalTask> children;

    Map<String, List<GenericWorkItem>> sprintWorkItems;

    private WorkplanContext context;

    public OctaneReleaseExternalTask(Release release, Map<String, List<GenericWorkItem>> sprintWorkItems, WorkplanContext context) {
        this.release = release;
        this.children = createChildren(release, sprintWorkItems, context);
        this.context = context;
    }

    private List<ExternalTask> createChildren(Release release, Map<String, List<GenericWorkItem>> sprintWorkItems, WorkplanContext context) {
        List<ExternalTask> children = new ArrayList<>();

        if (sprintWorkItems != null) {

            List<Sprint> releaseSprints = new ArrayList<>();

            if (release != null && release.getId() != null) {
                releaseSprints = OctaneUtils.getReleaseSprints(release.getId(), context.getSprints());
            }

            for (Sprint sprint : releaseSprints) {
                children.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneSprintExternalTask(sprint, sprintWorkItems.get(sprint.getId()), context)));
            }

            List<GenericWorkItem> noSprintWorkItems = sprintWorkItems.get(null);

            if (noSprintWorkItems != null && !noSprintWorkItems.isEmpty()) {
                Sprint noSprint = new Sprint();
                noSprint.sprintStart = release == null ?  null : release.startDatetime;
                noSprint.sprintEnd = release == null ?  null : release.endDatetime;
                noSprint.id = null;
                noSprint.name = context.getLocalizationProvider().getConnectorText("WORPLAN_NO_SPRINT_TASK_NAME");

                children.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneSprintExternalTask(noSprint, noSprintWorkItems, context)));
            }
        }

        return children;
    }

    @Override
    public String getName() {
        return release.getName();
    }

    @Override
    public Date getScheduledStart() {
        return adjustStartDateTime(this.release.startDatetime != null ? this.release.startDatetime : getEarliestScheduledStart(getChildren()));
    }

    @Override
    public Date getScheduledFinish() {
        return adjustFinishDateTime(this.release.endDatetime != null ?  this.release.endDatetime : getLastestScheduledFinish(getChildren()));
    }

    @Override
    public List<ExternalTask> getChildren() {
        return children;
    }
}
