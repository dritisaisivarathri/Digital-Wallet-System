package com.wallet.notification.controller;

import com.wallet.notification.entity.NotificationHistory;
import com.wallet.notification.repository.NotificationRepository;
import com.wallet.notification.service.NotificationService;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationRepository repository;

    @MockBean
    private NotificationService service;

    @Test
    void getAll_Success() throws Exception {
        NotificationHistory history = new NotificationHistory();
        history.setId(UUID.randomUUID());
        history.setUserId(UUID.randomUUID());
        history.setMessage("Test Message");

        when(repository.findAllByOrderBySentAtDesc()).thenReturn(List.of(history));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Test Message"));
    }

    @Test
    void getByUserId_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationHistory history = new NotificationHistory();
        history.setUserId(userId);
        history.setMessage("For one user");

        when(service.getNotifications(userId)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/notifications/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("For one user"));
    }

    @Test
    void sendManual_Success() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + userId + "\",\"message\":\"Manual ping\",\"topic\":\"ops.alert\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Manual notification triggered and logged successfully"));

        verify(service).sendManualNotification(eq(userId), eq("Manual ping"), eq("ops.alert"));
    }
}
