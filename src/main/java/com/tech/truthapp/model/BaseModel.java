package com.tech.truthapp.model;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseModel {

	
	protected Date createdAt;
	protected String createdBy;
	
	protected Date updatedAt;
	protected String lastModifiedBy;
	protected Long version;
	
	
}