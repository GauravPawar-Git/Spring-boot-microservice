package com.practice.first_project.dto;

import com.practice.first_project.bean.User;

import java.time.LocalDate;
import java.time.Period;

/**
 * What the API returns. It can differ from the entity: here we add a computed "age"
 * and could hide internal columns without touching the database model.
 */
public record UserResponse(Integer id, String name, LocalDate birthDate, int age) {

    public static UserResponse from(User user) {
        int age = user.getBirthDate() == null ? 0
                : Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        return new UserResponse(user.getId(), user.getName(), user.getBirthDate(), age);
    }
}
