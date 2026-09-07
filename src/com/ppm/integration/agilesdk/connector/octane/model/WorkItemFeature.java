package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.client.DateUtils;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/14.
 */
public class WorkItemFeature extends WorkItem {
    public int featurePoints = 0;

    public int aggStoryPoints = 0;

    public int numOfStories = 0;

    public int numbOfDefects = 0;

    public String epicId = null;

    public String releaseId = "";

    public String status;

    public String lastModified;

    public Date lastModifiedDatetime;

    public List<WorkItemStory> storyList = new LinkedList<WorkItemStory>();

    public void ParseJsonData(JSONObject Obj) {
        if (Obj == null) {
            return;
        }

        this.id = getStringValue(Obj, "id");
        this.name = getStringValue(Obj, "name");
        this.subType = getStringValue(Obj, "subtype");
        this.releaseId = getSubObjectItem("release", "id", Obj);
        this.epicId = getSubObjectItem("parent", "id", Obj);
        this.status = getSubObjectItem("phase", "name", Obj);
        this.lastModified = getStringValue(Obj, "last_modified");
        this.lastModifiedDatetime = DateUtils.convertDateTime(lastModified);
        this.numbOfDefects = getIntValue(Obj, "defects");
        this.numOfStories = getIntValue(Obj, "user_stories");
        this.featurePoints = getIntValue(Obj, "story_points");
    }
}
