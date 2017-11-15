package com.ppm.integration.agilesdk.connector.octane.model;


/**
 * StoryEntity is a JSON wrapper of story entity
 * <p/>
 *
 * @author Elva Zhu
 * @since 11/8/2017
 */

public class StoryEntity extends SimpleEntity{
	
	private String description;
	
	private Integer story_points;
	
	private SimpleEntity team;
	
	private SimpleEntity phase;
	
	private SimpleEntity release;
	
	private SimpleEntity sprint;
	
	private SimpleEntity author;
	
	private SimpleEntity owner;
	
	private SimpleEntity parent;

	public SimpleEntity getParent() {
		return parent;
	}

	public void setParent(SimpleEntity parent) {
		this.parent = parent;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public SimpleEntity getSprint() {
		return sprint;
	}

	public void setSprint(SimpleEntity sprint) {
		this.sprint = sprint;
	}

	public SimpleEntity getAuthor() {
		return author;
	}

	public void setAuthor(SimpleEntity author) {
		this.author = author;
	}

	public SimpleEntity getOwner() {
		return owner;
	}

	public void setOwner(SimpleEntity owner) {
		this.owner = owner;
	}
	

}
