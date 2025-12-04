package com.back.domain.chat.controller;

import com.back.config.TestConfig;
import com.back.domain.chat.dto.CreateChatRoomReqBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
@Sql("/sql/chat.sql")
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    // SQL 내부 정보 (user1 입장)
    private static final String MY_EMAIL = "user1@test.com";
    
    private static final int TOTAL_CHATROOM_COUNT = 3;

    private static final Long CHATROOM_ID_1 = 1L; // user1 - user2 채팅방
    private static final Long CHATROOM_ID_2 = 2L; // user1 - user3 채팅방
    private static final Long CHATROOM_ID_NOT_FOUND = 99999L;

    private static final Long POST_ID_EXISTING = 1L; // 이미 user1-user2 채팅방이 존재하는 게시글
    private static final Long POST_ID_NEW = 4L; // 아직 user1과 채팅방이 없는 게시글
    private static final Long POST_ID_MY = 5L;  // user1 본인이 작성한 게시글 (self-chat 예외용)

    private static final String POST_TITLE = "캠핑 텐트 대여"; // CHATROOM_ID_1의 게시글 제목

    private static final Long OTHER_MEMBER_ID = 2L; // CHATROOM_ID_1의 채팅 상대
    private static final String OTHER_MEMBER_EMAIL = "user2@test.com";
    private static final String OTHER_MEMBER_NICKNAME = "kim";

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 생성 성공")
    void createChatRoom_success() throws Exception {
        // given
        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(POST_ID_NEW);

        // when
        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("채팅방이 생성되었습니다."))
                .andExpect(jsonPath("$.data.chatRoomId").exists());
    }

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("이미 존재하는 채팅방일 때")
    void createChatRoom_alreadyExists() throws Exception {
        // given
        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(POST_ID_EXISTING);

        // when
        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 채팅방입니다."))
                .andExpect(jsonPath("$.data.chatRoomId").exists());
    }

    @Test
    @DisplayName("로그인 안 한 상태에서 채팅방 생성 시도")
    void createChatRoom_unauthorized() throws Exception {
        // given
        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(POST_ID_NEW);

        // when
        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("본인과 채팅방 생성 시도 - 예외 발생")
    void createChatRoom_withSelf_shouldThrow() throws Exception {
        // given
        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(POST_ID_MY);

        // when
        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("본인과 채팅방을 만들 수 없습니다."));
    }

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 검색어 없음")
    void getMyChatRooms_withoutKeyword() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(TOTAL_CHATROOM_COUNT));
    }

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 게시글 제목으로 검색")
    void getMyChatRooms_searchByPostTitle_tent() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "텐트"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].post.title").value(POST_TITLE))
                .andExpect(jsonPath("$.data.content[0].otherMember.nickname").value(OTHER_MEMBER_NICKNAME))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 상대방 닉네임으로 검색")
    void getMyChatRooms_searchByMemberNickname() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "kim"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].post.title").value(POST_TITLE))
                .andExpect(jsonPath("$.data.content[0].otherMember.nickname").value(OTHER_MEMBER_NICKNAME))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 페이징 테스트")
    void getMyChatRooms_pagination() throws Exception {
        // 첫 페이지 (size=2)
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "2"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(TOTAL_CHATROOM_COUNT))
                .andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.first").value(true))
                .andExpect(jsonPath("$.data.page.last").value(false))
                .andExpect(jsonPath("$.data.page.hasNext").value(true));

        // 두 번째 페이지
        ResultActions resultActions2 = mvc.perform(get("/api/v1/chats")
                        .param("page", "1")
                        .param("size", "2"))
                .andDo(print());

        // then
        resultActions2
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page.page").value(1))
                .andExpect(jsonPath("$.data.page.size").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(TOTAL_CHATROOM_COUNT))
                .andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.first").value(false))
                .andExpect(jsonPath("$.data.page.last").value(true))
                .andExpect(jsonPath("$.data.page.hasNext").value(false));
    }

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 상세 정보 조회 성공")
    void getChatRoom_success() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats/{chatRoomId}", CHATROOM_ID_1))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("채팅방 정보"))
                .andExpect(jsonPath("$.data.id").value(CHATROOM_ID_1))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.post.title").value("캠핑 텐트 대여"))
                .andExpect(jsonPath("$.data.otherMember.id").value(OTHER_MEMBER_ID))
                .andExpect(jsonPath("$.data.otherMember.nickname").value(OTHER_MEMBER_NICKNAME));
    }

    @Test
    @WithUserDetails(value = MY_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("존재하지 않는 채팅방 조회 시도")
    void getChatRoom_notFound() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats/{chatRoomId}", CHATROOM_ID_NOT_FOUND))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 채팅방입니다."));
    }

    @Test
    @WithUserDetails(value = OTHER_MEMBER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("권한 없는 채팅방 조회 시도")
    void getChatRoom_forbidden() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats/{chatRoomId}", CHATROOM_ID_2))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.msg").value("해당 채팅방에 접근할 수 없습니다."));
    }

}