package se.inera.nll.nlllight.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI nllLightOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NLL Light Medication API")
                        .description("REST API for electronic prescriptions and medication management")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("NLL Light Team")
                                .email("support@nll-light.example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://localhost:8000")
                                .description("Kong API Gateway")
                ));
    }
}
