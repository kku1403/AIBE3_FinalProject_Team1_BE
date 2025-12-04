package com.back.domain.post.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.BaseTestContainer;
import com.back.config.TestConfig;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Transactional
@Sql(scripts = {
	"/sql/members.sql",
	"/sql/categories.sql",
	"/sql/regions.sql",
	"/sql/posts.sql",
	"/sql/post_images.sql",
	"/sql/post_regions.sql",
})
@Sql(
	scripts = "/sql/post_options.sql",
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class PostControllerTest extends BaseTestContainer {

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("게시글 단건 조회 테스트")
	@WithUserDetails("user1@example.com")
	void getPostById_success() throws Exception {

		mockMvc.perform(get("/api/v1/posts/{id}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").exists())
			.andExpect(jsonPath("$.data.id").value(1));
	}
}
