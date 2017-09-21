package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.ppm.integration.agilesdk.connector.octane.OctaneIntegrationConnector;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegrationContext;
import com.ppm.integration.agilesdk.provider.LocalizationProvider;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to store context information during work plan generation.
 */
public class WorkplanContext {

    private List<Sprint> sprints;

    private Map<String, Sprint> sprintsMap;

    public Map<String, String> phases = new HashMap<>();

    public WorkPlanIntegrationContext wpiContext;

    private LocalizationProvider lp = null;

    private UserProvider up = null;

    public String percentComplete;

    public Map<String, String> usersEmails = new HashMap<>();

    public List<Sprint> getSprints() {
        return sprints;
    }

    public void setSprints(List<Sprint> sprints) {
        this.sprints = sprints;
        sprintsMap = new HashMap<>(sprints.size());
        for (Sprint s : sprints) {
            sprintsMap.put(s.getId(), s);
        }
    }

    public synchronized  LocalizationProvider getLocalizationProvider() {
        if (lp == null) {
            lp = Providers.getLocalizationProvider(OctaneIntegrationConnector.class);
        }

        return lp;
    }

    public synchronized  UserProvider getUserProvider() {
        if (up == null) {
            up = Providers.getUserProvider(OctaneIntegrationConnector.class);
        }

        return up;
    }

    public ExternalTask.TaskStatus getPPMStatus(String phaseId) {

        String phaseLabel = phases.get(phaseId);

        switch (phaseLabel) {
            case "In Progress":
            case "In Testing":
            case "Opened":
                return ExternalTask.TaskStatus.IN_PROGRESS;
            case "Done":
            case "Closed":
            case "Proposed Closed":
            case "Rejected":
            case "Duplicate":
            case "Fixed":
                return ExternalTask.TaskStatus.COMPLETED;
            case "New":
            case "Deferred":
                return ExternalTask.TaskStatus.READY;
        }

        // TODO remove debug
        System.out.println("Unknown status. id: "+phaseId+" label:"+phaseLabel);
        return ExternalTask.TaskStatus.READY;
    }

    public Sprint getSprint(String sprintId) {
        return sprintsMap.get(sprintId);
    }

}
