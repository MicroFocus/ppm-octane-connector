package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneTask;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import java.util.Date;

public class OctaneTaskActuals extends ExternalTaskActuals {

    private final WorkplanContext context;
    private final Date adjustedWorkItemLastModifiedDate;
    private OctaneTask task;
    private GenericWorkItem workItem;
    private ExternalTask workItemExternalTask;

    public OctaneTaskActuals(OctaneTask task, GenericWorkItem workItem, ExternalTask workItemExternalTask, WorkplanContext context, Date adjustedWorkItemLastModifiedDate) {
        this.task = task;
        this.workItem = workItem;
        this.workItemExternalTask = workItemExternalTask;
        this.context = context;
        this.adjustedWorkItemLastModifiedDate = adjustedWorkItemLastModifiedDate;
    }

    @Override
    public double getScheduledEffort() {
        return task.getEstimatedHours();// estimated_hours in task
    }

    @Override
    public Date getActualStart() {
        if (getPercentComplete() > 0.0d) {
            return workItemExternalTask.getScheduledStart();
        } else {
            return null;
        }
    }

    @Override
    public Date getActualFinish() {

        Date actualFinish = null;

        if (workItemExternalTask.getStatus() == ExternalTask.TaskStatus.COMPLETED) {
            actualFinish = adjustedWorkItemLastModifiedDate;
        }

        if (actualFinish != null && actualFinish.before(getActualStart())) {
            // Actual finish should always be after actual start.
            actualFinish = workItemExternalTask.getScheduledFinish();
        }

        return actualFinish;
    }

    @Override
    public double getActualEffort() {
        return task.getInvestedHours();
    }

    @Override
    public Double getEstimatedRemainingEffort() {
        return new Double(task.getRemainingHours());
    }

    @Override
    /**
     * A workItem Task is always a leaf task, so we must always have percent complete matching the PPM task formula while task is in progress.
     * If we try to force % complete of a leaf task, PPM will modify ERE value to match % complete, which is not something we want.
     */
    public double getPercentComplete() {

        if (ExternalTask.TaskStatus.COMPLETED.equals(workItemExternalTask.getStatus()) || ExternalTask.TaskStatus.CANCELLED.equals(workItemExternalTask.getStatus())) {
            return 100;
        }

        if (!(ExternalTask.TaskStatus.ON_HOLD.equals(workItemExternalTask.getStatus()) || ExternalTask.TaskStatus.IN_PROGRESS.equals(workItemExternalTask.getStatus()))) {
            // Task is READY, IN PLANNING, etc, but is not in progress.
            return 0;
        }

        // Here we're IN_PROGRESS, so % complete must match % work complete, whatever be the % complete for summary tasks.

        if (workItem.getRemainingHours() + workItem.getInvestedHours() == 0) {
            // We have no work information and task is not completed, so it's zero percent.
            return 0;
        } else {
            return ((float)  workItem.getInvestedHours()) / ((float)  (workItem.getRemainingHours() + workItem.getInvestedHours()) ) * 100;
        }
    }

    @Override
    public long getResourceId() {
        return task.getPPMUserId(context.getUserProvider(), context.getProjectId());
    }
}
