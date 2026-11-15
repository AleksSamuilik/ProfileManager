package com.sam.profilemanager.controller;

import com.sam.profilemanager.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Проверяет, что Swagger (OpenAPI) документация генерируется и описывает все методы API. */
@AutoConfigureMockMvc
class OpenApiDocsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_DescribeAllUserEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value(startsWith("ProfileManager")))
                .andExpect(jsonPath("$.paths['/api/users'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/api/users'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}'].put.summary").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}'].delete.summary").exists());
    }

    @Test
    void apiDocs_DescribeErrorResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/users'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists())
                .andExpect(jsonPath("$.components.schemas.UserRequest").exists());
    }
}
