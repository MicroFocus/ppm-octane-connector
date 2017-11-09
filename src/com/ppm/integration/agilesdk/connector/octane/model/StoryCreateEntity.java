package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.ArrayList;

/**
 * StoryEntity is a JSON wrapper of creating story entity
 * <p/>
 *
 * @author Elva Zhu
 * @since 11/8/2017
 */

import java.util.List;

public class StoryCreateEntity extends SimpleEntity{
	
    private List<StoryEntity> data;

    public List<StoryEntity> getData() {
      return data;
    }

    public void setData(List<StoryEntity> data) {
      this.data = data;
    }
    
    public void addStoryEntity(StoryEntity entity) {
        if (data == null) {
            data = new ArrayList<StoryEntity>();
        } else {
            data.add(entity);
        }
    }

}
