package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;
import com.ppm.integration.agilesdk.pm.ExternalTask.TaskStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OctaneUSIExternalTask extends ExternalTask {
    public List<ExternalTask> octaneIExternaltask = new ArrayList<>();

    private OctaneTaskData octData = null;

    private TaskStatus status = TaskStatus.READY;

    public OctaneUSIExternalTask(OctaneTaskData octData) {
        this.octData = octData;
        // TODO Auto-generated constructor stub
    }

    public OctaneUSIExternalTask(OctaneTaskData octData, TaskStatus statue) {
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
    
    @SuppressWarnings("deprecation")
    protected Date adjustFinishDateTime(Date date) {
        if (date == null) {
            return null;
        }

        if (date.getHours() < 23) {
            date.setHours(23);
        }

        return date;
    }

    @Override
	public Date getScheduledStart() {
		Date sprintStartDate = octData.dateValue("sprintStartDate", new Date(0));
		Date sprintEndDate = octData.dateValue("sprintEndDate", new Date(0));
		Date usCreationDate = octData.dateValue("creationTime", new Date(0));
		if (sprintStartDate.before(usCreationDate) && sprintEndDate.after(usCreationDate)) {
			return usCreationDate;
		}
		return sprintStartDate;
	}

    @Override
    public Date getScheduledFinish() { 
    	return this.adjustFinishDateTime(octData.dateValue("sprintEndDate", new Date(0)));
    }

    @Override public String getOwnerRole() {
        return octData.strValue("ownerName");
    }

    @Override public List<ExternalTaskActuals> getActuals() {
        ExternalTaskActuals taskActual = new ExternalTaskActuals() {
        	
        	@Override
            public double getScheduledEffort() {
                return octData.intValue("estimatedHours", 0);// estimated_hours in task
            }

            @Override public Date getActualStart() {
              if(getPercentComplete() !=0){
            	  
            	Date sprintStartDate = octData.dateValue("sprintStartDate", new Date(0));
          		Date sprintEndDate = octData.dateValue("sprintEndDate", new Date(0));
          		Date usCreationDate = octData.dateValue("creationTime", new Date(0));
          		if (sprintStartDate.before(usCreationDate) && sprintEndDate.after(usCreationDate)) {
          			return usCreationDate;
          		}
          		else{
          			return sprintStartDate;
          		}
              }
              return null;
            }

            @Override public Date getActualFinish() {
                if (status == TaskStatus.COMPLETED) {
                    return adjustFinishDateTime(octData.dateValue("lastModifiedTime", new Date(0)));
                }
                return null;
            }

            public double getActualEfforts() {

                return octData.intValue("investedHours", 0);//estimated_hours - remaining_hours in task
            }
            
            @Override
            public double getPercentComplete() {
            	if (status == TaskStatus.COMPLETED) {
                    return 1;
                }
                if ((float)octData.intValue("remainingHours", 0)+(float)octData.intValue("investedHours", 0) == 0)
                    return 0;

                return (float)octData.intValue("investedHours", 0) / ((float)octData.intValue("remainingHours", 0)+(float)octData.intValue("investedHours", 0))*100;
          
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
