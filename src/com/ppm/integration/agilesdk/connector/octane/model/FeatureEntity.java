package com.ppm.integration.agilesdk.connector.octane.model;

/**
 * {@code FeatureEntity} is a is a JSON wrapper of feature entity
 * <p/>
 *
 * @author ChunQi, Lu
 * @since 11/3/2017
 */
public class FeatureEntity extends SimpleEntity {
    private SimpleEntity author;

    private String description;

    private SimpleEntity owner;

    private SimpleEntity phase;

    private SimpleEntity release;

    private Integer story_points;

    private SimpleEntity team;

    public SimpleEntity getAuthor() {
        return author;
    }

    public void setAuthor(SimpleEntity author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SimpleEntity getOwner() {
        return owner;
    }

    public void setOwner(SimpleEntity owner) {
        this.owner = owner;
    }

    public SimpleEntity getPhase() {
        return phase;
    }

    public void setPhase(SimpleEntity phase) {
        this.phase = phase;
    }

    public SimpleEntity getRelease() {
        return release;
    }

    public void setRelease(SimpleEntity release) {
        this.release = release;
    }

    public Integer getStory_points() {
        return story_points;
    }

    public void setStory_points(Integer story_points) {
        this.story_points = story_points;
    }

    public SimpleEntity getTeam() {
        return team;
    }

    public void setTeam(SimpleEntity team) {
        this.team = team;
    }
}
