package com.practice.first_project.controller;

import com.practice.first_project.dto.UserRequest;
import com.practice.first_project.dto.UserResponse;
import com.practice.first_project.handlers.UserNotFoundException;
import com.practice.first_project.security.JsonSecurityErrorHandlers;
import com.practice.first_project.security.JwtService;
import com.practice.first_project.security.SecurityConfig;
import com.practice.first_project.security.TestTokens;
import com.practice.first_project.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: starts only the MVC part of Spring (controllers, JSON, validation,
 * @RestControllerAdvice) — no database. The service is replaced by a mock.
 * Our real security config is imported, so requests go through the JWT filter and @PreAuthorize.
 */
@WebMvcTest(UserJPAResources.class)
@Import({SecurityConfig.class, JwtService.class, JsonSecurityErrorHandlers.class})
class UserJPAResourcesTest {

    private static final String VALID_USER_JSON = """
            {"name": "Eve", "birthDate": "2000-01-01"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Test
    void getUser_returns200WithBody() throws Exception {
        when(userService.getUserById(1))
                .thenReturn(new UserResponse(1, "Adam", LocalDate.of(1995, 5, 20), 31));

        mockMvc.perform(get("/jpa/users/1").header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Adam"));
    }

    @Test
    void getUser_returns404_whenServiceThrowsNotFound() throws Exception {
        when(userService.getUserById(99)).thenThrow(new UserNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/jpa/users/99").header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void createUser_returns201WithLocationHeader_forAdmin() throws Exception {
        when(userService.createUser(any(UserRequest.class)))
                .thenReturn(new UserResponse(5, "Eve", LocalDate.of(2000, 1, 1), 26));

        mockMvc.perform(post("/jpa/users")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.admin(jwtService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/jpa/users/5"));
    }

    @Test
    void createUser_returns400WithAllErrors_whenInvalid() throws Exception {
        mockMvc.perform(post("/jpa/users")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.admin(jwtService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "A", "birthDate": "2999-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    // ---- security ----

    @Test
    void anyUserEndpoint_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/jpa/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required: send 'Authorization: Bearer <token>'"));
    }

    @Test
    void returns401_withGarbageToken() throws Exception {
        mockMvc.perform(get("/jpa/users/1").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid token: MalformedJwtException"));
    }

    @Test
    void createUser_returns403_forNormalUser() throws Exception {
        mockMvc.perform(post("/jpa/users")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isForbidden());
        verify(userService, never()).createUser(any());
    }

    @Test
    void deleteUser_returns403_forNormalUser_and204_forAdmin() throws Exception {
        mockMvc.perform(delete("/jpa/users/3").header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 3)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/jpa/users/3").header(HttpHeaders.AUTHORIZATION, TestTokens.admin(jwtService)))
                .andExpect(status().isNoContent());
        verify(userService).deleteUser(3);
    }

    @Test
    void updateUser_allowedForSelf_forbiddenForSomeoneElse() throws Exception {
        when(userService.updateUser(eq(2), any(UserRequest.class)))
                .thenReturn(new UserResponse(2, "Eve", LocalDate.of(2000, 1, 1), 26));

        mockMvc.perform(put("/jpa/users/2")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(put("/jpa/users/3")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.user(jwtService, 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isForbidden());
    }
}
