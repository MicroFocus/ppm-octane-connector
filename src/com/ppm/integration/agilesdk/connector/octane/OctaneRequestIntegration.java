
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
import java.util.StringTokenizer;

import javax.ws.rs.HttpMethod;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.hp.ppm.integration.model.AgileEntityFieldValue;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.FieldInfo;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.SimpleEntity;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.dm.DataField;
import com.ppm.integration.agilesdk.dm.DataField.DATA_TYPE;
import com.ppm.integration.agilesdk.dm.ListNode;
import com.ppm.integration.agilesdk.dm.ListNodeField;
import com.ppm.integration.agilesdk.dm.MemoField;
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
    
    public static final String SHARED_SPACE_MODE_SHARED = "SHARED";      // all vale:SHARED,ISOLATED

    @Override
    public List<AgileEntityInfo> getAgileEntitiesInfo(final String agileProjectValue,
            final ValueSet instanceConfigurationParameters)
    {

        List<AgileEntityInfo> entityList = new ArrayList<AgileEntityInfo>();
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        SharedSpace space = client.getActiveSharedSpace();
        if (null != space) {
            AgileEntityInfo epic = new AgileEntityInfo();
            epic.setName("Epic");
            epic.setType(OctaneConstants.SUB_TYPE_EPIC);
            entityList.add(epic);
            AgileEntityInfo feature = new AgileEntityInfo();
            feature.setName("Feature");
            feature.setType(OctaneConstants.SUB_TYPE_FEATURE);
            entityList.add(feature);
            AgileEntityInfo userStory = new AgileEntityInfo();
            userStory.setName("User Story");
            userStory.setType(OctaneConstants.SUB_TYPE_STORY);
            entityList.add(userStory);
            if(SHARED_SPACE_MODE_SHARED.equalsIgnoreCase(space.getMode())) {
                AgileEntityInfo sharedEpic = new AgileEntityInfo();
           	 	sharedEpic.setName("Shared Epic");
           	 	sharedEpic.setType(OctaneConstants.SUB_SHARED_EPIC);
                entityList.add(sharedEpic);
            }
        } 
        return entityList;
    }

    @Override
    public List<AgileEntityFieldInfo> getAgileEntityFieldsInfo(final String agileProjectValue, final String entityType,
            final ValueSet instanceConfigurationParameters)
    {
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        List<FieldInfo> fields = new ArrayList<>();
        if (OctaneConstants.WILDCARD_PLACEHOLDER.equalsIgnoreCase(agileProjectValue)) {
            fields = getWildcardFields(entityType, client);
        } else {
            JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
            String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
            String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
            fields = client.getEntityFields(sharedSpaceId, workSpaceId, entityType);
        }

        List<AgileEntityFieldInfo> fieldList = transferModel(fields);
        Collections.sort(fieldList, new AgileFieldComparator());
        client.signOut(instanceConfigurationParameters);
        return fieldList;
    }

    private List<AgileEntityFieldInfo> transferModel(List<FieldInfo> fields) {
        List<AgileEntityFieldInfo> fieldList = new ArrayList<AgileEntityFieldInfo>();
        for (FieldInfo field : fields) {
            AgileEntityFieldInfo info = new AgileEntityFieldInfo();
            info.setFieldType(getAgileFieldtype(field.getFieldType()));
            info.setLabel(field.getLabel());
            info.setListType(field.getListType());
            String fieldName = field.getName();
            info.setId(fieldName);
            JSONObject valueObj = new JSONObject();
            valueObj.put(OctaneConstants.KEY_FIELD_NAME, fieldName);
            valueObj.put(OctaneConstants.KEY_LOGICAL_NAME, field.getLogicalName());
            info.setListIdentifier(valueObj.toString());
            info.setMultiValue(field.isMultiValue());
            fieldList.add(info);
        }
        return fieldList;
    }

    private List<FieldInfo> getWildcardFields(final String entityType, ClientPublicAPI client) {
        SharedSpace space = client.getActiveSharedSpace();
        if (space != null) {
            if (OctaneConstants.SUB_SHARED_EPIC.equalsIgnoreCase(entityType)) {
                return client.getEntityFields(space.getId(), OctaneConstants.SHARED_EPIC_DEFAULT_WORKSPACE, entityType);
            } else {
                List<WorkSpace> workspaces = client.getWorkSpaces(Integer.parseInt(space.getId()));
                Map<String, FieldInfo> fieldsMap = new HashMap<>();
                if (!workspaces.isEmpty()) {
                    fieldsMap = getFieldInfoMap(client, space.getId(), workspaces.get(0).getId(), entityType);
                    for (int i = 1; i < workspaces.size(); i++) {
                        WorkSpace ws = workspaces.get(i);
                        List<FieldInfo> wsfields = client.getEntityFields(space.getId(), ws.getId(), entityType);
                        for (FieldInfo field : wsfields) {
                            if (!fieldsMap.containsKey(field.getName())) {
                                fieldsMap.put(field.getName(), field);
                            }
                        }
                    }
                }
                return new ArrayList(fieldsMap.values());
            }

        }
        return new ArrayList<FieldInfo>();
    }
    
    private String getAgileFieldtype(String fieldType) {
        if(fieldType == null) {
            return "";
        }
        
        switch(fieldType) {
            case OctaneConstants.KEY_FIELD_STRING:
                return DATA_TYPE.STRING.name();
            case OctaneConstants.KEY_FIELD_MEMO:
                return DATA_TYPE.MEMO.name();
            case OctaneConstants.KEY_FIELD_USER_LIST:
                return DATA_TYPE.USER.name();
            case OctaneConstants.KEY_FIELD_SUB_TYPE_LIST_NODE:
            case OctaneConstants.KEY_FIELD_AUTO_COMPLETE_LIST:
                return DATA_TYPE.ListNode.name();
            case OctaneConstants.KEY_FIELD_INTEGER:
                return DATA_TYPE.INTEGER.name();
            default :
                return DATA_TYPE.STRING.name();                
        }
    }

    @Override
    public List<AgileEntityFieldValue> getAgileEntityFieldsValueList(final String agileProjectValue, String entityType,
            final ValueSet instanceConfigurationParameters, final String fieldName, final boolean isLogicalName)
    {
        List<AgileEntityFieldValue> fields = new ArrayList<>();
        String workSpaceId = "";
        String sharedSpaceId = "";
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);

        SharedSpace sp = client.getActiveSharedSpace();
        if (sp != null) {
            sharedSpaceId = sp.getId();
            if (OctaneConstants.SUB_SHARED_EPIC.equalsIgnoreCase(entityType)) {
                workSpaceId = OctaneConstants.SHARED_EPIC_DEFAULT_WORKSPACE;
                entityType = OctaneConstants.KEY_EPIC_ENTITY_TYPE;
            } else {
                if (OctaneConstants.WILDCARD_PLACEHOLDER.equalsIgnoreCase(agileProjectValue)) {
                    // if use wildcard in agile project,find the specific
                    // workspace that the field belongs to
                    List<WorkSpace> wsList = client.getWorkSpaces(Integer.parseInt(sharedSpaceId));
                    for (WorkSpace ws : wsList) {
                        List<FieldInfo> candidatefields = client.getEntityFields(sharedSpaceId, ws.getId(), entityType);
                        for (FieldInfo field : candidatefields) {
                            if ((isLogicalName && fieldName.equals(field.getLogicalName()))||(!isLogicalName)&&fieldName.equals(field.getName())) {
                                workSpaceId = ws.getId();
                                break;
                            }
                        }

                        if (!workSpaceId.isEmpty()) {
                            break;
                        }
                    }
                } else {
                    JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
                    workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
                    sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
                }
            }
            if (!workSpaceId.isEmpty()) {
                fields = getAgileFieldValueList(sharedSpaceId, workSpaceId, entityType, fieldName, isLogicalName,
                        client);
            }

        }

        client.signOut(instanceConfigurationParameters);
        return fields;
    }

    /**
     * get detail field value list according to specific space and workspace id
     * @param agileProjectValue
     * @param entityType
     * @param fieldName
     * @param isLogicalName
     * @param client
     * @return
     */
    private List<AgileEntityFieldValue> getAgileFieldValueList(String sharedSpaceId, String workSpaceId,
            String entityType, final String fieldName, final boolean isLogicalName, ClientPublicAPI client)
    {
        List<AgileEntityFieldValue> fields = null;
        if (isLogicalName) {
            fields = client.getEntityFieldListNode(sharedSpaceId, workSpaceId, fieldName);
        } else {
            String newFieldName = null;
            switch (fieldName) {
                case OctaneConstants.KEY_FIELD_MILESTONE:
                    newFieldName = OctaneConstants.KEY_FIELD_MILESTONE_API_NAME;
                    break;
                case OctaneConstants.KEY_FIELD_PARENT:
                    if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
                        newFieldName = OctaneConstants.KEY_FIELD_EPIC_API_NAME;
                    } else {
                        newFieldName = OctaneConstants.KEY_FIELD_FEATURE_API_NAME;
                    }
                    break;
                case OctaneConstants.KEY_FIELD_PHASE:
                    newFieldName = OctaneConstants.KEY_FIELD_PHASE_API_NAME;
                    break;
                case OctaneConstants.KEY_FIELD_RELEASE:
                    newFieldName = OctaneConstants.KEY_FIELD_RELEASE_API_NAME;
                    break;
                case OctaneConstants.KEY_FIELD_SPRINT:
                    newFieldName = OctaneConstants.KEY_FIELD_SPRINT_API_NAME;
                    break;
                case OctaneConstants.KEY_FIELD_TEAM:
                    newFieldName = OctaneConstants.KEY_FIELD_TEAM_API_NAME;
                    break;
                default:
                    newFieldName = fieldName;
                    break;
            }
            fields = client.getEntityFieldValueList(sharedSpaceId, workSpaceId, entityType, newFieldName);
        }
        return fields;
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
        List<AgileEntity> entities = new ArrayList<>();

        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        String workSpaceId = OctaneConstants.WILDCARD_PLACEHOLDER;
        String sharedSpaceId = OctaneConstants.WILDCARD_PLACEHOLDER;
        List<String> workspaceIds = new ArrayList<>();
        if (OctaneConstants.WILDCARD_PLACEHOLDER.equalsIgnoreCase(agileProjectValue)) {
            SharedSpace sharedSpace = client.getActiveSharedSpace();
            if (sharedSpace != null) {
                sharedSpaceId = sharedSpace.getId();
                workspaceIds = client.getApiAccessWorkSpaces(Integer.parseInt(sharedSpaceId));
            }
        } else {
            JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
            workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
            sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
            workspaceIds.add(workSpaceId);
        }

        for (String id : workspaceIds) {
            if (entities.size() >= entityIds.size()) {
                break;
            }

            List<AgileEntity> entitiesCollection =
                    getSpecificWorkspaceEntities(client, sharedSpaceId, id,
                    entityType, entityIds, lastUpdateTime);
            entities.addAll(entitiesCollection);
        }
        client.signOut(instanceConfigurationParameters);
        return entities;
    }

    private List<AgileEntity> getSpecificWorkspaceEntities(ClientPublicAPI client, String sharedSpaceId,
            String workSpaceId, String entityType, Set<String> entityIds, Date lastUpdateTime)
    {
        List<AgileEntity> entities = new ArrayList<>();
        if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
            List<JSONObject> featureJson =
                    client.getFeaturesAfterDate(sharedSpaceId, workSpaceId, entityIds, lastUpdateTime);
            entities = getAgileEntities(client, featureJson, sharedSpaceId, workSpaceId, "feature");

        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            List<JSONObject> userStoryJson =
                    client.getUserStoriesAfterDate(sharedSpaceId, workSpaceId, entityIds, lastUpdateTime);
            entities = getAgileEntities(client, userStoryJson, sharedSpaceId, workSpaceId, "story");
        } else if (OctaneConstants.SUB_TYPE_EPIC.equals(entityType)
                || OctaneConstants.SUB_SHARED_EPIC.equals(entityType)) {
            if (OctaneConstants.SUB_SHARED_EPIC.equals(entityType)) {
                workSpaceId = OctaneConstants.SHARED_EPIC_DEFAULT_WORKSPACE;
            }
            List<JSONObject> userEpicJson =
                    client.getEpicsAfterDate(sharedSpaceId, workSpaceId, entityIds, lastUpdateTime);
            entities = getAgileEntities(client, userEpicJson, sharedSpaceId, workSpaceId, "epic");
        }
        return entities;

    }

    @Override
    public AgileEntity getEntity(final String agileProjectValue, String entityType,
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
                Map<String, JSONObject> usersMap = collectAllUsers(client,featureJson,sharedSpaceId,workSpaceId,fieldInfoMap);
                entity = wrapperEntity(featureJson.get(0), fieldInfoMap, usersMap);
            }
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            List<JSONObject> userStoryJson = client.getUserStory(sharedSpaceId, workSpaceId, entityId);
            if (userStoryJson.size() > 0) {
                Map<String, FieldInfo> fieldInfoMap  = getFieldInfoMap(client, sharedSpaceId, workSpaceId, "story");
                Map<String, JSONObject> usersMap = collectAllUsers(client,userStoryJson,sharedSpaceId,workSpaceId,fieldInfoMap);
                entity = wrapperEntity(userStoryJson.get(0), fieldInfoMap, usersMap);
            }
        } else if (OctaneConstants.SUB_TYPE_EPIC.equals(entityType)||OctaneConstants.SUB_SHARED_EPIC.equals(entityType)) {
			// shared epic is special epic who belong to a master workflow whose id is 500
        	if(OctaneConstants.SUB_SHARED_EPIC.equals(entityType)) {
            	workSpaceId = OctaneConstants.SHARED_EPIC_DEFAULT_WORKSPACE;
            	entityType = OctaneConstants.SUB_TYPE_EPIC;
            }
        	
            List<JSONObject> epicJson = client.getEpic(sharedSpaceId, workSpaceId, entityId);
            if (epicJson.size() > 0) {
                Map<String, FieldInfo> fieldInfoMap  = getFieldInfoMap(client, sharedSpaceId, workSpaceId, "epic");
                Map<String, JSONObject> usersMap = collectAllUsers(client,epicJson,sharedSpaceId,workSpaceId,fieldInfoMap);
                entity = wrapperEntity(epicJson.get(0), fieldInfoMap, usersMap);
            }
        }
        client.signOut(instanceConfigurationParameters);
        return entity;
    }

    private AgileEntity saveOrUpdateEntity(final String agileProjectValue, String entityType,
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
            String entityStr = buildEntity(client, sharedSpaceId, workSpaceId, fieldInfos, entityType, entity, null);
            JSONObject feature = client.saveFeatureInWorkspace(sharedSpaceId, workSpaceId, entityStr, method);
            agileEntity = wrapperEntity(feature, null, null);
        } else if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
            SimpleEntity root = client.getWorkItemRoot(Integer.parseInt(sharedSpaceId), Integer.parseInt(workSpaceId));
            String entityStr = buildEntity(client, sharedSpaceId, workSpaceId, fieldInfos, entityType, entity, root);
            JSONObject userStory = client.saveStoryInWorkspace(sharedSpaceId, workSpaceId, entityStr, method);
            agileEntity = wrapperEntity(userStory, null, null);
        } else if (OctaneConstants.SUB_TYPE_EPIC.equals(entityType)
                || OctaneConstants.SUB_SHARED_EPIC.equals(entityType)) {
            String finalWorkSpaceId = workSpaceId;
            // shared epic is special epic who belong to a master workflow whose
            // id is 500
            if (OctaneConstants.SUB_SHARED_EPIC.equals(entityType)) {
                finalWorkSpaceId = OctaneConstants.SHARED_EPIC_DEFAULT_WORKSPACE;
                entityType = OctaneConstants.SUB_TYPE_EPIC;
            }
            SimpleEntity root =
                    client.getWorkItemRoot(Integer.parseInt(sharedSpaceId), Integer.parseInt(finalWorkSpaceId));
            String entityStr =
                    buildEntity(client, sharedSpaceId, finalWorkSpaceId, fieldInfos, entityType, entity, root);
            JSONObject userEpic = client.saveEpicInWorkspace(sharedSpaceId, finalWorkSpaceId, entityStr, method);
            agileEntity = wrapperEntity(userEpic, null, null);
        }
        if (agileEntity != null) {
            agileEntity.setEntityUrl(
                    String.format(ClientPublicAPI.DEFAULT_ENTITY_ITEM_URL, client.getBaseURL(), sharedSpaceId, workSpaceId, agileEntity.getId()));
        }
        client.signOut(instanceConfigurationParameters);
        return agileEntity;
    }

    private String buildEntity(final ClientPublicAPI client, final String sharedSpaceId, final String workSpaceId,
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
                // cover case: user clear the <phase> field. if param null,
                // Octane will throw unfriendly error message
                if(OctaneConstants.KEY_FIELD_PHASE.equals(key)){
                    JSONObject complexObj = new JSONObject();
                    complexObj.put("id", "");
                    complexObj.put("type", key);
                    entityObj.put(key, complexObj);
                }else if(OctaneConstants.KEY_FIELD_COMMENTS.equals(key)){
                    field = new StringField();
                    field.set("");
                }else{
                    if (fieldInfo.isMultiValue()) {
                        entityObj.put(key, createNullJSONObject(true));
                    } else {
                        entityObj.put(key, createNullJSONObject(false));
                    }

                }
                continue;
            }
            switch (field.getType()) {
                // String and Memo field use same logic
                case STRING:
                case MEMO:
                    if(OctaneConstants.KEY_FIELD_INTEGER.equals(fieldInfo.getFieldType())) {
                        try {
                            entityObj.put(key, new Double((String)field.get()));
                        } catch(NumberFormatException e) {
                            entityObj.put(key, field.get());
                        }
                    } else if(fieldInfo.getFieldType().equals(OctaneConstants.KEY_FIELD_USER_LIST)){
                        List<String> userEmails = new ArrayList<String>();
                        userEmails.add((String)field.get());
                        entityObj.put(key,
                                transformUserEmails(client, fieldInfo, userEmails, sharedSpaceId, workSpaceId));
                    } else {
                        String value = (String)field.get();
                        value = null == value ? "" : value.trim();
                        switch (key){
                            //allow PPM text to Octane phase, release, if add new field in future, just add <case> field
                            case OctaneConstants.KEY_FIELD_PHASE:
                            case OctaneConstants.KEY_FIELD_RELEASE:
                                //if param "", regard as user clear the field(phase will not come there)
                                if( value.isEmpty()){
                                    entityObj.put(key, createNullJSONObject(false));
                                    break;
                                }
                                List<AgileEntityFieldValue> valueList = client
                                        .getEntityFieldValueList(sharedSpaceId, workSpaceId, entityType, getFieldNameInAPI(key));

                                JSONObject complexObj = new JSONObject();
                                complexObj.put("id", value);
                                complexObj.put("type", key);
                                for (AgileEntityFieldValue agileFieldValue: valueList) {
                                    if(agileFieldValue.getName().equalsIgnoreCase(value)){
                                        complexObj.put("id", agileFieldValue.getId());
                                        break;
                                    }
                                }
                                entityObj.put(key, complexObj);
                                break;
                            case OctaneConstants.KEY_FIELD_COMMENTS:
                                if( value.isEmpty()){
                                    //empty value will not be updated
                                    break;
                                }
                                String entityId = entity.getId();
                                if(null == entityId || "".equals(entityId)){
                                    //if param comment when first time sync to Octane, Octane will throw unfriendly error
                                    throw new RuntimeException("Comments cannot be updated from PPM until after the first successful synchronization");
                                }
                                // if update other field, comment will not be added.
                                // get last comment from octane and check whether need to update by compare plain text
                                List<String> commentsPlainTxtList = client.getCommentsPlainTxtForWorkItem(sharedSpaceId, workSpaceId, entity.getId());
                                if(null != commentsPlainTxtList && !commentsPlainTxtList.isEmpty()){
                                    //get ppm value plain text
                                    StringTokenizer pas = new StringTokenizer(value);
                                    StringBuilder originPlainText = new StringBuilder("");
                                    while (pas.hasMoreTokens()){
                                        originPlainText.append(pas.nextToken());
                                    }
                                    //get last comment plain text
                                    String lastCommentPlainTxt = commentsPlainTxtList.get(commentsPlainTxtList.size() - 1).replaceAll(" ", "");
                                    if(originPlainText.toString().equals(lastCommentPlainTxt)){
                                        break;
                                    }
                                }
                                //construct comments json obj
                                JSONObject workItemObj = new JSONObject();
                                workItemObj.put("type", "work_item");
                                workItemObj.put("id", entityId);
                                JSONObject dataObj = new JSONObject();
                                dataObj.put("text", value);
                                dataObj.put("owner_work_item", workItemObj);
                                JSONArray dataList = new JSONArray();
                                dataList.add(dataObj);
                                JSONObject finalJsonObj = new JSONObject();
                                finalJsonObj.put("data", dataList);
                                //construct comments json obj  end
                                entityObj.put(key, finalJsonObj);
                                break;
                            default:
                                entityObj.put(key, field.get());
                        }                      
                    } 
                    break;
                case USER:
                    if(fieldInfo.getFieldType().equals(OctaneConstants.KEY_FIELD_STRING) || fieldInfo.getFieldType().equals(OctaneConstants.KEY_FIELD_MEMO)) {
                        if (field.isList()) {
                            MultiUserField userField = (MultiUserField) field;
                            entityObj.put(entry.getKey(), getFullnames(userField.get()));
                        } else {
                            UserField userField = (UserField) field;
                            entityObj.put(entry.getKey(), userField.get().getFullName());
                        }
                    } else {
                        if (field.isList()) {
                            MultiUserField userField = (MultiUserField) field;
                            JSONObject obj =
                                    transformUsers(client, fieldInfo, userField.get(), sharedSpaceId, workSpaceId);
                            entityObj.put(entry.getKey(), obj);
                        } else {
                            UserField userField = (UserField) field;
                            List<User> userList = new ArrayList<User>();
                            userList.add(userField.get());
                            JSONObject obj = transformUsers(client, fieldInfo, userList, sharedSpaceId, workSpaceId);
                            entityObj.put(entry.getKey(), obj);
                        }

                    }
                    break;                
                case ListNode:
                    ListNodeField listNodeField = (ListNodeField) field;
                    JSONObject complexObj = new JSONObject();          
                    
                    String type = "";
                    if(OctaneConstants.KEY_FIELD_SUB_TYPE_LIST_NODE.equals(fieldInfo.getFieldType())) {
                        type = OctaneConstants.SUB_TYPE_LIST_NODE;
                    } else {
                        type = fieldInfo.getName();
                    }
                    
                    if(fieldInfo.isMultiValue()) {
                        String[] nameArr = listNodeField.get().getName().split(OctaneConstants.SPLIT_CHAR);
                        String[] idArr = listNodeField.get().getId().split(OctaneConstants.SPLIT_CHAR); 
                        
                        complexObj.put("total_count", nameArr.length);
                        
                        JSONArray tempArr = new JSONArray();
                        for (int i = 0; i < nameArr.length; i++) {
                            JSONObject tempObj = new JSONObject(); 
                            tempObj.put("type", type);
                            tempObj.put("name", nameArr[i]);                    
                            tempObj.put("id", idArr[i]);
                            tempObj.put("index", i);        
                            tempArr.add(tempObj);
                        }
                        complexObj.put("data", tempArr);
                        
                    } else {    
                        complexObj.put("type", type);
                        complexObj.put("id", -1); // give a default id to let
                                                  // octane return warning
                                                  // message in case no value
                                                  // matches
                        if (listNodeField.get().getId() == null || listNodeField.get().getId().equals("")) {

                            switch (key) {
                                // allow SQL CUSTOM ddl/acl sync with octane
                                // reserved fields phase and release
                                case OctaneConstants.KEY_FIELD_PHASE:
                                case OctaneConstants.KEY_FIELD_RELEASE:
                                    List<AgileEntityFieldValue> valueList = client.getEntityFieldValueList(
                                            sharedSpaceId, workSpaceId, entityType, getFieldNameInAPI(key));

                                    for (AgileEntityFieldValue agileFieldValue : valueList) {
                                        if (agileFieldValue.getName().equalsIgnoreCase(listNodeField.get().getName())) {
                                            complexObj.put("id", agileFieldValue.getId());
                                            break;
                                        }
                                    }
                                    entityObj.put(key, complexObj);
                                    break;
                                default:

                                    List<AgileEntityFieldValue> fieldValues = client.getEntityFieldListNode(
                                            sharedSpaceId, workSpaceId, fieldInfo.getLogicalName());
                                    for (AgileEntityFieldValue fieldValue : fieldValues) {
                                        if (fieldValue.getName().equalsIgnoreCase(listNodeField.get().getName())) {
                                            complexObj.put("id", fieldValue.getId());
                                            break;
                                        }
                                    }
                            }
                        } else {                            
                            complexObj.put("id", listNodeField.get().getId());
                        }
                        complexObj.put("multiple", fieldInfo.isMultiValue());
                    }
                        
                    entityObj.put(entry.getKey(), complexObj);
                    break;
                
            }
        }

        if (entity.getId() != null) {
            entityObj.put(OctaneConstants.KEY_FIELD_ID, entity.getId());
        } else {
            //when create a new octane request, if user set <phase>,
            //keep the value
            if(!entityObj.containsKey(OctaneConstants.KEY_FIELD_PHASE)){
                JSONObject complexObj = new JSONObject();
                if (OctaneConstants.SUB_TYPE_STORY.equals(entityType)) {
                    complexObj.put("id", "phase.story.new");
                } else if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)){
                    complexObj.put("id", "phase.feature.new");
                } else {
                    complexObj.put("id", "phase.epic.new");
                }
                complexObj.put("type", "phase");
                entityObj.put("phase", complexObj);
            }
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

    private String getFieldNameInAPI(String originName){
        switch (originName){
            case OctaneConstants.KEY_FIELD_PHASE:
                return OctaneConstants.KEY_FIELD_PHASE_API_NAME;
            case OctaneConstants.KEY_FIELD_RELEASE:
                return OctaneConstants.KEY_FIELD_RELEASE_API_NAME;
            default:
                return originName;
        }
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
        } else if (OctaneConstants.SUB_TYPE_EPIC.equals(entityType)|| OctaneConstants.SUB_SHARED_EPIC.equals(entityType)) {
			// shared epic is special epic who belong to a master workflow whose id is 500
        	if(OctaneConstants.SUB_SHARED_EPIC.equals(entityType)) {
        		workSpaceId = OctaneConstants.SHARED_EPIC_DEFAULT_WORKSPACE;
        	}
            client.deleteEntity(sharedSpaceId, workSpaceId, "epics", entityId);
        }
        client.signOut(instanceConfigurationParameters);

    }
    
    private JSONObject transformUserEmails(ClientPublicAPI client, FieldInfo userFieldInfo, List<String> userEmails,
            String shareSpaceId, String workSpaceId)
    {
        if (!userEmails.isEmpty()) {
            JSONArray usernamesArray =
                    client.getUsersByEmails(shareSpaceId, workSpaceId,
                            userEmails.toArray(new String[userEmails.size()]));
            if (usernamesArray.size() > 0) {
                if (userFieldInfo.isMultiValue()) {
                    JSONObject userList = new JSONObject();
                    JSONArray userArr = new JSONArray();
                    for (int i = 0; i < usernamesArray.size(); i++) {
                        JSONObject tempObj = usernamesArray.getJSONObject(i);
                        userArr.add(getUserJsonObject(tempObj));
                    }
                    userList.put("data", userArr);
                    return userList;
                } else {
                    return getUserJsonObject(usernamesArray.getJSONObject(0));
                }
            }
        }
        
        if (userFieldInfo.isMultiValue()) {
            return createNullJSONObject(true);
        } else {
            return createNullJSONObject(false);
        }
    }
    
    private String getFullnames(List<User> users) {
        String usernames = "";
        
        if (users != null && !users.isEmpty()) {
            for (User user : users) {
                if (user.getFullName() != null) {
                    usernames += user.getFullName() + OctaneConstants.MULTI_JOIN_CHAR;
                }
            }            
        }
        
        if(usernames.length() > 0) {
            usernames = usernames.substring(0, usernames.length() - OctaneConstants.MULTI_JOIN_CHAR.length());
        }
        
        return usernames;
    }

    private JSONObject transformUsers(ClientPublicAPI client, FieldInfo userFieldInfo, List<User> users,
            String shareSpaceId, String workSpaceId)
    {
        List<String> userEmails = new ArrayList<String>();
        
        if (users != null && !users.isEmpty()) {
            for (User user : users) {
                if (user.getEmail() != null) {
                    userEmails.add(user.getEmail());
                }
            }            
        }

        return transformUserEmails(client, userFieldInfo, userEmails, shareSpaceId, workSpaceId);
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
    
    

    private AgileEntity wrapperEntity(JSONObject item, Map<String, FieldInfo> fieldInfoMap, Map<String, JSONObject> usersMap)
    {

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
                if (info.getFieldType().equals(OctaneConstants.KEY_FIELD_USER_LIST)) {
                    JSONObject value = item.getJSONObject(key);
                    if (info.isMultiValue()) {
                        JSONArray users = value.getJSONArray("data");
                        if (!users.isEmpty()) {
                            List<User> userList = new ArrayList<User>();
                            for (int i = 0; i < users.size(); i++) {
                                JSONObject userObj = users.getJSONObject(i);
                                JSONObject detailUser = usersMap.get(userObj.getString("id"));
                                User user = buildUser(detailUser);
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
                            User user = buildUser(usersMap.get(value.getString("id")));
                            if (user != null) {
                                UserField userField = new UserField();
                                userField.set(user);
                                entity.addField(key, userField);
                            }
                        } else {
                            entity.addField(key, null);
                        }
                    }
                } else if (info.getFieldType().equals(OctaneConstants.KEY_FIELD_STRING) || info.getFieldType().equals(OctaneConstants.KEY_FIELD_INTEGER)) {
                    String value = item.getString(key);
                    if (value == null || value.equals("null")) {
                        value = "";
                    }
                    StringField stringField = new StringField();
                    stringField.set(value);
                    entity.addField(key, stringField);
                } else if (info.getFieldType().equals(OctaneConstants.KEY_FIELD_SUB_TYPE_LIST_NODE) || info.getFieldType().equals(OctaneConstants.KEY_FIELD_AUTO_COMPLETE_LIST)) {
                    JSONObject value = item.getJSONObject(key);
                    if(canParseJson(value, "name") || canParseJson(value, "data")) {
                        String ids = "";
                        String names = "";
                        if (info.isMultiValue()) {
                            JSONArray listNodes = value.getJSONArray("data");
                            if (!listNodes.isEmpty()) {
                                for (int i = 0; i < listNodes.size(); i++) {
                                    JSONObject listNode = listNodes.getJSONObject(i);
                                    String id = listNode.getString("id");
                                    String name = listNode.getString("name");                                    
                                    
                                    if(id != null && name != null) {                        
                                        ids = id + OctaneConstants.SPLIT_CHAR + ids;
                                        names = name + OctaneConstants.SPLIT_CHAR + names;
                                    }
                                }                         
                            }
                            
                            if(ids != null && !ids.isEmpty()) {
                                ids = ids.substring(0, ids.length() - OctaneConstants.SPLIT_CHAR.length());
                                names = names.substring(0, names.length() - OctaneConstants.SPLIT_CHAR.length());
                            }
                        } else {
                            ids = value.getString("id");
                            names = value.getString("name");
                        }
                        
                        if(ids != null && !ids.isEmpty()) {
                            ListNode listNode = new ListNode();                            
                            listNode.setId(ids);
                            listNode.setName(names);                            
                            
                            ListNodeField listNodeField = new ListNodeField();
                            listNodeField.set(listNode);
                            
                            entity.addField(key, listNodeField); 
                        } else{
                            entity.addField(key, null);
                        }                        
                    } else {
                        entity.addField(key, null);
                    }
                } else if (info.getFieldType().equals(OctaneConstants.KEY_FIELD_MEMO)) {
                    String value = item.getString(key);
                    if (value == null || value.equals("null")) {
                        value = "";
                    }
                    MemoField memoField = new MemoField();
                    memoField.set(value);
                    entity.addField(key, memoField);
                }
            }
        }
        return entity;
    }

    private User buildUser(JSONObject userJson) {
        User user = null;
        if (userJson!=null&&userJson.containsKey("name")) {
            long userId = userJson.getLong("id");
            com.hp.ppm.user.model.User userModel = new com.hp.ppm.user.model.User();
            userModel = getUserProvider().getByEmail(userJson.getString("email"));
            if (userModel != null && userModel.getUserId() != null) {
                user = new User();
                user.setUserId(userModel.getUserId());
                user.setEmail(userModel.getEmail());
                user.setFullName(userModel.getFullName());
                user.setUsername(userModel.getUserName());
            } else {
                user = new User();
                user.setUserId(userId);
                if (userJson.containsKey("full_name")) {
                    String fullName = userJson.getString("full_name");   
                    user.setFullName(fullName);
                }           
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
            Map<String, JSONObject> usersMap = collectAllUsers(client,workItemsJson,sharedspaceId,workspaceId,fieldInfoMap);
            for (JSONObject workItemJson : workItemsJson) {
                AgileEntity entity = wrapperEntity(workItemJson, fieldInfoMap, usersMap);
                agileEntities.add(entity);
            }
        }
        return agileEntities;
    }
    
    private Map<String,JSONObject> collectAllUsers(ClientPublicAPI client, List<JSONObject> workItemsJson, String sharedspaceId, String workspaceId, Map<String, FieldInfo> fieldInfoMap){
    	Map<String,JSONObject> usersMap = new HashMap<String,JSONObject>();
    	List<String> userIdList = new ArrayList<>();
    	for (JSONObject item : workItemsJson) {
    	    	Iterator<String> sIterator = item.keys();
    	        while (sIterator.hasNext()) {
    	            String key = sIterator.next();
    	            if (fieldInfoMap != null && fieldInfoMap.get(key) != null) {
    	                FieldInfo info = fieldInfoMap.get(key);
    	                if (info.getFieldType().equals(OctaneConstants.KEY_FIELD_USER_LIST)) {
    	                    JSONObject value = item.getJSONObject(key);
    	                    if (info.isMultiValue()) {
    	                        JSONArray users = value.getJSONArray("data");
    	                        	if (!users.isEmpty()) {
    	                                for (int i = 0; i < users.size(); i++) {
    	                                    JSONObject userObj = users.getJSONObject(i);
    	                                    userIdList.add(userObj.getString("id"));    	                                    
    	                                    }
    	                                }
    	                     }else {
    	                    	 if (canParseJson(value, "name")) {
    	                    		userIdList.add(value.getString("id"));    
    	                    	 }
    	                     }
    	                   }
    	                }
    	            }
    	        }
    	JSONArray userArray = client.getUsersByIds(sharedspaceId, workspaceId, userIdList.toArray(new String[userIdList.size()]));
    	for (int i = 0; i < userArray.size(); i++) {
            JSONObject userObj = userArray.getJSONObject(i);
            usersMap.put(userObj.getString("id"), userObj);
        }

    	return usersMap;
    }
}

class AgileFieldComparator implements Comparator<AgileEntityFieldInfo> {
    @Override
    public int compare(final AgileEntityFieldInfo o1, final AgileEntityFieldInfo o2) {
        return o1.getLabel().compareToIgnoreCase(o2.getLabel());
    }
}
