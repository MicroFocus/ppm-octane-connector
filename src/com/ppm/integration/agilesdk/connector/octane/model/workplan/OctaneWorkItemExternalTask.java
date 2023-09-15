package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneTask;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OctaneWorkItemExternalTask extends BaseOctaneExternalTask {

    private GenericWorkItem workItem;

    private WorkplanContext context;

    private TaskStatus status;

    public OctaneWorkItemExternalTask(GenericWorkItem workItem, WorkplanContext context) {
        this.workItem = workItem;
        this.context = context;
        this.status = context.getPPMStatus(workItem.getPhaseId());
    }

    @Override
    public String getId() {
        return workItem.getId();
    }

    @Override
    public String getName() {
        return workItem.getName();
    }

    @Override
    public TaskStatus getStatus() {
        return status;
    }


    @Override
    public Date getScheduledStart() {

        Date creationDate = workItem.getCreationDate();

        Sprint sprint = context.getSprint(workItem.getSprintId());

        if (sprint != null && sprint.sprintStart != null) {
            if (sprint.sprintEnd != null && sprint.sprintStart.before(creationDate) && sprint.sprintEnd.after(creationDate)) {
                return adjustStartDateTime(creationDate);
            } else {
                return adjustStartDateTime(sprint.sprintStart);
            }
        }

        return adjustStartDateTime(creationDate);
    }

    @Override
    public Date getScheduledFinish() {

        Sprint sprint = context.getSprint(workItem.getSprintId());

        if (sprint != null && sprint.sprintEnd != null) {
            return adjustFinishDateTime(sprint.sprintEnd);
        }

        // No sprint defined? We don't know when this will finish, so let's make it end on the same day it was created.
        return context.moveToNextWorkingDay(adjustFinishDateTime(getScheduledStart()));
    }

    @Override
    public List<ExternalTaskActuals> getActuals() {
        List<ExternalTaskActuals> actuals = new ArrayList<>();
        switch (context.effortMode) {
            case OctaneConstants.EFFORT_NO_IMPORT:
                // DO nothing - no actuals.
                break;
            case OctaneConstants.EFFORT_GROUP_IN_WORKITEM_TASKS_OWNERS:
                // one actual per task
                for (OctaneTask task: workItem.getTasks()) {
                    ExternalTaskActuals taskActual = new OctaneTaskActuals(task, workItem, this, context, adjustFinishDateTime(workItem.getLastModifiedTime()));
                    actuals.add(taskActual);
                }
                break;
            default: //OctaneConstants.EFFORT_GROUP_IN_WORKITEM_OWNER
                ExternalTaskActuals taskActual = new ExternalTaskActuals() {

                    @Override
                    public double getScheduledEffort() {
                        return workItem.getEstimatedHours();// estimated_hours in task
                    }

                    @Override
                    public Date getActualStart() {
                        if (getPercentComplete() > 0.0d) {
                            return getScheduledStart();
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public Date getActualFinish() {

                        Date actualFinish = null;

                        if (getStatus() == TaskStatus.COMPLETED) {
                            actualFinish = adjustFinishDateTime(workItem.getLastModifiedTime());
                        }

                        if (actualFinish != null && actualFinish.before(getActualStart())) {
                            // Actual finish should always be after actual start.
                            actualFinish = getScheduledFinish();
                        }

                        return actualFinish;
                    }

                    @Override
                    public double getActualEffort() {
                        return workItem.getInvestedHours();
                    }

                    @Override
                    public Double getEstimatedRemainingEffort() {
                        return new Double(workItem.getRemainingHours());
                    }

                    @Override
                    /**
                     * A workItem Task is always a leaf task, so we must always have percent complete matching the PPM task formula while task is in progress.
                     * If we try to force % complete of a leaf task, PPM will modify ERE value to match % complete, which is not something we want.
                     */
                    public double getPercentComplete() {

                        if (TaskStatus.COMPLETED.equals(getStatus()) || TaskStatus.CANCELLED.equals(getStatus())) {
                            return 100;
                        }

                        if (!(TaskStatus.ON_HOLD.equals(getStatus()) || TaskStatus.IN_PROGRESS.equals(getStatus()))) {
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
                        return workItem.getPPMUserId(context.getUserProvider(), context.getProjectId());
                    }

                };
                actuals.add(taskActual);
                break;
        }

        return actuals;
    }

    @Override
    public long getOwnerId() {
        return workItem.getPPMUserId(context.getUserProvider(), context.getProjectId());
    }

    @Override
    public boolean isMilestone() {
        return false;
    }

    @Override
    public List<ExternalTask> getChildren() {
        return null;
    }

}
