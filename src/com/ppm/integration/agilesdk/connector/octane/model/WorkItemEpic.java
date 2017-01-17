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

    public int planedStoryPoints = 0;

    public String author;

    public Map<String, WorkItemFeature> featureList = new HashMap<String, WorkItemFeature>();

    public void ParseJsonData(JSONObject Obj) {
        try {
            this.id = (String)Obj.get("id");
            this.name = (String)Obj.get("name");
            this.subType = (String)Obj.get("subtype");
            this.author = getSubObjectItem("author", "name", Obj);
            this.planedStoryPoints = Obj.getInt("story_points");
        } catch (net.sf.json.JSONException expected) {
            // the lable is null
        }
    }

}
