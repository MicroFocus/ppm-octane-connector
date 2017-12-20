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

    private double targetPercentComplete = 0.0d;

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

        WorkDrivenPercentCompleteExternalTask workDrivenTask = null;

        if (context.showItemsAsTasks) {
            workDrivenTask = WorkDrivenPercentCompleteExternalTask.forSummaryTask(this);
        } else {
            double workDone = 0;
            double workRemaining = 0;
            for (ExternalTask child : children) {
                WorkDrivenPercentCompleteExternalTask workChild = (WorkDrivenPercentCompleteExternalTask)child;
                workDone += workChild.getWorkDone();
                workRemaining += workChild.getWorkRemaining();
            }
            workDrivenTask = WorkDrivenPercentCompleteExternalTask.forLeafTask(this, workDone, workRemaining);
        }

        targetPercentComplete = workDrivenTask.getPercentCompleteOverrideValue();

        return workDrivenTask;
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
                    case OctaneConstants.PERCENT_COMPLETE_ITEMS_COUNT:
                        // It's like every backlog item is 1 story point.
                        if (TaskStatus.COMPLETED.equals(status) || TaskStatus.CANCELLED.equals(status)) {
                            workDone = 1.0;
                        } else {
                            workRemaining = 1.0;
                        }
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
        return getEarliestScheduledStart(children);
    }

    @Override
    public Date getScheduledFinish() {
        return getLastestScheduledFinish(children);
    }

    @Override
    public List<ExternalTask> getChildren() {
        return context.showItemsAsTasks ? children :  new ArrayList<ExternalTask>();
    }

    @Override
    public TaskStatus getStatus() {
        // The only possible statuses for work items are READY/IN_PROGRESS/COMPLETED.
        // If all children are ready, this status is READY.
        // If all children an complete, this status is COMPLETE.
        // In any other case (at least one IN_PROGRESS or both READY and COMPLETED,
        // then the status is IN_PROGRESS.

        boolean someCompleted = false;
        boolean someReady = false;

        for (ExternalTask child : children) {
            TaskStatus childStatus = child.getStatus();
            switch(childStatus) {
                case READY:
                    someReady = true;
                    break;
                case COMPLETED:
                    someCompleted = true;
                    break;
                default:
                    // IN_PROGRESS or some equivalent
                    return TaskStatus.IN_PROGRESS;
            }
        }

        if (someCompleted && !someReady) {
            // all COMPLETED
            return TaskStatus.COMPLETED;
        } else if (!someCompleted && someReady) {
            // all READY
            return TaskStatus.READY;
        } else {
            // No children or both some completed and some ready.
            return TaskStatus.IN_PROGRESS;
        }
    }

    @Override
    public List<ExternalTaskActuals> getActuals() {
        if (context.showItemsAsTasks) {
            // Summary task - actuals will be rolled up from leaf tasks.
            return new ArrayList<ExternalTaskActuals>();
        } else {
            // Leaf task, no child, return actuals of all children.
            List<ExternalTaskActuals> actuals = new ArrayList<ExternalTaskActuals>();

            // We want actuals to have the proper work information synched from Octane, but we need the % complete to be what
            // we decide it to be, whatever be the actual effort info.

            for (ExternalTask child : children) {
                List<ExternalTaskActuals> childActuals = child.getActuals();
                for (final ExternalTaskActuals childActual : childActuals) {
                    actuals.add(new ExternalTaskActuals() {

                        public double getScheduledEffort() {
                            return childActual.getScheduledEffort();
                        }

                        public Date getActualStart() {
                            return childActual.getActualStart();
                        }

                        public Date getActualFinish() {
                            return childActual.getActualFinish();
                        }

                        public double getActualEffort() {
                            return childActual.getActualEffort();
                        }

                        public double getPercentComplete() {
                            return targetPercentComplete;
                        }

                        public long getResourceId() {
                            return childActual.getResourceId();
                        }

                        public Double getEstimatedRemainingEffort() {
                            return childActual.getEstimatedRemainingEffort();
                        }

                        public Date getEstimatedFinishDate() {
                            return childActual.getEstimatedFinishDate();
                        }

                    });
                }
            }

            return actuals;
        }
    }
}
