package com.ppm.integration.agilesdk.connector.octane.client;

import java.util.List;

/**
 * @Author YanFeng
 * @Date 2/25/2023
 * @Description
 */

public class Permissions {
    private int total_count;
    private boolean exceeds_total_count;
    private int total_error_count;
    private List<Permission> data;

    public int getTotal_count() {
        return total_count;
    }

    public boolean isExceeds_total_count() {
        return exceeds_total_count;
    }

    public void setExceeds_total_count(boolean exceeds_total_count) {
        this.exceeds_total_count = exceeds_total_count;
    }

    public int getTotal_error_count() {
        return total_error_count;
    }

    public void setTotal_error_count(int total_error_count) {
        this.total_error_count = total_error_count;
    }

    public List<Permission> getData() {
        return data;
    }

    public void setData(List<Permission> data) {
        this.data = data;
    }

    public void setTotal_count(int total_count) {
        this.total_count = total_count;
    }
}
