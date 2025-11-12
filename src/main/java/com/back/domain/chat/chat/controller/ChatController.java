package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.dto.CreateChatRoomReqBody;
import com.back.domain.chat.chat.dto.CreateChatRoomResBody;
import com.back.domain.chat.chat.service.ChatService;
import com.back.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<CreateChatRoomResBody> createChatRoom(
            @RequestBody CreateChatRoomReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        CreateChatRoomResBody body = chatService.createChatRoom(
                reqBody.postId(),
                securityUser.getId()
        );

        return ResponseEntity.ok(body);
    }

    // TODO : 페이지네이션 & 검색 기능 추가
    @GetMapping
    public ResponseEntity<List<ChatRoomDto>> getMyChatRooms(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<ChatRoomDto> myChatRooms = chatService.getMyChatRooms(securityUser.getId());
        return ResponseEntity.ok(myChatRooms);
    }
}
