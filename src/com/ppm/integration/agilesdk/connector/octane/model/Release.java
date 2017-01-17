package com.ppm.integration.agilesdk.connector.octane.model;

import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/10.
 */
public class Release extends SimpleEntity {

    public String startDate = "";

    public String endDate = "";

    public void ParseData(String data) {
        JSONObject object = JSONObject.fromObject(data);
        this.startDate = (String)object.get("start_date");
        this.endDate = (String)object.get("end_date");
    }
}
