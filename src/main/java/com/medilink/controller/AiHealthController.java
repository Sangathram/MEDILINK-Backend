package com.medilink.controller;

import com.medilink.dto.AiChatRequest;
import com.medilink.dto.AiChatResponse;
import com.medilink.service.AiHealthService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173")
public class AiHealthController {

    private final AiHealthService aiHealthService;

    public AiHealthController(
            AiHealthService aiHealthService
    ) {
        this.aiHealthService = aiHealthService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestBody AiChatRequest request,
            Authentication authentication
    ) {

        try {

            if (request == null
                    || request.getMessage() == null
                    || request.getMessage().isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body("Please enter a message.");
            }

            if (authentication == null) {

                return ResponseEntity
                        .status(401)
                        .body("Authentication required.");
            }

            String email =
                    authentication.getName();

            AiChatResponse response =
                    aiHealthService.chat(
                            email,
                            request.getMessage().trim()
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}