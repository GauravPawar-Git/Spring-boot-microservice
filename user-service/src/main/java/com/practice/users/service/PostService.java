package com.practice.users.service;

import com.practice.users.entity.Post;
import com.practice.users.entity.User;
import com.practice.users.dto.PostRequest;
import com.practice.users.dto.PostResponse;
import com.practice.users.handlers.PostLimitExceededException;
import com.practice.users.handlers.PostNotFoundException;
import com.practice.users.handlers.UserNotFoundException;
import com.practice.users.repositories.PostRepository;
import com.practice.users.repositories.UserRepositories;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Posts: create, list, the N+1 demo, and the transaction (all-or-nothing) demo.
 *
 *   called by: PostJPAResources (all /jpa/posts and /jpa/users/{id}/posts endpoints)
 *              getPost() is also reached from comment-service: UserServiceClient.getPost() (Feign)
 *              -> GET /jpa/posts/{id} -> PostJPAResources.getPost() -> here
 *   calls    : PostRepository, UserRepositories
 *   cache    : "posts" (Caffeine, enabled by CacheConfig, size/TTL in application.properties);
 *              emptied by UserService.updateUser() / deleteUser() (@CacheEvict)
 *   errors   : PostNotFoundException -> 404, UserNotFoundException -> 404,
 *              PostLimitExceededException -> 409   (all mapped in ErrorResponseHandler)
 */
@Service
@Transactional(readOnly = true)
public class PostService {

    static final int MAX_POSTS_PER_USER = 10;

    private final PostRepository postRepository;
    private final UserRepositories userRepositories;

    public PostService(PostRepository postRepository, UserRepositories userRepositories) {
        this.postRepository = postRepository;
        this.userRepositories = userRepositories;
    }

    public List<PostResponse> getPostsOfUser(int userId) {
        findUserOrThrow(userId);
        // the user is already in this transaction's persistence context, so mapping authorName costs no extra query
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PostResponse::from)
                .toList();
    }

    // Used by comment-service (Phase 5) to check that a post exists before commenting on it.
    // Cached (Phase 6): repeated lookups of the same post skip the database for up to 60 s.
    @Cacheable(cacheNames = "posts", key = "#id")
    public PostResponse getPost(int id) {
        return postRepository.findById(id)
                .map(PostResponse::from)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    @Transactional
    public PostResponse createPost(int userId, PostRequest request) {
        User user = findUserOrThrow(userId);
        if (postRepository.countByUserId(userId) >= MAX_POSTS_PER_USER) {
            throw new PostLimitExceededException("User " + userId + " already has " + MAX_POSTS_PER_USER + " posts");
        }
        Post post = new Post(request.description());
        user.addPost(post);
        return PostResponse.from(postRepository.save(post));
    }

    /**
     * All-or-nothing: posts are inserted one by one, and if the limit is crossed part-way,
     * the exception rolls back the whole transaction, including the posts already inserted.
     */
    @Transactional
    public List<PostResponse> createPosts(int userId, List<PostRequest> requests) {
        User user = findUserOrThrow(userId);
        List<PostResponse> created = new ArrayList<>();
        for (PostRequest request : requests) {
            Post post = new Post(request.description());
            user.addPost(post);
            postRepository.save(post);
            created.add(PostResponse.from(post));

            if (postRepository.countByUserId(userId) > MAX_POSTS_PER_USER) {
                // RuntimeException -> Spring rolls back. (A checked Exception would COMMIT by default!)
                throw new PostLimitExceededException("User " + userId + " would exceed " + MAX_POSTS_PER_USER
                        + " posts; none of the " + requests.size() + " posts were saved");
            }
        }
        return created;
    }

    // N+1 version: 1 query for the posts, then 1 query per distinct author when PostResponse reads the user's name
    public List<PostResponse> getFeedNPlusOne() {
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(PostResponse::from)
                .toList();
    }

    // Fixed version: posts and their authors in ONE query (JOIN FETCH)
    public List<PostResponse> getFeed() {
        return postRepository.findAllWithUser().stream()
                .map(PostResponse::from)
                .toList();
    }

    private User findUserOrThrow(int userId) {
        return userRepositories.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }
}
