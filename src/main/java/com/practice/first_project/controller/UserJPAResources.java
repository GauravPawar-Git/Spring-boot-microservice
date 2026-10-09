package com.practice.first_project.controller;

import com.practice.first_project.dto.UserRequest;
import com.practice.first_project.dto.UserResponse;
import com.practice.first_project.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Thin controller: read the HTTP request, call the service, build the HTTP response.
 * No business logic and no repository access here.
 */
@RestController
@RequestMapping("/jpa/users")
public class UserJPAResources {

    private final UserService userService;

    public UserJPAResources(UserService userService) {
        this.userService = userService;
    }

    // GET /jpa/users?page=0&size=5&sort=name,asc
    @GetMapping
    public PagedModel<UserResponse> retrieveAllUsers(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        // PagedModel gives a stable JSON shape: { "content": [...], "page": {size, number, totalElements, totalPages} }
        return new PagedModel<>(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable int id) {
        return userService.getUserById(id);
    }

    // 201 Created + Location: /jpa/users/{newId}
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // users with logins come from /auth/register; this creates one without a login
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse created = userService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") // admins, or the user editing themselves
    public UserResponse updateUser(@PathVariable int id, @Valid @RequestBody UserRequest request) {
        return userService.updateUser(id, request);
    }

    // 204 No Content: success, nothing to return
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserById(@PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
