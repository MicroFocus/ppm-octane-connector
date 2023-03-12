package com.ppm.integration.agilesdk.connector.octane.client;

import com.google.gson.annotations.SerializedName;

/**
 * @Author YanFeng
 * @Date 2/25/2023
 * @Description
 */

public class Permission2 {

    @SerializedName("logical_name")
    private String logicalName;

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

}


