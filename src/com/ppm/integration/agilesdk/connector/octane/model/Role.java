package com.ppm.integration.agilesdk.connector.octane.model;

import com.google.gson.annotations.SerializedName;

public class Role {
    @SerializedName("logical_name")
    private String logicalName;

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

}
