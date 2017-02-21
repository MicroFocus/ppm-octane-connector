package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.client.Client;
import java.util.Date;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/10.
 */
public class Release extends SimpleEntity {

    public String startDate = "";

    public String endDate = "";

    public Date startDatetime = null;

    public Date endDatetime = null;

    public void ParseData(String data) {
        JSONObject tempObj = JSONObject.fromObject(data);
        try {
            this.id = (String)tempObj.get("id");
            this.name = (String)tempObj.get("name");
            this.type = (String)tempObj.get("type");

            this.startDate = (String)tempObj.get("start_date");
            this.endDate = (String)tempObj.get("end_date");
            this.startDatetime = Client.convertDateTime(startDate);
            this.endDatetime = Client.convertDateTime(endDate);
        }catch(Exception e) {}

    }
}
