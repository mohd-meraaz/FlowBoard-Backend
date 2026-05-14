package com.flowboard.board_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BoardServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(BoardServiceApplication.class, args);
	}

}
