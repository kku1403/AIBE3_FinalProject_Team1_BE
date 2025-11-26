package com.back.domain.post.service;

import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostFavoriteRepository;
import com.back.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final PostVectorService postVectorService;
    private final PostRepository postRepository;
    private final PostFavoriteRepository postfavoriteRepository;
    private final ChatClient chatClient;

    @Value("${custom.ai.rag-llm-answer-prompt}")
    private String ragPrompt;

    public List<PostListResBody> searchPosts(String query, Long memberId) {

        List<Long> postIds = postVectorService.searchPostIds(query, 10);

        if (postIds.isEmpty()) return List.of();

        List<Post> posts = postIds.stream()
                .map(id -> postRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        return posts.stream()
                .map(post -> {

                    boolean isFavorite = (memberId != null)
                            && postfavoriteRepository.existsByMemberIdAndPostId(memberId, post.getId());

                    String thumbnail = post.getImages().isEmpty()
                            ? null
                            : post.getImages().get(0).getImageUrl();

                    return PostListResBody.of(post, isFavorite, thumbnail);
                })
                .toList();
    }

    public String searchWithLLM(String query) {

        List<Document> docs = postVectorService.searchDocuments(query, 5);

        String context = docs.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                %s
                
                ---------------------
                [사용자 질문]
                %s
                
                [관련 게시글 정보]
                %s
                """.formatted(ragPrompt, query, context);

        return chatClient.prompt(prompt)
                .options(ChatOptions.builder()
                        .temperature(1.0)
                        .build())
                .call()
                .content();
    }
}
