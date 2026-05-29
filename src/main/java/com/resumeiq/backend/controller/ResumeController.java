package com.resumeiq.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeiq.backend.model.AnalysisResponse;
import com.resumeiq.backend.service.GeminiService;
import com.resumeiq.backend.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResumeController {

    private final PdfService pdfService;
    private final GeminiService geminiService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam("jobDescription") String jobDescription
    ) throws Exception {

        // 1. Extract resume text from PDF
        String resumeText = pdfService.extractText(resume);

        // 2. Call Gemini AI
        String aiResponse = geminiService.analyze(resumeText, jobDescription);

        ObjectMapper mapper = new ObjectMapper();

        try {
            // 3. Try parsing into structured model
            AnalysisResponse response =
                    mapper.readValue(aiResponse, AnalysisResponse.class);

            return ResponseEntity.ok(response);

        } catch (Exception parseError) {

            // 4. SAFE FALLBACK (frontend will NEVER break)
            return ResponseEntity.ok(Map.of(
                    "matchScore", 0,
                    "missingSkills", List.of("AI parsing failed"),
                    "improvements", List.of("Try again after some time"),
                    "rewrittenSummary", "AI response could not be parsed",
                    "overallFeedback", "Backend returned invalid JSON"
            ));
        }
    }

    @GetMapping("/health")
    public String health() {
        return "ResumeIQ Backend is running!";
    }
}