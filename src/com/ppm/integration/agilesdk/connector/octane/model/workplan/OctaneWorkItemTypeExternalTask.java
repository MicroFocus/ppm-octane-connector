package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneUtils;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class groups together items of a same type (stories, defects, quality stories).
 */
public class OctaneWorkItemTypeExternalTask extends BaseOctaneExternalTask {

    private String type;

    private List<ExternalTask> children;

    private WorkplanContext context;

    public OctaneWorkItemTypeExternalTask(String type, List<GenericWorkItem> typeContent, WorkplanContext context) {
        this.type = type;
        this.context = context;

        // We always generate children, but we don't always return them as children tasks;
        // it depends whether we insert all items as tasks or not.
        this.children = createChildren(typeContent, context);
    }

    /**
     * This is the only OctaneExternalTask with this method, as whether to be a leaf task or a summary task depends on context,
     * and we need the work of children even when it's a leaf task.
     */
    public WorkDrivenPercentCompleteExternalTask toWorkDrivenPercentCompleteExternalTask() {
        if (context.showItemsAsTasks) {
            return WorkDrivenPercentCompleteExternalTask.forSummaryTask(this);
        } else {
            double workDone = 0;
            double workRemaining = 0;
            for (ExternalTask child : children) {
                WorkDrivenPercentCompleteExternalTask workChild = (WorkDrivenPercentCompleteExternalTask)child;
                workDone += workChild.getWorkDone();
                workRemaining += workChild.getWorkRemaining();
            }
            return WorkDrivenPercentCompleteExternalTask.forLeafTask(this, workDone, workRemaining);
        }
    }

    private List<ExternalTask> createChildren(List<GenericWorkItem> typeContent, WorkplanContext context) {
        List<ExternalTask> children = new ArrayList<>();

        OctaneUtils.sortWorkItemsByName(typeContent);

        if (typeContent != null) {
            for (GenericWorkItem wi : typeContent) {

                ExternalTask wiTask = new OctaneWorkItemExternalTask(wi, context);

                double workDone = 0;
                double workRemaining = 0;

                TaskStatus status = wiTask.getStatus();

                switch(context.percentComplete) {
                    case OctaneConstants.PERCENT_COMPLETE_WORK:
                        // Work based percent complete
                        workDone = wi.getInvestedHours();
                        workRemaining = wi.getRemainingHours();
                        break;
                    default:
                        // Story Points done percent complete
                        if (TaskStatus.COMPLETED.equals(status) || TaskStatus.CANCELLED.equals(status)) {
                            workDone = wi.getStoryPoints();
                        } else {
                            workRemaining = wi.getStoryPoints();
                        }

                        break;
                }

                children.add(WorkDrivenPercentCompleteExternalTask.forLeafTask(wiTask, workDone, workRemaining));
            }
        }

        return children;
    }

    @Override
    public String getName() {
        return context.getLocalizationProvider().getConnectorText("WORKPLAN_TYPE_NAME_" + type.toUpperCase());
    }

    @Override
    public Date getScheduledStart() {
        return getEarliestScheduledStart(getChildren());
    }

    @Override
    public Date getScheduledFinish() {
        return getLastestScheduledFinish(getChildren());
    }

    @Override
    public List<ExternalTask> getChildren() {
        return context.showItemsAsTasks ? children :  new ArrayList<ExternalTask>();
    }

    @Override
    public List<ExternalTaskActuals> getActuals() {
        if (context.showItemsAsTasks) {
            // Summary task;
            return new ArrayList<ExternalTaskActuals>();
        } else {
            // Leaf task, no child, return actuals of all children.
            List<ExternalTaskActuals> actuals = new ArrayList<ExternalTaskActuals>();
            for (ExternalTask child : children) {
                actuals.addAll(child.getActuals());
            }

            return actuals;
        }
    }
}
