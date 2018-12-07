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
    private String fieldType;

    private boolean multiValue = false;

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

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

    public boolean isMultiValue() {
        return multiValue;
    }

    public void setMultiValue(boolean multiValue) {
        this.multiValue = multiValue;
    }

    private void parseData(final JSONObject dataObj) {
        try {
            label = dataObj.getString(OctaneConstants.KEY_FIELD_LABEL);
            name = dataObj.getString(OctaneConstants.KEY_FIELD_NAME);

            /*
            * We regard all <reference> type field as <list type > field, default list type is true
            * all reference field with list type true can do value mapping, except <user list> type field,
            * we regard user type as special field type
            * First validate if this field is reference type, if true, then validate <user list> type or others.
            * if <user list> type, then set <list type> false for the field
            *
            * out put type:
            * string, userList, reference
            * */
            if (OctaneConstants.KEY_FIELD_REFERENCE.equals(dataObj.getString(OctaneConstants.KEY_FIELD_FIELD_TYPE))) {
                fieldType = OctaneConstants.KEY_FIELD_REFERENCE;
                listType = true;
            } else {
                fieldType = OctaneConstants.KEY_FIELD_STRING;
            }

            if (dataObj.containsKey(OctaneConstants.KEY_FIELD_TYPE_DATA)) {
                JSONObject typeData = dataObj.getJSONObject(OctaneConstants.KEY_FIELD_TYPE_DATA);
                JSONArray targets = typeData.getJSONArray(OctaneConstants.KEY_FIELD_TARGETS);
                boolean multiple = typeData.getBoolean(OctaneConstants.KEY_FIELD_MUlTIPLE);
                for (int i = 0; i < targets.size(); i++) {
                    JSONObject target = targets.getJSONObject(i);
                    if (OctaneConstants.SUB_TYPE_LIST_NODE.equals(target.getString(OctaneConstants.KEY_FIELD_TYPE))) {
                        listType = true;
                        logicalName = target.getString(OctaneConstants.KEY_LOGICAL_NAME);
                        break;
                    } else if (OctaneConstants.SUB_TYPE_USER_NODE
                            .equals(target.getString(OctaneConstants.KEY_FIELD_TYPE))) {// multiple
                        listType = false;
                        fieldType = OctaneConstants.KEY_FIELD_USER_LIST;
                        multiValue = multiple;
                        break;

                    }
                }
            }
        } catch (Exception e) {
            throw new OctaneClientException("AGM_APP", "Error when reading JSon data from Octane: "+ e.getMessage());
        }
    }
}
