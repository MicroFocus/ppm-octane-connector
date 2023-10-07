package com.ppm.integration.agilesdk.connector.octane.model;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.connector.octane.client.DateUtils;
import com.ppm.integration.agilesdk.provider.UserProvider;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Object used to store the results from the /work_items Octane REST API. We keep the JSon object as underlying data storage.
 */
public class GenericWorkItem extends BaseOctaneObject {

    private List<OctaneTask> tasks = new ArrayList<>();

    public GenericWorkItem(JSONObject obj) {
        super(obj);
    }


    @Override
    public String getName() {
        return getPrefix() + super.getName();
    }

    private String getPrefix() {
        if (isEpic()) {
            return "[E] ";
        } else if (isFeature()) {
            return "[F] ";
        }

        return "";
    }

    public String getPhaseId() {
        return getString("id", getObj("phase"));
    }



    public String getSubType() {
        return getString("subtype");
    }

    /* public String getSubTypeLabel() {
        return getString("subtype_label");
    }*/

    public String getReleaseId() {
        return getString("id", getObj("release"));
    }

    public String getSprintId() {
        return getString("id", getObj("sprint"));
    }

    public Date getCreationDate() {
        String creationTime = getString("creation_time");
        return creationTime == null ? null : DateUtils.convertDateTime(creationTime);
    }

    public Date getLastModifiedTime() {
        String lastModifiedTime = getString("last_modified");
        return lastModifiedTime == null ? new Date() : DateUtils.convertDateTime(lastModifiedTime);
    }


    public boolean isEpic() {
        return "epic".equals(getSubType());
    }

    public boolean isInBacklog() {
        return "work_item_root".equals(getParentSubType());
    }

    public boolean isFeature() {
        return "feature".equals(getSubType());
    }

    public String getParentId() {
        return getString("id", getObj("parent"));
    }

    public String getParentSubType() {
        return getString("subtype", getObj("parent"));
    }

    public int getStoryPoints() {
        return getInt("story_points", 0);
    }

    public Integer getAggregatedStoryPoints() {
        return getInteger("actual_story_points");
    }

    public boolean isDefectOrStory() {
        switch(getSubType()) {
            case "defect":
                return true;
            case "story":
                return true;
            case "quality_story":
                return true;
            default:
                return false;
        }

    }


    public void removeEffort() {
        updateNumericProperty("estimated_hours", 0);
        updateNumericProperty("invested_hours", 0);
        updateNumericProperty("remaining_hours", 0);
    }


    public void addTask(OctaneTask task) {
        tasks.add(task);
    }

    public List<OctaneTask> getTasks() {
        return new ArrayList<>(tasks);
    }
}
