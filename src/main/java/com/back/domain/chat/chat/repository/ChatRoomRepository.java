package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // TODO : 추후 QueryDSL로 수정

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        JOIN cr.chatMembers cm
        WHERE cr.post.id = :postId
          AND (cm.member.id = :hostId OR cm.member.id = :guestId)
        GROUP BY cr.id
        HAVING COUNT(cm) = 2
    """)
    Optional<ChatRoom> findByPostAndMembers(Long postId, Long hostId, Long guestId);

    @Query("""
        SELECT new com.back.domain.chat.chat.dto.ChatRoomDto(
            cr.id,
            cr.createdAt,
            new com.back.domain.chat.chat.dto.PostDto(p.title),
            new com.back.domain.chat.chat.dto.OtherMemberDto(
                m.id,
                m.nickname,
                m.profileImgUrl
            )
        )
        FROM ChatRoom cr
        JOIN cr.post p
        JOIN cr.chatMembers cm
        JOIN cm.member m
        WHERE m.id <> :memberId
          AND cr.id IN (
            SELECT cr2.id
            FROM ChatRoom cr2
            JOIN cr2.chatMembers cm2
            WHERE cm2.member.id = :memberId
          )
    """)
    List<ChatRoomDto> findByMemberId(Long memberId);
}
