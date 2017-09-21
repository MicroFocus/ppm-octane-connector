package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneUtils;
import com.ppm.integration.agilesdk.pm.ExternalTask;

import java.util.*;

/**
 * Created by canaud on 9/14/2017.
 */
public class OctaneEpicExternalTask extends BaseOctaneExternalTask {

    GenericWorkItem epic;
    private List<ExternalTask> children;

    public OctaneEpicExternalTask(GenericWorkItem epic, List<GenericWorkItem> features, Map<String, List<GenericWorkItem>> featuresItems, WorkplanContext context) {
        this.epic = epic;
        this.children = createChildren(features, featuresItems, context);
    }

    private List<ExternalTask> createChildren(List<GenericWorkItem> features, Map<String, List<GenericWorkItem>> featuresItems, WorkplanContext context) {

        // Children of Epic are always features.
        List<ExternalTask> children = new ArrayList<>();

        if (features != null) {

            OctaneUtils.sortWorkItemsByName(features);

            for (GenericWorkItem feature : features) {
                children.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneFeatureExternalTask(feature, featuresItems.get(feature.getId()), context)));
            }
        }

        return children;
    }

    @Override
    public String getName() {
        return epic.getName();
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
