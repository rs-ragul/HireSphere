package com.hiresphere.dao;

import com.hiresphere.config.DatabaseConfig;
import com.hiresphere.model.Application;
import com.hiresphere.model.MatchResult;
import com.hiresphere.model.SkillGap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for job applications and analytics audit logs.
 * Demonstrates transactional atomicity when submitting applications and persisting analytics.
 */
public class ApplicationDAO {

    public int applyForJob(int jobId, int candidateId, MatchResult matchResult, String notes) throws SQLException {
        String checkExistingSql = "SELECT application_id FROM applications WHERE job_id = ? AND candidate_id = ?";
        String updateAppSql = "UPDATE applications SET match_score = ?, skill_coverage_score = ?, experience_factor = ?, proficiency_factor = ?, notes = ? WHERE application_id = ?";
        String insertAppSql = "INSERT INTO applications (job_id, candidate_id, match_score, skill_coverage_score, experience_factor, proficiency_factor, notes, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'APPLIED')";
        String deleteAuditSql = "DELETE FROM match_audit_logs WHERE application_id = ?";
        String insertAuditSql = "INSERT INTO match_audit_logs (application_id, missing_mandatory_skills, gap_summary) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Transaction begin

            int targetAppId = -1;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkExistingSql)) {
                checkStmt.setInt(1, jobId);
                checkStmt.setInt(2, candidateId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        targetAppId = rs.getInt(1);
                    }
                }
            }

            if (targetAppId > 0) {
                // Update existing application
                try (PreparedStatement upStmt = conn.prepareStatement(updateAppSql)) {
                    upStmt.setDouble(1, matchResult.getCompositeScore());
                    upStmt.setDouble(2, matchResult.getSkillCoverageScore());
                    upStmt.setDouble(3, matchResult.getExperienceFactor());
                    upStmt.setDouble(4, matchResult.getProficiencyFactor());
                    upStmt.setString(5, notes);
                    upStmt.setInt(6, targetAppId);
                    upStmt.executeUpdate();
                }
                try (PreparedStatement delAudit = conn.prepareStatement(deleteAuditSql)) {
                    delAudit.setInt(1, targetAppId);
                    delAudit.executeUpdate();
                }
            } else {
                // Insert new application
                try (PreparedStatement appStmt = conn.prepareStatement(insertAppSql, Statement.RETURN_GENERATED_KEYS)) {
                    appStmt.setInt(1, jobId);
                    appStmt.setInt(2, candidateId);
                    appStmt.setDouble(3, matchResult.getCompositeScore());
                    appStmt.setDouble(4, matchResult.getSkillCoverageScore());
                    appStmt.setDouble(5, matchResult.getExperienceFactor());
                    appStmt.setDouble(6, matchResult.getProficiencyFactor());
                    appStmt.setString(7, notes);
                    appStmt.executeUpdate();

                    try (ResultSet keys = appStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            targetAppId = keys.getInt(1);
                        }
                    }
                }
            }

            if (targetAppId <= 0) {
                throw new SQLException("Failed to retrieve or generate application ID.");
            }

            // Record audit log
            StringBuilder missingMandatory = new StringBuilder();
            for (SkillGap g : matchResult.getSkillGaps()) {
                if (g.getGapType() == SkillGap.GapType.MISSING_MANDATORY_SKILL) {
                    if (missingMandatory.length() > 0) missingMandatory.append(", ");
                    missingMandatory.append(g.getSkillName());
                }
            }

            try (PreparedStatement auditStmt = conn.prepareStatement(insertAuditSql)) {
                auditStmt.setInt(1, targetAppId);
                auditStmt.setString(2, missingMandatory.toString());
                auditStmt.setString(3, matchResult.getGapsSummary());
                auditStmt.executeUpdate();
            }

            conn.commit(); // Transaction commit
            return targetAppId;

        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rbEx) {
                    System.err.println("[ERROR] Transaction rollback failed: " + rbEx.getMessage());
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    public boolean hasAlreadyApplied(int jobId, int candidateId) throws SQLException {
        String sql = "SELECT application_id FROM applications WHERE job_id = ? AND candidate_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            stmt.setInt(2, candidateId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Application> getApplicationsForJob(int jobId) throws SQLException {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.application_id, a.job_id, j.title AS job_title, a.candidate_id, c.full_name AS candidate_name, " +
                "c.email AS candidate_email, a.applied_at, a.status, a.match_score, a.skill_coverage_score, " +
                "a.experience_factor, a.proficiency_factor, a.notes " +
                "FROM applications a " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "JOIN candidates c ON a.candidate_id = c.candidate_id " +
                "WHERE a.job_id = ? " +
                "ORDER BY a.match_score DESC, a.applied_at ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Application app = new Application();
                    app.setApplicationId(rs.getInt("application_id"));
                    app.setJobId(rs.getInt("job_id"));
                    app.setJobTitle(rs.getString("job_title"));
                    app.setCandidateId(rs.getInt("candidate_id"));
                    app.setCandidateName(rs.getString("candidate_name"));
                    app.setCandidateEmail(rs.getString("candidate_email"));
                    app.setAppliedAt(rs.getString("applied_at"));
                    app.setStatus(rs.getString("status"));
                    app.setMatchScore(rs.getDouble("match_score"));
                    app.setSkillCoverageScore(rs.getDouble("skill_coverage_score"));
                    app.setExperienceFactor(rs.getDouble("experience_factor"));
                    app.setProficiencyFactor(rs.getDouble("proficiency_factor"));
                    app.setNotes(rs.getString("notes"));
                    list.add(app);
                }
            }
        }
        return list;
    }

    public void updateApplicationStatus(int applicationId, String status) throws SQLException {
        String sql = "UPDATE applications SET status = ? WHERE application_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.toUpperCase());
            stmt.setInt(2, applicationId);
            stmt.executeUpdate();
        }
    }

    public List<Application> getAllApplications() throws SQLException {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.application_id, a.job_id, j.title AS job_title, a.candidate_id, c.full_name AS candidate_name, " +
                "c.email AS candidate_email, a.applied_at, a.status, a.match_score, a.skill_coverage_score, " +
                "a.experience_factor, a.proficiency_factor, a.notes " +
                "FROM applications a " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "JOIN candidates c ON a.candidate_id = c.candidate_id " +
                "ORDER BY a.application_id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Application app = new Application();
                app.setApplicationId(rs.getInt("application_id"));
                app.setJobId(rs.getInt("job_id"));
                app.setJobTitle(rs.getString("job_title"));
                app.setCandidateId(rs.getInt("candidate_id"));
                app.setCandidateName(rs.getString("candidate_name"));
                app.setCandidateEmail(rs.getString("candidate_email"));
                app.setAppliedAt(rs.getString("applied_at"));
                app.setStatus(rs.getString("status"));
                app.setMatchScore(rs.getDouble("match_score"));
                app.setSkillCoverageScore(rs.getDouble("skill_coverage_score"));
                app.setExperienceFactor(rs.getDouble("experience_factor"));
                app.setProficiencyFactor(rs.getDouble("proficiency_factor"));
                app.setNotes(rs.getString("notes"));
                list.add(app);
            }
        }
        return list;
    }

    public void addApplicationNote(int applicationId, String author, String noteText, int rating) throws SQLException {
        String sql = "INSERT INTO application_notes (application_id, author, note_text, rating) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            stmt.setString(2, author);
            stmt.setString(3, noteText);
            stmt.setInt(4, rating);
            stmt.executeUpdate();
        }
    }

    public List<Map<String, Object>> getApplicationNotes(int applicationId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT note_id, author, note_text, rating, created_at FROM application_notes WHERE application_id = ? ORDER BY note_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("noteId", rs.getInt("note_id"));
                    map.put("author", rs.getString("author"));
                    map.put("noteText", rs.getString("note_text"));
                    map.put("rating", rs.getInt("rating"));
                    map.put("createdAt", rs.getString("created_at"));
                    list.add(map);
                }
            }
        }
        return list;
    }
}

