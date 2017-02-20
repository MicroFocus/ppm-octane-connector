package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.client.Client;
import java.util.Date;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/14.
 */
public class WorkItemStory extends WorkItem {

    public String creationTime;

    public Date creationDateTime;

    public String lastModifiedTime;

    public Date lastModifiedDateTime;

    public String status;

    public String ownerId;

    public String ownerName;

    public int estimatedHours;

    public int remainingHours;

    public int investedHours;

    public String releaseId = "";

    public Date sprintStart;

    public Date sprintEnd;

    public String sprintStartDate;

    public String sprintEndDate;

    public int storyPoints;

    public long teamId;

    public long themeId = -1;

    public long featureId;

    public long sprintId;

    public String priority;

    public String defectStatus;

    public void ParseJsonData(JSONObject Obj) {

        JSONObject tempJsonObj = null;
        try {
            this.id = (String)Obj.get("id");
            this.name = (String)Obj.get("name");
            this.subType = (String)Obj.get("subtype");
            this.creationTime = (String)Obj.get("creation_time");
            this.creationDateTime = Client.convertDateTime(creationTime);
            this.lastModifiedTime = (String)Obj.get("last_modified");
            this.lastModifiedDateTime = Client.convertDateTime(lastModifiedTime);
            this.investedHours = (int)Obj.get("invested_hours");
            this.remainingHours = (int)Obj.get("remaining_hours");
            this.estimatedHours = (int)Obj.get("estimated_hours");

            this.status = this.getSubObjectItem("phase", "name", Obj);

            this.defectStatus = this.status;
            if (!"".equals(this.getSubObjectItem("team", "id", Obj))) {
                this.teamId = Long.parseLong(this.getSubObjectItem("team", "id", Obj));
            }

            this.priority = getSubObjectItem("severity", "name", Obj);
            if (!"".equals(this.getSubObjectItem("parent", "id", Obj))) {
                this.featureId = Long.parseLong(this.getSubObjectItem("parent", "id", Obj));
            }
            if (!"".equals(this.getSubObjectItem("release", "id", Obj))) {
                this.releaseId = this.getSubObjectItem("release", "id", Obj);
            }
            if (!"".equals(this.getSubObjectItem("sprint", "id", Obj))) {
                tempJsonObj = (JSONObject)Obj.get("sprint");
                this.sprintStartDate = (String)tempJsonObj.get("start_date");
                this.sprintEndDate = (String)tempJsonObj.get("end_date");
                this.sprintStart = Client.convertDateTime(sprintStartDate);
                this.sprintEnd = Client.convertDateTime(sprintEndDate);
                this.sprintId = tempJsonObj.getLong("id");
            }
            if (!"".equals(this.getSubObjectItem("owner", "id", Obj))) {
                tempJsonObj = (JSONObject)Obj.get("owner");
                this.ownerId = (String)tempJsonObj.get("id");
                this.ownerName = (String)tempJsonObj.get("name");
            }
            this.storyPoints = Obj.getInt("story_points");
        } catch (JSONException expected) {
            // the release is null
        }

    }
}
