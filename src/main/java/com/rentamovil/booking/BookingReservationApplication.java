package com.rentamovil.booking;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookingReservationApplication {

	public static void main(String[] args) {
		// Todas las fechas en UTC (modeling-conventions.md): la base es compartida entre servicios,
		// y así LocalDateTime.now() da la misma hora sin importar dónde corra el servicio.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(BookingReservationApplication.class, args);
	}

}