package com.rentamovil.booking.infrastructure.web.controller;

import java.sql.Connection;
import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Sonda de salud: 200 si la base responde, 503 si no. */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean dbUp;
        try (Connection connection = dataSource.getConnection()) {
            dbUp = connection.isValid(2);
        } catch (Exception e) {
            dbUp = false;
        }

        Map<String, String> body = Map.of(
                "status", dbUp ? "ok" : "degraded",
                "db", dbUp ? "connected" : "disconnected",
                "timestamp", Instant.now().toString());

        return ResponseEntity.status(dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}