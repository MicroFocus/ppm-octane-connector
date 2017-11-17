package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.FeatureCreateEntity;
import com.ppm.integration.agilesdk.connector.octane.model.FeatureEntity;
import com.ppm.integration.agilesdk.connector.octane.model.FieldInfo;
import com.ppm.integration.agilesdk.connector.octane.model.SimpleEntity;
import com.ppm.integration.agilesdk.connector.octane.model.StoryCreateEntity;
import com.ppm.integration.agilesdk.connector.octane.model.StoryEntity;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.dm.AgileEntityFieldInfo;
import com.ppm.integration.agilesdk.dm.AgileEntityInfo;
import com.ppm.integration.agilesdk.dm.FieldValue;
import com.ppm.integration.agilesdk.dm.RequestIntegration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import edu.emory.mathcs.backport.java.util.Collections;

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
    public List<AgileEntityFieldInfo> getAgileEntityFieldsInfo(final String agileProjectValue, final String entityType, final ValueSet instanceConfigurationParameters) {
        List<AgileEntityFieldInfo> fieldList = new ArrayList<AgileEntityFieldInfo>();
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
        List<FieldInfo> fields = client.getEntityFields(sharedSpaceId, workSpaceId, entityType);
        for (FieldInfo field : fields) {
            AgileEntityFieldInfo info = new AgileEntityFieldInfo();
            info.setDisplayName(field.getLabel());
            info.setName(field.getName());
            info.setListType(field.getListType());
            JSONObject valueObj = new JSONObject();
            valueObj.put(OctaneConstants.KEY_FIELD_NAME, field.getName());
            valueObj.put(OctaneConstants.KEY_LOGICAL_NAME, field.getLogicalName());
            info.setValue(valueObj.toString());
            fieldList.add(info);
        }
        Collections.sort(fieldList, new AgileFieldComparator());
        return fieldList;
    }

    public List<FieldValue> getAgileEntityFieldValueList(final String agileProjectValue, final String fieldInfo, final ValueSet instanceConfigurationParameters) {
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);

        JSONObject fieldObj = (JSONObject)JSONSerializer.toJSON(fieldInfo);
        String logicalName = fieldObj.getString(OctaneConstants.KEY_LOGICAL_NAME);
        return client.getEntityFieldValueList(sharedSpaceId, workSpaceId, logicalName);
    }

    @Override
    public String createEntity(final String agileProjectValue, final String entityType, final Map<String, List<FieldValue>> entityMap,
            final ValueSet instanceConfigurationParameters) {
        String entityId = null;
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
        if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
            FeatureCreateEntity entity = buildFeatureEntity(entityMap);
            List<FeatureEntity> features = client.createFeatureInWorkspace(sharedSpaceId, workSpaceId, entity);
            if (features != null && features.size() > 0) {
                entityId = features.get(0).getId();
            }
        } else if(OctaneConstants.SUB_TYPE_STORY.equals(entityType)){
        	WorkItemRoot root = new WorkItemRoot();
        	root = client.getWorkItemRoot(Integer.parseInt(sharedSpaceId), Integer.parseInt(workSpaceId));
        	StoryCreateEntity entity = buildStoryCreateEntity(entityMap,root);
            List<StoryEntity> stories = client.createStoryInWorkspace(sharedSpaceId, workSpaceId, entity);
            if (stories != null && stories.size() > 0) {
                entityId = stories.get(0).getId();
            }
        }
        return entityId;
    }
    
    @Override
    public Map<String, Map<String, List<FieldValue>>> getEntities(final String agileProjectValue,final String entityType, final ValueSet instanceConfigurationParameters, Set<String> entityIds){
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
        Map<String, Map<String, List<FieldValue>>> entitiesInfo =null;
        if (OctaneConstants.SUB_TYPE_FEATURE.equals(entityType)) {
        	entitiesInfo = client.getUserStories(sharedSpaceId, workSpaceId, entityIds);        	
        } else if(OctaneConstants.SUB_TYPE_STORY.equals(entityType)){
        	entitiesInfo = client.getUserStories(sharedSpaceId, workSpaceId, entityIds);
        }
    	
    	return entitiesInfo;
    }
    
    private StoryCreateEntity buildStoryCreateEntity(Map<String, List<FieldValue>> entityMap,WorkItemRoot root)
    {
    	StoryCreateEntity storyCreateEntity = new StoryCreateEntity();
    	StoryEntity storyEntity = new StoryEntity();    	
    	List<FieldValue> name = entityMap.get(OctaneConstants.KEY_FIELD_NAME);
    	if (name != null && name.size() > 0)
    	{
    		storyEntity.setName(name.get(0).getValue());
    	}
    	storyEntity.setName("ppm request default name");
    	List<FieldValue> description = entityMap.get(OctaneConstants.KEY_FIELD_DESCRIPTION);
        if (description != null && description.size() > 0) {
        	storyEntity.setDescription(description.get(0).getValue());
        }
        SimpleEntity phase = new SimpleEntity();
        phase.setId("phase.story.new");
        phase.setType("phase");
        storyEntity.setPhase(phase);
        SimpleEntity parent = new SimpleEntity();
        parent.setId(root.id);
        parent.setType(root.type);
        storyEntity.setParent(parent);
        storyEntity.setPhase(phase);
        storyCreateEntity.addStoryEntity(storyEntity);
    	return storyCreateEntity;
    	
    }

    private FeatureCreateEntity buildFeatureEntity(Map<String, List<FieldValue>> entityMap) {
        FeatureCreateEntity entity = new FeatureCreateEntity();
        FeatureEntity featureEntity = new FeatureEntity();
        List<FieldValue> name = entityMap.get(OctaneConstants.KEY_FIELD_NAME);
        if (name != null && name.size() > 0) {
            featureEntity.setName(name.get(0).getValue());
        }
        List<FieldValue> description = entityMap.get(OctaneConstants.KEY_FIELD_DESCRIPTION);
        if (description != null && description.size() > 0) {
            featureEntity.setDescription(description.get(0).getValue());
        }

        SimpleEntity phase = new SimpleEntity();
        phase.setId("phase.feature.new");
        phase.setType("phase");
        featureEntity.setPhase(phase);
        entity.addFeatureEntity(featureEntity);
        return entity;
    }
}

class AgileFieldComparator implements Comparator<AgileEntityFieldInfo> {
    @Override
    public int compare(final AgileEntityFieldInfo o1, final AgileEntityFieldInfo o2) {
        return o1.getDisplayName().compareTo(o2.getDisplayName());
    }
}
