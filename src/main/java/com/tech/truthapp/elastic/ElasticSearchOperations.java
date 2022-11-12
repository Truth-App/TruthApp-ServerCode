package com.tech.truthapp.elastic;


import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ElasticSearchOperations {

	
    @Autowired
	private ElasticSearchConfiguration elasticSearchConfig;
    
    public IndexResponse createIndex(IndexRequest indexRequest) throws Exception {
    	IndexResponse response = null;
    	try {
    		response = elasticSearchConfig
					.getRestClient()
					.index(indexRequest, RequestOptions.DEFAULT);
    	} catch (Exception e) {
    		log.error(e.getMessage());
    	}
    	return response;
    }
    
    public SearchResponse getSearchResults(SearchRequest searchRequest) {
    	SearchResponse response = null;
    	try {
    		response = elasticSearchConfig
    				   .getRestClient()
    				  .search(searchRequest, RequestOptions.DEFAULT);
    	} catch (Exception e) {
    		
    	}
    	return response;
    }
    
    public UpdateResponse updateIndex(UpdateRequest updateRequest) throws Exception {
    	UpdateResponse response = null;
    	try {
    		response = elasticSearchConfig
					.getRestClient()
					.update(updateRequest, RequestOptions.DEFAULT);
    	} catch (Exception e) {
    		log.error(e.getMessage());
    	}
    	return response;
    }
}
