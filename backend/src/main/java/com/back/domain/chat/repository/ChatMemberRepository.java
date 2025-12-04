package com.back.domain.chat.repository;

import com.back.domain.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    Optional<ChatMember> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);
}
