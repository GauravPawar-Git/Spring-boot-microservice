package com.practice.comments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * postId and authorId are plain numbers, not @ManyToOne relations: posts and users live in
 * user-service's database. authorUsername is copied in at write time so reads don't need another call.
 *
 *   table     : "comment" in comment-service's own database (Flyway V1__create_comment.sql)
 *   created in: CommentService.addComment(), after PostLookup.findPost() confirmed the post exists
 *   deleted by: CommentService.deleteComment() (DELETE /comments/{id}) and, when the post is deleted in
 *               user-service, by PostsDeletedListener (Kafka) -> CommentService.deleteCommentsOfPosts()
 */
@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer postId;

    @Column(nullable = false)
    private Integer authorId;

    @Column(nullable = false, length = 30)
    private String authorUsername;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Comment() {
    }

    public Comment(Integer postId, Integer authorId, String authorUsername, String text) {
        this.postId = postId;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getPostId() {
        return postId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
