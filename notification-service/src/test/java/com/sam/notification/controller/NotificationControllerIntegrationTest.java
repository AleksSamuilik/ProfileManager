package com.sam.notification.controller;

import com.sam.notification.AbstractMailIntegrationTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIntegrationTest extends AbstractMailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void send_Create_SendsAccountCreatedEmail() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alex@example.com", "operation": "CREATE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.operation").value("CREATE"));

        MimeMessage mail = awaitSingleMail();
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("alex@example.com");
        assertThat(bodyOf(mail))
                .isEqualTo("Your account is created.");
    }

    @Test
    void send_Delete_SendsAccountDeletedEmail() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "maria@example.com", "operation": "DELETE"}"""))
                .andExpect(status().isOk());

        MimeMessage mail = awaitSingleMail();
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("maria@example.com");
        assertThat(bodyOf(mail)).isEqualTo("Здравствуйте! Ваш аккаунт был удалён.");
    }

    @Test
    void send_InvalidEmail_ReturnsBadRequestAndSendsNothing() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "operation": "CREATE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    void send_MissingOperation_ReturnsBadRequestAndSendsNothing() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alex@example.com"}"""))
                .andExpect(status().isBadRequest());

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    void send_UnknownOperation_ReturnsBadRequestAndSendsNothing() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alex@example.com", "operation": "UPDATE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    void openApiDocs_DescribeNotificationEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/notifications'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/api/notifications'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/notifications'].post.responses['502']").exists());
    }
}
