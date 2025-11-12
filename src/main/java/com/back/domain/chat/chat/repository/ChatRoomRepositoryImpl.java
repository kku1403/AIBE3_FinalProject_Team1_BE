package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.dto.OtherMemberDto;
import com.back.domain.chat.chat.dto.PostDto;
import com.back.domain.chat.chat.entity.QChatMember;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.back.domain.chat.chat.entity.QChatMember.chatMember;
import static com.back.domain.chat.chat.entity.QChatRoom.chatRoom;
import static com.back.domain.member.member.entity.QMember.member;
import static com.back.domain.post.post.entity.QPost.post;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Long> findIdByPostAndMembers(Long postId, Long hostId, Long guestId) {
        Long chatRoomId = queryFactory
                .select(chatRoom.id)
                .from(chatRoom)
                .join(chatRoom.chatMembers, chatMember)
                .where(
                        chatRoom.post.id.eq(postId),
                        chatMember.member.id.in(hostId, guestId)
                )
                .groupBy(chatRoom.id)
                .having(chatMember.count().eq(2L))
                .fetchOne();

        return Optional.ofNullable(chatRoomId);
    }

    @Override
    public List<ChatRoomDto> findByMemberId(Long memberId) {
        QChatMember me = new QChatMember("me");
        QChatMember other = new QChatMember("otherMember");

        return queryFactory
                .select(Projections.constructor(ChatRoomDto.class,
                        chatRoom.id,
                        chatRoom.createdAt,
                        Projections.constructor(PostDto.class,
                                post.title
                        ),
                        Projections.constructor(OtherMemberDto.class,
                                member.id,
                                member.nickname,
                                member.profileImgUrl
                        )
                ))
                .from(chatRoom)
                .join(chatRoom.post, post)
                .join(chatRoom.chatMembers, me)
                .join(chatRoom.chatMembers, other)
                .join(other.member, member)
                .where(
                        me.member.id.eq(memberId),
                        other.member.id.ne(memberId)
                )
                .distinct()
                .fetch();
    }
}