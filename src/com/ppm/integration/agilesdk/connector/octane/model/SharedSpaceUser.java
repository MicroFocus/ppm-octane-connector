package com.ppm.integration.agilesdk.connector.octane.model;

import com.google.gson.annotations.SerializedName;
import com.ppm.integration.agilesdk.connector.octane.model.LicenseType;

/**
 * {@code SharedSpaceUser} Main class of "SharedSpaceUser", used to convert
 * octane user json to this model.
 * @since PPM 10.0.3
 */
public class SharedSpaceUser {

    private String name;

    private String email;

    private String id;

    @SerializedName("last_modified")
    private String lastModified;

    @SerializedName("activity_level")
    private String activityLevel;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("workspace_roles")
    private WorkspaceRole workspaceRoles;

    @SerializedName("license_type")
    private LicenseType licenseType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public WorkspaceRole getWorkspaceRoles() {
        return workspaceRoles;
    }

    public void setWorkspaceRoles(WorkspaceRole workspaceRoles) {
        this.workspaceRoles = workspaceRoles;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(LicenseType licenseType) {
        this.licenseType = licenseType;
    }


}
