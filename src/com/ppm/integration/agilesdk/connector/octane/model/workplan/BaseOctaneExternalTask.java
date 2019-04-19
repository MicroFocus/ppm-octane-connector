package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.pm.ExternalTask;

public abstract class BaseOctaneExternalTask extends ExternalTask {


    @Override
    public TaskStatus getStatus() {
        // Default status will depend on the status of children (if any).
        // We need to return some status on summary tasks so that % complete is set correctly if there's zero work to do the computation.

        List<ExternalTask> children = getChildren();

        if (children.isEmpty()) {
            // If there's no child even though it's supposed to be a summary task, it's considered still in planning.
            return TaskStatus.IN_PLANNING;
        }

        boolean allChildrenCompletedOrCancelled = true;

        boolean allChildrenReady = true;

        for (ExternalTask child : children) {
            if (!TaskStatus.COMPLETED.equals(child.getStatus()) && !TaskStatus.CANCELLED.equals(child.getStatus())) {
                allChildrenCompletedOrCancelled = false;
            }

            if (!TaskStatus.READY.equals(child.getStatus())) {
                allChildrenReady = false;
            }

            if (TaskStatus.IN_PROGRESS.equals(child.getStatus()) || (!allChildrenCompletedOrCancelled && !allChildrenReady)) {
                return TaskStatus.IN_PROGRESS;
            }
        }

        if (allChildrenCompletedOrCancelled) {
            return TaskStatus.COMPLETED;
        } else if (allChildrenReady) {
            return TaskStatus.READY;
        }

        // We shouldn't arrive here unless there are some strange status used
        return TaskStatus.IN_PROGRESS;
    }

    protected Date getLastestScheduledFinish(List<ExternalTask> children) {
        Date date = null;
        for (ExternalTask child : children) {
            if (child.getScheduledFinish() == null) {
                continue;
            }
            if (date == null || child.getScheduledFinish().after(date)) {
                date = child.getScheduledFinish();
            }
        }

        return date != null ? date : getDefaultFinishDate();
    }

    protected Date getEarliestScheduledStart(List<ExternalTask> children) {
        Date date = null;
        for (ExternalTask child : children) {
            if (child.getScheduledStart() == null) {
                continue;
            }
            if (date == null || child.getScheduledStart().before(date)) {
                date = child.getScheduledStart();
            }
        }

        return date != null ? date : getDefaultStartDate();
    }

    protected double getNullSafeDouble(Double d) {
        return d == null ? 0d : d.doubleValue();
    }

    private Date getDefaultStartDate() {
        Calendar todayMorning = new GregorianCalendar();
        todayMorning.set(Calendar.HOUR_OF_DAY, 1);
        todayMorning.set(Calendar.MINUTE, 0);
        todayMorning.set(Calendar.SECOND, 0);
        todayMorning.set(Calendar.MILLISECOND, 0);
        return todayMorning.getTime();
    }

    private Date getDefaultFinishDate() {
        Calendar todayEvening = new GregorianCalendar();
        todayEvening.set(Calendar.HOUR_OF_DAY, 23);
        todayEvening.set(Calendar.MINUTE, 0);
        todayEvening.set(Calendar.SECOND, 0);
        todayEvening.set(Calendar.MILLISECOND, 0);
        return todayEvening.getTime();
    }

    /**
     * Dates returned from external systems don't have time set and are set to midnight (i.e.
     * start of the day). However, a date set as a finish date usually means
     * "after the day as finished", and we need to set the hour to the end of
     * the day so that PPM computes duration correctly. <br>
     * This will return a modified date, and will keep date passed in parameter unmodified.
     */
    @Override
    protected Date adjustFinishDateTime(Date date) {
        return modifyDateTime(23, date);
    }

    /**
     * Dates returned from external systems don't always have time set or use some default time.
     * This method will ensure that time is set at 1:00 am (start of the date) so that PPM computes duration correctly. <br>
     * This will return a modified date, and will keep date passed in parameter unmodified.
     */
    @Override
    protected Date adjustStartDateTime(Date date) {
        return modifyDateTime(1, date);
    }

    private Date modifyDateTime(int hour, Date date) {
        if (date == null) {
            return null;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTime();
    }

    protected List<ExternalTask> createBacklogItemsChildrenByType(List<GenericWorkItem> backlogItems,
            WorkplanContext context, String parentId)
    {
        List<ExternalTask> children = new ArrayList<>();

        if (backlogItems != null) {

            List<GenericWorkItem> stories = new ArrayList<>();
            List<GenericWorkItem> qualityStories = new ArrayList<>();
            List<GenericWorkItem> defects = new ArrayList<>();
            List<GenericWorkItem> others = new ArrayList<>();

            for (GenericWorkItem workItem : backlogItems) {
                switch(workItem.getSubType()) {
                    case "defect":
                        defects.add(workItem);
                        break;
                    case "story":
                        stories.add(workItem);
                        break;
                    case "quality_story":
                        qualityStories.add(workItem);
                        break;
                    case "epic":
                        // Skip
                        break;
                    case "feature":
                        // Skip
                        break;
                    default:
                        others.add(workItem);
                        break;
                }
            }

            if (!defects.isEmpty()) {
                children.add(new OctaneWorkItemTypeExternalTask("defect", defects, context, parentId)
                        .toWorkDrivenPercentCompleteExternalTask());
            }
            if (!stories.isEmpty()) {
                children.add(new OctaneWorkItemTypeExternalTask("story", stories, context, parentId)
                        .toWorkDrivenPercentCompleteExternalTask());
            }
            if (!qualityStories.isEmpty()) {
                children.add(new OctaneWorkItemTypeExternalTask("quality_story", qualityStories, context, parentId)
                        .toWorkDrivenPercentCompleteExternalTask());
            }
            if (!others.isEmpty()) {
                children.add(new OctaneWorkItemTypeExternalTask("other", others, context, parentId)
                        .toWorkDrivenPercentCompleteExternalTask());
            }

            return children;
        }

        return children;
    }

}
