package com.rentamovil.booking.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rentamovil.booking.application.usecase.InsuranceUseCaseImpl;
import com.rentamovil.booking.application.usecase.NotificationUseCaseImpl;
import com.rentamovil.booking.application.usecase.PaymentEventUseCaseImpl;
import com.rentamovil.booking.application.usecase.RentalUseCaseImpl;
import com.rentamovil.booking.application.usecase.ReservationUseCaseImpl;
import com.rentamovil.booking.domain.port.in.InsuranceUseCases;
import com.rentamovil.booking.domain.port.in.NotificationUseCases;
import com.rentamovil.booking.domain.port.in.PaymentEventUseCases;
import com.rentamovil.booking.domain.port.in.RentalUseCases;
import com.rentamovil.booking.domain.port.in.ReservationUseCases;
import com.rentamovil.booking.domain.port.out.AuditPort;
import com.rentamovil.booking.domain.port.out.DomainEventPublisherPort;
import com.rentamovil.booking.domain.port.out.InsuranceTypeRepositoryPort;
import com.rentamovil.booking.domain.port.out.NotificationRepositoryPort;
import com.rentamovil.booking.domain.port.out.RealtimeNotificationPort;
import com.rentamovil.booking.domain.port.out.RentalRepositoryPort;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort;
import com.rentamovil.booking.domain.port.out.VehicleAvailabilityPort;

/**
 * Conecta los casos de uso con sus adaptadores.
 * Los casos de uso no llevan @Service para que el dominio no dependa de Spring.
 */
@Configuration
public class BeanConfig {

    @Bean
    public ReservationUseCases reservationUseCases(ReservationRepositoryPort reservationRepository,
                                                   InsuranceTypeRepositoryPort insuranceTypeRepository,
                                                   NotificationRepositoryPort notificationRepository,
                                                   RealtimeNotificationPort realtimeNotification,
                                                   VehicleAvailabilityPort vehicleAvailability,
                                                   DomainEventPublisherPort events,
                                                   AuditPort audit) {
        return new ReservationUseCaseImpl(reservationRepository, insuranceTypeRepository,
                notificationRepository, realtimeNotification, vehicleAvailability, events, audit);
    }

    @Bean
    public RentalUseCases rentalUseCases(RentalRepositoryPort rentalRepository,
                                         ReservationRepositoryPort reservationRepository,
                                         DomainEventPublisherPort events,
                                         AuditPort audit) {
        return new RentalUseCaseImpl(rentalRepository, reservationRepository, events, audit);
    }

    @Bean
    public InsuranceUseCases insuranceUseCases(InsuranceTypeRepositoryPort insuranceTypeRepository) {
        return new InsuranceUseCaseImpl(insuranceTypeRepository);
    }

    @Bean
    public NotificationUseCases notificationUseCases(NotificationRepositoryPort notificationRepository) {
        return new NotificationUseCaseImpl(notificationRepository);
    }

    @Bean
    public PaymentEventUseCases paymentEventUseCases(ReservationRepositoryPort reservationRepository,
                                                     NotificationRepositoryPort notificationRepository,
                                                     RealtimeNotificationPort realtimeNotification) {
        return new PaymentEventUseCaseImpl(reservationRepository, notificationRepository, realtimeNotification);
    }
}