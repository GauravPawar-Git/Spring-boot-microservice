package com.practice.first_project.controller;

import com.practice.first_project.dto.UserRequest;
import com.practice.first_project.dto.UserResponse;
import com.practice.first_project.handlers.UserNotFoundException;
import com.practice.first_project.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: starts only the MVC part of Spring (controllers, JSON, validation,
 * @RestControllerAdvice) — no database. The service is replaced by a mock.
 */
@WebMvcTest(UserJPAResources.class)
class UserJPAResourcesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getUser_returns200WithBody() throws Exception {
        when(userService.getUserById(1))
                .thenReturn(new UserResponse(1, "Adam", LocalDate.of(1995, 5, 20), 31));

        mockMvc.perform(get("/jpa/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Adam"));
    }

    @Test
    void getUser_returns404_whenServiceThrowsNotFound() throws Exception {
        when(userService.getUserById(99)).thenThrow(new UserNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/jpa/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void createUser_returns201WithLocationHeader() throws Exception {
        when(userService.createUser(any(UserRequest.class)))
                .thenReturn(new UserResponse(5, "Eve", LocalDate.of(2000, 1, 1), 26));

        mockMvc.perform(post("/jpa/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Eve", "birthDate": "2000-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/jpa/users/5"));
    }

    @Test
    void createUser_returns400WithAllErrors_whenInvalid() throws Exception {
        mockMvc.perform(post("/jpa/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "A", "birthDate": "2999-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }
}
