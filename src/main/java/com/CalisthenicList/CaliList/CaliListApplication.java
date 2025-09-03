package com.CalisthenicList.CaliList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CaliListApplication {
	public static void main(String[] args) {
		SpringApplication.run(CaliListApplication.class, args);
	}

}
