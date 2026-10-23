package com.rentamovil.booking.infrastructure.security;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.exception.DependencyUnavailableException;
import com.rentamovil.booking.domain.exception.InvalidTokenException;

/**
 * Verifica los access tokens que firma iam con RS256, usando su clave PÚBLICA (GET iam/jwks).
 *
 * <p>Las claves se descargan la primera vez que llega un token y quedan en caché; si llega un
 * token con un {@code kid} desconocido (iam rotó su clave), se vuelven a descargar solas. Por eso
 * este servicio arranca aunque iam todavía no esté arriba.
 */
@Component
public class JwtVerifierAdapter {

    private final DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

    @Autowired
    public JwtVerifierAdapter(@Value("${app.booking-reservation.iam-url}") String iamUrl)
            throws MalformedURLException {
        this(JWKSourceBuilder.create(URI.create(iamUrl + "/jwks").toURL()).build());
    }

    /** Para tests: claves en memoria en vez de descargarlas de iam. */
    JwtVerifierAdapter(JWKSource<SecurityContext> keys) {
        // Solo RS256: un token con "alg": "none" o HS256 se rechaza.
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
    }

    public AuthenticatedUser verify(String accessToken) {
        try {
            // process ya revisa firma y expiración (exp).
            JWTClaimsSet claims = processor.process(accessToken, null);

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
        } catch (KeySourceException e) {
            // iam está caído: no es culpa del token.
            throw new DependencyUnavailableException("IAM_UNAVAILABLE",
                    "No se pudieron descargar las claves de iam para verificar el token", e);
        } catch (ParseException | BadJOSEException | JOSEException | NumberFormatException e) {
            throw new InvalidTokenException("Token inválido o vencido");
        }
    }
}
