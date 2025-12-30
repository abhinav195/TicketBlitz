package com.ticketblitz.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketblitz.notification.client.EventClient;
import com.ticketblitz.notification.event.RecommendationReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.url}")
    private String apiUrl;

    private final RestClient restClient = RestClient.create();
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final EventClient eventClient;

    @Async
    public void generateAndNotify(String username, String email, Long lastEventId) {
        log.info("🤖 AI Analysis started for user: {}", username);

        try {
            // 1. FETCH ALL EVENTS
            List<EventClient.EventDto> allEvents = eventClient.getAllEvents();

            if (allEvents.isEmpty()) {
                log.warn("Event Service returned no events. Skipping recommendation.");
                return;
            }

            // 2. IDENTIFY LAST BOOKED EVENT
            EventClient.EventDto lastBooked = allEvents.stream()
                    .filter(e -> e.getId().equals(lastEventId))
                    .findFirst()
                    .orElse(new EventClient.EventDto());

            String lastTitle = lastBooked.getTitle() != null ? lastBooked.getTitle() : "General Admission";
            String lastCategory = lastBooked.getCategory() != null ? lastBooked.getCategory() : "Entertainment";

            // 3. PREPARE PROMPT DATA
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            String candidateEventsList = allEvents.stream()
                    .map(e -> String.format("[%d] | %s | %s | %s | %s",
                            e.getId(), e.getDate(), e.getTitle(), e.getCategory(), e.getDescription()))
                    .collect(Collectors.joining("\n"));

            String prompt = String.format("""
                You are a smart Event Recommendation Engine for 'TicketBlitz'.
                ### CONTEXT
                The user recently booked the following event:
                - Title: %s
                - Category: %s
                
                ### SYSTEM DATA
                - Current Date & Time: %s
                
                ### CANDIDATE EVENTS (Database Dump)
                Format: [ID] | [Date] | [Title] | [Category] | [Description]
                --------------------------------------------------------------
                %s
                --------------------------------------------------------------
                
                ### INSTRUCTIONS
                1. Select the Top 3 recommendations based on semantic similarity to the user's last booked event.
                2. CRITICAL: ONLY recommend events where 'Date' is strictly AFTER 'Current Date & Time'.
                3. If fewer than 3 future events match, fill with popular future events.
                
                ### OUTPUT FORMAT
                Return ONLY a raw JSON array of integers representing the Event IDs. Do not include markdown.
                Example Output: [101, 25, 8]
                """,
                    lastTitle, lastCategory, currentDateTime, candidateEventsList
            );

            // 4. CALL GEMINI
            String rawJson = callGeminiApi(prompt);
            log.info("🤖 Gemini Raw Response: {}", rawJson);

            // 5. PARSE RESPONSE
            JsonNode recommendedIds = parseGeminiResponse(rawJson);

            // 6. BUILD EMAIL CONTENT
            StringBuilder emailContent = new StringBuilder();
            if (recommendedIds != null && recommendedIds.isArray()) {
                for (JsonNode idNode : recommendedIds) {
                    Long recId = idNode.asLong();
                    allEvents.stream()
                            .filter(e -> e.getId().equals(recId))
                            .findFirst()
                            .ifPresent(e -> emailContent.append("• ").append(e.getTitle())
                                    .append(" (").append(e.getDate()).append(")\n"));
                }
            }

            // 7. PUBLISH EVENT
            String finalRecs = emailContent.length() > 0 ? emailContent.toString() : "Check our app for more upcoming events!";
            try {
                eventPublisher.publishEvent(new RecommendationReadyEvent(this, email, username, finalRecs));
            } catch (Exception ex) {
                log.error("Failed to publish RecommendationReadyEvent", ex);
            }
        } catch (Exception e) {
            log.error("AI Recommendation Logic Failed", e);
        }
    }

    private String callGeminiApi(String prompt) {
        var requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        log.info("🚀 Sending Request to Gemini: {}", apiUrl);
        // Log the prompt to ensure it's not empty
        log.debug("Prompt: {}", prompt);

        try {
            // Use ResponseEntity to inspect status code
            var responseEntity = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class); // Get full entity

            log.info("📩 Gemini Response Status: {}", responseEntity.getStatusCode());
            String responseBody = responseEntity.getBody();
            log.info("📩 Gemini Raw Body: {}", responseBody); // <--- CRITICAL LOG

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("Gemini returned empty body");
            }

            JsonNode root = objectMapper.readTree(responseBody);

            // Check for error payload (Gemini returns 200 OK but with error field sometimes?)
            // Actually Gemini returns 4xx for errors usually.

            // Safe extraction
            if (!root.path("candidates").isArray() || root.path("candidates").isEmpty()) {
                log.error("❌ No candidates found in Gemini response! Safety block?");
                // Check prompt feedback
                if (root.has("promptFeedback")) {
                    log.error("Prompt Feedback: {}", root.get("promptFeedback"));
                }
                return "[]"; // Return empty JSON array string
            }

            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().trim();

            return text;

        } catch (Exception e) {
            log.error("❌ Failed to call Gemini API", e);
            throw new RuntimeException("Gemini API Failure", e);
        }
    }


    private JsonNode parseGeminiResponse(String rawJson) {
        try {
            String cleanJson = rawJson.replace("``````", "").trim();
            // Handle cases where Gemini might return [1, 2, 3] inside text
            int startIndex = cleanJson.indexOf("[");
            int endIndex = cleanJson.lastIndexOf("]");
            if (startIndex != -1 && endIndex != -1) {
                cleanJson = cleanJson.substring(startIndex, endIndex + 1);
            }
            return objectMapper.readTree(cleanJson);
        } catch (Exception e) {
            log.error("Failed to parse IDs from: {}", rawJson);
            return null;
        }
    }
}
