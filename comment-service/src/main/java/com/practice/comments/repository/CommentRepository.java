package com.practice.comments.repository;

import com.practice.comments.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(Integer postId);

    // derived delete query: loads the matching comments, deletes them, returns how many
    long deleteByPostIdIn(List<Integer> postIds);
}
