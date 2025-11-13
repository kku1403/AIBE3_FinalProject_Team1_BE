package com.back.domain.chat.dto;

public record CreateChatRoomResBody(
        String message,
        Long chatRoomId
) {
}
