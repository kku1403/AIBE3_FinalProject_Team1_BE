package com.back.domain.chat.entity;


import com.back.domain.member.entity.Member;
import com.back.domain.post.entity.Post;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMember> chatMembers = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    public static ChatRoom create(Post post, Member... members) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.post = post;
        for (Member member : members) {
            chatRoom.addMember(member);
        }
        return chatRoom;
    }

    private void addMember(Member member) {
        ChatMember chatMember = new ChatMember(this, member);
        this.chatMembers.add(chatMember);
    }
}
