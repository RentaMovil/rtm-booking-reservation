package com.rentamovil.booking.infrastructure.web.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Protege /internal/**: solo otro microservicio de la red interna debe poder llamarlo, nunca un
 * cliente externo. El api-gateway ya no enruta /internal/**, pero esto es una segunda capa de
 * defensa por si alguien llega directo al puerto del contenedor (auditoría de seguridad, hallazgo
 * A2: puertos publicados sin bind a 127.0.0.1 + /internal/** sin autenticación propia).
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private final String expectedKey;

    public InternalApiKeyFilter(@Value("${app.internal.api-key}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedKey = request.getHeader("X-Internal-Key");
        if (expectedKey == null || expectedKey.isBlank() || !expectedKey.equals(providedKey)) {
            log.warn("Llamada a {} sin X-Internal-Key valida (ip={})", request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid internal API key\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }
}
