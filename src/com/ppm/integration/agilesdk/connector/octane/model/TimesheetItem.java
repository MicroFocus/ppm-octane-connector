package com.ppm.integration.agilesdk.connector.octane.model;

/**
 * Created by zhudo on 11/15/2016.
 */
public class TimesheetItem {

    private String loginName;

    private String fullName;

    private int invested;

    private String date;

    private String entityName;

    private int entityId;

    private String entityType;

    private String sprintName;

    private int sprintId;

    private int releaseId;

    private String releaseName;

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getInvested() {
        return invested;
    }

    public void setInvested(int invested) {
        this.invested = invested;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getSprintName() {
        return sprintName;
    }

    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }

    public int getSprintId() {
        return sprintId;
    }

    public void setSprintId(int sprintId) {
        this.sprintId = sprintId;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public int getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(int releaseId) {
        this.releaseId = releaseId;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" loginName:" + loginName);
        sb.append(" ,fullName:" + fullName);
        sb.append(" ,invested:" + invested);
        sb.append(" ,date:" + date);
        sb.append(" ,entityName:" + entityName);
        sb.append(" ,entityType:" + entityType);
        sb.append(" ,entityId:" + entityId);
        sb.append(" ,sprintId:" + sprintId);
        sb.append(" ,sprintName:" + sprintName);
        sb.append(" ,releaseName:" + releaseName);
        sb.append(" ,releaseId:" + releaseId);
        return sb.toString();
    }

}
