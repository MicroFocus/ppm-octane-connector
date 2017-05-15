package com.ppm.integration.agilesdk.connector.octane;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import com.ppm.integration.agilesdk.ValueSet;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.client.RestResponse;
import com.ppm.integration.agilesdk.connector.octane.model.EpicAttr;
import com.ppm.integration.agilesdk.connector.octane.model.EpicCreateEntity;
import com.ppm.integration.agilesdk.connector.octane.model.EpicEntity;

public class MockClientPublicAPI extends ClientPublicAPI{

    ValueSet values = new ValueSet();

	public MockClientPublicAPI(String baseUrl) {
		super(baseUrl);
		 values = CommonParameters.getDefaultValueSet();
	}
	

    public List<EpicEntity> createEpicInWorkspace(String sharedspaceId, String workspaceId, EpicCreateEntity epicCreateEntity) throws JsonProcessingException, IOException, JSONException {
    	
    	return (List<EpicEntity>)getDataContent(values.get(OctaneConstants.CREATE_EPIC_IN_WORKSPACE_RESPONSE_JSONDATA), new TypeReference<List<EpicEntity>>(){});
    }

    public List<EpicAttr> getEpicPhase(final String sharedspaceId, final String workspaceId, final String phaseLogicName) throws JsonProcessingException, IOException, JSONException {
        
        return (List<EpicAttr>)getDataContent(values.get(OctaneConstants.GET_EPIC_PHASE_JSONDATA), new TypeReference<List<EpicAttr>>(){});
    }
    
    public List<EpicAttr> getEpicParent(final String sharedspaceId, final String workspaceId, final String workitemSubtype) throws JsonProcessingException, IOException, JSONException {	
        return (List<EpicAttr>)getDataContent(values.get(OctaneConstants.GET_EPIC_PARENT_JSONDATA), new TypeReference<List<EpicAttr>>(){});
    }
    
    private List<?> getDataContent(String jsonData, TypeReference<?> typeRef)
            throws JSONException, JsonParseException, JsonMappingException, IOException {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JSONObject obj = new JSONObject(jsonData);
            Object dataObj = obj.get("data");
            if(dataObj != null) {
                String arrayStr = dataObj.toString();			
                if(arrayStr.length() > 2){
                    return mapper.readValue(arrayStr, typeRef);
                }
            }
            return null;
        }

}
