package com.wallet.core.controller;

import com.wallet.core.dto.TopUpRequest;
import com.wallet.core.dto.TransferRequest;
import com.wallet.core.dto.InternalCreditRequest;
import com.wallet.core.service.WalletService;
import com.wallet.core.util.JwtUtil;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WalletController.class);

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/health-check")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Wallet Controller is reachable");
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@Parameter(hidden = true) @RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        BigDecimal balance = walletService.getBalance(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("userId", userId, "balance", balance));
    }

    @PostMapping("/topup")
    public ResponseEntity<?> initiateTopUp(@Parameter(hidden = true) @RequestHeader("Authorization") String token, 
                                           @jakarta.validation.Valid @RequestBody TopUpRequest request) {
        String userId = jwtUtil.extractUserId(token);
        try {
            return ResponseEntity.ok(walletService.topUp(UUID.fromString(userId), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@Parameter(hidden = true) @RequestHeader("Authorization") String token, 
                                           @jakarta.validation.Valid @RequestBody TransferRequest request) {
        String userId = jwtUtil.extractUserId(token);
        try {
            return ResponseEntity.ok(walletService.transfer(UUID.fromString(userId), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdrawMoney(@Parameter(hidden = true) @RequestHeader("Authorization") String token, 
                                           @jakarta.validation.Valid @RequestBody com.wallet.core.dto.WithdrawRequest request) {
        String userId = jwtUtil.extractUserId(token);
        try {
            return ResponseEntity.ok(walletService.withdraw(UUID.fromString(userId), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/internal/credit")
    public ResponseEntity<?> creditWalletInternally(@jakarta.validation.Valid @RequestBody InternalCreditRequest request) {
        logger.info("Internal credit request received for user: {} | amount: {}", request.getUserId(), request.getAmount());
        try {
            BigDecimal balance = walletService.creditFromReward(
                    request.getUserId(),
                    request.getAmount(),
                    request.getSource(),
                    request.getNote()
            );
            return ResponseEntity.ok(Map.of(
                    "userId", request.getUserId(),
                    "balance", balance,
                    "status", "CREDITED"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
