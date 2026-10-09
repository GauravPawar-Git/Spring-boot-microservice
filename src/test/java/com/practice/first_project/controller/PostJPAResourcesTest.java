package com.practice.first_project.controller;

import com.practice.first_project.dto.PostRequest;
import com.practice.first_project.dto.PostResponse;
import com.practice.first_project.handlers.PostLimitExceededException;
import com.practice.first_project.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostJPAResources.class)
class PostJPAResourcesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    void createPost_returns201WithLocation() throws Exception {
        when(postService.createPost(eq(1), any(PostRequest.class)))
                .thenReturn(new PostResponse(7, "Hello", LocalDateTime.of(2026, 10, 10, 9, 0), 1, "Rambo"));

        mockMvc.perform(post("/jpa/users/1/posts")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkCreate_returns400InOurErrorShape_whenOneItemInvalid() throws Exception {
        mockMvc.perform(post("/jpa/users/1/posts/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"description": "fine"}, {"description": ""}]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("Description is required"));
    }
}
