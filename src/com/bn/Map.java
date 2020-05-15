package com.bn;

public class Map {

	int id;
	String name;
	String creatorName;
	
	public Map()
	{
		
	}
	
	public Map(int id, String name, String creatorName) {
		super();
		this.id = id;
		this.name = name;
		this.creatorName = creatorName;
	}
	
	public Map(String name, String creatorName) {
		super();
		this.name = name;
		this.creatorName = creatorName;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCreatorName() {
		return creatorName;
	}
	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}
	
	
}
