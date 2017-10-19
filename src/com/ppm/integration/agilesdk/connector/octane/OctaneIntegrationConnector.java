package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.IntegrationConnector;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneConnectivityExceptionHandler;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.ui.*;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OctaneIntegrationConnector extends IntegrationConnector {

    private final Logger logger = Logger.getLogger(this.getClass());

    @Override public String getExternalApplicationName() {
        return "Octane";
    }

    @Override public String getExternalApplicationVersionIndication() {
        return null;
    }

    @Override public String getTargetApplicationIcon() {
        return "data:img/jpg;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABc0lEQVQ4jX3TPUiWURQH8N+rluAgFJWiUWNjizRp1FCENjiY7hUlDhpCUkMORtmSIEVQYNGgJTT2ATUUTbU2iRUNFlISQeQQKDnc88jt7Xn7w+Fe7vn6n3PPqUBlfkINHEMb7pYp/wyMq6vhuA/TeIYZzOFwmWFDydskutGOe3iDK9iN0xjG98I4Z9CNb+jAfrzCZdzBIk5gBR9xvjrADTzEAI7gNnbiU+if4DnO4QBO4n4eoBPzeIntOI7+jN1V/EJvsBnGjrwHK2iK+9Ng9LWqNxfxGM3Yhp85g7qg24xWXPMvXkcp/XhXJC8YrIc8wFCJc4EhfMAFqeGbDBaC4mqUUAs/cEn6meU8QBu2BL16bP2P3IrsR/MSluKcxVvsRWNV9jV8Cd0uvMgDjOIzrkvdH433elTCGc7glDQnI3kJMIU90h4s46zU2DUcwntpgLowiN85gwJL6EEfxiLjqjQ0N6UF+wtlywSPQqbQgoM17GwAvklPnFgwUy4AAAAASUVORK5CYII=";
    }

    @Override public List<Field> getDriverConfigurationFields() {

        return Arrays.asList(new Field[] {new PlainText(OctaneConstants.KEY_BASE_URL, "BASE_URL", "", "", true),
                new LineBreaker(), new PlainText(OctaneConstants.KEY_PROXY_HOST, "PROXY_HOST", "", "", false),
                new PlainText(OctaneConstants.KEY_PROXY_PORT, "PROXY_PORT", "", "", false),
                new CheckBox(OctaneConstants.KEY_USE_GLOBAL_PROXY, "USE_GLOBAL_PROXY", "", false), new LineBreaker(),
                new PlainText(OctaneConstants.APP_CLIENT_ID, "CLIENT_ID", "", "", true),
                new PasswordText(OctaneConstants.APP_CLIENT_SECRET, "CLIENT_SECRET", "", "", true), new LineBreaker()});
    }

    /**
     * This is needed for keeping the Octane connector backward compatible with PPM 9.41.
     */
    @Override public List<FunctionIntegration> getIntegrations() {
        return Arrays
                .asList(new FunctionIntegration[] {new OctaneWorkPlanIntegration(), new OctaneTimeSheetIntegration()});
    }

    @Override public List<String> getIntegrationClasses() {
        return Arrays
                .asList(new String[] {"com.ppm.integration.agilesdk.connector.octane.OctaneWorkPlanIntegration","com.ppm.integration.agilesdk.connector.octane.OctaneTimeSheetIntegration", "com.ppm.integration.agilesdk.connector.octane.OctaneAgileDataIntegration"
                        , "com.ppm.integration.agilesdk.connector.octane.OctanePortfolioEpicIntegration", "com.ppm.integration.agilesdk.connector.octane.OctaneRequestIntegration"});
    }

    @Override public String getConnectorVersion() {
        return "1.0";
    }

    /**
     * Get all available agile projects(agile workspaces) in a instance
     *
     * @param paramValueSet a value set which contains the information of the instance
     * @return a list of agile project
     */
    @Override public List<AgileProject> getAgileProjects(ValueSet paramValueSet) {
        List<AgileProject> agileProjectList = new ArrayList();
        try {
            ClientPublicAPI client = ClientPublicAPI.getClient(paramValueSet);
            List<SharedSpace> sharedSpacesList = client.getSharedSpaces();
            for (SharedSpace sharedSpace : sharedSpacesList) {
                //workspace
                int sharedSpaceId = Integer.parseInt(sharedSpace.getId());
                String sharedSpaceName = sharedSpace.getName();
                List<WorkSpace> workspaces = client.getWorkSpaces(sharedSpaceId);
                for (WorkSpace workspace : workspaces) {
                    AgileProject project = new AgileProject();
                    String displayName = workspace.getName() + "(" + sharedSpaceName + ")";
                    project.setDisplayName(displayName);
                    JSONObject workspaceJson = new JSONObject();
                    workspaceJson.put(OctaneConstants.WORKSPACE_ID, Integer.parseInt(workspace.getId()));
                    workspaceJson.put(OctaneConstants.SHARED_SPACE_ID, sharedSpaceId);
                    project.setValue(workspaceJson.toString());
                    agileProjectList.add(project);
                }
            }
        } catch (Throwable e) {
            logger.error("Error when retrieving Octane workspaces list", e);
            new OctaneConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
        return agileProjectList;
    }

}
