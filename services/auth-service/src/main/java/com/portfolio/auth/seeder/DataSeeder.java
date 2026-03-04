package com.portfolio.auth.seeder;

import com.portfolio.auth.entity.User;
import com.portfolio.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds two demo users on first startup if the users table is empty.
 * <p>
 * Demo credentials:
 * <ul>
 *   <li>alice@demo.com / password123</li>
 *   <li>bob@demo.com   / password123</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return; // Already seeded
        }

        String encoded = passwordEncoder.encode("password123");

        userRepository.save(User.builder()
                .firstName("Alice").lastName("Demo")
                .email("alice@demo.com").password(encoded)
                .build());

        userRepository.save(User.builder()
                .firstName("Bob").lastName("Demo")
                .email("bob@demo.com").password(encoded)
                .build());

        log.info("Seeded demo users: alice@demo.com, bob@demo.com (password: password123)");
    }
}
