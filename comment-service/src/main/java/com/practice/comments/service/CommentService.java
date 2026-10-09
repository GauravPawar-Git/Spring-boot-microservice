package com.practice.comments.service;

import com.practice.comments.client.PostLookup;
import com.practice.comments.client.PostSummary;
import com.practice.comments.dto.CommentRequest;
import com.practice.comments.dto.CommentResponse;
import com.practice.comments.dto.PostComments;
import com.practice.comments.entity.Comment;
import com.practice.comments.handlers.BannedWordException;
import com.practice.comments.handlers.NotFoundException;
import com.practice.comments.repository.CommentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostLookup postLookup;
    private final CommentRules rules;
    // Business metrics: show up at /actuator/prometheus as comments_saved_total, comments_rejected_total{reason=...}
    private final Counter created;
    private final Counter rejectedBannedWord;

    public CommentService(CommentRepository commentRepository, PostLookup postLookup,
                          CommentRules rules, MeterRegistry meterRegistry) {
        this.commentRepository = commentRepository;
        this.postLookup = postLookup;
        this.rules = rules;
        // not "comments.created": Prometheus treats a "_created" suffix on counters as reserved and strips it
        this.created = Counter.builder("comments.saved")
                .description("Comments successfully saved")
                .register(meterRegistry);
        this.rejectedBannedWord = Counter.builder("comments.rejected")
                .tag("reason", "banned_word")
                .description("Comments rejected by content rules")
                .register(meterRegistry);
    }

    // Our data always comes back; post details only if user-service answers (graceful degradation)
    public PostComments getCommentsOfPost(int postId) {
        List<CommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
        Optional<PostSummary> post = postLookup.findPostOrEmpty(postId);
        return new PostComments(postId, post.isPresent(),
                post.map(PostSummary::description).orElse(null),
                post.map(PostSummary::authorName).orElse(null),
                comments);
    }

    /**
     * The remote call happens BEFORE the transaction: never hold a DB transaction (and connection)
     * open while waiting on another service over the network.
     */
    public CommentResponse addComment(CommentRequest request, int authorId, String authorUsername) {
        // cheap local check first, before any network call
        String banned = rules.findBannedWord(request.text());
        if (banned != null) {
            rejectedBannedWord.increment();
            throw new BannedWordException(banned);
        }
        postLookup.findPost(request.postId())   // throws UserServiceUnavailableException -> 503
                .orElseThrow(() -> new NotFoundException("Post not found with id: " + request.postId()));
        Comment saved = commentRepository.save(new Comment(request.postId(), authorId, authorUsername, request.text()));
        created.increment();
        return CommentResponse.from(saved);
    }

    @Transactional
    public void deleteComment(int commentId, int userId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found with id: " + commentId));
        if (!isAdmin && !comment.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("Not the author");
        }
        commentRepository.delete(comment);
    }

    // Called by the Kafka listener when user-service deleted posts: our copies would be orphans
    @Transactional
    public long deleteCommentsOfPosts(List<Integer> postIds) {
        return commentRepository.deleteByPostIdIn(postIds);
    }
}
