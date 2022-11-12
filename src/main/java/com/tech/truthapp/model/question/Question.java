package com.tech.truthapp.model.question;

import java.util.ArrayList;
import java.util.List;

import com.tech.truthapp.model.BaseModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@ToString
public class Question extends BaseModel{

	String id;
	String question;
	String category;
	String subCategory;
	String group;
	Long score;
	Boolean isPublic;
	Boolean isApproved;
	String ageGroup;
	String gender;
	List<String> tags = new ArrayList<String>();
	List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
	List<QuestionReviewer> reviews = new ArrayList<QuestionReviewer>();
}
