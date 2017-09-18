package com.ppm.integration.agilesdk.connector.octane;

import com.hp.ppm.integration.model.Workspace;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataBacklogItem;
import com.ppm.integration.agilesdk.agiledata.AgileDataFeature;
import com.ppm.integration.agilesdk.agiledata.AgileDataReleaseTeam;
import com.ppm.integration.agilesdk.agiledata.AgileDataRelease;
import com.ppm.integration.agilesdk.agiledata.AgileDataSprint;
import com.ppm.integration.agilesdk.agiledata.AgileDataEpic;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OctaneAgileDataIntegrationTest {
    ValueSet values = new ValueSet();
    Workspace wp=new Workspace();
    OctaneAgileDataIntegration ori = new OctaneAgileDataIntegration();

    @Before public void setUp() throws Exception {
        values = CommonParameters.getDefaultValueSet();
        wp.setId("InstanceId-1");
        ori.setUp(values, "3001");
    }

    @Test public void testGetBacklogItems() throws Exception {
        
        List<AgileDataBacklogItem> bItem = ori.getBacklogItems();
        Assert.assertNotNull(bItem);
        Assert.assertTrue(bItem.size() > 0);
        for (AgileDataBacklogItem b : bItem) {
            System.out.println(b.getBacklogItemId() + "," + b.getName() +"," + b.getLastModified());
        }
    }

    @Test public void testGetFeatures() throws Exception {
        List<AgileDataFeature> fItem = ori.getFeatures();
        Assert.assertNotNull(fItem);
        Assert.assertTrue(fItem.size() > 0);
        for (AgileDataFeature f : fItem) {
            System.out.println(f.getName());
        }
    }

    @Test public void testGetReleaseTeams() throws Exception {
        List<AgileDataReleaseTeam> rtItem = ori.getReleaseTeams();
        Assert.assertNotNull(rtItem);
        Assert.assertTrue(rtItem.size() > 0);
        for (AgileDataReleaseTeam b : rtItem) {
            System.out.println(b.getReleaseTeamId());
        }
    }

    @Test public void testGetReleases() throws Exception {
        List<AgileDataRelease> rItem = ori.getReleases();
        Assert.assertNotNull(rItem);
        Assert.assertTrue(rItem.size() > 0);
        for (AgileDataRelease r : rItem) {
            System.out.println(r.getName());
        }
    }

    @Test public void testGetSprints() throws Exception {
        List<AgileDataSprint> sItem = ori.getSprints();
        Assert.assertNotNull(sItem);
        Assert.assertTrue(sItem.size() > 0);
        for (AgileDataSprint s : sItem) {
            System.out.println(s.getName());
        }
    }

    @Test public void testGetTeams() throws Exception {
        List<com.ppm.integration.agilesdk.agiledata.AgileDataTeam> rtItem = ori.getTeams();
        Assert.assertNotNull(rtItem);
        Assert.assertTrue(rtItem.size() > 0);
        for (com.ppm.integration.agilesdk.agiledata.AgileDataTeam rt : rtItem) {
            System.out.println(rt.getName());
        }
    }

    @Test public void testGetThemes() throws Exception {
        List<AgileDataEpic> tItem = ori.getEpics();
        Assert.assertNotNull(tItem);
        Assert.assertTrue(tItem.size() > 0);
        for (AgileDataEpic t : tItem) {
            System.out.println(t.getName());
        }
    }
}
