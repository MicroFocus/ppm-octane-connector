package com.ppm.integration.agilesdk.connector.octane.client;

/**
 * @Author YanFeng
 * @Date 2/25/2023
 * @Description
 */

public class OctaneUser {
    private String type;
    private String last_modified;
    private String email;
    private String last_name;
    private int activity_level;
    private String id;
    private String first_name;
    private String name;
    private Permissions permissions;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLast_modified() {
        return last_modified;
    }

    public void setLast_modified(String last_modified) {
        this.last_modified = last_modified;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public int getActivity_level() {
        return activity_level;
    }

    public void setActivity_level(int activity_level) {
        this.activity_level = activity_level;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }
}
