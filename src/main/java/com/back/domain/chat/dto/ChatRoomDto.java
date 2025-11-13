package com.back.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomDto(
        Long id,
        LocalDateTime createdAt,
        ChatPostDto post,
        OtherMemberDto otherMember
) { }

