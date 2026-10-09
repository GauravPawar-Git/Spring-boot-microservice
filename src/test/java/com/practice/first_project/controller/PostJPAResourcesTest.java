package com.practice.first_project.controller;

import com.practice.first_project.dto.PostRequest;
import com.practice.first_project.dto.PostResponse;
import com.practice.first_project.handlers.PostLimitExceededException;
import com.practice.first_project.security.JsonSecurityErrorHandlers;
import com.practice.first_project.security.JwtService;
import com.practice.first_project.security.SecurityConfig;
import com.practice.first_project.security.TestTokens;
import com.practice.first_project.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostJPAResources.class)
@Import({SecurityConfig.class, JwtService.class, JsonSecurityErrorHandlers.class})
class PostJPAResourcesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PostService postService;

    @Test
    void createPost_returns201WithLocation() throws Exception {
        when(postService.createPost(eq(1), any(PostRequest.class)))
                .thenReturn(new PostResponse(7, "Hello", LocalDateTime.of(2026, 10, 10, 9, 0), 1, "Rambo"));

        mockMvc.perform(post("/jpa/users/1/posts")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Hello"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/jpa/posts/7"))
                .andExpect(jsonPath("$.authorName").value("Rambo"));
    }

    @Test
    void createPost_returns409_whenLimitReached() throws Exception {
        when(postService.createPost(eq(1), any(PostRequest.class)))
                .thenThrow(new PostLimitExceededException("User 1 already has 10 posts"));

        mockMvc.perform(post("/jpa/users/1/posts")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "One too many"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User 1 already has 10 posts"));
    }

    @Test
    void createPost_returns400_whenDescriptionBlank() throws Exception {
        mockMvc.perform(post("/jpa/users/1/posts")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkCreate_returns400InOurErrorShape_whenOneItemInvalid() throws Exception {
        mockMvc.perform(post("/jpa/users/1/posts/bulk")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"description": "fine"}, {"description": ""}]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("Description is required"));
    }

    // ---- security ----

    @Test
    void createPost_returns403_whenPostingAsSomeoneElse() throws Exception {
        mockMvc.perform(post("/jpa/users/1/posts")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Pretending to be user 1"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission for this action"));
        verify(postService, never()).createPost(anyInt(), any());
    }

    @Test
    void readingPosts_isPublic() throws Exception {
        when(postService.getPostsOfUser(1)).thenReturn(List.of());

        mockMvc.perform(get("/jpa/users/1/posts")).andExpect(status().isOk());
    }

    @Test
    void createPost_returns401_withoutToken() throws Exception {
        mockMvc.perform(post("/jpa/users/1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
