package com.hiresphere.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an open job requisition within the ATS.
 */
public class Job {
    private int jobId;
    private String title;
    private String department;
    private double minExpYears;
    private String minEducation;
    private String status; // OPEN, CLOSED, ON_HOLD
    private String createdAt;
    private List<JobSkill> requiredSkills = new ArrayList<>();

    public Job() {
        this.status = "OPEN";
        this.minEducation = "Bachelor";
    }

    public Job(int jobId, String title, String department, double minExpYears, String minEducation, String status) {
        this.jobId = jobId;
        this.title = title;
        this.department = department;
        this.minExpYears = minExpYears;
        this.minEducation = minEducation;
        this.status = status;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getMinExpYears() {
        return minExpYears;
    }

    public void setMinExpYears(double minExpYears) {
        this.minExpYears = minExpYears;
    }

    public String getMinEducation() {
        return minEducation;
    }

    public void setMinEducation(String minEducation) {
        this.minEducation = minEducation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<JobSkill> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<JobSkill> requiredSkills) {
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
    }

    public void addSkill(JobSkill skill) {
        this.requiredSkills.add(skill);
    }

    @Override
    public String toString() {
        return "#" + jobId + " - " + title + " (" + department + ", Min Exp: " + minExpYears + " yrs)";
    }
}

