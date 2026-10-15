package com.rentamovil.booking.infrastructure.persistence.adapter;

import com.rentamovil.booking.domain.entity.InsuranceType;
import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.entity.Rental;
import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.NotificationType;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.enums.RentalStatus;
import com.rentamovil.booking.infrastructure.persistence.entity.InsuranceTypeEntity;
import com.rentamovil.booking.infrastructure.persistence.entity.NotificationEntity;
import com.rentamovil.booking.infrastructure.persistence.entity.RentalEntity;
import com.rentamovil.booking.infrastructure.persistence.entity.ReservationEntity;

/** Traduce entre entidades JPA y entidades de dominio. Sin Spring: lo usan los adaptadores. */
public final class PersistenceMapper {

    private PersistenceMapper() {
    }

    public static ReservationEntity toEntity(Reservation reservation) {
        ReservationEntity entity = new ReservationEntity();
        entity.setId(reservation.getId());
        entity.setClientId(reservation.getClientId());
        entity.setVehicleId(reservation.getVehicleId());
        entity.setInsuranceTypeId(reservation.getInsuranceTypeId());
        entity.setPickupBranchId(reservation.getPickupBranchId());
        entity.setReturnBranchId(reservation.getReturnBranchId());
        entity.setReservationDate(reservation.getReservationDate());
        entity.setStartDate(reservation.getStartDate());
        entity.setEndDate(reservation.getEndDate());
        entity.setVehicleSubtotal(reservation.getVehicleSubtotal());
        entity.setInsuranceSubtotal(reservation.getInsuranceSubtotal());
        entity.setTotalAmount(reservation.getTotalAmount());
        entity.setStatus(reservation.getStatus().name());
        return entity;
    }

    public static Reservation toDomain(ReservationEntity entity) {
        return Reservation.restore(
                entity.getId(),
                entity.getClientId(),
                entity.getVehicleId(),
                entity.getInsuranceTypeId(),
                entity.getPickupBranchId(),
                entity.getReturnBranchId(),
                entity.getReservationDate(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getVehicleSubtotal(),
                entity.getInsuranceSubtotal(),
                ReservationStatus.valueOf(entity.getStatus()));
    }

    public static RentalEntity toEntity(Rental rental) {
        RentalEntity entity = new RentalEntity();
        entity.setId(rental.getId());
        entity.setReservationId(rental.getReservationId());
        entity.setGpsId(rental.getGpsId());
        entity.setActualStartDate(rental.getActualStartDate());
        entity.setActualEndDate(rental.getActualEndDate());
        entity.setInitialMileage(rental.getInitialMileage());
        entity.setFinalMileage(rental.getFinalMileage());
        entity.setStatus(rental.getStatus().name());
        return entity;
    }

    // vehicleId no se persiste en rental: la columna no existe en la tabla. Cuando se necesita,
    // se lee de reservation (rental.reservation_id es 1:1). Por eso se pasa null aquí.
    public static Rental toDomain(RentalEntity entity) {
        return Rental.restore(
                entity.getId(),
                entity.getReservationId(),
                null,
                entity.getGpsId(),
                entity.getActualStartDate(),
                entity.getActualEndDate(),
                entity.getInitialMileage(),
                entity.getFinalMileage(),
                RentalStatus.valueOf(entity.getStatus()));
    }

    public static NotificationEntity toEntity(Notification notification) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(notification.getId());
        entity.setPersonId(notification.getPersonId());
        entity.setNotificationType(notification.getType().name());
        entity.setMessage(notification.getMessage());
        entity.setSentDate(notification.getSentDate());
        entity.setRead(notification.isRead());
        entity.setReservationId(notification.getReservationId());
        entity.setInvoiceId(notification.getInvoiceId());
        entity.setPaymentId(notification.getPaymentId());
        entity.setMaintenanceId(notification.getMaintenanceId());
        return entity;
    }

    public static Notification toDomain(NotificationEntity entity) {
        return Notification.restore(
                entity.getId(),
                entity.getPersonId(),
                NotificationType.valueOf(entity.getNotificationType()),
                entity.getMessage(),
                entity.getSentDate(),
                entity.isRead(),
                entity.getReservationId(),
                entity.getInvoiceId(),
                entity.getPaymentId(),
                entity.getMaintenanceId());
    }

    public static InsuranceTypeEntity toEntity(InsuranceType insuranceType) {
        InsuranceTypeEntity entity = new InsuranceTypeEntity();
        entity.setId(insuranceType.getId());
        entity.setName(insuranceType.getName());
        entity.setCoverageDetails(insuranceType.getCoverageDetails());
        entity.setDailyCost(insuranceType.getDailyCost());
        return entity;
    }

    public static InsuranceType toDomain(InsuranceTypeEntity entity) {
        return InsuranceType.restore(entity.getId(), entity.getName(), entity.getCoverageDetails(),
                entity.getDailyCost());
    }
}