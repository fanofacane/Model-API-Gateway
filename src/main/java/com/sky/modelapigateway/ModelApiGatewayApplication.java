package com.sky.modelapigateway;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sky.modelapigateway.mapper")
public class ModelApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModelApiGatewayApplication.class, args);
    }

}
