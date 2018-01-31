package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.client.DateUtils;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.util.Date;

/**
 * Object used to store the results from the /work_items Octane REST API. We keep the JSon object as underlying data storage.
 */
public class GenericWorkItem {

    private JSONObject obj = null;

    public GenericWorkItem(JSONObject obj) {
        this.obj = obj;
    }


    private String getString(String key, JSONObject obj) {

        if (obj == null) {
            return null;
        }

        try {
            return obj.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    private String getString(String key) {
        return getString(key, obj);
    }


    private JSONObject getObj(String key) {
        return getObj(key, obj);
    }

    private JSONObject getObj(String key, JSONObject obj) {
        if (obj == null) {
            return null;
        }

        try {
            return obj.getJSONObject(key);
        } catch (JSONException e) {
            return null;
        }
    }

    private int getInt(String key, int defaultValue) {
        try {
            return obj.getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Integer getInteger(String key) {
        try {
            return obj.getInt(key);
        } catch (Exception e) {
            return null;
        }
    }

    public String getId() {
        return getString("id");
    }

    public String getName() {
        return getPrefix() + getString("name");
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

    public int getEstimatedHours() {
        return getInt("estimated_hours", 0);
    }

    public int getInvestedHours() {
        return getInt("invested_hours", 0);
    }

    public int getRemainingHours() {
        return getInt("remaining_hours", 0);
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

    public String getOwnerId() {
        return getString("id", getObj("owner"));
    }

    public String getOwnerEmail() {

        String id = getOwnerId();
        if (id != null && id.contains("@")) {
            return id;
        }
        String name = getString("name", getObj("owner"));
        if (name != null && name.contains("@")) {
            return name;
        }

        return null;
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
}
