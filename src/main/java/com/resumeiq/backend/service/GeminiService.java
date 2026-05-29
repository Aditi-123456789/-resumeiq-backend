package com.resumeiq.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private long lastCallTime = 0;

    public GeminiService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public String analyze(String resumeText, String jobDescription) {

        // ⛔ basic rate limit protection
        if (System.currentTimeMillis() - lastCallTime < 8000) {
            return fallback("Rate limit hit - wait 8 seconds");
        }

        lastCallTime = System.currentTimeMillis();

        // ⏳ extra delay to reduce 429 error
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String prompt = """
        You are an ATS resume analyzer.

        Return ONLY valid JSON in this format:
        {
          "matchScore": number (0-100),
          "missingSkills": [string],
          "improvements": [string],
          "rewrittenSummary": string,
          "overallFeedback": string
        }

        Resume:
        """ + resumeText + """

        Job Description:
        """ + jobDescription;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        try {
            Map response = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                return fallback("Empty response from Gemini");
            }

            List candidates = (List) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return fallback("No candidates found");
            }

            Map candidate = (Map) candidates.get(0);
            Map content = (Map) candidate.get("content");
            List parts = (List) content.get("parts");
            Map part = (Map) parts.get(0);

            String text = part.get("text").toString();

            return cleanJson(text);

        } catch (Exception e) {
            return fallback("API error: " + e.getMessage());
        }
    }

    private String cleanJson(String text) {
        return text
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private String fallback(String msg) {
        return """
        {
          "matchScore": 0,
          "missingSkills": ["API issue"],
          "improvements": ["%s"],
          "rewrittenSummary": "AI unavailable",
          "overallFeedback": "Backend failed or rate limit reached"
        }
        """.formatted(msg);
    }
}