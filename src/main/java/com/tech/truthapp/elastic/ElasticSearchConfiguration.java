package com.tech.truthapp.elastic;

import java.util.Arrays;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties("es")
@Getter
@Setter
public class ElasticSearchConfiguration {

	private String hostName;
    private int port;
    private String username;
    private String password;
    
	
    @Autowired
    Environment env;
   
	@Bean
    public RestHighLevelClient getRestClient() {
		System.out.println(hostName + " >>" + port);
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
               new UsernamePasswordCredentials(username, password));
        
        RestClientBuilder builder = RestClient
                .builder(getHost())
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider));

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
	
	/**
	 * 
	 * @return
	 */
	private HttpHost getHost() {
		HttpHost httpHost = null;
		String profile[] = env.getActiveProfiles();		
		if (Arrays.asList(profile).contains("local")) {
			 httpHost = new HttpHost(hostName, port , "http");
		} else {
			httpHost = new HttpHost(hostName, port , "https");
		}
		return httpHost;
		
	}

 }
