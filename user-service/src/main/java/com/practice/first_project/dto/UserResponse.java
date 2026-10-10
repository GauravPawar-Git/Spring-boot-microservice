package com.practice.first_project.dto;

import com.practice.first_project.entity.User;

import java.time.LocalDate;
import java.time.Period;

/**
 * What the API returns. It can differ from the entity: here we add a computed "age"
 * and could hide internal columns without touching the database model.
 * Built with UserResponse.from() in UserService and AuthService; returned by UserJPAResources and AuthController.
 * Note what is NOT here: username, password hash and role stay inside the service.
 */
public record UserResponse(Integer id, String name, LocalDate birthDate, int age) {

    public static UserResponse from(User user) {
        int age = user.getBirthDate() == null ? 0
                : Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        return new UserResponse(user.getId(), user.getName(), user.getBirthDate(), age);
    }
}
