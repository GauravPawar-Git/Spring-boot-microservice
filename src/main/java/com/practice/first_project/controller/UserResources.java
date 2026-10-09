package com.practice.first_project.controller;

import com.practice.first_project.bean.User;
import com.practice.first_project.dao.UserDAO;
import com.practice.first_project.handlers.UserNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Older in-memory version (no database), kept for comparison with UserJPAResources.
 */
@RestController
public class UserResources {

    private final UserDAO userDAO;

    public UserResources(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GetMapping("/users")
    public List<User> retrieveAllUsers() {
       return userDAO.findAll();
    }

    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable int id) {
       User user = userDAO.getUserById(id);
       if (user == null){
           throw new UserNotFoundException("User not found with id: " + id);
       }
       return user;
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user){
        User saved = userDAO.createUser(user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable int id){
        if (!userDAO.deleteById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        return ResponseEntity.noContent().build();
    }

}
