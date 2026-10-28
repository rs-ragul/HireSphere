package com.hiresphere.model;

/**
 * Entity representing an application submitted by a candidate for a job requisition.
 */
public class Application {
    private int applicationId;
    private int jobId;
    private String jobTitle;
    private int candidateId;
    private String candidateName;
    private String candidateEmail;
    private String appliedAt;
    private String status; // APPLIED, SCREENED, SHORTLISTED, REJECTED, HIRED
    private double matchScore;
    private double skillCoverageScore;
    private double experienceFactor;
    private double proficiencyFactor;
    private String notes;

    public Application() {
        this.status = "APPLIED";
    }

    public Application(int applicationId, int jobId, int candidateId, String appliedAt, String status, double matchScore) {
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.appliedAt = appliedAt;
        this.status = status;
        this.matchScore = matchScore;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public String getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(String appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "App #" + applicationId + " [Job: " + jobId + ", Cand: " + candidateId + ", Score: " + matchScore + "%, Status: " + status + "]";
    }
}

