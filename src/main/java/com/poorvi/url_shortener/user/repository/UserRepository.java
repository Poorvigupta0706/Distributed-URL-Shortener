package com.poorvi.url_shortener.user.repository;

import com.poorvi.url_shortener.user.entity.user;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<user, Long> {

    Optional<user> findByEmail(String email);

    boolean existsByEmail(String email);
}