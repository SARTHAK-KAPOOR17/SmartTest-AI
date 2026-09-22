package com.smarttestai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartTestAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartTest AI API")
                        .description("Self-Healing QA Automation Platform — Phase 1 Project Foundation API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartTest AI Engineering Team")
                                .email("engineering@smarttestai.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
