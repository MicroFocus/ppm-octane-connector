package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.*;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.epic.PortfolioEpicCreationInfo;
import com.ppm.integration.agilesdk.epic.PortfolioEpicIntegration;
import com.ppm.integration.agilesdk.epic.PortfolioEpicSyncInfo;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luch on 5/9/2017.
 */
public class OctanePortfolioEpicIntegration extends PortfolioEpicIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    private static final String DEFAULT_OCTANE_EPIC_URL =
            "/ui/entity-navigation?p={sharedSpaceId}/{workSpaceId}&entityType=work_item&id={epicId}";


    @Override public String createEpicInAgileProject(final PortfolioEpicCreationInfo epicInfo, final String value,
            final ValueSet paramValueSet)
    {
        Long epicId = null;
        EpicEntity epic = new EpicEntity();
        String epicName = epicInfo.getEpicName();
        String epicDescription = epicInfo.getEpicDescription();

        try {
            if (epicName == null || epicName.trim().length() == 0) {
                throw new OctaneClientException("AGM_APP", "Epic name is empty.");
            }
            epic.setName(epicName);
            epic.setDescription(epicDescription);
            ClientPublicAPI client = OnctaneIntegrationHelper.getClient(paramValueSet);
            JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(value);
            String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
            String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);
            List<EpicAttr> epicPhases = client.getEpicPhase(sharedSpaceId, workSpaceId, "phase.epic.new");
            epic.setPhase(epicPhases.get(0));

            List<EpicAttr> epicParents = client.getEpicParent(sharedSpaceId, workSpaceId, "work_item_root");
            epic.setParent(epicParents.get(0));
            List<EpicEntity> epicList = new ArrayList<EpicEntity>();
            epicList.add(epic);

            EpicCreateEntity epicCreateEntity = new EpicCreateEntity();
            epicCreateEntity.setData(epicList);

            List<EpicEntity> epics = client.createEpicInWorkspace(sharedSpaceId, workSpaceId, epicCreateEntity);
            epicId = epics.size() > 0 ? Long.valueOf(epics.get(0).id) : null;
        } catch (Exception e) {
            logger.error(e.getMessage());
            new OctaneConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        return epicId.toString();
    }

    @Override
    public PortfolioEpicSyncInfo getPortfolioEpicSyncInfo(String epicId, String agileProjectValue, ValueSet instanceConfigurationParameters)
    {
        PortfolioEpicSyncInfo epic = new PortfolioEpicSyncInfo();
        try {
            ClientPublicAPI client = OnctaneIntegrationHelper.getClient(instanceConfigurationParameters);
            JSONObject valueJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
            int shareSpaceId = valueJson.getInt("SHARED_SPACE_ID");
            int workSpaceId = valueJson.getInt("WORKSPACE_ID");
            String[] doneStatusIDs = client.getDoneDefinationOfUserStoryAndDefect(shareSpaceId, workSpaceId);

            WorkItemEpic epic1 = client.getEpicActualStoryPointsAndPath(shareSpaceId, workSpaceId, epicId);
            epic.setEpicName(epic1.name);
            WorkItemEpic epic2 = client.getEpicDoneStoryPoints(shareSpaceId, workSpaceId, epic1.path, doneStatusIDs);

            epic.setDoneStoryPoints(epic2.doneStoryPoints);
            epic.setTotalStoryPoints(epic1.totalStoryPoints);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return epic;
    }

    /**
     * Get the epic URI, which is used to generate a link of epic in PPM.
     *
     * @param epicId epic id in agile tool
     * @param value  this is a json format value, which contains the necessary information to create epic.
     * @return epic URI
     */
    @Override public String getEpicURI(final String epicId, final String value) {
        JSONObject valueJson = (JSONObject)JSONSerializer.toJSON(value);
        String epicUrl = DEFAULT_OCTANE_EPIC_URL;
        epicUrl = StringUtils.replace(epicUrl, "{sharedSpaceId}", valueJson.getString("SHARED_SPACE_ID"));
        epicUrl = StringUtils.replace(epicUrl, "{workSpaceId}", valueJson.getString("WORKSPACE_ID"));
        epicUrl = StringUtils.replace(epicUrl, "{epicId}", epicId);
        return epicUrl;
    }


}