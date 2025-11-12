package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.dto.ChatRoomDto;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepositoryCustom {
    Optional<Long> findIdByPostAndMembers(Long postId, Long hostId, Long guestId);
    List<ChatRoomDto> findByMemberId(Long memberId);
}
