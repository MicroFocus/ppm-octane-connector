
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

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.FieldInfo;
import com.ppm.integration.agilesdk.connector.octane.model.SimpleEntity;
import com.ppm.integration.agilesdk.dm.DataField;
import com.ppm.integration.agilesdk.dm.MultiUserField;
import com.ppm.integration.agilesdk.dm.RequestIntegration;
import com.ppm.integration.agilesdk.dm.StringField;
import com.ppm.integration.agilesdk.dm.User;
import com.ppm.integration.agilesdk.dm.UserField;
import com.ppm.integration.agilesdk.model.AgileEntity;
import com.ppm.integration.agilesdk.model.AgileEntityFieldInfo;
import com.ppm.integration.agilesdk.model.AgileEntityInfo;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;

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

    private UserProvider up = null;

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
            info.setFieldType(field.getFieldType() != null ? field.getFieldType().toUpperCase() : "");
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
    public AgileEntity updateEntity(final String agileProjectValue, final String entityType, final AgileEntity entity,
            final ValueSet instanceConfigurationParameters)
    {
        AgileEntity result = null;
        try {
            result = saveOrUpdateEntity(agileProjectValue, entityType, entity, instanceConfigurationParameters);
        } catch (OctaneClientException ex) {
            throw ex;
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
        } catch (OctaneClientException ex) {
            throw ex;
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
            List<JSONObject> featureJson  = client.getFeaturesAfterDate(sharedSpaceId, workSpaceId, entityIds, lastUpdateTime);
            entities = getAgileEntities(client, featureJson, sharedSpaceId, workSpaceId, "feature");
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            List<JSONObject> userStoryJson = client.getUserStoriesAfterDate(sharedSpaceId, workSpaceId, entityIds, lastUpdateTime);
            entities = getAgileEntities(client, userStoryJson, sharedSpaceId, workSpaceId, "story");
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
            List<JSONObject> featureJson = client.getFeature(sharedSpaceId, workSpaceId, entityId);
            if (featureJson.size() > 0) {
                Map<String, FieldInfo> fieldInfoMap  = getFieldInfoMap(client, sharedSpaceId, workSpaceId, "feature");
                entity = wrapperEntity(featureJson.get(0), fieldInfoMap);
            }

        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            List<JSONObject> userStoryJson = client.getUserStory(sharedSpaceId, workSpaceId, entityId);
            if (userStoryJson.size() > 0) {
                Map<String, FieldInfo> fieldInfoMap  = getFieldInfoMap(client, sharedSpaceId, workSpaceId, "story");
                entity = wrapperEntity(userStoryJson.get(0), fieldInfoMap);
            }
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
            JSONObject feature = client.saveFeatureInWorkspace(sharedSpaceId, workSpaceId, entityStr, method);
            agileEntity = wrapperEntity(feature, null);
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            SimpleEntity root = client.getWorkItemRoot(Integer.parseInt(sharedSpaceId), Integer.parseInt(workSpaceId));
            String entityStr = buildEntity(client, sharedSpaceId, fieldInfos, entityType, entity, root);
            JSONObject userStory = client.saveStoryInWorkspace(sharedSpaceId, workSpaceId, entityStr, method);
            agileEntity = wrapperEntity(userStory, null);
        }
        if (agileEntity != null) {
            agileEntity.setEntityUrl(
                    String.format(ClientPublicAPI.DEFAULT_ENTITY_ITEM_URL, client.getBaseURL(), sharedSpaceId, workSpaceId, agileEntity.getId()));
        }
        return agileEntity;
    }

    private String buildEntity(final ClientPublicAPI client, final String sharedSpceId,
            final List<FieldInfo> fieldInfos, final String entityType, AgileEntity entity, SimpleEntity root)
    {
        JSONArray entityList = new JSONArray();
        JSONObject entityObj = new JSONObject();

        Map<String, FieldInfo> fieldInfoMap = new HashMap<String, FieldInfo>();
        for (FieldInfo info : fieldInfos) {
            fieldInfoMap.put(info.getName(), info);
        }

        Iterator<Entry<String, DataField>> it = entity.getAllFields();
        while (it.hasNext()) {
            Entry<String, DataField> entry = it.next();
            String key = entry.getKey();
            FieldInfo fieldInfo = fieldInfoMap.get(key);
            DataField field = entry.getValue();
            if (field == null) {
                if (fieldInfo.isMultiValue()) {
                    entityObj.put(key, createNullJSONObject(true));
                } else {
                    entityObj.put(key, createNullJSONObject(false));
                }
                continue;
            }
            switch (field.getType()) {
                case STRING:
                    StringField stringField = (StringField) field;
                    entityObj.put(key, stringField.get());
                    break;
                case USER:
                    if (field.isList()) {
                        MultiUserField userField = (MultiUserField) field;
                        JSONObject obj = transformUsers(client, fieldInfo, userField.get(), sharedSpceId);
                        entityObj.put(entry.getKey(), obj);
                    } else {
                        UserField userField = (UserField) field;
                        List<User> userList = new ArrayList<User>();
                        userList.add(userField.get());
                        JSONObject obj = transformUsers(client, fieldInfo, userList, sharedSpceId);
                        entityObj.put(entry.getKey(), obj);
                    }
                    break;
            }
        }

        if (entity.getId() != null) {
            entityObj.put(OctaneConstants.KEY_FIELD_ID, entity.getId());
        } else {
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

    private JSONObject transformUsers(ClientPublicAPI client, FieldInfo userFieldInfo, List<User> users,
            String shareSpaceId)
    {
        if (users != null && !users.isEmpty()) {
            List<String> emails = new ArrayList<String>();

            for (User user : users) {
                if (user.getEmail() != null) {
                    emails.add(user.getEmail());
                }
            }
            if (!emails.isEmpty()) {
                JSONArray emailArray = client.getUsersByEmail(shareSpaceId, emails.toArray(new String[emails.size()]));
                if (emailArray.size() > 0) {
                    if (userFieldInfo.isMultiValue()) {
                        JSONObject userList = new JSONObject();
                        JSONArray userArr = new JSONArray();
                        for (int i = 0; i < emailArray.size(); i++) {
                            JSONObject tempObj = emailArray.getJSONObject(i);
                            userArr.add(getUserJsonObject(tempObj));
                        }
                        userList.put("data", userArr);
                        return userList;
                    } else {
                        return getUserJsonObject(emailArray.getJSONObject(0));
                    }
                }
            }
        }

        if (userFieldInfo.isMultiValue()) {
            return createNullJSONObject(true);
        } else {
            return createNullJSONObject(false);
        }
    }

    private JSONObject getUserJsonObject(JSONObject tempObj) {
        String id = tempObj.getString("id");
        JSONObject obj = new JSONObject();
        obj.put("type", "workspace_user");
        obj.put("id", id);
        return obj;
    }

    private synchronized UserProvider getUserProvider() {
        if (up == null) {
            up = Providers.getUserProvider(OctaneIntegrationConnector.class);
        }
        return up;
    }

    private Date parserDate(String dateStr) {
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
        DateTime dateTimeHere = parser.parseDateTime(dateStr);
        return dateTimeHere.toDate();

    }

    private AgileEntity wrapperEntity(JSONObject item, Map<String, FieldInfo> fieldInfoMap) {

        AgileEntity entity = new AgileEntity();
        Iterator<String> sIterator = item.keys();
        while (sIterator.hasNext()) {
            String key = sIterator.next();
            if (key.equals(ClientPublicAPI.KEY_LAST_UPDATE_DATE)) {
                String value = item.getString(key);
                entity.setLastUpdateTime(parserDate(value));
            } else if (key.equals("id")) {
                String value = item.getString(key);
                entity.setId(value);
            } else if (fieldInfoMap != null && fieldInfoMap.get(key) != null) {
                FieldInfo info = fieldInfoMap.get(key);
                if (info.getFieldType().equals("userList")) {
                    JSONObject value = item.getJSONObject(key);
                    if (info.isMultiValue()) {
                        JSONArray users = value.getJSONArray("data");
                        if (!users.isEmpty()) {
                            List<User> userList = new ArrayList<User>();
                            for (int i = 0; i < users.size(); i++) {
                                JSONObject userObj = users.getJSONObject(i);
                                User user = buildUser(userObj);
                                if (user != null) {
                                    userList.add(user);
                                }
                            }
                            MultiUserField multiUserField = new MultiUserField();
                            multiUserField.set(userList);
                            entity.addField(key, multiUserField);
                        } else {
                            entity.addField(key, null);
                        }

                    } else {
                        if (canParseJson(value, "name")) {
                            User user = buildUser(value);
                            if (user != null) {
                                UserField userField = new UserField();
                                userField.set(user);
                                entity.addField(key, userField);
                            }
                        } else {
                            entity.addField(key, null);
                        }

                    }

                } else if (info.getFieldType().equals("string")) {
                    String value = item.getString(key);
                    if (value == null || value.equals("null")) {
                        value = "";
                    }
                    StringField stringField = new StringField();
                    stringField.set(value);
                    entity.addField(key, stringField);
                }
            }
        }
        return entity;
    }

    private User buildUser(JSONObject userJson) {
        User user = null;
        if (userJson.containsKey("name")) {
            String email = userJson.getString("name");
            com.hp.ppm.user.model.User userModel = getUserProvider().getByEmail(email);
            if (userModel != null) {
               user = new User();
                user.setUserId(userModel.getUserId());
                user.setEmail(userModel.getEmail());
                user.setFullName(userModel.getFullName());
                user.setUsername(userModel.getUserName());
            }
        }
        return user;
    }

    private boolean canParseJson(JSONObject jsonObj, String key) {
        if (jsonObj != null && jsonObj.containsKey(key)) {
            return true;
        } else {
            return false;
        }
    }

    private JSONObject createNullJSONObject(boolean isMulti) {
        if (isMulti) {
            JSONObject nullObj = new JSONObject();
            nullObj.put("data",  new JSONArray());
            return nullObj;
        } else {
            return new JSONObject(true);
        }
    }

    private Map<String, FieldInfo> getFieldInfoMap(ClientPublicAPI client, String sharedspaceId, String workspaceId, String entityType) {
        Map<String, FieldInfo> fieldInfoMap = new HashMap<String, FieldInfo>();
        List<FieldInfo> fieldInfos = client.getEntityFields(sharedspaceId, workspaceId, entityType);
        for (FieldInfo info : fieldInfos) {
            fieldInfoMap.put(info.getName(), info);
        }
        return fieldInfoMap;
    }

    private List<AgileEntity> getAgileEntities(ClientPublicAPI client, List<JSONObject> workItemsJson, String sharedspaceId, String workspaceId, String entityType) {
        List<AgileEntity> agileEntities = new ArrayList<>();
        if (workItemsJson != null && workItemsJson.size() > 0) {
            Map<String, FieldInfo> fieldInfoMap  = getFieldInfoMap(client, sharedspaceId, workspaceId, entityType);
            for (JSONObject workItemJson : workItemsJson) {
                AgileEntity entity = wrapperEntity(workItemJson, fieldInfoMap);
                agileEntities.add(entity);
            }
        }
        return agileEntities;
    }

}

class AgileFieldComparator implements Comparator<AgileEntityFieldInfo> {
    @Override
    public int compare(final AgileEntityFieldInfo o1, final AgileEntityFieldInfo o2) {
        return o1.getLabel().compareToIgnoreCase(o2.getLabel());
    }
}
