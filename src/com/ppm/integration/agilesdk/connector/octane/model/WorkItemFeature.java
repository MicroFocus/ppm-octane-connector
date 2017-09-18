package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.client.DateUtils;
import com.ppm.integration.agilesdk.connector.octane.client.UsernamePasswordClient;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.sf.json.JSONException;
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
        try {
            this.id = (String)Obj.get("id");
            this.name = (String)Obj.get("name");
            this.subType = (String)Obj.get("subtype");
            this.releaseId = getSubObjectItem("release", "id", Obj);
            this.epicId = getSubObjectItem("parent", "id", Obj);
            this.status = this.getSubObjectItem("phase", "name", Obj);
            this.lastModified = (String)Obj.get("last_modified");
            this.lastModifiedDatetime = DateUtils.convertDateTime(lastModified);
            this.numbOfDefects = Obj.getInt("defects");
            this.numOfStories = Obj.getInt("user_stories");
            this.featurePoints = Obj.getInt("story_points");
        } catch (JSONException expected) {
            // the release is null
        }
    }
}
