
package com.ppm.integration.agilesdk.connector.octane.model;

import com.google.gson.annotations.SerializedName;

public class LicenseType {

    private String id;

    private String name;

    @SerializedName("logical_name")
    private String logicalName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

}
