package com.ppm.integration.agilesdk.connector.octane.model;

/**
 * Created by lutian on 2016/11/10.
 */
public class WorkSpace extends SimpleEntity {
    public String description;

    public String logicalName;

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }
}
