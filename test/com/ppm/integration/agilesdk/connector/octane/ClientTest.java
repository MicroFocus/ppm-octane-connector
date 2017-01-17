package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.Client;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemEpic;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemFeature;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemStory;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientTest {
    Client client = null;

    ValueSet values = new ValueSet();

    @Before public void setUp() throws Exception {
        values = CommonParameters.getDefaultValueSet();
        Client simpleClient =
                OctaneFunctionIntegration.setupClient(new Client(values.get(OctaneConstants.KEY_BASE_URL)), values);
        client = OctaneFunctionIntegration
                .setupClient(new Client(values.get(OctaneConstants.KEY_BASE_URL)), simpleClient.getCookies(), values);
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

    @Test public void testGetReleases() throws Exception {
        List<Release> releases = client.getReleases(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID));
        Assert.assertNotNull(releases);
        Assert.assertTrue(releases.size() > 0);
        for (Release d : releases) {
            System.out.println("name=" + d.name + ", id=" + d.id);
        }
    }

    @Test public void testGetRelease() throws Exception {
        Release release = client.getRelease(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), values.get(OctaneConstants.KEY_RELEASEID));
        Assert.assertNotNull(release);
        System.out.println("start_date" + release.startDate + ", end_date=" + release.endDate);
    }

    @Test public void testGetWorkItems() throws Exception {
        WorkItemRoot workItemRoot = client.getWorkItems(values.get(OctaneConstants.KEY_SHAREDSPACEID),
                values.get(OctaneConstants.KEY_WORKSPACEID), values.get(OctaneConstants.KEY_RELEASEID));
        Assert.assertNotNull(workItemRoot);
        for (WorkItemStory d : workItemRoot.storyList) {
            System.out
                    .println("name=" + d.name + ", id=" + d.id + ", type=" + d.subType + ", releaseid=" + d.releaseId);
        }
        Set<String> keySet = workItemRoot.epicList.keySet();
        for (String key : keySet) {
            WorkItemEpic tempEpic = workItemRoot.epicList.get(key);
            System.out.println("                        ");
            System.out.println("name=" + tempEpic.name + ", id=" + tempEpic.id + ", type=" + tempEpic.subType);
            System.out.println("------------------------");
            Set<String> keySetFeature = tempEpic.featureList.keySet();
            for (String keyFeature : keySetFeature) {
                WorkItemFeature tempFeature = tempEpic.featureList.get(keyFeature);
                System.out.println(
                        "name=" + tempFeature.name + ", id=" + tempFeature.id + ", type=" + tempFeature.subType
                                + "releaseid=" + tempFeature.releaseId);
                System.out.println("--------------");
                for (WorkItemStory d : tempFeature.storyList) {
                    System.out.println(
                            "name=" + d.name + ", id=" + d.id + ", type=" + d.subType + "releaseid=" + d.releaseId);
                }
            }
        }
    }

}
