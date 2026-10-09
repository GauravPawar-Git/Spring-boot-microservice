package com.practice.first_project.repositories;

import com.practice.first_project.bean.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepositories extends JpaRepository<User, Integer> {
}
