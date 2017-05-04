package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.List;

public class EpicCreateEntity extends SimpleEntity{
    private List<EpicEntity> data;

    public List<EpicEntity> getData() {
      return data;
    }

    public void setData(List<EpicEntity> data) {
      this.data = data;
    }
}