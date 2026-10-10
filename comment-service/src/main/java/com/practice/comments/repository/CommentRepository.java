package com.practice.comments.repository;

import com.practice.comments.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Database access for Comment; Spring Data JPA generates the implementation. Used only by CommentService.
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    // GET /comments?postId=  ->  CommentService.getCommentsOfPost()
    List<Comment> findByPostIdOrderByCreatedAtAsc(Integer postId);

    // derived delete query: loads the matching comments, deletes them, returns how many.
    // Kafka path: PostsDeletedListener.onPostsDeleted() -> CommentService.deleteCommentsOfPosts() -> here
    long deleteByPostIdIn(List<Integer> postIds);
}
