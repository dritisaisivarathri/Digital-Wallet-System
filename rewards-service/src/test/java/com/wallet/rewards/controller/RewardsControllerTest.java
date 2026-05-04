package com.wallet.rewards.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.rewards.entity.RewardCatalog;
import com.wallet.rewards.entity.RewardPoints;
import com.wallet.rewards.dto.RewardRedeemResponse;
import com.wallet.rewards.service.RewardsService;
import com.wallet.rewards.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RewardsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RewardsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RewardsService rewardsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getSummary_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        RewardPoints points = new RewardPoints();
        points.setUserId(userId);
        points.setTotalPoints(100);

        when(jwtUtil.extractUserId(anyString())).thenReturn(userId.toString());
        when(rewardsService.getSummary(userId)).thenReturn(points);

        mockMvc.perform(get("/api/rewards/summary")
                .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(100));
    }

    @Test
    void getCatalog_Success() throws Exception {
        RewardCatalog item = new RewardCatalog();
        item.setName("Gift Card");
        
        when(rewardsService.getCatalog()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/rewards/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gift Card"));
    }

    @Test
    void redeem_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        RewardRedeemResponse response = new RewardRedeemResponse();
        response.setRewardName("Amazon Voucher");
        response.setRewardType("VOUCHER");
        response.setRemainingStock(9);
        
        when(jwtUtil.extractUserId(anyString())).thenReturn(userId.toString());
        when(rewardsService.redeemItem(userId, catalogId)).thenReturn(response);

        mockMvc.perform(post("/api/rewards/redeem/" + catalogId)
                .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardName").value("Amazon Voucher"))
                .andExpect(jsonPath("$.rewardType").value("VOUCHER"))
                .andExpect(jsonPath("$.remainingStock").value(9));
    }

    @Test
    void redeem_CashbackSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        RewardRedeemResponse response = new RewardRedeemResponse();
        response.setRewardName("Cashback Rs 100");
        response.setRewardType("CASHBACK");
        response.setWalletBalance(new BigDecimal("450.00"));
        response.setCashbackCredited(new BigDecimal("100.00"));

        when(jwtUtil.extractUserId(anyString())).thenReturn(userId.toString());
        when(rewardsService.redeemItem(userId, catalogId)).thenReturn(response);

        mockMvc.perform(post("/api/rewards/redeem/" + catalogId)
                .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardType").value("CASHBACK"))
                .andExpect(jsonPath("$.walletBalance").value(450.00))
                .andExpect(jsonPath("$.cashbackCredited").value(100.00));
    }

    @Test
    void redeem_Failure() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        
        when(jwtUtil.extractUserId(anyString())).thenReturn(userId.toString());
        when(rewardsService.redeemItem(any(), any())).thenThrow(new RuntimeException("Insufficient points"));

        mockMvc.perform(post("/api/rewards/redeem/" + catalogId)
                .header("Authorization", "Bearer testToken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient points"));
    }
}
