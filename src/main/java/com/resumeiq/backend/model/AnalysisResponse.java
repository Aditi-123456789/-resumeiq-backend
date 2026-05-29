package com.resumeiq.backend.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResponse {
    private int matchScore;
    private List<String> missingSkills;
    private List<String> improvements;
    private String rewrittenSummary;
    private String overallFeedback;
}