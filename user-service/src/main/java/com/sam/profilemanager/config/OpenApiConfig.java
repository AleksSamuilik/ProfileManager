package com.sam.profilemanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI profileManagerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ProfileManager — User API")
                .version("1.0")
                .description("""
                        REST API управления пользователями. Ответы следуют принципам HATEOAS (формат HAL): \
                        вместе с данными возвращается блок `_links` со ссылками на связанные действия \
                        (`self`, `update`, `delete`, `users`), по которым можно перемещаться по API.""")
                .contact(new Contact().name("ProfileManager team")));
    }
}
