
package com.ppm.integration.agilesdk.connector.octane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.HttpMethod;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.FieldInfo;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.dm.AgileEntityInfo;
import com.ppm.integration.agilesdk.dm.CodeMeaningField;
import com.ppm.integration.agilesdk.dm.DataField;
import com.ppm.integration.agilesdk.dm.DateField;
import com.ppm.integration.agilesdk.dm.MultiCodeMeaningField;
import com.ppm.integration.agilesdk.dm.MultiUserField;
import com.ppm.integration.agilesdk.dm.RequestIntegration;
import com.ppm.integration.agilesdk.dm.TextField;
import com.ppm.integration.agilesdk.dm.UserField;
import com.ppm.integration.agilesdk.model.AgileEntity;
import com.ppm.integration.agilesdk.model.AgileEntityField;
import com.ppm.integration.agilesdk.model.AgileEntityFieldInfo;
import com.ppm.integration.agilesdk.model.AgileEntityFieldInfo.AgileEntityFieldType;

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
    public List<AgileEntityInfo> getAgileEntitiesInfo(final String agileProjectValue,
            final ValueSet instanceConfigurationParameters)
    {
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
            info.setFieldType(AgileEntityFieldType.fromValue(field.getFieldType()));
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
    public List<AgileEntityField> getAgileEntityFieldValueList(final String agileProjectValue, final String entityType,
            final String fieldInfo, final ValueSet instanceConfigurationParameters)
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
    public AgileEntity updateEntity(final String agileProjectValue, final String entityType, final AgileEntity entity,
            final ValueSet instanceConfigurationParameters)
    {
        AgileEntity result = null;
        try {
            result = saveOrUpdateEntity(agileProjectValue, entityType, entity, instanceConfigurationParameters);
        } catch (Exception e) {
            throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR", new String[] {e.getMessage()});
        }
        return result;
    }

    @Override
    public AgileEntity createEntity(final String agileProjectValue, final String entityType, final AgileEntity entity,
            final ValueSet instanceConfigurationParameters)
    {
        AgileEntity result = null;
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
            entities = client.getFeaturesAfterDate(sharedSpaceId, workSpaceId, entityIds, lastUpdateTime);
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            entities = client.getUserStoriesAfterDate(sharedSpaceId, workSpaceId, entityIds, lastUpdateTime);
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

    private AgileEntity saveOrUpdateEntity(final String agileProjectValue, final String entityType,
            final AgileEntity entity, final ValueSet instanceConfigurationParameters)
    {
        AgileEntity agileEntity = null;
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);

        List<FieldInfo> fieldInfos = client.getEntityFields(sharedSpaceId, workSpaceId, entityType);
        String method = HttpMethod.POST;
        if (entity.getId() != null) {
            method = HttpMethod.PUT;
        }
        if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
            String entityStr = buildEntity(client, sharedSpaceId, fieldInfos, entityType, entity, null);
            try {
                agileEntity = client.saveFeatureInWorkspace(sharedSpaceId, workSpaceId, entityStr, method);
            } catch (Exception e) {
                throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR",
                        new String[] {e.getMessage()});
            }
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            WorkItemRoot root = client.getWorkItemRoot(Integer.parseInt(sharedSpaceId), Integer.parseInt(workSpaceId));
            String entityStr = buildEntity(client, sharedSpaceId, fieldInfos, entityType, entity, root);
            agileEntity = client.saveStoryInWorkspace(sharedSpaceId, workSpaceId, entityStr, method);
        }
        return agileEntity;

    }

    private String buildEntity(final ClientPublicAPI client, final String sharedSpceId,
            final List<FieldInfo> fieldInfos, final String entityType, AgileEntity entity, WorkItemRoot root)
    {
        JSONArray entityList = new JSONArray();
        JSONObject entityObj = new JSONObject();
        boolean existName = false;

        Map<String, FieldInfo> fieldInfoMap = new HashMap<String, FieldInfo>();
        for (FieldInfo info : fieldInfos) {
            fieldInfoMap.put(info.getName(), info);
        }

        Iterator<Entry<String, DataField>> it = entity.getAllFields();
        while (it.hasNext()) {
            Entry<String, DataField> entry = it.next();
            String key = entry.getKey();
            if (key.equals(OctaneConstants.KEY_FIELD_NAME))
                existName = true;

            FieldInfo fieldInfo = fieldInfoMap.get(entry.getKey());

            DataField field = entry.getValue();
            if (field instanceof TextField) {
                TextField textField = (TextField)field;
                entityObj.put(key, textField.getText());
            } else if (field instanceof MultiUserField) {
                if (fieldInfo != null && fieldInfo.getFieldType().equals("userList")) {
                    MultiUserField userFields = (MultiUserField)field;
                    JSONObject obj = transformUserField(client, fieldInfo, userFields.getFields(), sharedSpceId);
                    entityObj.put(entry.getKey(), obj);
                }
            } else if (field instanceof CodeMeaningField) {

            } else if (field instanceof MultiCodeMeaningField) {

            } else if (field instanceof DateField) {

            }
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

    @Override
    public void postDeleteRequest(final String agileProjectValue, final String entityType,
            final ValueSet instanceConfigurationParameters, String entityId)
    {
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);

        if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
            client.deleteEntity(sharedSpaceId, workSpaceId, "features", entityId);
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            client.deleteEntity(sharedSpaceId, workSpaceId, "stories", entityId);
        }

    }

    private JSONObject transformUserField(ClientPublicAPI client, FieldInfo userFieldInfo, Set<UserField> fields,
            String shareSpaceId)
    {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        String[] fullNames = new String[fields.size()];
        int index = 0;
        for (UserField field : fields) {
            fullNames[index] = field.getFullName();
            index++;
        }
        net.sf.json.JSONArray jsonArray = client.getUsersByFullName(shareSpaceId, fullNames);
        if (jsonArray.size() < 1)
            return null;
        if (userFieldInfo.isMultiValue()) {
            JSONObject userList = new JSONObject();
            JSONArray userArr = new JSONArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                net.sf.json.JSONObject tempObj = jsonArray.getJSONObject(i);
                String id = (String)tempObj.get("id");
                JSONObject obj = new JSONObject();
                obj.put("type", "workspace_user");
                obj.put("id", id);
                userArr.add(obj);
            }
            userList.put("data", userArr);
            return userList;

        } else {
            net.sf.json.JSONObject tempObj = jsonArray.getJSONObject(0);
            String id = (String)tempObj.get("id");
            JSONObject obj = new JSONObject();
            obj.put("type", "workspace_user");
            obj.put("id", id);
            return obj;
        }
    }
}

class AgileFieldComparator implements Comparator<AgileEntityFieldInfo> {
    @Override
    public int compare(final AgileEntityFieldInfo o1, final AgileEntityFieldInfo o2) {
        return o1.getLabel().compareToIgnoreCase(o2.getLabel());
    }
}
