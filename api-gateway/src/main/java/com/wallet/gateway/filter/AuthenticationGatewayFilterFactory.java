package com.wallet.gateway.filter;

import com.wallet.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationGatewayFilterFactory.class);

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            logger.info("[GATEWAY] Processing path: {}", path);

            if (config.isAuthRequired(path)) {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    logger.warn("[GATEWAY] Missing Authorization header for path: {}", path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                var authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
                if (authHeaders == null || authHeaders.isEmpty()) {
                    logger.warn("[GATEWAY] Empty Authorization header for path: {}", path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String authHeader = authHeaders.get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                } else {
                    logger.warn("[GATEWAY] Invalid Authorization format for path: {}", path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                if (authHeader.isBlank()) {
                    logger.warn("[GATEWAY] Blank JWT token for path: {}", path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                try {
                    jwtUtil.validateToken(authHeader);
                } catch (Exception e) {
                    logger.error("[GATEWAY] JWT Validation FAILED for path: {} | Error: {}", path, e.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }
            
            return chain.filter(exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-Internal-Gateway-Token", "DigitalWalletInternalSecret2026")
                            .build())
                    .build());
        });
    }

    public static class Config {
        public boolean isAuthRequired(String path) {
            return !path.contains("/api/auth/signup") && !path.contains("/api/auth/login")
                && !path.contains("/api/auth/refresh-token")
                && !path.contains("/api/auth/forgot-password")
                && !path.contains("/api/auth/reset-password")
                && !path.contains("/api/auth/send-otp")
                && !path.contains("/api/auth/verify-otp")
                && !path.contains("/api/users/uploads/")
                && !path.contains("/v3/api-docs") && !path.contains("/swagger-ui") 
                && !path.contains("/webjars") && !path.contains("/actuator");
        }
    }
}
