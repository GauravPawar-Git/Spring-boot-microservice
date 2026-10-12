package com.practice.users.handlers;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown by UserService / PostService when no user has the id; ErrorResponseHandler.handleUserNotFoundException() -> 404.
// (@ResponseStatus would also give 404 on its own, but the handler adds the JSON body.)
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String s) {
        super(s);
    }
}
