package com.example.nicechicken.user.entity;

import jakarta.persistence.*;
import lombok.*;

import com.example.nicechicken.common.entity.BaseTimeEntity;

@Entity
@Table(name = "users") 
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder
    public UserEntity(String email, String name, String password, Role role) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.role = role;
    }

    public UserEntity updateName(String name) {
        this.name = name;
        return this;
    }
}