package com.ppm.integration.agilesdk.connector.octane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.IntegrationConnector;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.ui.CheckBox;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.LineBreaker;
import com.ppm.integration.agilesdk.ui.PasswordText;
import com.ppm.integration.agilesdk.ui.PlainText;

import net.sf.json.JSONObject;

public class OctaneIntegrationConnector extends IntegrationConnector {

    private final Logger logger = Logger.getLogger(this.getClass());

    @Override public String getExternalApplicationName() {
        return "Octane";
    }

    @Override public String getExternalApplicationVersionIndication() {
        return null;
    }

    @Override public String getTargetApplicationIcon() {
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyhpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNi1jMTM4IDc5LjE1OTgyNCwgMjAxNi8wOS8xNC0wMTowOTowMSAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTcgKE1hY2ludG9zaCkiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6OTM2MkE5Rjk0OUMyMTFFOEEzQjA5RTNCNUI4RTI3NkIiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6OTM2MkE5RkE0OUMyMTFFOEEzQjA5RTNCNUI4RTI3NkIiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW5zdGFuY2VJRD0ieG1wLmlpZDo5MzYyQTlGNzQ5QzIxMUU4QTNCMDlFM0I1QjhFMjc2QiIgc3RSZWY6ZG9jdW1lbnRJRD0ieG1wLmRpZDo5MzYyQTlGODQ5QzIxMUU4QTNCMDlFM0I1QjhFMjc2QiIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/PgOPbQgAAAFrSURBVHjafNNPKARhGMfx2bUXWRFXysXBvygHLg7KQYpI3CguImmP8mddyE22bKKUgxxcJCmJUg4uDvI3nIRIKLU5aGp8p34zvW0z3vr0Tjvv+8zzPO+7EcdxrOKmIStkjCMXM0EvP09WrGjIxjacYR5JXKInaGF2gHKcYg/1+MYHqrGFC9QGBcjDLO5Rgk793otmPfdr/TlSKDADbGISEyhFN35wgCs8YUCZjGIMO+7GmAJU4U41N6DP+LI7WnGtPqSVYZ0ZIANbz4dq2rER4Ab76kMEX9rjl5CjRe6X4mgJaHi75kE8aI+fgdvpRqW+jveAALYavaYMX80MXnSEb2pW2JjWUdZoj59BXPMqKlAYEiCjNUvINwP8ak6Ko2b9N2yzhBEdoTemUKQ7UaZLkzDep7xSo0YT3UtUiV3MYRnPeMSiHOmKJ9QvvwRv3KIDXbqdjvHOPaGN7DpiIfVt62+8oHk4rBF/AgwAbelQHo31YmMAAAAASUVORK5CYII=";
    }

    @Override public List<Field> getDriverConfigurationFields() {

        return Arrays.asList(new Field[] {new PlainText(OctaneConstants.KEY_BASE_URL, "BASE_URL", "", "", true),
                new LineBreaker(), new PlainText(OctaneConstants.KEY_PROXY_HOST, "PROXY_HOST", "", "", false),
                new PlainText(OctaneConstants.KEY_PROXY_PORT, "PROXY_PORT", "", "", false),
                new CheckBox(OctaneConstants.KEY_USE_GLOBAL_PROXY, "USE_GLOBAL_PROXY", "", false), new LineBreaker(),
                new PlainText(OctaneConstants.APP_CLIENT_ID, "CLIENT_ID", "", "", true),
                new PasswordText(OctaneConstants.APP_CLIENT_SECRET, "CLIENT_SECRET", "", "", true), new LineBreaker(),
                new CheckBox(OctaneConstants.KEY_ALLOW_WILDCARD_PROJECT, "KEY_ALLOW_WILDCARD_PROJECT_MAPPING", false)});
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
                        , "com.ppm.integration.agilesdk.connector.octane.OctanePortfolioEpicIntegration",
                        "com.ppm.integration.agilesdk.connector.octane.OctaneRequestIntegration",
                        "com.ppm.integration.agilesdk.connector.octane.OctaneUserIntegration",
                        "com.ppm.integration.agilesdk.connector.octane.OctanePortfolioIntegration"});
    }

    @Override public String getConnectorVersion() {
        return "2.0";
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
                  int sharedSpaceId = Integer.parseInt(sharedSpace.getId());
                  String sharedSpaceName = sharedSpace.getName();
                //workspace
                List<WorkSpace> workspaces = client.getWorkSpaces(sharedSpaceId, true);
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
            if(!agileProjectList.isEmpty() && 
                    "true".equals(paramValueSet.get(OctaneConstants.KEY_ALLOW_WILDCARD_PROJECT))) {
                AgileProject agileProject = new AgileProject();
                agileProject.setDisplayName("*");
                agileProject.setValue("*");
                agileProjectList.add(0,agileProject);
            }
        } catch (Throwable e) {
            logger.error("Error when retrieving Octane workspaces list", e);
            throw new OctaneClientException("AGM_APP", "ERROR_IN_HTTP_CONNECTIVITY", new String[] {e.getMessage()});
        }
        return agileProjectList;
    }

    @Override
    /** @since 10.0.3 */
    public String testConnection(ValueSet instanceConfigurationParameters) {

        try {
            // It will login with client id and secret to test connection.
            ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
            logger.debug("Login successfully!");
        } catch (Exception e) {
            logger.error("Error when testing connectivity", e);
            return e.getMessage();
        }
        return null;
    }

}
