package com.searchlight.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI searchlightOpenAPI(
            @Value("${server.port:8080}") String serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("Searchlight API")
                        .description("Production-grade document retrieval API with hybrid search (keyword + vector)")
                        .version("v0.1.0")
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development server")
                ));
    }
}
