package com.practice.first_project.repositories;

import com.practice.first_project.bean.Post;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Integer> {

    // Derived query: Spring Data reads the method name -> "where p.user.id = ? order by p.createdAt desc"
    List<Post> findByUserIdOrderByCreatedAtDesc(Integer userId);

    long countByUserId(Integer userId);

    // N+1 fix #1: JOIN FETCH loads every post AND its user in a single SQL statement
    @Query("select p from Post p join fetch p.user order by p.createdAt desc")
    List<Post> findAllWithUser();

    // N+1 fix #2: same result, declared with an entity graph instead of JPQL
    @EntityGraph(attributePaths = "user")
    List<Post> findAllBy(Sort sort);
}
