package com.tech.truthapp.question.controller;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tech.truthapp.dto.question.QuestionDTO;
import com.tech.truthapp.dto.question.QuestionResponsesDTO;
import com.tech.truthapp.dto.question.ValidateQuestionDTO;
import com.tech.truthapp.exception.HeaderUtil;
import com.tech.truthapp.question.service.QuestionService;
import com.tech.truthapp.question.validation.QuestionValidator;

import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api")
@Log4j2
@Api(tags = "Question Controller", value = "Question Controller", description = "Question Controller")
public class QuestionController {

	/** The Constant ENTITY_NAME. */
	private static final String ENTITY_NAME = "Question";

	@Value("${truth-app.clientApp.name}")
	private String applicationName;

	@Autowired
	private QuestionValidator questionValidator;

	@Autowired
	private QuestionService questionService;

	/**
	 * 
	 * @param questionDTO
	 * @return questionDTO
	 * @throws Exception
	 */
	@PostMapping("/questions")
	public ResponseEntity<QuestionDTO> saveQuestion(@Valid @RequestBody QuestionDTO questionDTO) throws Exception {

		log.info("REST request to Save Question {} ", questionDTO);
		questionValidator.validateCreateQuestion(questionDTO);
		questionDTO = questionService.saveQuestion(questionDTO);
		return ResponseEntity
				.created(new URI("/api/questions/" + questionDTO.getId())).headers(HeaderUtil
						.createEntityCreationAlert(applicationName, true, ENTITY_NAME, questionDTO.getId().toString()))
				.body(questionDTO);
	}

	@PutMapping("/questions/{questionId}")
	public ResponseEntity<QuestionDTO> updateQuestion(@PathVariable("questionId") String questionId,
			@Valid @RequestBody QuestionDTO questionDTO) throws Exception {
		log.info("REST request to get update Question {},{}", questionId, questionDTO);
		questionDTO.setId(questionId);
		QuestionDTO updateObject = questionService.updateQuestion(questionDTO);
		return ResponseEntity.created(new URI("/api/questions/reviewedquestions"))
				.headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, questionId))
				.body(updateObject);
	}
	
	/**
	 * 
	 * @param questionId
	 * @return
	 * @throws Exception
	 */
	@DeleteMapping("/questions/{questionId}")
	public ResponseEntity<QuestionDTO> deleteQuestion(
			@PathVariable("questionId") String questionId) throws Exception {
		log.info("REST request to get Delete Question {}", questionId);
		QuestionDTO questionDTO = questionService.deleteQuestion(questionId);
		return ResponseEntity.ok().body(questionDTO);
	}
	
	
	@GetMapping("/questions")
	public ResponseEntity<List<QuestionDTO>> getReviewedQuestions() throws Exception {

		log.info("REST request to get Reviewed Questions");
		List<QuestionDTO> userQuestionList = questionService.getReviewedQuestions();
		return ResponseEntity.ok().body(userQuestionList);
	}
	
	/**
	 * 
	 * @param questionDTO
	 * @return questionDTO
	 * @throws Exception
	 */
	@GetMapping("/questions/reviewquestions")
	public ResponseEntity<List<QuestionDTO>> getReviewQuestions() throws Exception {
		log.info("REST request to get Review Questions");
		List<QuestionDTO> userQuestionList = questionService.getReviewQuestions();
		return ResponseEntity.ok().body(userQuestionList);
	}
	/**
	 * 
	 * @param userId
	 * @param questionDTO
	 * @return
	 * @throws Exception
	 */
	@PutMapping("/questions/{userId}/validatequestion")
	public ResponseEntity<QuestionDTO> validateQuestion(@PathVariable("userId") String userId,
			@Valid @RequestBody ValidateQuestionDTO questionDTO) throws Exception {
		log.info("REST request to get Validate Question {},{}", userId, questionDTO);
		questionValidator.validationOnValidateQuestion(questionDTO);
		QuestionDTO updateObject = questionService.validateQuestion(userId, questionDTO);
		return ResponseEntity.created(new URI("/api/questions/validateQuestion"))
				.headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, userId))
				.body(updateObject);
	}
	
	/**
	 * 
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/questions/{userId}/myquestions")
	public ResponseEntity<List<QuestionDTO>> getQuestionsByUserId(@PathVariable("userId") String userId)
			throws Exception {

		log.info("REST request to get Questions By User Id {}", userId);
		List<QuestionDTO> userQuestionList = questionService.getAllQuestionsByUser(userId);
		return ResponseEntity.ok().body(userQuestionList);
	}
	
	/**
	 * 
	 * @param questionDTO
	 * @return questionDTO
	 * @throws Exception
	 */
	@PostMapping("/questions/{questionId}/responses")
	public ResponseEntity<QuestionDTO> createResponse(@PathVariable("questionId") String questionId,
			@Valid @RequestBody QuestionResponsesDTO responseDTO) throws Exception {
		log.info("REST request to get create Response for Question {},{}", questionId, responseDTO);
		QuestionDTO updateObject = questionService.createQuestionResponse(questionId, responseDTO);
		return ResponseEntity.created(new URI("/api/questions/reviewedquestions"))
				.headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, questionId))
				.body(updateObject);
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/questions/reviewresponses")
	public ResponseEntity<List<QuestionDTO>> getReviewResponses() throws Exception {
		log.info("REST request to get Review Responses");
		List<QuestionDTO> userQuestionList = questionService.getReviewResponses();
		return ResponseEntity.ok().body(userQuestionList);
	}
	
	/**
	 * 
	 * @param questionId
	 * @param userId
	 * @param responseDTO
	 * @return
	 * @throws Exception
	 */
	@PutMapping("/questions/{question-id}/responses/{userId}/validateresponses")
	public ResponseEntity<QuestionDTO> validateResponse(@PathVariable("question-id") String questionId,
			@PathVariable("userId") String userId,
			@Valid @RequestBody QuestionResponsesDTO responseDTO) throws Exception {
		responseDTO.setReviewerId(userId);
		log.info("REST request to get Validate Response for Question {},{}", questionId, responseDTO);
		QuestionDTO updateObject = questionService.validateQuestionResponse(questionId, responseDTO);
		return ResponseEntity.created(new URI("/api/questions/reviewedquestions"))
				.headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, questionId))
				.body(updateObject);
	}
	
	/**
	 * 
	 * @param questionId
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/questions/{questionId}/responses")
	public ResponseEntity<List<QuestionResponsesDTO>> getQuestionResponse(
			@PathVariable("questionId") String questionId) throws Exception {
		log.info("REST request to get Get Response for Question {}", questionId);
		List<QuestionResponsesDTO> responses = questionService.getQuestionResponse(questionId);
		return ResponseEntity.ok().body(responses);
	}
	
	/**
	 * 
	 * @return questionDTO
	 * @throws Exception
	 */
	@GetMapping("/questions/{userId}/myreviewedresponses")
	public ResponseEntity<List<QuestionDTO>> getMyReviewedResponses(@PathVariable("userId") String userId) throws Exception {
		log.info("REST request to get Review Responses {} ", userId);
		List<QuestionDTO> userQuestionList = questionService.getMyOutbox(userId);
		return ResponseEntity.ok().body(userQuestionList);
	}
	
	/**
	 * 
	 * @param keyword
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/questions/search")
	public ResponseEntity<List<QuestionDTO>> search(@RequestParam(name = "keyword", required = false) String keyword) throws Exception {
		log.info("REST request to get Search questions {} ", keyword);
		List<QuestionDTO> userQuestionList = questionService.search(keyword);
		return ResponseEntity.ok().body(userQuestionList);
	}
	
	/**
	 * 
	 * @param category
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/questions/categoryreviewedquestions")
	public ResponseEntity<List<QuestionDTO>> getReviewedQuestionsForCategory
				(@RequestParam(name = "category") String category) throws Exception {

		log.info("REST request to get Reviewed Questions for category {} ", category);
		List<QuestionDTO> userQuestionList = questionService.getReviewedQuestionsForCategory(category);
		return ResponseEntity.ok().body(userQuestionList);
	}
	
	/**
	 * 
	 * @param category
	 * @param subCategory
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/questions/categorysubcategoryreviewedquestions")
	public ResponseEntity<List<QuestionDTO>> getReviewedQuestionsByCategoryAndSubCategory
				(@RequestParam(name = "category") String category, 
				@RequestParam(name = "subcategory") String subCategory) throws Exception {

		log.info("REST request to get Reviewed Questions for category {} ", category);
		List<QuestionDTO> userQuestionList = questionService.getReviewedQuestionsByCategoryAndSubCategory(category, subCategory);
		return ResponseEntity.ok().body(userQuestionList);
	}
	
	/**
	 * 
	 * @param group
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/questions/groupeviewedquestions")
	public ResponseEntity<List<QuestionDTO>> getReviewedQuestionsByGroup
				(@RequestParam(name = "group") String group) throws Exception {

		log.info("REST request to get Reviewed Questions for group {} ", group);
		List<QuestionDTO> userQuestionList = questionService.getReviewedQuestionsByGroup(group);
		return ResponseEntity.ok().body(userQuestionList);
	}
}
