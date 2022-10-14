

package com.ppm.integration.agilesdk.connector.octane;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataPortfolio;
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
    public void createPortfolioEntities(ValueSet valueSet, List<AgileDataPortfolio> portfolios) {
        ClientPublicAPI client = ClientPublicAPI.getClient(valueSet);
        SharedSpace space = client.getActiveSharedSpace();
        JSONArray entityList = new JSONArray();
        for (AgileDataPortfolio p : portfolios) {
            JSONObject entityObj = new JSONObject();
            entityObj.put("name", p.getName());
            entityObj.put("type", "product");
            JSONObject parent = new JSONObject();
            parent.put("type", "product");
            if (p.getParent() == null) {
                parent.put("id", DEFAULT_ROOT_PRODUCT_ID);
            } else {
                parent.put("id", p.getParent().getId());
            }
            entityObj.put("parent", parent);
            entityList.add(entityObj);
        }
        client.saveProducts(space.getId(), HttpMethod.POST, entityList.toString());
    }

    /**
     * @param instance valueset
     * @param product it that want to delete
     * @see com.ppm.integration.agilesdk.pfm.PortfolioIntegration#deletePortfolioEntities(java.lang.String,
     *      java.lang.String, java.util.List)
     */
    @Override
    public void deletePortfolioEntities(final ValueSet instanceConfigurationParameters, List<String> portfolioIds) {

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
                aP.setId(p.getLong("id"));
                aP.setName(p.getString("name"));
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
    public void updatePortfolioEntities(ValueSet valueSet, List<AgileDataPortfolio> list) {
        // TODO Auto-generated method stub

    }

}
