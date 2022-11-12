package com.tech.truthapp.question.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech.truthapp.audit.question.QuestionAuditService;
import com.tech.truthapp.audit.question.QuestionResponseAuditService;
import com.tech.truthapp.audit.question.QuestionResponseReviewAuditService;
import com.tech.truthapp.audit.question.QuestionReviewAuditService;
import com.tech.truthapp.dto.question.QuestionDTO;
import com.tech.truthapp.dto.question.QuestionResponsesDTO;
import com.tech.truthapp.dto.question.ValidateQuestionDTO;
import com.tech.truthapp.elastic.ElasticSearchOperations;
import com.tech.truthapp.model.question.Question;
import com.tech.truthapp.model.question.QuestionResponse;
import com.tech.truthapp.model.question.QuestionResponseReviewer;
import com.tech.truthapp.model.question.QuestionReviewer;
import com.tech.truthapp.question.mapper.QuestionMapper;
import com.tech.truthapp.question.mapper.QuestionResponseMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class QuestionService {

	private final String indexName = "questions";

	@Autowired
	private QuestionMapper questionMapper;
	
	@Autowired
	private QuestionAuditService questionAuditService;
	
	@Autowired
	private ElasticSearchOperations elasticSearchOperations;
	
	@Autowired
	private QuestionResponseAuditService questionResponseAuditService;

	@Autowired
	private QuestionResponseReviewAuditService questionResponseReviewAuditService;
	
	@Autowired
	private QuestionReviewAuditService questionReviewAuditService;
	
	@Autowired
	private QuestionResponseMapper questionResponseMapper;
	
	
	public QuestionDTO saveQuestion(QuestionDTO questionDTO) throws Exception {
		Question question = questionMapper.toEntity(questionDTO);
		question.setIsApproved(Boolean.FALSE);
		question.setScore(2L);
		question.setId(UUID.randomUUID().toString());
		questionAuditService.updateQuestionAuditOnCreate(question);
		IndexRequest indexRequest = new IndexRequest(indexName);
		indexRequest.id(question.getId());
		
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(question);
		System.out.println(jsonString);
		indexRequest.source(jsonString, XContentType.JSON);
		IndexResponse response = elasticSearchOperations.createIndex(indexRequest);
		
		System.out.println(response);
		if (response != null) {
			if (response.getResult().name().equalsIgnoreCase("Created")) {
				log.info("Successfully Created");
			} else {
				throw new Exception("Exception while creation of question request");
			}
		}
		questionDTO = questionMapper.toDto(question);
		return questionDTO;
	}

	public QuestionDTO updateQuestion(QuestionDTO questionDTO) throws Exception {
		Integer maxScore = 0;
		Boolean isApproved = false;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
				        .filter(QueryBuilders.matchQuery("id", questionDTO.getId()))
				        .filter(QueryBuilders.matchQuery("isApproved", isApproved))
				        .filter(QueryBuilders.rangeQuery("score").gt(maxScore));
		
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		if (hits != null && hits.length == 0) {
			throw new Exception("There is no record found for question id " + questionDTO.getId());
		}
		SearchHit searchHit = hits[0];
		Map<String, Object> doc	 = searchHit.getSourceAsMap();
		Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
				
		dbQuestion.setQuestion(questionDTO.getQuestion());
		dbQuestion.setIsPublic(questionDTO.getIsPublic());
		dbQuestion.setCategory(questionDTO.getCategory());
		dbQuestion.setSubCategory(questionDTO.getSubCategory());
		questionAuditService.updateQuestionAuditOnUpdate(dbQuestion);
		
		UpdateRequest updateRequest = new UpdateRequest(indexName , dbQuestion.getId());
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(dbQuestion);
		System.out.println(jsonString);
		updateRequest.doc(jsonString, XContentType.JSON);
		// updateRequest.upsert(jsonString, XContentType.JSON);
		UpdateResponse response = elasticSearchOperations.updateIndex(updateRequest);
		System.out.println(response);
		if (response != null) {
			if (response.getResult().name().equalsIgnoreCase("Updated")) {
				log.info("Successfully Updated Question Object");
			} else {
				throw new Exception("Exception while updation of question request");
			}	
		}	
		QuestionDTO dtoObject = questionMapper.toDto(dbQuestion);
		return dtoObject;
	}
	
	/***
	 * 
	 * @param questionId
	 * @return
	 * @throws Exception
	 */
	public QuestionDTO deleteQuestion(String questionId) throws Exception {
		Integer maxScore = 0;
		Boolean isApproved = false;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
				        .filter(QueryBuilders.matchQuery("id", questionId))
				        .filter(QueryBuilders.matchQuery("isApproved", isApproved))
				        .filter(QueryBuilders.rangeQuery("score").gt(maxScore));
		
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		if (hits != null && hits.length == 0) {
			throw new Exception("There is no record found for question id " + questionId);
		}
		SearchHit searchHit = hits[0];
		Map<String, Object> doc	 = searchHit.getSourceAsMap();
		Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
				
		dbQuestion.setScore(-1L);
		questionAuditService.updateQuestionAuditOnUpdate(dbQuestion);
		
		UpdateRequest updateRequest = new UpdateRequest(indexName , dbQuestion.getId());
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(dbQuestion);
		System.out.println(jsonString);
		updateRequest.doc(jsonString, XContentType.JSON);
		// updateRequest.upsert(jsonString, XContentType.JSON);
		UpdateResponse response = elasticSearchOperations.updateIndex(updateRequest);
		System.out.println(response);
		if (response != null) {
			if (response.getResult().name().equalsIgnoreCase("Updated")) {
				log.info("Successfully Updated Question Object");
			} else {
				throw new Exception("Exception while updation of question request");
			}	
		}	
		QuestionDTO dtoObject = questionMapper.toDto(dbQuestion);
		return dtoObject;
	}
	
	public List<QuestionDTO> getReviewedQuestions() throws Exception {
		Boolean isPublic = true;
		Boolean isApproved = true;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
				        .filter(QueryBuilders.matchQuery("isPublic", isPublic))
				        .filter(QueryBuilders.matchQuery("isApproved", isApproved));
		

		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			if (dbQuestion.getResponses() != null) {
				dbQuestion.getResponses().removeIf(object -> !object.getIsApproved());
			}
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
	
	public List<QuestionDTO> getReviewQuestions() throws Exception {
		Integer maxScore = 0;
		Boolean isApproved = false;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
				        .filter(QueryBuilders.matchQuery("isApproved", isApproved))
				        .filter(QueryBuilders.rangeQuery("score").gt(maxScore));
		

		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			if (dbQuestion.getResponses() != null) {
				dbQuestion.getResponses().removeIf(object -> !object.getIsApproved());
			}
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
	
	
	public QuestionDTO validateQuestion(String userId, ValidateQuestionDTO questionDTO) throws Exception {

		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("id", questionDTO.getId()));

		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		if (hits != null && hits.length == 0) {
			throw new Exception("There is no record found for question id " + questionDTO.getId());
		}
		SearchHit searchHit = hits[0];
		Map<String, Object> doc	 = searchHit.getSourceAsMap();
		Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
		
		if (questionDTO.getIsApproved()) {
			dbQuestion.setIsApproved(Boolean.TRUE);
		} else {
			dbQuestion.setIsApproved(Boolean.FALSE);
			Long score = dbQuestion.getScore();
			score = score - 1L;
			dbQuestion.setScore(score);
		}
		dbQuestion.setAgeGroup(questionDTO.getAgeGroup());
		dbQuestion.setGender(questionDTO.getGender());
		dbQuestion.setGroup(questionDTO.getGroup());
		dbQuestion.setTags(questionDTO.getTags());
		questionAuditService.updateQuestionAuditOnUpdate(dbQuestion);
		QuestionReviewer questionReviewer = new QuestionReviewer();
		questionReviewer.setComments(userId);
		questionReviewer.setId(UUID.randomUUID().toString());
		
		questionReviewAuditService.updateQuestionAuditOnCreate(questionReviewer);
		if (dbQuestion.getReviews() != null) {
			dbQuestion.getReviews().add(questionReviewer);
		} else {
			List<QuestionReviewer> reviews = new ArrayList<>();
			reviews.add(questionReviewer);
			dbQuestion.setReviews(reviews);
		}
		
		UpdateRequest updateRequest = new UpdateRequest(indexName , dbQuestion.getId());
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(dbQuestion);
		System.out.println(jsonString);
		updateRequest.doc(jsonString, XContentType.JSON);
		UpdateResponse response = elasticSearchOperations.updateIndex(updateRequest);
		if (response != null) {
			if (response.getResult().name().equalsIgnoreCase("Updated")) {
				log.info("Successfully Updated Question Object");
			} else {
				throw new Exception("Exception while updation of question request");
			}	
		}		
		QuestionDTO updatedQuestionDTO = questionMapper.toDto(dbQuestion);
		return updatedQuestionDTO;
	}
	
	/**
	 * 
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	public List<QuestionDTO> getAllQuestionsByUser(String userId) throws Exception {
		Integer maxScore = 0;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
				        .filter(QueryBuilders.rangeQuery("score").gt(maxScore));
		
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			if (dbQuestion.getResponses() != null) {
				dbQuestion.getResponses().removeIf(object -> !object.getIsApproved());
			}
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
	
	/**
	 * 
	 * @param questionId
	 * @param responseDTO
	 * @return
	 * @throws Exception
	 */
	public QuestionDTO createQuestionResponse(String questionId, QuestionResponsesDTO responseDTO) throws Exception {
		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("id", questionId));

		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		if (hits != null && hits.length == 0) {
			throw new Exception("There is no record found for question id " + questionId);
		}
		SearchHit searchHit = hits[0];
		Map<String, Object> doc	 = searchHit.getSourceAsMap();
		Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
		
		QuestionResponse questionResponse = questionResponseMapper.toEntity(responseDTO);
		questionResponse.setResponse(responseDTO.getResponse());
		questionResponse.setResponderId("system");
		questionResponse.setId(UUID.randomUUID().toString());		
		questionResponse.setScore(2L);
		questionResponse.setIsApproved(Boolean.FALSE);
		questionResponse.setIsPublic(Boolean.TRUE);
		questionResponseAuditService.updateQuestionAuditOnCreate(questionResponse);
		questionAuditService.updateQuestionAuditOnUpdate(dbQuestion);
		
		if (dbQuestion.getResponses() != null) {
			dbQuestion.getResponses().add(questionResponse);
		} else {
			List<QuestionResponse> list = new ArrayList<>();
			list.add(questionResponse);
			dbQuestion.setResponses(list);
		}
		
		UpdateRequest updateRequest = new UpdateRequest(indexName , dbQuestion.getId());
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(dbQuestion);
		System.out.println(jsonString);
		updateRequest.doc(jsonString, XContentType.JSON);
		UpdateResponse response = elasticSearchOperations.updateIndex(updateRequest);
		System.out.println(response);
		if (response != null) {
			if (response.getResult().name().equalsIgnoreCase("Updated")) {
				log.info("Successfully Updated Question Object");
			} else {
				throw new Exception("Exception while updation of question request");
			}	
		}
		QuestionDTO dtoObject = questionMapper.toDto(dbQuestion);
		return dtoObject;
	}
	
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<QuestionDTO> getReviewResponses() throws Exception {
		Long maxScore = 0L; 
		Boolean isApproved = false; 

		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("responses.isApproved", isApproved))
		        .filter(QueryBuilders.rangeQuery("responses.score").gt(maxScore));
		
		NestedQueryBuilder nestedQuery =  QueryBuilders.nestedQuery("responses", query, ScoreMode.Total);
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(nestedQuery);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			if (dbQuestion.getResponses() != null) {
				dbQuestion.getResponses().removeIf(object -> object.getIsApproved() && object.getScore() > 0);
			}
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
	
	/**
	 * 
	 * @param questionId
	 * @param responseDTO
	 * @return
	 * @throws Exception
	 */
	public QuestionDTO validateQuestionResponse(String questionId, QuestionResponsesDTO responseDTO) throws Exception {
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("id", questionId));

		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		if (hits != null && hits.length == 0) {
			throw new Exception("There is no record found for question id " + questionId);
		}
		SearchHit searchHit = hits[0];
		Map<String, Object> doc	 = searchHit.getSourceAsMap();
		Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);

		List<QuestionResponse> responses = dbQuestion.getResponses();

		Optional<QuestionResponse> matchingObject = responses.stream()
				.filter(object -> object.getId() != null && object.getId().equalsIgnoreCase(responseDTO.getId()))
				.findFirst();

		if (matchingObject.isEmpty()) {
			throw new Exception("Exception while fetching response of question  " + responseDTO.getId());
		}
		
		QuestionResponse responseObject = matchingObject.get();
		responseObject.setIsApproved(Boolean.TRUE);			
		List<QuestionResponseReviewer> reviewerList = responseObject.getReviews();
		QuestionResponseReviewer questionReviewer = new QuestionResponseReviewer();
		questionReviewer.setId(UUID.randomUUID().toString());			
		questionReviewer.setComments(responseDTO.getComments());
		questionReviewer.setReviewerId(questionReviewer.getReviewerId());
		reviewerList.add(questionReviewer);			
		questionResponseReviewAuditService.updateQuestionAuditOnCreate(questionReviewer);
		questionResponseAuditService.updateQuestionAuditOnUpdate(responseObject);
		questionAuditService.updateQuestionAuditOnUpdate(dbQuestion);
		
		UpdateRequest updateRequest = new UpdateRequest(indexName , dbQuestion.getId());
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(dbQuestion);
		System.out.println(jsonString);
		updateRequest.doc(jsonString, XContentType.JSON);
		UpdateResponse response = elasticSearchOperations.updateIndex(updateRequest);
		System.out.println(response);
		if (response != null) {
			if (response.getResult().name().equalsIgnoreCase("Updated")) {
				log.info("Successfully Updated Question Object");
			} else {
				throw new Exception("Exception while updation of question request");
			}	
		}
		QuestionDTO dtoObject = questionMapper.toDto(dbQuestion);
		return dtoObject;
	}
	
	/**
	 * 
	 * @param questionId
	 * @return
	 * @throws Exception
	 */
	public List<QuestionResponsesDTO> getQuestionResponse(String questionId) throws Exception {
		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("id", questionId));
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		if (hits != null && hits.length == 0) {
			throw new Exception("There is no record found for question id " + questionId);
		}
		SearchHit searchHit = hits[0];
		Map<String, Object> doc	 = searchHit.getSourceAsMap();
		Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
		QuestionDTO dtoObject = questionMapper.toDto(dbQuestion);
		return dtoObject.getResponses();
	}
	
	
	/**
	 * @param userId
	 * @return
	 */
	public List<QuestionDTO> getMyOutbox(String userId) throws Exception {
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("responses.createdBy", userId));
		
		NestedQueryBuilder nestedQuery =  QueryBuilders.nestedQuery("responses", query, ScoreMode.Total);

		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(nestedQuery);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			if (dbQuestion.getResponses() != null) {
				dbQuestion.getResponses().removeIf(object -> object.getResponderId() == null || 
						!object.getResponderId().equalsIgnoreCase(userId));
			}
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
	
	/**
	 * 
	 * @param keyword
	 * @return
	 * @throws Exception
	 */
	public List<QuestionDTO> search(String keyword) throws Exception {
		Integer maxScore = 0;
		Boolean isPublic = true;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
				        .filter(QueryBuilders.matchQuery("search_field", keyword))
				        .filter(QueryBuilders.matchQuery("isPublic", isPublic))
				        .filter(QueryBuilders.rangeQuery("score").gt(maxScore));
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
	
	public List<QuestionDTO> getReviewedQuestionsForCategory(String category) throws Exception {
		Boolean isApproved = true;
		Boolean isPublic = true;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("isApproved", isApproved))
		        .filter(QueryBuilders.matchQuery("isPublic", isPublic))
		        .filter(QueryBuilders.matchQuery("category", category));
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
	
	/**
	 * 
	 * @param category
	 * @param subCategory
	 * @return
	 * @throws Exception
	 */
	public List<QuestionDTO> getReviewedQuestionsByCategoryAndSubCategory(String category, String subCategory) throws Exception {
		Boolean isApproved = true;
		Boolean isPublic = true;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("isApproved", isApproved))
		        .filter(QueryBuilders.matchQuery("isPublic", isPublic))
		        .filter(QueryBuilders.matchQuery("category", category))
		        .filter(QueryBuilders.matchQuery("subCategory", subCategory));
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
	
	public List<QuestionDTO> getReviewedQuestionsByGroup(String group) throws Exception {
		Boolean isApproved = true;
		Boolean isPublic = true;
		
		BoolQueryBuilder query = QueryBuilders.boolQuery()
		        .filter(QueryBuilders.matchQuery("isApproved", isApproved))
		        .filter(QueryBuilders.matchQuery("isPublic", isPublic))
		        .filter(QueryBuilders.matchQuery("group", group));
		
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(query);
		sourceBuilder.from(0);
		sourceBuilder.size(10000);
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticSearchOperations.getSearchResults(searchRequest);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		List<Question> dbList = new ArrayList<Question>();
		for (SearchHit searchHit : hits) {
			Map<String, Object> doc	 = searchHit.getSourceAsMap();
			Question dbQuestion = new ObjectMapper().convertValue(doc, Question.class);
			dbList.add(dbQuestion);
		}
		List<QuestionDTO> dtoList = questionMapper.toDto(dbList);
		return dtoList;
	}
}
