package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneUtils;
import com.ppm.integration.agilesdk.pm.ExternalTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OctaneWorkItemTypeExternalTask extends BaseOctaneExternalTask {

    private String type;

    private List<ExternalTask> children;

    private WorkplanContext context;

    public OctaneWorkItemTypeExternalTask(String type, List<GenericWorkItem> typeContent, WorkplanContext context) {
        this.type = type;
        this.context = context;
        this.children = createChildren(typeContent, context);
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
        return children;

    }


}
