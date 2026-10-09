package com.practice.first_project.dao;

import com.practice.first_project.bean.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Component
public class UserDAO {

     private static List<User> userList = new ArrayList<>();
     private static int userCount = 0;

     static {
         userList.add(new User(++userCount, "Adam", LocalDate.now().minusYears(30)));
         userList.add(new User(++userCount, "Eve", LocalDate.now().minusYears(24)));
         userList.add(new User(++userCount, "John", LocalDate.now().minusYears(25)));
     }

     public List<User> findAll(){
         return userList;
     }

    public User getUserById(int id) {
        Predicate<? super User> predicate = user -> user.getId().equals(id);
        return userList.stream().filter(predicate).findFirst().orElse(null);
    }

    public User createUser(User user) {
         user.setId(++userCount);
         userList.add(user);
         return user;
    }

    // returns true if a user was removed
    public boolean deleteById(int id) {
         return userList.removeIf(user -> user.getId().equals(id));
    }
}
