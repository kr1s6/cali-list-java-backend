package com.CalisthenicList.CaliList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class CaliListApplication {
    public static void main(String[] args) {
        SpringApplication.run(CaliListApplication.class, args);
    }

}
