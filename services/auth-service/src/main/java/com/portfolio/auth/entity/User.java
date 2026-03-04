package com.portfolio.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User record stored in {@code auth_db}.
 * Implements {@link UserDetails} so Spring Security can load it directly.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override public String getUsername()                              { return email; }
    @Override public boolean isAccountNonExpired()                     { return true; }
    @Override public boolean isAccountNonLocked()                      { return true; }
    @Override public boolean isCredentialsNonExpired()                 { return true; }
    @Override public boolean isEnabled()                               { return true; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
}
