package com.portfolio.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification microservice entry point.
 *
 * Responsibilities:
 *   - Consumes task.created / task.updated / task.deleted Kafka topics
 *   - Persists a notification record per event into the shared PostgreSQL DB
 *   - Exposes a JWT-protected REST API so the frontend can display a feed
 */
@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
