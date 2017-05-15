package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.EpicAttr;
import com.ppm.integration.agilesdk.connector.octane.model.EpicCreateEntity;
import com.ppm.integration.agilesdk.connector.octane.model.EpicEntity;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.epic.AgileProject;
import com.ppm.integration.agilesdk.epic.PortfolioEpicIntegration;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luch on 5/9/2017.
 */
public class OctanePortfolioEpicIntegration extends PortfolioEpicIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    @Override
    public List<AgileProject> getAgileProjects(ValueSet paramValueSet) {
        List<AgileProject> agileProjectList = new ArrayList();
        try {
            ClientPublicAPI client = OnctaneIntegrationHelper.getClient(paramValueSet);
            List<SharedSpace> sharedSpacesList = client.getSharedSpaces();
            for (SharedSpace sharedSpace : sharedSpacesList) {
                //workspace
                int sharedSpaceId = Integer.parseInt(sharedSpace.getId());
                String sharedSpaceName = sharedSpace.getName();
                List<WorkSpace> workspaces = client.getWorkSpaces(sharedSpaceId);
                for (WorkSpace workspace : workspaces) {
                    AgileProject project = new AgileProject();
                    String displayName = sharedSpaceName + "/" + workspace.getName();
                    project.setDisplayName(displayName);
                    JSONObject workspaceJson = new JSONObject();
                    workspaceJson.put(OctaneConstants.WORKSPACE_ID, Integer.parseInt(workspace.getId()));
                    workspaceJson.put(OctaneConstants.SHARED_SPACE_ID, sharedSpaceId);
                    project.setValue(workspaceJson.toString());
                    agileProjectList.add(project);
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
            new OctaneConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
        return agileProjectList;
    }

    @Override
    public Long createEpicInAgileProject(final String epicName, final String value, final ValueSet paramValueSet) {
        Long epicId = null;
        EpicEntity epic = new EpicEntity();

        try {
            if (epicName == null || epicName.trim().length() == 0) {
                throw new OctaneClientException("AGM_APP", "Epic name is empty.");
            }
            epic.setName(epicName);
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

        return epicId;
    }
}
