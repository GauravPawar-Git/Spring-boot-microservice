package com.practice.first_project.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity = how a user is stored in the database.
 * It is NOT sent to clients directly; see UserRequest / UserResponse in the dto package.
 * Validation rules live on the DTO (the API contract), not here.
 */
@Entity
@Table(name = "user_details")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    private LocalDate birthDate;

    // Login credentials (Phase 3). Null for users created by an admin without a login.
    @Column(unique = true, length = 30)
    private String username;

    // BCrypt hash, never the plain password. Never put this field in a response DTO.
    @Column(length = 100)
    private String password;

    // Stored as the text 'USER' / 'ADMIN'. Never use EnumType.ORDINAL: reordering the enum would corrupt data.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    // The "one" side. mappedBy = "user" means Post.user owns the foreign key; this list is just its mirror.
    // @OneToMany is LAZY by default. cascade ALL + orphanRemoval: deleting a user deletes their posts.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    // JPA needs a no-args constructor to create objects when reading rows
    protected User() {
    }

    public User(String name, LocalDate birthDate) {
        this.name = name;
        this.birthDate = birthDate;
    }

    // sets the id directly: handy in unit tests, where no database generates it
    public User(Integer id, String name, LocalDate birthDate) {
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    // Called on registration: the password must already be hashed
    public void setCredentials(String username, String passwordHash) {
        this.username = username;
        this.password = passwordHash;
    }

    public List<Post> getPosts() {
        return posts;
    }

    // Always change both sides of a bidirectional relationship together
    public void addPost(Post post) {
        posts.add(post);
        post.setUser(this);
    }
}
