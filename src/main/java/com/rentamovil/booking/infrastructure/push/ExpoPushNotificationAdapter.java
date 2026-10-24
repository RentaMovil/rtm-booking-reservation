package com.rentamovil.booking.infrastructure.push;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.port.out.PushNotificationPort;

/**
 * Manda el push a través de Expo (https://exp.host): primero le pregunta a iam qué tokens de
 * dispositivo tiene el usuario (GET /internal/users/{id}/push-tokens), y por cada uno llama a la
 * API de Expo Push. Expo se encarga de entregarlo a Android/iOS.
 *
 * <p>Nunca propaga un error hacia quien crea la notificación: si iam, Expo, o la red fallan, el
 * push simplemente no sale (la fila en booking.notification ya quedó guardada igual).
 */
@Component
public class ExpoPushNotificationAdapter implements PushNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushNotificationAdapter.class);
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestClient iamClient;
    private final RestClient expoClient;

    public ExpoPushNotificationAdapter(@Value("${app.booking-reservation.iam-url}") String iamUrl,
                                       @Value("${app.internal.api-key}") String internalApiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.iamClient = RestClient.builder().baseUrl(iamUrl).requestFactory(requestFactory)
                .defaultHeader("X-Internal-Key", internalApiKey).build();
        this.expoClient = RestClient.builder().baseUrl(EXPO_PUSH_URL).requestFactory(requestFactory).build();
    }

    @Override
    public void sendToUser(Notification notification) {
        List<DeviceToken> tokens;
        try {
            tokens = iamClient.get()
                    .uri("/internal/users/{id}/push-tokens", notification.getPersonId())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DeviceToken>>() { });
        } catch (RestClientException e) {
            log.warn("No se pudieron obtener los tokens de push de iam para personId={}",
                    notification.getPersonId(), e);
            return;
        }

        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (DeviceToken deviceToken : tokens) {
            send(deviceToken, notification);
        }
    }

    private void send(DeviceToken deviceToken, Notification notification) {
        Map<String, Object> message = Map.of(
                "to", deviceToken.token(),
                "title", "RentaMovil",
                "body", notification.getMessage(),
                "data", Map.of(
                        "type", notification.getType().name(),
                        "notificationId", notification.getId()));
        try {
            expoClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo enviar el push a Expo (token truncado={}...)",
                    deviceToken.token().substring(0, Math.min(12, deviceToken.token().length())), e);
        }
    }

    private record DeviceToken(String token, String platform) {
    }
}
