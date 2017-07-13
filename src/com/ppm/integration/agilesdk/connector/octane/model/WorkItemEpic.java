package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/14.
 */
public class WorkItemEpic extends WorkItem {

    public int aggFeatureStoryPoints = 0;

    public int totalStoryPoints = 0;

    public int plannedStoryPoints = 0;

    public int doneStoryPoints = 0;

    public String author;

    public String path;

    public Map<String, WorkItemFeature> featureList = new HashMap<String, WorkItemFeature>();

    public void ParseJsonData(JSONObject Obj) {
        try {
            this.id = (String)Obj.get("id");
            this.name = (String)Obj.get("name");
            this.subType = (String)Obj.get("subtype");
            this.author = getSubObjectItem("author", "name", Obj);
            this.plannedStoryPoints = Obj.getInt("story_points");
        } catch (net.sf.json.JSONException expected) {
            // the lable is null
        }
    }

}
