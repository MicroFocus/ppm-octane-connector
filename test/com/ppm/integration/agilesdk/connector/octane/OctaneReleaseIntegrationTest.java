package com.ppm.integration.agilesdk.connector.octane;

import com.hp.ppm.integration.model.Workspace;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.release.ReleaseBacklogItem;
import com.ppm.integration.agilesdk.release.ReleaseFeature;
import com.ppm.integration.agilesdk.release.ReleaseRelease;
import com.ppm.integration.agilesdk.release.ReleaseReleaseTeam;
import com.ppm.integration.agilesdk.release.ReleaseSprint;
import com.ppm.integration.agilesdk.release.ReleaseTheme;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OctaneReleaseIntegrationTest {
    ValueSet values = new ValueSet();
    Workspace wp=new Workspace();

    @Before public void setUp() throws Exception {
        values = CommonParameters.getDefaultValueSet();
        wp.setId("InstanceId-1");
    }

    @Test public void testGetBacklogItems() throws Exception {
        OctaneReleaseIntegration ori = new OctaneReleaseIntegration();
        List<ReleaseBacklogItem> bItem = ori.getBacklogItems(wp, values);
        Assert.assertNotNull(bItem);
        Assert.assertTrue(bItem.size() > 0);
        for (ReleaseBacklogItem b : bItem) {
            System.out.println(b.getBacklogItemId() + "," + b.getName() +"," + b.getLastModified());
        }
    }

    @Test public void testGetFeatures() throws Exception {
        OctaneReleaseIntegration ori = new OctaneReleaseIntegration();
        List<ReleaseFeature> fItem = ori.getFeatures(wp,values);
        Assert.assertNotNull(fItem);
        Assert.assertTrue(fItem.size() > 0);
        for (ReleaseFeature f : fItem) {
            System.out.println(f.getName());
        }
    }

    @Test public void testGetReleaseTeams() throws Exception {
        OctaneReleaseIntegration ori = new OctaneReleaseIntegration();
        List<ReleaseReleaseTeam> rtItem = ori.getReleaseTeams(wp,values);
        Assert.assertNotNull(rtItem);
        Assert.assertTrue(rtItem.size() > 0);
        for (ReleaseReleaseTeam b : rtItem) {
            System.out.println(b.getReleaseTeamId());
        }
    }

    @Test public void testGetReleases() throws Exception {
        OctaneReleaseIntegration ori = new OctaneReleaseIntegration();
        List<ReleaseRelease> rItem = ori.getReleases(wp,values);
        Assert.assertNotNull(rItem);
        Assert.assertTrue(rItem.size() > 0);
        for (ReleaseRelease r : rItem) {
            System.out.println(r.getName());
        }
    }

    @Test public void testGetSprints() throws Exception {
        OctaneReleaseIntegration ori = new OctaneReleaseIntegration();
        List<ReleaseSprint> sItem = ori.getSprints(wp,values);
        Assert.assertNotNull(sItem);
        Assert.assertTrue(sItem.size() > 0);
        for (ReleaseSprint s : sItem) {
            System.out.println(s.getName());
        }
    }

    @Test public void testGetTeams() throws Exception {
        OctaneReleaseIntegration ori = new OctaneReleaseIntegration();
        List<com.ppm.integration.agilesdk.release.ReleaseTeam> rtItem = ori.getTeams(wp,values);
        Assert.assertNotNull(rtItem);
        Assert.assertTrue(rtItem.size() > 0);
        for (com.ppm.integration.agilesdk.release.ReleaseTeam rt : rtItem) {
            System.out.println(rt.getName());
        }
    }

    @Test public void testGetThemes() throws Exception {
        OctaneReleaseIntegration ori = new OctaneReleaseIntegration();
        List<ReleaseTheme> tItem = ori.getThemes(wp,values);
        Assert.assertNotNull(tItem);
        Assert.assertTrue(tItem.size() > 0);
        for (ReleaseTheme t : tItem) {
            System.out.println(t.getName());
        }
    }
}
