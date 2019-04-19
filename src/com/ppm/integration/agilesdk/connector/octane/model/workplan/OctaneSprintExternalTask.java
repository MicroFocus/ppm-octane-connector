package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import java.util.Date;
import java.util.List;

import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.pm.ExternalTask;

public class OctaneSprintExternalTask extends BaseOctaneExternalTask {

    private Sprint sprint;

    private List<ExternalTask> children;

    private WorkplanContext context;

    public OctaneSprintExternalTask(Sprint sprint, List<GenericWorkItem> sprintContent, WorkplanContext context) {
        this.sprint = sprint;
        this.children = createBacklogItemsChildrenByType(sprintContent, context, sprint.getId());
        this.context = context;

    }



    @Override
    public String getId() {
        return sprint.getId();
    }

    @Override
    public String getName() {
        return sprint.getName();
    }

    @Override
    public Date getScheduledStart() {
        return sprint.sprintStart != null ? adjustStartDateTime(sprint.sprintStart) : getEarliestScheduledStart(getChildren());
    }

    @Override
    public Date getScheduledFinish() {
        return sprint.sprintEnd != null ? adjustFinishDateTime(sprint.sprintEnd) : getLastestScheduledFinish(getChildren());
    }

    @Override
    public List<ExternalTask> getChildren() {
        return children;
    }


}
