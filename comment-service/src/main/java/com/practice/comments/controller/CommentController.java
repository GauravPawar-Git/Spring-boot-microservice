package com.practice.comments.controller;

import com.practice.comments.dto.CommentRequest;
import com.practice.comments.dto.CommentResponse;
import com.practice.comments.dto.PostComments;
import com.practice.comments.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Comment endpoints.
 *
 *   requests arrive from: api-gateway route "comment-service" (Path=/comments/**) or directly on port 8082
 *   security            : SecurityConfig: GET is public; POST/DELETE need a JWT issued by user-service.
 *                         The verified token arrives as @AuthenticationPrincipal Jwt (claims "sub", "uid", "role").
 *   calls               : CommentService
 *   errors              : thrown in CommentService, turned into JSON by ErrorResponseHandler
 */
@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // GET /comments?postId=1  (public)
    @GetMapping
    public PostComments getComments(@RequestParam int postId) {
        return commentService.getCommentsOfPost(postId);
    }

    // The author comes from the verified token, never from the request body
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(@Valid @RequestBody CommentRequest request,
                                                      @AuthenticationPrincipal Jwt jwt) {
        CommentResponse created = commentService.addComment(request, userId(jwt), jwt.getSubject());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable int id, @AuthenticationPrincipal Jwt jwt) {
        commentService.deleteComment(id, userId(jwt), "ADMIN".equals(jwt.getClaimAsString("role")));
        return ResponseEntity.noContent().build();
    }

    // "uid" is written by user-service's JwtService.generateToken(). JSON numbers in JWT claims are decoded as Long
    private static int userId(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).intValue();
    }
}
