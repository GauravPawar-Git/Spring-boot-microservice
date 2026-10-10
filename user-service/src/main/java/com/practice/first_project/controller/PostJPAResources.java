package com.practice.first_project.controller;

import com.practice.first_project.dto.PostRequest;
import com.practice.first_project.dto.PostResponse;
import com.practice.first_project.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Posts endpoints. Thin, like UserJPAResources: HTTP in, PostService call, HTTP out.
 *
 *   requests arrive from: api-gateway route "user-service" (Path=/jpa/**), and from comment-service:
 *                         UserServiceClient.getPost() (Feign) calls GET /jpa/posts/{id} to check a post exists
 *   security            : GET feed/post/user-posts are public (SecurityConfig); creating needs a token and
 *                         @PreAuthorize "ADMIN or yourself" (principal = AppUserPrincipal from JwtAuthenticationFilter)
 *   calls               : PostService
 */
@RestController
@RequestMapping("/jpa")
public class PostJPAResources {

    private final PostService postService;

    public PostJPAResources(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/users/{userId}/posts")
    public List<PostResponse> getPostsOfUser(@PathVariable int userId) {
        return postService.getPostsOfUser(userId);
    }

    @PostMapping("/users/{userId}/posts")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id") // you can only post as yourself
    public ResponseEntity<PostResponse> createPost(@PathVariable int userId, @Valid @RequestBody PostRequest request) {
        PostResponse created = postService.createPost(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/jpa/posts/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // Transaction demo: all posts are saved, or none are
    @PostMapping("/users/{userId}/posts/bulk")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PostResponse> createPosts(@PathVariable int userId, @RequestBody List<@Valid PostRequest> requests) {
        return postService.createPosts(userId, requests);
    }

    // Also called service-to-service by comment-service (UserServiceClient.getPost via PostLookup):
    // its JSON is read there as PostSummary. Cached in PostService.getPost().
    @GetMapping("/posts/{id}")
    public PostResponse getPost(@PathVariable int id) {
        return postService.getPost(id);
    }

    // Feed of all posts with author names: one SQL query (JOIN FETCH)
    @GetMapping("/posts")
    public List<PostResponse> getFeed() {
        return postService.getFeed();
    }

    // Same response, built the naive way: watch the console run 1 + N queries
    @GetMapping("/posts/n-plus-one")
    public List<PostResponse> getFeedNPlusOne() {
        return postService.getFeedNPlusOne();
    }
}
