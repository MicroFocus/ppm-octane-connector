package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
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
            public double getPercentComplete() {

                if (TaskStatus.COMPLETED.equals(getStatus()) || TaskStatus.CANCELLED.equals(getStatus())) {
                    return 100;
                }

                if (OctaneConstants.PERCENT_COMPLETE_STORY_POINTS.equals(context.percentComplete) || OctaneConstants.PERCENT_COMPLETE_ITEMS_COUNT.equals(context.percentComplete)) {
                    // If we are in story points mode or backlog items count mode and task is not completed, then it's zero percent.
                    return 0;
                }

                if (workItem.getRemainingHours() + workItem.getInvestedHours() == 0) {
                    // We have no work information and task is not completed, so it's zero percent.
                    return 0;
                } else {
                    return ((float)  workItem.getInvestedHours()) / ((float)  (workItem.getRemainingHours() + workItem.getInvestedHours()) ) * 100;
                }
            }

            @Override
            public long getResourceId() {
                User user =  context.getUserProvider().getByEmail(workItem.getOwnerEmail());
                return (user == null ? -1 : user.getUserId());
            }

        };
        List<ExternalTaskActuals> actuals = new ArrayList<>();
        actuals.add(taskActual);
        return actuals;
    }

    @Override
    public long getOwnerId() {
        User user =  context.getUserProvider().getByEmail(workItem.getOwnerId());
        return (user == null ? -1 : user.getUserId());
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
