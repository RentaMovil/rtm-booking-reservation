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

            // PENDIENTE (ver README): iam todavía no emite este claim. Hasta que lo añada, las
            // peticiones que necesitan el titular fallan con 401 en vez de dar un error 500.
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