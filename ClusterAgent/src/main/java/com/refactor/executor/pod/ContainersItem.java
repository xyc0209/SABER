package com.refactor.executor.pod;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;

@Generated("com.robohorse.robopojogenerator")
public class ContainersItem{

	@SerializedName("usage")
	private Usage usage;

	@SerializedName("name")
	private String name;

	public void setUsage(Usage usage){
		this.usage = usage;
	}

	public Usage getUsage(){
		return usage;
	}

	public void setName(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	@Override
 	public String toString(){
		return 
			"ContainersItem{" + 
			"usage = '" + usage + '\'' + 
			",name = '" + name + '\'' + 
			"}";
		}
}