package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/14.
 */
public class WorkItemRoot extends SimpleEntity {

    public Map<String, WorkItemEpic> epicList = new HashMap<String, WorkItemEpic>();

    public List<WorkItemStory> storyList = new LinkedList<WorkItemStory>();

    public int length = -1;

    List<JSONObject> tempStoryWithoutAddToFeatureList = new LinkedList<JSONObject>();

    Map<String, JSONObject> tempFeatureWithoutAddToEpicList = new HashMap<String, JSONObject>();

    public void GetTempParseData(String data, boolean isGetDefect) {

        JSONObject object = JSONObject.fromObject(data);
        JSONArray jsonarray = (JSONArray)(object.get("data"));
        this.length = jsonarray.size();

        for (int i = 0; i < this.length; i++) {
            JSONObject tempObj = jsonarray.getJSONObject(i);
            switch ((String)tempObj.get("subtype")) {
                case OctaneConstants.SUB_TYPE_WORK_ITEM_ROOT:
                    this.id = (String)tempObj.get("id");
                    this.name = (String)tempObj.get("name");
                    this.type = (String)tempObj.get("subtype");
                    break;
                case OctaneConstants.SUB_TYPE_EPIC:
                    WorkItemEpic tempEpic = new WorkItemEpic();
                    tempEpic.ParseJsonData(tempObj);
                    epicList.put((String)tempObj.get("id"), tempEpic);
                    break;
                case OctaneConstants.SUB_TYPE_FEATURE:
                    tempFeatureWithoutAddToEpicList.put((String)tempObj.get("id"), tempObj);
                    break;
                case OctaneConstants.SUB_TYPE_DEFECT:
                    if (!isGetDefect) {
                        break;
                    }
                case OctaneConstants.SUB_TYPE_STORY:
                    tempStoryWithoutAddToFeatureList.add(tempObj);
                    break;
                default:
                    break;
            }
        }
    }

    public void ParseDataIntoDetail() {

        while (!tempStoryWithoutAddToFeatureList.isEmpty()) {

            JSONObject tempStoryObj = tempStoryWithoutAddToFeatureList.remove(0);
            try {

                JSONObject tempStoryParentObj = (JSONObject)tempStoryObj.get("parent");
                WorkItemStory tempStory = new WorkItemStory();
                tempStory.ParseJsonData(tempStoryObj);

                if (OctaneConstants.SUB_TYPE_WORK_ITEM_ROOT.equals((String)tempStoryParentObj.get("subtype"))) {
                    storyList.add(tempStory);
                    continue;
                }
                if (OctaneConstants.SUB_TYPE_FEATURE.equals((String)tempStoryParentObj.get("subtype"))) {

                    JSONObject tempFeatureObj =
                            tempFeatureWithoutAddToEpicList.remove((String)tempStoryParentObj.get("id"));
                    //this Id exits in tempFeatureWithoutAddToEpicList? if yes,new Feature
                    if (null != tempFeatureObj) {
                        //feature jsonobj to parent
                        JSONObject tempFeatureParentObj = (JSONObject)tempFeatureObj.get("parent");
                        //find the feature in epicList use  the feature parent id
                        WorkItemEpic tempEpic = epicList.get((String)tempFeatureParentObj.get("id"));
                        WorkItemFeature tempFeature = new WorkItemFeature();
                        //initialization the WorkItemFeature
                        tempFeature.ParseJsonData(tempFeatureObj);
                        tempStory.themeId = tempFeature.themeId;
                        tempFeature.aggStoryPoints += tempStory.storyPoints;
                        tempFeature.storyList.add(tempStory);
                        // add the WorkItemFeature into epicList
                        tempEpic.featureList.put((String)tempFeatureObj.get("id"), tempFeature);
                        epicList.put((String)tempFeatureParentObj.get("id"), tempEpic);
                        continue;
                    } else {
                        //if not exits, Traversing the epicList, and use the story id find the right WorkItemFeature
                        Set<String> keySet = epicList.keySet();
                        for (String key : keySet) {
                            WorkItemEpic tempEpic = epicList.get(key);
                            WorkItemFeature tempFeature = tempEpic.featureList.remove(tempStoryParentObj.get("id"));
                            if (tempFeature != null) {
                                tempStory.themeId = tempFeature.themeId;
                                tempFeature.aggStoryPoints += tempStory.storyPoints;
                                tempFeature.storyList.add(tempStory);
                                tempEpic.featureList.put((String)tempStoryParentObj.get("id"), tempFeature);
                                epicList.put(tempEpic.id, tempEpic);
                                break;
                            }
                        }
                    }
                }

            } catch (JSONException expected) {
                // the owner is null
            }

        }
        Set<String> keySetFeature = tempFeatureWithoutAddToEpicList.keySet();
        for (String keyFeature : keySetFeature) {
            JSONObject tempFeatureObj = tempFeatureWithoutAddToEpicList.get(keyFeature);
            //feature jsonobj to parent
            JSONObject tempFeatureParentObj = (JSONObject)tempFeatureObj.get("parent");
            //find the feature in epicList use  the feature parent id
            WorkItemEpic tempEpic = epicList.get((String)tempFeatureParentObj.get("id"));

            //initialization the WorkItemFeature
            WorkItemFeature tempFeature = new WorkItemFeature();
            tempFeature.ParseJsonData(tempFeatureObj);
            // add the WorkItemFeature into epicList
            tempEpic.featureList.put((String)tempFeatureObj.get("id"), tempFeature);
            epicList.put((String)tempFeatureParentObj.get("id"), tempEpic);
        }
        tempFeatureWithoutAddToEpicList.clear();
        Set<String> keySetEpic = epicList.keySet();
        for (String keyEpic : keySetEpic) {
            WorkItemEpic tempEpic = epicList.get(keyEpic);
            keySetFeature = tempEpic.featureList.keySet();
            for (String keyFeature : keySetFeature) {
                WorkItemFeature tempFeature = tempEpic.featureList.get(keyFeature);
                tempEpic.aggFeatureStoryPoints += tempFeature.featurePoints;
                tempEpic.totalStoryPoints += tempFeature.aggStoryPoints;
            }
            epicList.put(tempEpic.id, tempEpic);
        }
    }

    public void ParseDataIntoDetail(String releaseId) {

        while (!tempStoryWithoutAddToFeatureList.isEmpty()) {

            JSONObject tempStoryObj = tempStoryWithoutAddToFeatureList.remove(0);
            //these two line code depends on the releaseId
            try {
                String tempReleaseId = WorkItem.getSubObjectItem("release", "id", tempStoryObj);//find the release id
                if (!tempReleaseId.equals(releaseId)) {//if the US is not exit in releaseId, go to the next StoryJsonObj
                    continue;
                }
                //else
                JSONObject tempStoryParentObj = (JSONObject)tempStoryObj.get("parent");
                WorkItemStory tempStory = new WorkItemStory();
                tempStory.ParseJsonData(tempStoryObj);

                if (OctaneConstants.SUB_TYPE_WORK_ITEM_ROOT.equals((String)tempStoryParentObj.get("subtype"))) {
                    storyList.add(tempStory);
                    continue;
                }
                if (OctaneConstants.SUB_TYPE_FEATURE.equals((String)tempStoryParentObj.get("subtype"))) {

                    JSONObject tempFeatureObj =
                            tempFeatureWithoutAddToEpicList.remove((String)tempStoryParentObj.get("id"));
                    //this Id exits in tempFeatureWithoutAddToEpicList? if yes,new Feature
                    if (null != tempFeatureObj) {
                        //feature jsonobj to parent
                        JSONObject tempFeatureParentObj = (JSONObject)tempFeatureObj.get("parent");
                        //find the feature in epicList use  the feature parent id
                        WorkItemEpic tempEpic = epicList.get((String)tempFeatureParentObj.get("id"));
                        WorkItemFeature tempFeature = new WorkItemFeature();
                        //initialization the WorkItemFeature
                        tempFeature.ParseJsonData(tempFeatureObj);
                        tempFeature.storyList.add(tempStory);
                        // add the WorkItemFeature into epicList
                        tempEpic.featureList.put((String)tempFeatureObj.get("id"), tempFeature);
                        epicList.put((String)tempFeatureParentObj.get("id"), tempEpic);
                        continue;
                    } else {
                        //if not exits, Traversing the epicList, and use the story id find the right WorkItemFeature
                        Set<String> keySet = epicList.keySet();
                        for (String key : keySet) {
                            WorkItemEpic tempEpic = epicList.get(key);
                            WorkItemFeature tempFeature = tempEpic.featureList.remove(tempStoryParentObj.get("id"));
                            if (tempFeature != null) {
                                tempFeature.storyList.add(tempStory);
                                tempEpic.featureList.put((String)tempStoryParentObj.get("id"), tempFeature);
                                epicList.put(tempEpic.id, tempEpic);
                                break;
                            }
                        }
                    }
                }

            } catch (JSONException expected) {
                // the owner is null
            }

        }
        Set<String> keySetFeature = tempFeatureWithoutAddToEpicList.keySet();
        for (String keyFeature : keySetFeature) {
            JSONObject tempFeatureObj = tempFeatureWithoutAddToEpicList.get(keyFeature);//feature jsonobj to parent
            String tempReleaseId = WorkItem.getSubObjectItem("release", "id",
                    tempFeatureObj);//find the feature is in the given release
            if (!tempReleaseId
                    .equals(releaseId)) {//if the feature is not exit in releaseId, go to the next tempFeatureObj
                continue;
            }
            //else add the feature to the parent epic,
            JSONObject tempFeatureParentObj = (JSONObject)tempFeatureObj.get("parent");
            //find the feature in epicList use  the feature parent id
            WorkItemEpic tempEpic = epicList.get((String)tempFeatureParentObj.get("id"));

            //initialization the WorkItemFeature
            WorkItemFeature tempFeature = new WorkItemFeature();
            tempFeature.ParseJsonData(tempFeatureObj);
            // add the WorkItemFeature into epicList
            tempEpic.featureList.put((String)tempFeatureObj.get("id"), tempFeature);
            epicList.put((String)tempFeatureParentObj.get("id"), tempEpic);
        }
        tempFeatureWithoutAddToEpicList.clear();
        Iterator it = epicList.keySet().iterator();
        while (it.hasNext()) {
            String tempMap = (String)it.next();
            WorkItemEpic tempEpic = epicList.get(tempMap);
            if (0 == tempEpic.featureList.size()) {//if the epic has no feature, delete this epic
                it.remove();
            }
        }
        // delete the epic has no feature from epicList
    }

}

