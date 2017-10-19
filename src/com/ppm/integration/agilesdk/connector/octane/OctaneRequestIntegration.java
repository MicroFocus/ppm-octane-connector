package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.FieldInfo;
import com.ppm.integration.agilesdk.dm.AgileEntityMap;
import com.ppm.integration.agilesdk.dm.AgileEntityFieldInfo;
import com.ppm.integration.agilesdk.dm.AgileEntityInfo;
import com.ppm.integration.agilesdk.dm.RequestIntegration;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * {@code OctaneRequestIntegration} description
 * <p/>
 *
 * @author ChunQi, Lu
 * @since 10/16/2017
 */
public class OctaneRequestIntegration extends RequestIntegration {
    @Override
    public List<AgileEntityInfo> getAgileEntitiesInfo(final String agileProjectValue) {
        List<AgileEntityInfo> entityList = new ArrayList<AgileEntityInfo>();
        AgileEntityInfo feature = new AgileEntityInfo();
        feature.setName("Feature");
        feature.setType(OctaneConstants.SUB_TYPE_FEATURE);
        entityList.add(feature);
        AgileEntityInfo userStory = new AgileEntityInfo();
        userStory.setName("User Story");
        userStory.setType(OctaneConstants.SUB_TYPE_STORY);
        entityList.add(userStory);
        return entityList;
    }

    @Override
    public List<AgileEntityFieldInfo> getAgileEntityFieldsInfo(final String entityType, final String agileProjectValue, final ValueSet instanceConfigurationParameters) {
        List<AgileEntityFieldInfo> fieldList = new ArrayList<AgileEntityFieldInfo>();
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
        List<FieldInfo> fields = client.getEntityFields(sharedSpaceId, workSpaceId, entityType);
        for (FieldInfo field : fields) {
            AgileEntityFieldInfo info = new AgileEntityFieldInfo();
            info.setLabel(field.getLabel());
            info.setKey(field.getName());
            fieldList.add(info);
        }
        return fieldList;
    }

    @Override
    public String createEntity(AgileEntityMap entity) {
        return null;
    }
}
