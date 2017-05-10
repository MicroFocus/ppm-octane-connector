package com.ppm.integration.agilesdk.connector.octane;

import org.junit.Test;

import java.util.List;

import junit.framework.Assert;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.EpicAttr;
import com.ppm.integration.agilesdk.connector.octane.model.EpicCreateEntity;
import com.ppm.integration.agilesdk.connector.octane.model.EpicEntity;

public class ClientPublicAPIMockTest {

	ValueSet values = new ValueSet();
	
	
	@Test public void testCreateEpicInWorkspace() throws Exception {
		ClientPublicAPI client= new MockClientPublicAPI("");
		 values = CommonParameters.getDefaultValueSet();
		List<EpicEntity> result= client.createEpicInWorkspace(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), null);
    	Assert.assertNotNull(result);
    	for(EpicEntity e : result){
    		 System.out.println("id:"+e.getId()+"   name:"+e.getName()+"   type:"+e.getType()+
    				 "  parent:"+e.getParent().name+ "  phase:"+e.getPhase().getName());
    	}
	}
	
	@Test public void testGetEpicPhase() throws Exception {
		ClientPublicAPI client= new MockClientPublicAPI("");
		values = CommonParameters.getDefaultValueSet();
		List<EpicAttr> result = client.getEpicPhase(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), values.get(OctaneConstants.KEY_PHASE_LOGICNAME));
    	Assert.assertNotNull(result);
    	for(EpicAttr e : result){
    		System.out.println("id: " + e.getId() +"  name:"+ e.getName() + "  type:"+ e.getType());
    	}
	}
	
	@Test public void testGetEpicParent() throws Exception {
		ClientPublicAPI client= new MockClientPublicAPI("");
		values = CommonParameters.getDefaultValueSet();
    	List<EpicAttr> result = client.getEpicParent(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), values.get(OctaneConstants.KEY_WORKITEM_SUBTYPE));
    	Assert.assertNotNull(result);
    	for(EpicAttr e : result){
    		System.out.println("id: " + e.getId() +"  name:"+ e.getName() + "  type:"+ e.getType());
    	}
	}
}
