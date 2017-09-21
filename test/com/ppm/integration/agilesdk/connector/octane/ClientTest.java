package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientHelper;
import com.ppm.integration.agilesdk.connector.octane.client.UsernamePasswordClient;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientTest {
    UsernamePasswordClient client = null;

    ValueSet values = new ValueSet();

    @Before public void setUp() throws Exception {
        values = CommonParameters.getDefaultValueSet();
        UsernamePasswordClient simpleClient =
                OctaneClientHelper.setupClient(new UsernamePasswordClient(values.get(OctaneConstants.KEY_BASE_URL)), values);
        client = OctaneClientHelper
                .setupClient(new UsernamePasswordClient(values.get(OctaneConstants.KEY_BASE_URL)), simpleClient.getCookies(), values);
    }

    @Test public void testGetSharedSpaces() throws Exception {
        List<SharedSpace> sharedSpaces = client.getSharedSpaces();
        Assert.assertNotNull(sharedSpaces);
        Assert.assertTrue(sharedSpaces.size() > 0);
        for (SharedSpace d : sharedSpaces) {
            System.out.println(d.name);
        }
    }

    @Test public void testGetWorkSpaces() throws Exception {
        List<WorkSpace> workSpace = client.getWorkSpaces(values.get(OctaneConstants.KEY_SHAREDSPACEID));
        Assert.assertNotNull(workSpace);
        Assert.assertTrue(workSpace.size() > 0);
        for (WorkSpace d : workSpace) {
            System.out.println(d.name);
        }
    }
}
