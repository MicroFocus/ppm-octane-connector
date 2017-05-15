package com.ppm.integration.agilesdk.connector.octane.model;

public class EpicEntity extends SimpleEntity {
    private EpicAttr parent;

    private EpicAttr phase;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public EpicAttr getParent() {
        return parent;
    }

    public void setParent(final EpicAttr parent) {
        this.parent = parent;
    }

    public EpicAttr getPhase() {
        return phase;
    }

    public void setPhase(final EpicAttr phase) {
        this.phase = phase;
    }
}
