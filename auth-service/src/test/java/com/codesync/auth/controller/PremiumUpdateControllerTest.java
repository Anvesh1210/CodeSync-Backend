package com.codesync.auth.controller;

import com.codesync.auth.dto.request.PremiumUpdateRequest;
import com.codesync.auth.dto.response.UserResponse;
import com.codesync.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.codesync.auth.security.JwtService;
import com.codesync.auth.security.TokenStateService;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PremiumUpdateController.class)
@AutoConfigureMockMvc(addFilters = false)
class PremiumUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private TokenStateService tokenStateService;

    @Test
    void updatePremiumStatusShouldReturnOk() throws Exception {
        UUID userId = UUID.randomUUID();
        PremiumUpdateRequest request = new PremiumUpdateRequest(userId, true, "PRO", LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        UserResponse response = UserResponse.builder().userId(userId).premium(true).planType("PRO").build();

        when(authService.updatePremiumStatus(any(PremiumUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/premium/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(true))
                .andExpect(jsonPath("$.planType").value("PRO"));
    }
}
