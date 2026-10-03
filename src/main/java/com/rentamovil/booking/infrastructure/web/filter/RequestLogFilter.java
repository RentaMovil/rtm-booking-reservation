package com.rentamovil.booking.infrastructure.web.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Una línea de log por petición, ej:
 *   POST /reservations -> 201 (85 ms) corr=3f2a...
 * El corr (X-Correlation-Id) lo pone el gateway: con él se encuentra la misma petición en los
 * dos logs. No escribe cuerpos ni headers: nunca aparecen contraseñas ni tokens en el log.
 */
@Slf4j
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            String correlationId = request.getHeader("X-Correlation-Id");
            log.info("{} {} -> {} ({} ms) corr={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    System.currentTimeMillis() - start,
                    correlationId != null ? correlationId : "-");
        }
    }

    /** El HEALTHCHECK de Docker llama a /health seguido: no se registra. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/health".equals(request.getRequestURI());
    }
}