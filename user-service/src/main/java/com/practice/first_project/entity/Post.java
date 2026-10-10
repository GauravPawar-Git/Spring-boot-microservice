package com.practice.first_project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * The "many" side of User 1 --- * Post. It owns the relationship: the foreign key column user_id is in this table.
 *
 *   table     : "post", created by Flyway V2__create_post.sql (Hibernate only validates it)
 *   created in: PostService.createPost() / createPosts(), attached with User.addPost()
 *   sent out  : as PostResponse.from(post), never directly
 * comment-service refers to a post only by its id (Comment.postId); it never sees this class.
 */
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // @ManyToOne is EAGER by default (JPA spec). LAZY = the user is loaded only when getUser() is used.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    protected Post() {
    }

    public Post(String description) {
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    // package-private: only User.addPost should set it, so both sides stay in sync
    void setUser(User user) {
        this.user = user;
    }
}
