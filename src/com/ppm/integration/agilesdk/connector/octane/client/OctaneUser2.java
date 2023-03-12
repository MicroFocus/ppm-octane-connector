package com.ppm.integration.agilesdk.connector.octane.client;

import com.google.gson.annotations.SerializedName;

/**
 * @Author YanFeng
 * @Date 2/25/2023
 * @Description
 */

public class OctaneUser2 {
    private String type;

    @SerializedName("last_modified")
    private String lastModified;

    private String email;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("activity_level")
    private int activityLevel;

    private String id;

    @SerializedName("first_name")
    private String firstName;

    private String name;

    private Permissions2 permissions;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(int activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

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

    public Permissions2 getPermissions() {
        return permissions;
    }

    public void setPermissions(Permissions2 permissions) {
        this.permissions = permissions;
    }
}
