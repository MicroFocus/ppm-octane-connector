package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.client.DateUtils;
import java.util.Date;
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

    public String teamId;

    public String epicId = null;

    public String featureId;

    public String sprintId;

    public String priority;

    public String defectStatus;
    
    public String detectedInRelease;
    
    public String severity;

    public void ParseJsonData(JSONObject Obj) {
        if (Obj == null) {
            return;
        }

        JSONObject tempJsonObj = null;
        this.id = getStringValue(Obj, "id");
        this.name = getStringValue(Obj, "name");
        this.subType = getStringValue(Obj, "subtype");
        this.creationTime = getStringValue(Obj, "creation_time");
        this.creationDateTime = DateUtils.convertDateTime(creationTime);
        this.lastModifiedTime = getStringValue(Obj, "last_modified");
        this.lastModifiedDateTime = DateUtils.convertDateTime(lastModifiedTime);
        this.investedHours = getIntValue(Obj, "invested_hours");
        this.remainingHours = getIntValue(Obj, "remaining_hours");
        this.estimatedHours = getIntValue(Obj, "estimated_hours");

        this.status = this.getSubObjectItem("phase", "name", Obj);

        this.detectedInRelease = getSubObjectItem("detected_in_release", "id", Obj);

        this.defectStatus = this.status;
        if (!"".equals(this.getSubObjectItem("team", "id", Obj))) {
            this.teamId = this.getSubObjectItem("team", "id", Obj);
        }

        this.severity = getSubObjectItem("severity", "name", Obj);
        this.priority = getSubObjectItem("priority", "name", Obj);
        if (!"".equals(this.getSubObjectItem("parent", "id", Obj))) {
            this.featureId = this.getSubObjectItem("parent", "id", Obj);
        }
        if (!"".equals(this.getSubObjectItem("release", "id", Obj))) {
            this.releaseId = this.getSubObjectItem("release", "id", Obj);
        }
        if (!"".equals(this.getSubObjectItem("sprint", "id", Obj))) {
            tempJsonObj = getObjectValue(Obj, "sprint");
            if (tempJsonObj != null) {
                this.sprintStartDate = getStringValue(tempJsonObj, "start_date");
                this.sprintEndDate = getStringValue(tempJsonObj, "end_date");
                this.sprintStart = DateUtils.convertDateTime(sprintStartDate);
                this.sprintEnd = DateUtils.convertDateTime(sprintEndDate);
                this.sprintId = getStringValue(tempJsonObj, "id");
            }
        }
        if (!"".equals(this.getSubObjectItem("owner", "id", Obj))) {
            tempJsonObj = getObjectValue(Obj, "owner");
            if (tempJsonObj != null) {
                this.ownerId = getStringValue(tempJsonObj, "id");
                this.ownerName = getStringValue(tempJsonObj, "name");
            }
        }
        this.storyPoints = getIntValue(Obj, "story_points");

    }
}
