package com.assignments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enable scheduling for unpaid reservations
public class RoomReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomReservationApplication.class, args);
	}

}
