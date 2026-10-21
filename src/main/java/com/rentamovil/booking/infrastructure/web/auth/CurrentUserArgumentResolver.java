package com.rentamovil.booking.infrastructure.web.auth;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.exception.InvalidTokenException;
import com.rentamovil.booking.infrastructure.security.JwtVerifierAdapter;

import lombok.RequiredArgsConstructor;

/**
 * Lee el access token del header Authorization y lo verifica con la clave pública de iam.
 * Si falta o no es válido responde 401.
 *
 * <p>Si no hay header, cae a {@code ?token=}: lo único que lo necesita es el stream SSE de
 * notificaciones, porque EventSource no puede mandar headers propios.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER = "Bearer ";

    private final JwtVerifierAdapter jwtVerifier;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String authorization = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String token;
        if (authorization != null && authorization.startsWith(BEARER)) {
            token = authorization.substring(BEARER.length());
        } else {
            token = webRequest.getParameter("token");
        }

        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Falta el token de acceso");
        }
        return jwtVerifier.verify(token);
    }
}