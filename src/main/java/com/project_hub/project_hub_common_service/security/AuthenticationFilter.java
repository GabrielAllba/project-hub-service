package com.project_hub.project_hub_common_service.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_hub.project_hub_common_service.infrastructure.grpc.ProjectHubAccountServiceGrpcClient;
import com.project_hub.project_hub_common_service.misc.ApiResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import projecthubaccount.ProjectHubAccountServiceOuterClass.ValidateTokenResponse;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private final ProjectHubAccountServiceGrpcClient grpcClient;

    public AuthenticationFilter(ProjectHubAccountServiceGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader;

        try {
            ValidateTokenResponse validateResponse = grpcClient.validate(token);

            String userId = validateResponse.getId(); 

            if (userId == null || userId.isEmpty()) {
                writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token: userId missing");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null,
                    List.of());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        }catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().name().equals("UNAUTHENTICATED")) {
                writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token tidak valid atau sudah kedaluwarsa.");
            } else if (e.getStatus().getCode().name().equals("UNAVAILABLE")) {
                writeErrorResponse(response, HttpStatus.SERVICE_UNAVAILABLE, "Layanan otentikasi sedang tidak tersedia.");
            } else {
                writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan autentikasi: " + e.getMessage());
            }
        } catch (Exception e) {
            writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Kesalahan internal: " + e.getMessage());
        }
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");

        ApiResponse<Object> apiResponse = new ApiResponse<>(String.valueOf(status.value()), message, null);

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(apiResponse));
    }

}
