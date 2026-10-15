package com.rentamovil.booking.infrastructure.web.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un parámetro para que reciba el usuario del access token.
 *
 * <p>El id sale siempre del token, nunca de la URL ni del cuerpo.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}