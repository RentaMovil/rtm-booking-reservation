package com.rentamovil.booking.infrastructure.security;

import java.text.ParseException;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.exception.InvalidTokenException;

/**
 * Verifica los access tokens que firma iam con RS256.
 *
 * <p>Usa solo la clave pública (la que iam publica en GET /jwks): este servicio no puede firmar
 * tokens ni tiene la privada. Verifica por su cuenta en vez de llamar a iam por cada petición.
 *
 * <p><b>Temporalmente vuelta a clave estática (revert de A4)</b>: el 2026-10-23 se cambió a
 * descarga dinámica de JWKS, pero en producción causó fallos intermitentes del endpoint SSE
 * (/notifications/stream, el que más llamadas por segundo recibe) por un problema todavía sin
 * diagnosticar del todo con el caché de nimbus bajo reconexiones frecuentes. Se revierte a esta
 * versión sin dependencia de red en tiempo de verificación mientras se investiga con calma.
 */
@Component
public class JwtVerifierAdapter {

    private final RSAKey iamPublicKey;

    public JwtVerifierAdapter(RSAKey iamPublicKey) {
        this.iamPublicKey = iamPublicKey;
    }

    public AuthenticatedUser verify(String accessToken) {
        try {
            SignedJWT jwt = SignedJWT.parse(accessToken);

            // Solo se aceptan tokens RS256 firmados con la clave de iam.
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new RSASSAVerifier(iamPublicKey.toRSAPublicKey()))) {
                throw new InvalidTokenException("Token inválido");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                throw new InvalidTokenException("Token vencido");
            }

            Integer personId = claims.getIntegerClaim("personId");
            if (personId == null) {
                throw new InvalidTokenException("El token no incluye el titular (personId)");
            }

            return new AuthenticatedUser(
                    Integer.valueOf(claims.getSubject()),
                    personId,
                    claims.getStringClaim("role"),
                    claims.getStringListClaim("permissions"));
        } catch (ParseException | JOSEException | NumberFormatException e) {
            throw new InvalidTokenException("Token inválido");
        }
    }
}
