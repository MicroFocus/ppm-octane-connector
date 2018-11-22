package com.ppm.integration.agilesdk.connector.octane.model.workplan;

import com.hp.itg.pm.dao.impl.WorkplanDAOImpl;
import com.kintana.core.arch.Home;
import com.kintana.core.region.bean.Region;
import com.mercury.itg.core.calendar.dao.ITGCalendarDAO;
import com.mercury.itg.core.calendar.model.CalendarWorkingDayCache;
import com.mercury.itg.pm.service.WorkPlanService;
import com.mercury.itg.pm.service.impl.PMServiceFactory;
import com.mercury.itg.pm.service.util.PMServiceHelper;
import com.mercury.itg.util.HibernateUtil;
import com.ppm.integration.agilesdk.connector.octane.OctaneIntegrationConnector;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegrationContext;
import com.ppm.integration.agilesdk.provider.LocalizationProvider;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;

import java.util.*;

/**
 * Class used to store context information during work plan generation.
 */
public class WorkplanContext {

    private CalendarWorkingDayCache workingDayCache = null;

    private boolean isWorkingDayCacheInit = false;

    private List<Sprint> sprints;

    private Map<String, Sprint> sprintsMap;

    public Map<String, String> phases = new HashMap<>();

    public WorkPlanIntegrationContext wpiContext;

    private LocalizationProvider lp = null;

    private UserProvider up = null;

    public String percentComplete;

    public Map<String, String> usersEmails = new HashMap<>();

    public boolean showItemsAsTasks;

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
            case "Rejected":
            case "Duplicate":
                return ExternalTask.TaskStatus.CANCELLED;
            case "Done":
            case "Closed":
            case "Proposed Closed":
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

    public Date moveToNextWorkingDay(Date date) {
        synchronized (this) {
            if (this.isWorkingDayCacheInit == false) {

                try {
                    workingDayCache = new WorkplanDAOImpl().getWorkPlanById(wpiContext.currentTask().getWorkplanId()).getCalendar().getWorkingDayCache();
                } catch (Exception e) {
                    // Too bad, we won't adjust working days, this may result in tasks with zero duration if occuring during week ends.
                    workingDayCache = null;
                }

                isWorkingDayCacheInit = true;
            }
        }

        if (this.workingDayCache == null) {
            return date;
        }

        if (workingDayCache.isWorkDay(date)) {
            return date;
        }

        do {
            Calendar c = new GregorianCalendar();
            c.setTime(date);
            c.add(Calendar.DATE, 1);
            date = c.getTime();
        } while (!workingDayCache.isWorkDay(date));

        return date;
    }
}
