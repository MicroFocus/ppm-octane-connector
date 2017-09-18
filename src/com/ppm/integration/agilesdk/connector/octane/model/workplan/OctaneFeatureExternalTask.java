package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneUtils;
import com.ppm.integration.agilesdk.pm.ExternalTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by canaud on 9/14/2017.
 */
public class OctaneFeatureExternalTask extends BaseOctaneExternalTask {

    GenericWorkItem feature;
    private List<ExternalTask> children;

    public OctaneFeatureExternalTask(GenericWorkItem feature, List<GenericWorkItem> featureItems, WorkplanContext context) {
        this.feature = feature;
        this.children = createBacklogItemsChildrenByType(featureItems, context);
    }

    @Override
    public String getName() {
        return feature.getName();
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
