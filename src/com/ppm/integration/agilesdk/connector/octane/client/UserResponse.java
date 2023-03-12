package com.ppm.integration.agilesdk.connector.octane.client;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpaceUser;

/**
 * @Author YanFeng
 * @Date 2/25/2023
 * @Description
 */

public class UserResponse {
    @SerializedName("total_count")
    private int totalCount;

    private List<SharedSpaceUser> data;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<SharedSpaceUser> getData() {
        return data;
    }

    public void setData(List<SharedSpaceUser> data) {
        this.data = data;
    }

}
