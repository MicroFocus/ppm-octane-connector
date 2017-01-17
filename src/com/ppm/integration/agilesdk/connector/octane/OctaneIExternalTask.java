package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OctaneIExternalTask extends ExternalTask {
    public List<ExternalTask> octaneIExternaltask = new ArrayList<>();

    private OctaneTaskData octData = null;

    private TaskStatus status = TaskStatus.READY;

    public OctaneIExternalTask(OctaneTaskData octData) {
        this.octData = octData;
        // TODO Auto-generated constructor stub
    }

    public OctaneIExternalTask(OctaneTaskData octData, TaskStatus statue) {
        this.octData = octData;
        this.status = statue;
    }

    @Override public String getId() {
        return octData.strValue("id");
    }

    @Override public String getName() {
        return octData.strValue("name");
    }

    @Override public TaskStatus getStatus() {
        return status;
    }

    public Date getScheduleStart() {
        if (octData.dateValue("releaseStartDate", new Date(0, 0, 1)).equals(new Date(0, 0, 1))) {
            Date date = octData.dateValue("creationTime", new Date(0, 0, 1));
            return date;
        }
        Date biggerDate;
        if (octData.dateValue("sprintStartDate", new Date(0, 0, 1)).equals(new Date(0, 0, 1))) {
            biggerDate = octData.dateValue("releaseStartDate", new Date(0, 0, 1));
            if (biggerDate.before(octData.dateValue("creationTime", new Date(0, 0, 1)))) {
                biggerDate = octData.dateValue("creationTime", new Date(0, 0, 1));
            }
            return biggerDate;
        }
        biggerDate = octData.dateValue("sprintStartDate", new Date(0, 0, 1));
        if (biggerDate.before(octData.dateValue("creationTime", new Date(0, 0, 1)))) {
            biggerDate = octData.dateValue("creationTime", new Date(0, 0, 1));
        }
        return biggerDate;
    }

    @SuppressWarnings("deprecation") public Date getScheduleFinish() {
        if (octData.dateValue("releaseEndDate", new Date(0, 0, 1)).equals(new Date(0, 0, 1))) {//has no release
            Date date = octData.dateValue("lastModifiedTime", new Date(0, 0, 2));
            if (this.getScheduleStart().after(date)) {
                return this.getScheduleStart();
            }
            return date;
        }
        if (octData.dateValue("sprintEndDate", new Date(0, 0, 1)).equals(new Date(0, 0, 1))) {
            //has release, but has no sprint
            Date date = octData.dateValue("releaseEndDate", new Date(0, 0, 2));
            if (this.getScheduleStart().after(date)) {
                return this.getScheduleStart();
            }
            return date;
        }
        //has release and sprint,use sprintEndDate as task ScheduleFinish date
        Date date = octData.dateValue("sprintEndDate", new Date(0, 0, 2));
        if (this.getScheduleStart().after(date)) {
            return this.getScheduleStart();
        }
        return date;
    }

    @Override public String getOwnerRole() {
        return octData.strValue("ownerName");
    }

    @Override public List<ExternalTaskActuals> getActuals() {
        ExternalTaskActuals taskActual = new ExternalTaskActuals() {
            public double getScheduleEfforts() {
                return octData.intValue("estimatedHours", 0);// estimated_hours in task
            }

            @Override public Date getActualStart() {
                if (status == TaskStatus.READY) {
                    return null;
                }
                Date biggerDate;
                if (octData.dateValue("sprintStartDate", new Date(0, 0, 1)).equals(new Date(0, 0, 1))) {
                    biggerDate = octData.dateValue("releaseStartDate", new Date(0, 0, 1));
                    if (biggerDate.before(octData.dateValue("creationTime", new Date(0, 0, 1)))) {
                        return octData.dateValue("creationTime", new Date(0, 0, 1));
                    }
                    return biggerDate;
                }
                biggerDate = octData.dateValue("sprintStartDate", new Date(0, 0, 1));
                if (biggerDate.before(octData.dateValue("creationTime", new Date(0, 0, 1)))) {
                    return octData.dateValue("creationTime", new Date(0, 0, 1));
                }
                return biggerDate;
            }

            @Override public Date getActualFinish() {
                if (status == TaskStatus.COMPLETED) {
                    return octData.dateValue("lastModifiedTime", new Date(0, 0, 1));
                }
                return null;
            }

            public double getActualEfforts() {

                return octData.intValue("investedHours", 0);//estimated_hours - remaining_hours in task
            }

            public float getCompletePercent() {
                if (status == TaskStatus.COMPLETED) {
                    return 1;
                }
                if (octData.intValue("estimatedHours", 0) == 0)
                    return 0;
                return (float)octData.intValue("investedHours", 0) / (float)octData.intValue("estimatedHours", 0);
            }

            @Override public long getResourceId() {
                return octData.intValue("ownerName", -1);
            }
        };
        List<ExternalTaskActuals> iTaskActuals = new ArrayList<>();
        iTaskActuals.add(taskActual);
        return iTaskActuals;
    }

    @Override public long getOwnerId() {
        return octData.intValue("ownerName", -1);
    }

    @Override public boolean isMilestone() {
        return false;
    }

    @Override public List<ExternalTask> getChildren() {
        return octaneIExternaltask;
    }

}
