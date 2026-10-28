package com.hiresphere.service;

import com.hiresphere.analytics.SkillMatchEngine;
import com.hiresphere.dao.AnalyticsDAO;
import com.hiresphere.dao.ApplicationDAO;
import com.hiresphere.dao.CandidateDAO;
import com.hiresphere.dao.JobDAO;
import com.hiresphere.model.Application;
import com.hiresphere.model.Candidate;
import com.hiresphere.model.Job;
import com.hiresphere.model.MatchResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service managing ATS matching computations, candidate rank sorting,
 * gap diagnosis, and high-level recruiter metrics.
 */
public class AnalyticsService {

    private final JobDAO jobDAO = new JobDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();

    public MatchResult evaluateMatch(int jobId, int candidateId) throws SQLException {
        Job job = jobDAO.getJobById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job requisition #" + jobId + " does not exist.");
        }

        Candidate candidate = candidateDAO.getCandidateById(candidateId);
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate profile #" + candidateId + " does not exist.");
        }

        return SkillMatchEngine.evaluate(job, candidate);
    }

    public int submitApplication(int jobId, int candidateId, String notes) throws SQLException {
        MatchResult match = evaluateMatch(jobId, candidateId);
        return applicationDAO.applyForJob(jobId, candidateId, match, notes);
    }

    /**
     * Batch evaluates all registered candidates against a job and registers applications
     * with their dynamic match scores.
     */
    public int batchScreenAllCandidatesForJob(int jobId) throws SQLException {
        Job job = jobDAO.getJobById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job requisition #" + jobId + " does not exist.");
        }

        List<Candidate> candidates = candidateDAO.getAllCandidates();
        int screenedCount = 0;

        for (Candidate c : candidates) {
            MatchResult match = SkillMatchEngine.evaluate(job, c);
            String initialNote = "Auto-screened via ATS Engine. Computed Tier: " + match.getMatchTier();
            applicationDAO.applyForJob(jobId, c.getCandidateId(), match, initialNote);
            screenedCount++;
        }
        return screenedCount;
    }

    public List<Application> getRankedApplicantsForJob(int jobId) throws SQLException {
        return applicationDAO.getApplicationsForJob(jobId);
    }

    public void updateApplicationStatus(int applicationId, String status) throws SQLException {
        applicationDAO.updateApplicationStatus(applicationId, status);
    }

    public List<Application> getAllApplications() throws SQLException {
        return applicationDAO.getAllApplications();
    }

    public Map<String, Object> getRecruitmentFunnelSummary() throws SQLException {
        return analyticsDAO.getRecruitmentFunnelSummary();
    }

    public Map<String, Integer> getSkillDemandMatrix() throws SQLException {
        return analyticsDAO.getSkillDemandMatrix();
    }

    public List<Map<String, Object>> getApplicationStatusBreakdown() throws SQLException {
        return analyticsDAO.getApplicationStatusBreakdown();
    }

    public void addApplicationNote(int applicationId, String author, String noteText, int rating) throws SQLException {
        applicationDAO.addApplicationNote(applicationId, author, noteText, rating);
    }

    public List<Map<String, Object>> getApplicationNotes(int applicationId) throws SQLException {
        return applicationDAO.getApplicationNotes(applicationId);
    }
}

