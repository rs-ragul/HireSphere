package com.hiresphere.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an applicant / candidate profile.
 */
public class Candidate {
    private int candidateId;
    private String fullName;
    private String email;
    private String phone;
    private double totalExpYears;
    private String highestEducation;
    private String resumeSummary;
    private String createdAt;
    private List<CandidateSkill> skills = new ArrayList<>();

    public Candidate() {}

    public Candidate(int candidateId, String fullName, String email, String phone, double totalExpYears, String highestEducation, String resumeSummary) {
        this.candidateId = candidateId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.totalExpYears = totalExpYears;
        this.highestEducation = highestEducation;
        this.resumeSummary = resumeSummary;
    }

    public Candidate(String fullName, String email, String phone, double totalExpYears, String highestEducation, String resumeSummary) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.totalExpYears = totalExpYears;
        this.highestEducation = highestEducation;
        this.resumeSummary = resumeSummary;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getTotalExpYears() {
        return totalExpYears;
    }

    public void setTotalExpYears(double totalExpYears) {
        this.totalExpYears = totalExpYears;
    }

    public String getHighestEducation() {
        return highestEducation;
    }

    public void setHighestEducation(String highestEducation) {
        this.highestEducation = highestEducation;
    }

    public String getResumeSummary() {
        return resumeSummary;
    }

    public void setResumeSummary(String resumeSummary) {
        this.resumeSummary = resumeSummary;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<CandidateSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<CandidateSkill> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public void addSkill(CandidateSkill skill) {
        this.skills.add(skill);
    }

    @Override
    public String toString() {
        return "#" + candidateId + " - " + fullName + " (" + email + ", Exp: " + totalExpYears + " yrs)";
    }
}

