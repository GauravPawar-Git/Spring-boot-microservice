package com.practice.comments.service;

import com.practice.comments.client.PostLookup;
import com.practice.comments.client.PostSummary;
import com.practice.comments.dto.CommentRequest;
import com.practice.comments.dto.PostComments;
import com.practice.comments.entity.Comment;
import com.practice.comments.handlers.NotFoundException;
import com.practice.comments.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostLookup postLookup;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addComment_404_whenPostDoesNotExist_andNothingSaved() {
        when(postLookup.findPost(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(new CommentRequest(999, "hi"), 3, "vigo"))
                .isInstanceOf(NotFoundException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void getComments_stillReturnsOurData_whenPostDetailsUnavailable() {
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(1)).thenReturn(List.of(new Comment(1, 3, "vigo", "Great post!")));
        when(postLookup.findPostOrEmpty(1)).thenReturn(Optional.empty());

        PostComments result = commentService.getCommentsOfPost(1);

        assertThat(result.postDetailsAvailable()).isFalse();
        assertThat(result.comments()).hasSize(1);
    }

    @Test
    void getComments_includesPostDetails_whenAvailable() {
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(1)).thenReturn(List.of());
        when(postLookup.findPostOrEmpty(1)).thenReturn(Optional.of(new PostSummary(1, "Learning Spring Boot", "Rambo")));

        PostComments result = commentService.getCommentsOfPost(1);

        assertThat(result.postDescription()).isEqualTo("Learning Spring Boot");
        assertThat(result.postAuthor()).isEqualTo("Rambo");
    }

    @Test
    void deleteComment_onlyAuthorOrAdmin() {
        Comment byDiga = new Comment(1, 4, "diga", "Agreed");
        when(commentRepository.findById(2)).thenReturn(Optional.of(byDiga));

        assertThatThrownBy(() -> commentService.deleteComment(2, 3, false)).isInstanceOf(AccessDeniedException.class);

        commentService.deleteComment(2, 1, true); // admin
        verify(commentRepository).delete(byDiga);
    }
}
