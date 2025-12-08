package com.back.config;

import static org.mockito.Mockito.*;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

	@Bean
	@Primary
	public VectorStore testVectorStore() {
		return mock(VectorStore.class);
	}
	
}