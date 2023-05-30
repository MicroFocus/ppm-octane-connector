

package com.ppm.integration.agilesdk.connector.octane;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import javax.ws.rs.HttpMethod;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataError;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ppm.integration.agilesdk.agiledata.AgileDataStrategicThemeList;
import com.ppm.integration.agilesdk.agiledata.AgileDataStrategicTheme;

import com.ppm.integration.agilesdk.pfm.StrategicThemeIntegration;


public class OctaneStrategicThemeIntegration extends StrategicThemeIntegration {

    @Override
    public List<AgileDataStrategicTheme> getAgileStrategicThemeEntities(final ValueSet valueSet) {
        return new ArrayList<>();
    }
    /**
     * @param instance  valueset
     * @param StrategicTheme list
     * @see com.ppm.integration.agilesdk.pfm.StrategicThemeIntegration#createStrategicThemeEntities(com.ppm.integration.agilesdk.ValueSet,
     * java.lang.String, java.util.List)
     */
    @Override
    public AgileDataStrategicThemeList createStrategicThemeEntities(ValueSet valueSet, List<AgileDataStrategicTheme> strategicThemes) {
        return saveStrategicThemeEntities(valueSet, strategicThemes, HttpMethod.POST);
    }


    private AgileDataStrategicThemeList saveStrategicThemeEntities(ValueSet valueSet, List<AgileDataStrategicTheme> strategicThemes, String method) {
         AgileDataStrategicThemeList data = new AgileDataStrategicThemeList();
        if (strategicThemes.isEmpty()) return data;
        ClientPublicAPI client = ClientPublicAPI.getClient(valueSet);
        SharedSpace space = client.getActiveSharedSpace();
        JSONArray entityList = convertToJsonArray(strategicThemes);
        JSONObject dataObj = client.saveStrategicThemes(space.getId(), method, entityList.toString());

        convertDataArrayToBean(dataObj, data);
        convertErrorArrayToBean(dataObj, data, strategicThemes);
        convertExistErrorToDataBean(dataObj, strategicThemes, data, client, space);
        return data;
    }

    private void convertErrorArrayToBean(JSONObject dataObj, AgileDataStrategicThemeList list, List<AgileDataStrategicTheme> strategicThemes) {
        if (!dataObj.containsKey("errors")) return;
        JSONArray errorArray = JSONArray.fromObject(dataObj.get("errors"));
        if (errorArray.size() > 0) {
            for (int j = 0; j < errorArray.size(); j++) {
                AgileDataError error = new AgileDataError();
                JSONObject tempObj = errorArray.getJSONObject(j);
                if ("platform.duplicate_entity_error".equals(tempObj.getString("error_code"))) {
                    continue;
                }
                // default index
                error.setIndex(j);
                if (tempObj.containsKey("index")) {
                    error.setIndex(tempObj.getInt("index"));
                } else if (tempObj.containsKey("properties")) {
                    JSONObject proObject = tempObj.getJSONObject("properties");
                    if (proObject.containsKey("entity_id")) {
                        for (int k = 0; k < strategicThemes.size(); k++) {
                            if (String.valueOf(proObject.get("entity_id")).equals(strategicThemes.get(k).getId())) {
                                error.setIndex(k);
                                break;
                            }
                        }
                    }
                }
                error.setCode(tempObj.getString("error_code"));
                error.setMessage(tempObj.getString("description_translated"));
                list.addError(error);
            }
        }
    }

    private void convertExistErrorToDataBean(JSONObject dataObj, List<AgileDataStrategicTheme> strategicThemes, AgileDataStrategicThemeList list, ClientPublicAPI client, SharedSpace space) {
        if (!dataObj.containsKey("errors")) return;
        JSONArray errorArray = JSONArray.fromObject(dataObj.get("errors"));
        if (errorArray.size() == 0) {
            return;
        }
        for (int j = 0; j < errorArray.size(); j++) {
            AgileDataError error = new AgileDataError();
            JSONObject tempObj = errorArray.getJSONObject(j);
            if (!"platform.duplicate_entity_error".equals(tempObj.getString("error_code"))) {
                continue;
            }
            AgileDataStrategicTheme strategyData = new AgileDataStrategicTheme();
            AgileDataStrategicTheme existProd;
            if (strategicThemes.size() == 1) {
                existProd = strategicThemes.get(0);
            } else {
                existProd = strategicThemes.get(tempObj.getInt("index"));
            }
            strategyData.setName(existProd.getName());
            strategyData.setOriginalId(existProd.getOriginalId());
            List<String> queryFields = new ArrayList<>();
            queryFields.add("id");
            queryFields.add("name");
            List<String> originalIds = new ArrayList<>();
            originalIds.add(String.valueOf(existProd.getOriginalId()));

        }
    }

    private void convertDataArrayToBean(JSONObject dataObj, AgileDataStrategicThemeList list) {
        if (!dataObj.containsKey("data")) return;
        JSONArray strategyArray = JSONArray.fromObject(dataObj.get("data"));
        if (strategyArray.size() > 0) {
            for (int i = 0; i < strategyArray.size(); i++) {
                AgileDataStrategicTheme strategyData = new AgileDataStrategicTheme();
                JSONObject tempObj = strategyArray.getJSONObject(i);
                strategyData.setId(tempObj.getString("id"));
                strategyData.setOriginalId(tempObj.getLong("original_id"));
                if (tempObj.containsKey("name")) {
                    strategyData.setName(tempObj.getString("name"));
                }
                list.addAgileStrategicTheme(strategyData);
            }
        }
    }

    private JSONArray convertToJsonArray(List<AgileDataStrategicTheme> strategicThemes) {
        JSONArray entityList = new JSONArray();
        for (AgileDataStrategicTheme st : strategicThemes) {
            JSONObject entityObj = new JSONObject();
            if (!StringUtils.isBlank(st.getId())) {
                entityObj.put("id", st.getId());
            }
            entityObj.put("original_id", st.getOriginalId());
            entityObj.put("name", st.getName());
            JSONObject parent = new JSONObject();
            if (st.getParent() != null) {
                parent.put("id", st.getParent().getId());
                parent.put("type", "strategic_theme");
                entityObj.put("parent", parent);
            }
            JSONObject product = new JSONObject();
            if (st.getProduct() != null) {
                product.put("id", st.getProduct().getId());
                product.put("type", "product");
                entityObj.put("product", product);
            }
            entityList.add(entityObj);
        }
        return entityList;
    }



}
