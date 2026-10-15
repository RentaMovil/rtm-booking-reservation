package com.rentamovil.booking.infrastructure.fleet;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.exception.ResourceNotFoundException;
import com.rentamovil.booking.domain.port.out.VehicleAvailabilityPort;

/**
 * Cliente REST de fleet-maintenance: disponibilidad y precio del vehículo.
 *
 * <p>Los dos endpoints son públicos en fleet (el catálogo se consulta sin sesión), así que no se
 * reenvía el token. Va directo por la red interna, no por el gateway.
 *
 * <p>Si fleet no responde no se inventa un precio: la reserva falla con FLEET_SERVICE_UNAVAILABLE.
 */
@Component
public class FleetVehicleClientAdapter implements VehicleAvailabilityPort {

    private final RestClient restClient;

    public FleetVehicleClientAdapter(@Value("${app.booking-reservation.fleet-url}") String fleetUrl,
                                     @Value("${app.booking-reservation.fleet-timeout}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(fleetUrl).requestFactory(requestFactory).build();
    }

    // fleet trabaja por días (LocalDate): el rango de la reserva se recorta a sus fechas
    @Override
    public boolean isAvailable(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        AvailabilityResponse response = call(vehicleId, () -> restClient.get()
                .uri(uri -> uri.path("/vehicles/{id}/availability")
                        .queryParam("from", startDate.toLocalDate())
                        .queryParam("to", endDate.toLocalDate())
                        .build(vehicleId))
                .retrieve()
                .body(AvailabilityResponse.class));
        return response != null && response.available();
    }

    // El precio por día es el del vehículo; el rango no lo cambia (no hay tarifas por temporada)
    @Override
    public BigDecimal dailyRate(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        VehicleResponse vehicle = call(vehicleId, () -> restClient.get()
                .uri("/vehicles/{id}", vehicleId)
                .retrieve()
                .body(VehicleResponse.class));
        if (vehicle == null || vehicle.dailyPrice() == null) {
            throw unavailable();
        }
        return vehicle.dailyPrice();
    }

    private <T> T call(Integer vehicleId, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("El vehículo " + vehicleId + " no existe");
            }
            throw unavailable();
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private BusinessRuleException unavailable() {
        return new BusinessRuleException("FLEET_SERVICE_UNAVAILABLE",
                "El catálogo de vehículos no está disponible en este momento");
    }

    record AvailabilityResponse(boolean available) {
    }

    record VehicleResponse(Integer id, BigDecimal dailyPrice) {
    }
}
