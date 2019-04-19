package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneUtils;
import com.ppm.integration.agilesdk.pm.ExternalTask;

/**
 * This class is used when grouping items by backlog structure, to hold features without Epic or Backlog Items without Feature.
 */
public class OctaneRootBacklogExternalTask extends BaseOctaneExternalTask {

    private List<ExternalTask> children;

    private String parentId;

    private String backlogTaskName;

    public OctaneRootBacklogExternalTask(List<GenericWorkItem> featuresInBacklog, List<GenericWorkItem> itemsInBacklog,
            Map<String, List<GenericWorkItem>> featuresItems, WorkplanContext context, String parentId) {

        backlogTaskName = context.getLocalizationProvider().getConnectorText("WORKPLAN_BACKLOG_TASK_NAME");
        this.parentId = parentId;
        this.children = new ArrayList<>();

        // First are Features without items
        if (featuresInBacklog != null) {
            OctaneUtils.sortWorkItemsByName(featuresInBacklog);

            for (GenericWorkItem feature : featuresInBacklog) {
                children.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneFeatureExternalTask(feature, featuresItems.get(feature.getId()), context)));
            }
        }

        // Then are Backlog Items without Feature
        if (itemsInBacklog != null) {
            children.addAll(createBacklogItemsChildrenByType(itemsInBacklog, context, parentId));
        }
    }

    @Override
    public String getId() {
        return "WORKPLAN_BACKLOG_TASK_KEY" + this.parentId;
    }

    @Override
    public String getName() {
        return backlogTaskName;
    }

    @Override
    public Date getScheduledStart() {
        return adjustStartDateTime(getEarliestScheduledStart(getChildren()));
    }

    @Override
    public Date getScheduledFinish() {
        return adjustFinishDateTime(getLastestScheduledFinish(getChildren()));
    }

    @Override
    public List<ExternalTask> getChildren() {
        return children;
    }
}
