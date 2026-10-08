package com.example.ordermanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management System API")
                        .description("Enterprise-grade Order Management REST API. " +
                                "Supports customer management, product catalogue, " +
                                "inventory control, and transactional order processing.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Order Management Team")
                                .email("ordermgmt@example.com"))
                        .license(new License()
                                .name("Internal Use Only")));
    }
}
