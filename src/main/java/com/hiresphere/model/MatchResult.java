package com.hiresphere.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the algorithmic evaluation of a candidate profile against a job requirement.
 * Shows detailed breakdown of skill coverage, experience alignment, proficiency scoring,
 * and specific skill gaps.
 */
public class MatchResult {
    private int jobId;
    private int candidateId;
    private double compositeScore; // 0 to 100%
    private double skillCoverageScore; // 0 to 100%
    private double experienceFactor; // 0 to 100%
    private double proficiencyFactor; // 0 to 100%
    private String matchTier; // TIER 1 (>=80%), TIER 2 (60-79%), TIER 3 (<60%)
    private List<SkillGap> skillGaps = new ArrayList<>();
    private List<String> matchedSkills = new ArrayList<>();

    public MatchResult() {}

    public MatchResult(int jobId, int candidateId, double compositeScore, double skillCoverageScore,
                       double experienceFactor, double proficiencyFactor) {
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.compositeScore = compositeScore;
        this.skillCoverageScore = skillCoverageScore;
        this.experienceFactor = experienceFactor;
        this.proficiencyFactor = proficiencyFactor;
        this.matchTier = computeTier(compositeScore);
    }

    public static String computeTier(double score) {
        if (score >= 80.0) return "TIER 1 (Strong Match)";
        if (score >= 60.0) return "TIER 2 (Moderate Match)";
        return "TIER 3 (Low Match / Underqualified)";
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public double getCompositeScore() {
        return compositeScore;
    }

    public void setCompositeScore(double compositeScore) {
        this.compositeScore = compositeScore;
        this.matchTier = computeTier(compositeScore);
    }

    public double getSkillCoverageScore() {
        return skillCoverageScore;
    }

    public void setSkillCoverageScore(double skillCoverageScore) {
        this.skillCoverageScore = skillCoverageScore;
    }

    public double getExperienceFactor() {
        return experienceFactor;
    }

    public void setExperienceFactor(double experienceFactor) {
        this.experienceFactor = experienceFactor;
    }

    public double getProficiencyFactor() {
        return proficiencyFactor;
    }

    public void setProficiencyFactor(double proficiencyFactor) {
        this.proficiencyFactor = proficiencyFactor;
    }

    public String getMatchTier() {
        return matchTier;
    }

    public List<SkillGap> getSkillGaps() {
        return skillGaps;
    }

    public void setSkillGaps(List<SkillGap> skillGaps) {
        this.skillGaps = skillGaps != null ? skillGaps : new ArrayList<>();
    }

    public void addSkillGap(SkillGap gap) {
        this.skillGaps.add(gap);
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills != null ? matchedSkills : new ArrayList<>();
    }

    public void addMatchedSkill(String skill) {
        this.matchedSkills.add(skill);
    }

    public String getGapsSummary() {
        if (skillGaps.isEmpty()) {
            return "No critical gaps identified. High alignment.";
        }
        StringBuilder sb = new StringBuilder();
        for (SkillGap g : skillGaps) {
            sb.append("• ").append(g.getDescription()).append("\n");
        }
        return sb.toString().trim();
    }
}

