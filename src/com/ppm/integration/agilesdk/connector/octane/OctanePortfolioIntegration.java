

package com.ppm.integration.agilesdk.connector.octane;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import javax.ws.rs.HttpMethod;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataError;
import com.ppm.integration.agilesdk.agiledata.AgileDataPortfolio;
import com.ppm.integration.agilesdk.agiledata.AgileDataPortfolioList;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.pfm.PortfolioIntegration;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * TODO Class/Interface description.
 */

public class OctanePortfolioIntegration extends PortfolioIntegration {

    /** TODO Comment for <code>DEFAULT_ROOT_PRODUCT_ID</code>. */
    
    private static final int DEFAULT_ROOT_PRODUCT_ID = 1001;

    /**
     * @param instance valueset
     * @param portfolio list
     * @see com.ppm.integration.agilesdk.pfm.PortfolioIntegration#createPortfolioEntities(com.ppm.integration.agilesdk.ValueSet,
     *      java.lang.String, java.util.List)
     */
    @Override
    public AgileDataPortfolioList createPortfolioEntities(ValueSet valueSet, List<AgileDataPortfolio> products) {
        return savePortfolioEntities(valueSet, products, HttpMethod.POST);
    }

    /**
     * @param instance valueset
     * @param product it that want to delete
     * @see com.ppm.integration.agilesdk.pfm.PortfolioIntegration#deletePortfolioEntities(java.lang.String,
     *      java.lang.String, java.util.List)
     */
    @Override
    public AgileDataPortfolioList deletePortfolioEntities(final ValueSet valueSet, List<String> productIds) {
        ClientPublicAPI client = ClientPublicAPI.getClient(valueSet);
        SharedSpace space = client.getActiveSharedSpace();
        JSONObject dataObj = client.deleteProducts(space.getId(), convertToDeleteJsonArray(productIds).toString());
        AgileDataPortfolioList data = new AgileDataPortfolioList();
        if (dataObj == null) return data;
        convertDataArrayToBean(dataObj, data);
        convertErrorArrayToBean(dataObj, data);
        return data;
    }

    /**
     * @param instance valueset
     * @return
     * @see com.ppm.integration.agilesdk.pfm.PortfolioIntegration#getAgilePortfolioEntities(com.ppm.integration.agilesdk.ValueSet,
     *      java.lang.String)
     */
    @Override
    public List<AgileDataPortfolio> getAgilePortfolioEntities(ValueSet valueSet) {
        ClientPublicAPI client = ClientPublicAPI.getClient(valueSet);
        SharedSpace space = client.getActiveSharedSpace();
        List<AgileDataPortfolio> ps = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        fields.add("name");
        if (space != null) {
            List<JSONObject> products = client.getProducts(space.getId(), fields);
            for (JSONObject p : products) {
                AgileDataPortfolio aP = new AgileDataPortfolio();
                aP.setId(p.getString("id"));
                aP.setName(p.getString("name"));
                aP.setOriginalId(p.getLong("original_id"));
                ps.add(aP);
            }
        }
        return ps;
    }

    /**
     * @param instance valueset
     * @param portfolio list
     * @see com.ppm.integration.agilesdk.pfm.PortfolioIntegration#updatePortfolioEntities(com.ppm.integration.agilesdk.ValueSet,
     *      java.lang.String, java.util.List)
     */
    @Override
    public AgileDataPortfolioList updatePortfolioEntities(ValueSet valueSet, List<AgileDataPortfolio> products) {
        return savePortfolioEntities(valueSet, products, HttpMethod.PUT);
    }

    private AgileDataPortfolioList savePortfolioEntities(ValueSet valueSet, List<AgileDataPortfolio> products, String method) {
        AgileDataPortfolioList data = new AgileDataPortfolioList();
        if (products.isEmpty()) return data;
        ClientPublicAPI client = ClientPublicAPI.getClient(valueSet);
        SharedSpace space = client.getActiveSharedSpace();
        JSONArray entityList = convertToJsonArray(products);
        JSONObject dataObj = client.saveProducts(space.getId(), method, entityList.toString());

        convertDataArrayToBean(dataObj, data);
        convertErrorArrayToBean(dataObj, data);

        return data;
    }

    private void convertErrorArrayToBean(JSONObject dataObj, AgileDataPortfolioList list) {
        if (!dataObj.containsKey("errors")) return;
        JSONArray errorArray = JSONArray.fromObject(dataObj.get("errors"));
        if (errorArray.size() > 0) {
            for (int j = 0; j < errorArray.size(); j++) {
                AgileDataError error = new AgileDataError();
                JSONObject tempObj = errorArray.getJSONObject(j);
                error.setIndex(tempObj.getInt("index"));
                error.setCode(tempObj.getString("error_code"));
                error.setMessage(tempObj.getString("description_translated"));
                list.addError(error);
            }
        }
    }

    private void convertDataArrayToBean(JSONObject dataObj, AgileDataPortfolioList list) {
        if (!dataObj.containsKey("data")) return;
        JSONArray productArray = JSONArray.fromObject(dataObj.get("data"));
        if (productArray.size() > 0) {
            for (int i = 0; i < productArray.size(); i++) {
                AgileDataPortfolio productData = new AgileDataPortfolio();
                JSONObject tempObj = productArray.getJSONObject(i);
                productData.setId(tempObj.getString("id"));
                productData.setOriginalId(tempObj.getLong("original_id"));
                if (tempObj.containsKey("name")) {
                    productData.setName(tempObj.getString("name"));
                }
                list.addAgilePortfolio(productData);
            }
        }
    }

    private JSONArray convertToJsonArray(List<AgileDataPortfolio> products) {
        JSONArray entityList = new JSONArray();
        for (AgileDataPortfolio p : products) {
            JSONObject entityObj = new JSONObject();
            if (!StringUtils.isBlank(p.getId())) {
                entityObj.put("id", p.getId());
            }
            entityObj.put("original_id", p.getOriginalId());
            entityObj.put("name", p.getName());
            entityObj.put("type", "product");
            JSONObject parent = new JSONObject();
            if (p.getParent() == null) {
                parent.put("id", DEFAULT_ROOT_PRODUCT_ID);
            } else {
                parent.put("id", p.getParent().getId());
                parent.put("original_id", p.getParent().getOriginalId());
            }
            parent.put("type", "product");
            entityObj.put("parent", parent);
            entityList.add(entityObj);
        }
        return entityList;
    }

    private JSONArray convertToDeleteJsonArray(List<String> entityIds) {
        JSONArray entityList = new JSONArray();
        for (String entityId : entityIds) {
            JSONObject entityObj = new JSONObject();
            entityObj.put("id", entityId);
            entityObj.put("type", "product");
            // long
            entityObj.put("activity_level", 1);
            entityList.add(entityObj);
        }
        return entityList;
    }

}
