package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code FeatureCreateEntity} is a JSON wrapper of creating feature entity
 * <p/>
 *
 * @author ChunQi, Lu
 * @since 11/3/2017
 */
public class FeatureCreateEntity {

    private List<FeatureEntity> data;

    public List<FeatureEntity> getData() {
        return data;
    }

    public void setData(List<FeatureEntity> data) {
        this.data = data;
    }

    public void addFeatureEntity(FeatureEntity entity) {
        if (data == null) {
            data = new ArrayList<FeatureEntity>();
        }
        data.add(entity);
    }
}
