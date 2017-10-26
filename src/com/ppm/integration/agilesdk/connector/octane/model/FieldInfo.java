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
            label = dataObj.getString(OctaneConstants.KEY_FIELD_LABEL);
            name = dataObj.getString(OctaneConstants.KEY_FIELD_NAME);
            if (dataObj.containsKey(OctaneConstants.KEY_FIELD_TYPE_DATA)) {
                JSONObject typeData = dataObj.getJSONObject(OctaneConstants.KEY_FIELD_TYPE_DATA);
                JSONArray targets = typeData.getJSONArray(OctaneConstants.KEY_FIELD_TARGETS);
                for (int i = 0; i < targets.size(); i++) {
                    JSONObject target = targets.getJSONObject(i);
                    if (OctaneConstants.SUB_TYPE_LIST_NODE.equals(target.getString(OctaneConstants.KEY_FIELD_TYPE))) {
                        listType = true;
                        logicalName = target.getString(OctaneConstants.KEY_LOGICAL_NAME);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new OctaneClientException("AGM_APP", "Error when reading JSon data from Octane: "+ e.getMessage());
        }
    }
}
