package com.wallet.user.controller;

import com.wallet.user.entity.KycDetails;
import com.wallet.user.service.UserService;
import com.wallet.user.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void submitKyc_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf".getBytes());
        
        when(jwtUtil.extractUserId(anyString())).thenReturn(userId.toString());
        when(jwtUtil.extractEmail(anyString())).thenReturn("test@example.com");
        when(userService.submitKyc(any(), any(), anyString(), anyString(), any())).thenReturn("KYC details submitted successfully");

        mockMvc.perform(multipart("/api/users/kyc")
                .file(file)
                .param("documentType", "PASSPORT")
                .param("documentNumber", "ABC12345")
                .header("Authorization", "Bearer testToken")
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string("KYC details submitted successfully"));
    }

    @Test
    void getKycStatus_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        KycDetails kyc = new KycDetails();
        kyc.setUserId(userId);
        kyc.setStatus("PENDING");

        when(jwtUtil.extractUserId(anyString())).thenReturn(userId.toString());
        when(userService.getKycStatus(any(), any(), any())).thenReturn(kyc);

        mockMvc.perform(get("/api/users/kyc/status")
                .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void submitKyc_ValidationError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf".getBytes());
        
        when(jwtUtil.extractUserId(anyString())).thenReturn(UUID.randomUUID().toString());
        when(userService.submitKyc(any(), any(), anyString(), anyString(), any())).thenThrow(new RuntimeException("KYC is already approved"));

        mockMvc.perform(multipart("/api/users/kyc")
                .file(file)
                .param("documentType", "PASSPORT")
                .param("documentNumber", "ABC12345")
                .header("Authorization", "Bearer testToken")
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error: KYC is already approved"));
    }

    @Test
    void submitKyc_SystemError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf".getBytes());
        
        when(jwtUtil.extractUserId(anyString())).thenReturn(UUID.randomUUID().toString());
        when(userService.submitKyc(any(), any(), anyString(), anyString(), any())).thenThrow(new RuntimeException("Database down"));

        mockMvc.perform(multipart("/api/users/kyc")
                .file(file)
                .param("documentType", "PASSPORT")
                .param("documentNumber", "ABC12345")
                .header("Authorization", "Bearer testToken")
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error: Database down"));
    }

    @Test
    void getKycStatus_Failure() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(UUID.randomUUID().toString());
        when(userService.getKycStatus(any(), any(), any())).thenThrow(new RuntimeException("KYC details not found"));

        mockMvc.perform(get("/api/users/kyc/status")
                .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_SUBMITTED"))
                .andExpect(jsonPath("$.rejectionReason").value(""))
                .andExpect(jsonPath("$.documentType").value(""))
                .andExpect(jsonPath("$.documentNumber").value(""))
                .andExpect(jsonPath("$.documentUrl").value(""));
    }

    @Test
    void getKycStatus_SystemError() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(UUID.randomUUID().toString());
        when(userService.getKycStatus(any(), any(), any())).thenThrow(new RuntimeException("Database down"));

        mockMvc.perform(get("/api/users/kyc/status")
                .header("Authorization", "Bearer testToken"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Error: Database down"));
    }

    @Test
    void getKycStatus_InvalidToken() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/users/kyc/status")
                .header("Authorization", "Bearer invalidToken"))
                .andExpect(status().isUnauthorized());
    }

}
