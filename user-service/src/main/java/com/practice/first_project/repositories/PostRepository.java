package com.practice.first_project.repositories;

import com.practice.first_project.entity.Post;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Database access for Post. Only an interface: Spring Data JPA generates the implementation at startup.
 * JpaRepository already gives save(), findById(), findAll(), delete()...; the methods below are extra.
 *
 *   used by: PostService (every method). Table "post" is created by Flyway V2__create_post.sql.
 */
public interface PostRepository extends JpaRepository<Post, Integer> {

    // Derived query: Spring Data reads the method name -> "where p.user.id = ? order by p.createdAt desc"
    List<Post> findByUserIdOrderByCreatedAtDesc(Integer userId);

    // "select count(*) ... where user_id = ?": the 10-posts limit in PostService.createPost() / createPosts()
    long countByUserId(Integer userId);

    // N+1 fix #1: JOIN FETCH loads every post AND its user in a single SQL statement
    @Query("select p from Post p join fetch p.user order by p.createdAt desc")
    List<Post> findAllWithUser();

    // N+1 fix #2: same result, declared with an entity graph instead of JPQL
    // (not used by PostService; PostRepositoryNPlusOneTest compares it with fix #1)
    @EntityGraph(attributePaths = "user")
    List<Post> findAllBy(Sort sort);
}
