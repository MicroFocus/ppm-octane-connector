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
	
	private Integer stroy_point;
	
	private SimpleEntity team;
	
	private SimpleEntity phase;
	
	private SimpleEntity release;
	
	private SimpleEntity sprint;
	
	private SimpleEntity author;
	
	private SimpleEntity owner;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getStroy_point() {
		return stroy_point;
	}

	public void setStroy_point(Integer stroy_point) {
		this.stroy_point = stroy_point;
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
