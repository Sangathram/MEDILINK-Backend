package com.medilink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilink.dto.AiChatResponse;
import com.medilink.entity.PatientProfile;
import com.medilink.entity.User;
import com.medilink.repository.PatientProfileRepository;
import com.medilink.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Service
public class AiHealthService {

    private final UserRepository userRepository;

    private final PatientProfileRepository patientProfileRepository;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    public AiHealthService(
            UserRepository userRepository,
            PatientProfileRepository patientProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.patientProfileRepository =
                patientProfileRepository;
        this.objectMapper = objectMapper;
    }

    public AiChatResponse chat(
            String email,
            String patientMessage
    ) throws Exception {

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException(
                    "Patient account not found."
            );
        }

        User user = optionalUser.get();

        if (!"PATIENT".equals(user.getRole())) {
            throw new RuntimeException(
                    "Only patients can use the health assistant."
            );
        }

        Optional<PatientProfile> optionalProfile =
                patientProfileRepository.findByUserId(
                        user.getId()
                );

        PatientProfile profile =
                optionalProfile.orElse(null);

        String context =
                buildPatientContext(profile);

        boolean emergency =
                detectEmergency(patientMessage);

        String language =
                detectLanguage(patientMessage);

        String apiKey =
                System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {

            return new AiChatResponse(
                    buildFallbackResponse(
                            patientMessage,
                            emergency,
                            language
                    ),
                    emergency,
                    language
            );
        }

        String instructions = buildInstructions(
                context,
                emergency,
                language
        );

        String requestBody =
                objectMapper.writeValueAsString(
                        java.util.Map.of(
                                "model",
                                "gpt-5.6-luna",

                                "store",
                                false,

                                "instructions",
                                instructions,

                                "input",
                                patientMessage
                        )
                );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        "https://api.openai.com/v1/responses"
                                )
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "Authorization",
                                "Bearer " + apiKey
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(requestBody)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new RuntimeException(
                    "AI service error: "
                            + response.body()
            );
        }

        JsonNode root =
                objectMapper.readTree(
                        response.body()
                );

        String aiText =
                extractOutputText(root);

        if (aiText == null || aiText.isBlank()) {

            throw new RuntimeException(
                    "AI returned an empty response."
            );
        }

        return new AiChatResponse(
                aiText,
                emergency,
                language
        );
    }

    private String buildPatientContext(
            PatientProfile profile
    ) {

        if (profile == null) {
            return "No patient profile is available.";
        }

        return
                "Patient profile:\n"
                + "Name: "
                + safe(profile.getName())
                + "\nAge: "
                + safe(profile.getAge())
                + "\nGender: "
                + safe(profile.getGender())
                + "\nBlood group: "
                + safe(profile.getBloodGroup())
                + "\nLanguages known: "
                + safe(profile.getLanguagesKnown())
                + "\nPrimary language: "
                + safe(profile.getPreferredLanguage())
                + "\nHealth conditions: "
                + safe(profile.getHealthConditions())
                + "\nAllergies: "
                + safe(profile.getAllergies())
                + "\nCurrent medications: "
                + safe(profile.getCurrentMedications());
    }

    private String buildInstructions(
            String context,
            boolean emergency,
            String language
    ) {

        return """
                You are MEDILINK Health Assistant.

                You are a health information and triage
                assistant, not a doctor.

                Patient context:
                %s

                Detected language:
                %s

                Emergency/red-flag detected:
                %s

                Rules:

                1. Communicate naturally in the detected
                   language when possible.

                2. If the patient profile contains a
                   primary language, prefer that language
                   when appropriate.

                3. Understand English, Telugu and Hindi.

                4. Ask useful follow-up questions when
                   important information is missing.

                5. Consider the patient's known allergies,
                   existing health conditions and current
                   medications when responding.

                6. Never invent a diagnosis.

                7. Do not present a medicine or dose as a
                   guaranteed prescription.

                8. If discussing medicines, provide general
                   information and advise consultation with
                   a qualified healthcare professional when
                   appropriate.

                9. If serious warning symptoms are present,
                   clearly recommend urgent medical care.

                10. Do not tell the patient to ignore severe
                    or worsening symptoms.

                11. Keep normal responses understandable
                    and reasonably concise.

                12. The patient's uploaded medical records
                    are only trusted as patient-provided
                    information. Do not invent information
                    that is not present.

                Provide practical, safe health guidance.
                """.formatted(
                context,
                language,
                emergency
        );
    }

    private String extractOutputText(
            JsonNode root
    ) {

        JsonNode outputText =
                root.get("output_text");

        if (outputText != null
                && outputText.isTextual()) {

            return outputText.asText();
        }

        JsonNode output =
                root.get("output");

        if (output != null && output.isArray()) {

            StringBuilder result =
                    new StringBuilder();

            for (JsonNode item : output) {

                JsonNode content =
                        item.get("content");

                if (content == null
                        || !content.isArray()) {
                    continue;
                }

                for (JsonNode contentItem : content) {

                    JsonNode text =
                            contentItem.get("text");

                    if (text != null
                            && text.isTextual()) {

                        result.append(
                                text.asText()
                        );
                    }
                }
            }

            return result.toString();
        }

        return "";
    }

    private boolean detectEmergency(
            String message
    ) {

        String text =
                message.toLowerCase();

        String[] redFlags = {
                "chest pain",
                "difficulty breathing",
                "can't breathe",
                "cannot breathe",
                "severe bleeding",
                "unconscious",
                "fainted",
                "seizure",
                "stroke",
                "paralysis",
                "suicidal",
                "suicide",
                "blood vomiting",
                "vomiting blood",
                "severe allergic reaction",
                "face swelling",
                "blue lips"
        };

        for (String flag : redFlags) {

            if (text.contains(flag)) {
                return true;
            }
        }

        return false;
    }

    private String detectLanguage(
            String message
    ) {

        String text =
                message.toLowerCase();

        if (containsTeluguCharacters(text)) {
            return "Telugu";
        }

        if (containsHindiCharacters(text)) {
            return "Hindi";
        }

        return "English";
    }

    private boolean containsTeluguCharacters(
            String text
    ) {

        for (char c : text.toCharArray()) {

            if (c >= '\u0C00'
                    && c <= '\u0C7F') {

                return true;
            }
        }

        return false;
    }

    private boolean containsHindiCharacters(
            String text
    ) {

        for (char c : text.toCharArray()) {

            if (c >= '\u0900'
                    && c <= '\u097F') {

                return true;
            }
        }

        return false;
    }

    private String buildFallbackResponse(
            String message,
            boolean emergency,
            String language
    ) {

        if (emergency) {

            return """
                    Your message contains symptoms that may
                    require urgent medical attention.

                    Please seek immediate medical care or
                    contact your local emergency service,
                    especially if the symptoms are severe,
                    sudden, or getting worse.

                    MEDILINK cannot safely diagnose or
                    prescribe treatment from this message.
                    """;
        }

        return """
                I can help you understand your symptoms and
                provide general health information.

                Please describe:
                • What symptoms you have
                • When they started
                • How severe they are
                • Any important existing health conditions
                • Any allergies
                • Medicines you currently take

                I will help you identify what information
                may be important and when professional
                medical care may be appropriate.
                """;
    }

    private String safe(Object value) {

        if (value == null) {
            return "Not provided";
        }

        String text =
                String.valueOf(value).trim();

        return text.isEmpty()
                ? "Not provided"
                : text;
    }
}