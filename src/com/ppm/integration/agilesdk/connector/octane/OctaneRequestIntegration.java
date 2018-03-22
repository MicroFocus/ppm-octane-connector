
package com.ppm.integration.agilesdk.connector.octane;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.HttpMethod;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.FieldInfo;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.dm.AgileEntityInfo;
import com.ppm.integration.agilesdk.dm.RequestIntegration;
import com.ppm.integration.agilesdk.model.AgileEntity;
import com.ppm.integration.agilesdk.model.AgileEntityField;
import com.ppm.integration.agilesdk.model.AgileEntityFieldInfo;
import com.ppm.integration.agilesdk.model.AgileEntityFieldValue;
import com.ppm.integration.agilesdk.model.AgileEntityUrl;

import edu.emory.mathcs.backport.java.util.Collections;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * {@code OctaneRequestIntegration} description
 * <p/>
 * @author ChunQi, Lu
 * @since 10/16/2017
 */
public class OctaneRequestIntegration extends RequestIntegration {

    @Override
    public List<AgileEntityInfo> getAgileEntitiesInfo(final String agileProjectValue, final ValueSet instanceConfigurationParameters) {
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
    public List<AgileEntityFieldInfo> getAgileEntityFieldsInfo(final String agileProjectValue, final String entityType,
            final ValueSet instanceConfigurationParameters)
    {
        List<AgileEntityFieldInfo> fieldList = new ArrayList<AgileEntityFieldInfo>();
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
        List<FieldInfo> fields = client.getEntityFields(sharedSpaceId, workSpaceId, entityType);
        for (FieldInfo field : fields) {
            AgileEntityFieldInfo info = new AgileEntityFieldInfo();
            info.setLabel(field.getLabel());
            info.setId(field.getName());
            info.setListType(field.getListType());
            JSONObject valueObj = new JSONObject();
            valueObj.put(OctaneConstants.KEY_FIELD_NAME, field.getName());
            valueObj.put(OctaneConstants.KEY_LOGICAL_NAME, field.getLogicalName());
            info.setListIdentifier(valueObj.toString());
            fieldList.add(info);
        }
        Collections.sort(fieldList, new AgileFieldComparator());
        return fieldList;
    }

    @Override
    public List<AgileEntityField> getAgileEntityFieldValueList(final String agileProjectValue, final String fieldInfo,
            final ValueSet instanceConfigurationParameters)
    {
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);

        JSONObject fieldObj = (JSONObject)JSONSerializer.toJSON(fieldInfo);
        String logicalName = fieldObj.getString(OctaneConstants.KEY_LOGICAL_NAME);
        return client.getEntityFieldValueList(sharedSpaceId, workSpaceId, logicalName);
    }

    @Override
    public AgileEntityUrl updateEntity(final String agileProjectValue, final String entityType,
            final AgileEntity entity, final ValueSet instanceConfigurationParameters)
    {
        AgileEntityUrl result = null;
        try {
            result = saveOrUpdateEntity(agileProjectValue, entityType, entity, instanceConfigurationParameters);
        } catch (Exception e) {
            throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR", new String[] {e.getMessage()});
        }
        return result;
    }

    @Override
    public AgileEntityUrl createEntity(final String agileProjectValue, final String entityType,
            final AgileEntity entity, final ValueSet instanceConfigurationParameters)
    {
        AgileEntityUrl result = null;
        try {
            result = saveOrUpdateEntity(agileProjectValue, entityType, entity, instanceConfigurationParameters);
        } catch (Exception e) {
            throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR", new String[] {e.getMessage()});
        }
        return result;

    }

    @Override
    public List<AgileEntity> getEntities(final String agileProjectValue, final String entityType,
            final ValueSet instanceConfigurationParameters, Set<String> entityIds, Date lastUpdateTime)
    {
        List<AgileEntity> entities = null;

        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);

        if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
            entities = client.getFeatures(sharedSpaceId, workSpaceId, entityIds);
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            entities = client.getUserStories(sharedSpaceId, workSpaceId, entityIds);
        }

        return entities;
    }

    @Override
    public AgileEntity getEntity(final String agileProjectValue, final String entityType,
            final ValueSet instanceConfigurationParameters, String entityId)
    {
        AgileEntity entity = null;

        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);

        if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
            entity = client.getFeature(sharedSpaceId, workSpaceId, entityId);
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            entity = client.getUserStory(sharedSpaceId, workSpaceId, entityId);
        }

        return entity;
    }

    private AgileEntityUrl saveOrUpdateEntity(final String agileProjectValue, final String entityType,
            final AgileEntity entity, final ValueSet instanceConfigurationParameters)
    {
        AgileEntityUrl resultUrl = null;
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
        String method = HttpMethod.POST;
        if (entity.getId() != null) {
            method = HttpMethod.PUT;
        }
        if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
            String entityStr = buildEntity(entityType, entity, null);
            try {
                resultUrl = client.saveFeatureInWorkspace(sharedSpaceId, workSpaceId, entityStr, method);
            } catch (Exception e) {
                throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR",
                        new String[] {e.getMessage()});
            }
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            WorkItemRoot root = client.getWorkItemRoot(Integer.parseInt(sharedSpaceId), Integer.parseInt(workSpaceId));
            String entityStr = buildEntity(entityType, entity, root);
            resultUrl = client.saveStoryInWorkspace(sharedSpaceId, workSpaceId, entityStr, method);
        }
        return resultUrl;

    }

    private String buildEntity(final String entityType, AgileEntity entity, WorkItemRoot root) {
        JSONArray entityList = new JSONArray();
        JSONObject entityObj = new JSONObject();
        boolean existName = false;

        Iterator<Entry<String, List<AgileEntityFieldValue>>> it = entity.getAllFields();
        while (it.hasNext()) {
            Entry<String, List<AgileEntityFieldValue>> entry = it.next();
            String key = entry.getKey();
            if (key.equals(OctaneConstants.KEY_FIELD_NAME))
                existName = true;

            entityObj.put(entry.getKey(), entry.getValue().get(0).getValue());
        }

        if (!existName) {
            entityObj.put(OctaneConstants.KEY_FIELD_NAME, OctaneConstants.KEY_FIELD_NAME_DEFAULT_VALUE);
        }
        if (entity.getId() != null) {
            entityObj.put(OctaneConstants.KEY_FIELD_ID, entity.getId());
        }

        JSONObject complexObj = new JSONObject();
        if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            complexObj.put("id", "phase.story.new");
        } else {
            complexObj.put("id", "phase.feature.new");
        }
        complexObj.put("type", "phase");
        entityObj.put("phase", complexObj);

        if (root != null) {
            JSONObject parent = new JSONObject();
            parent.put("id", root.id);
            parent.put("type", root.type);
            entityObj.put("parent", parent);
        }
        entityList.add(entityObj);

        return entityList.toString();
    }
}

class AgileFieldComparator implements Comparator<AgileEntityFieldInfo> {
    @Override
    public int compare(final AgileEntityFieldInfo o1, final AgileEntityFieldInfo o2) {
        return o1.getLabel().compareToIgnoreCase(o2.getLabel());
    }
}
