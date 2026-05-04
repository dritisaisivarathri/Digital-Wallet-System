package com.wallet.user.controller;

import com.wallet.user.entity.KycDetails;
import com.wallet.user.entity.User;
import com.wallet.user.service.UserService;
import com.wallet.user.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalKycController.class)
@AutoConfigureMockMvc(addFilters = false)
public class InternalKycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void getPendingKycs_Success() throws Exception {
        KycDetails kyc = new KycDetails();
        kyc.setStatus("PENDING");
        when(userService.getPendingKycs()).thenReturn(List.of(kyc));

        mockMvc.perform(get("/api/users/internal/kyc/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void approveKyc_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        KycDetails kyc = new KycDetails();
        kyc.setUserId(userId);
        kyc.setStatus("APPROVED");
        kyc.setEmail("test@example.com");
        
        when(userService.updateKycStatus(eq(userId), eq("APPROVED"), any())).thenReturn(kyc);

        mockMvc.perform(post("/api/users/internal/kyc/" + userId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.message").value("Internal Approval Success"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void rejectKyc_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        KycDetails kyc = new KycDetails();
        kyc.setUserId(userId);
        kyc.setStatus("REJECTED");
        kyc.setEmail("test@example.com");
        
        when(userService.updateKycStatus(eq(userId), eq("REJECTED"), anyString())).thenReturn(kyc);

        mockMvc.perform(post("/api/users/internal/kyc/" + userId + "/reject")
                        .param("reason", "Incomplete documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Internal Rejection Success"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserInternal_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("internal@test.com");

        when(userService.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/internal/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("internal@test.com"));
    }

    @Test
    void getUserInternal_NotFound() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/internal/" + userId))
                .andExpect(status().isNotFound());
    }
}
