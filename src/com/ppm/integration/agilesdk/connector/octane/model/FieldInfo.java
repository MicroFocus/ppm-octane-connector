package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * {@code FieldInfo} description
 * <p/>
 *
 * @author ChunQi, Lu
 * @since 10/18/2017
 */
public class FieldInfo {
    private String label;
    private String name;
    private String logicalName;
    private Boolean listType = false;

    public FieldInfo(JSONObject jsonObject) {
        parseData(jsonObject);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public Boolean getListType() {
        return listType;
    }

    public void setListType(Boolean listType) {
        this.listType = listType;
    }



    private void parseData(final JSONObject dataObj) {
        try {
            label = dataObj.getString("label");
            name = dataObj.getString("name");
            if (dataObj.containsKey("field_type_data")) {
                JSONObject typeData = dataObj.getJSONObject("field_type_data");
                JSONArray targets = typeData.getJSONArray("targets");
                for (int i = 0; i < targets.size(); i++) {
                    JSONObject target = targets.getJSONObject(i);
                    if (OctaneConstants.SUB_TYPE_LIST_NODE.equals(target.getString("type"))) {
                        listType = true;
                        logicalName = target.getString("logical_name");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new OctaneClientException("AGM_APP", "Error when reading JSon data from Octane: "+ e.getMessage());
        }
    }
}
