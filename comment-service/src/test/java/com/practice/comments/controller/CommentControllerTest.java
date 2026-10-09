package com.practice.comments.controller;

import com.practice.comments.dto.CommentRequest;
import com.practice.comments.dto.CommentResponse;
import com.practice.comments.handlers.UserServiceUnavailableException;
import com.practice.comments.security.SecurityConfig;
import com.practice.comments.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * jwt() from spring-security-test puts a ready-made Jwt in the SecurityContext, skipping signature checks,
 * so these tests focus on the controller: who the author is, status codes, error mapping.
 */
@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
class CommentControllerTest {

    private static final String BODY = """
            {"postId": 1, "text": "Great post!"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Test
    void addComment_takesAuthorFromToken_andReturns201() throws Exception {
        when(commentService.addComment(any(CommentRequest.class), eq(3), eq("vigo")))
                .thenReturn(new CommentResponse(1, 1, 3, "vigo", "Great post!", LocalDateTime.of(2026, 10, 10, 9, 0)));

        mockMvc.perform(post("/comments")
                        .with(jwt().jwt(token -> token.subject("vigo").claim("uid", 3).claim("role", "USER"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUsername").value("vigo"));
        verify(commentService).addComment(any(CommentRequest.class), eq(3), eq("vigo"));
    }

    @Test
    void addComment_returns401_withoutToken() throws Exception {
        mockMvc.perform(post("/comments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addComment_returns503_whenUserServiceIsDown() throws Exception {
        when(commentService.addComment(any(CommentRequest.class), eq(3), eq("vigo")))
                .thenThrow(new UserServiceUnavailableException());

        mockMvc.perform(post("/comments")
                        .with(jwt().jwt(token -> token.subject("vigo").claim("uid", 3)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void readingComments_isPublic() throws Exception {
        mockMvc.perform(get("/comments").param("postId", "1")).andExpect(status().isOk());
    }
}
