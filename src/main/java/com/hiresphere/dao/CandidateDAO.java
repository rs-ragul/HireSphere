package com.hiresphere.dao;

import com.hiresphere.config.DatabaseConfig;
import com.hiresphere.model.Candidate;
import com.hiresphere.model.CandidateSkill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Candidate profiles and Candidate skills.
 * Showcases JDBC batch processing and transaction rollback handling.
 */
public class CandidateDAO {

    public int createCandidate(Candidate candidate, List<CandidateSkill> skills) throws SQLException {
        String insertCandSql = "INSERT INTO candidates (full_name, email, phone, total_exp_years, highest_education, resume_summary) VALUES (?, ?, ?, ?, ?, ?)";
        String insertSkillSql = "INSERT INTO candidate_skills (candidate_id, skill_id, proficiency_level, years_experience) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            int generatedCandId = -1;
            try (PreparedStatement candStmt = conn.prepareStatement(insertCandSql, Statement.RETURN_GENERATED_KEYS)) {
                candStmt.setString(1, candidate.getFullName());
                candStmt.setString(2, candidate.getEmail());
                candStmt.setString(3, candidate.getPhone());
                candStmt.setDouble(4, candidate.getTotalExpYears());
                candStmt.setString(5, candidate.getHighestEducation());
                candStmt.setString(6, candidate.getResumeSummary());
                candStmt.executeUpdate();

                try (ResultSet keys = candStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedCandId = keys.getInt(1);
                    }
                }
            }

            if (generatedCandId == -1) {
                throw new SQLException("Failed to retrieve generated candidate ID.");
            }

            if (skills != null && !skills.isEmpty()) {
                try (PreparedStatement skillStmt = conn.prepareStatement(insertSkillSql)) {
                    for (CandidateSkill cs : skills) {
                        skillStmt.setInt(1, generatedCandId);
                        skillStmt.setInt(2, cs.getSkillId());
                        skillStmt.setInt(3, cs.getProficiencyLevel());
                        skillStmt.setDouble(4, cs.getYearsExperience());
                        skillStmt.addBatch();
                    }
                    skillStmt.executeBatch();
                }
            }

            conn.commit(); // Commit Transaction
            candidate.setCandidateId(generatedCandId);
            return generatedCandId;

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

    public List<Candidate> getAllCandidates() throws SQLException {
        List<Candidate> candidates = new ArrayList<>();
        String sql = "SELECT candidate_id, full_name, email, phone, total_exp_years, highest_education, resume_summary, created_at FROM candidates ORDER BY candidate_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Candidate c = new Candidate(
                        rs.getInt("candidate_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getDouble("total_exp_years"),
                        rs.getString("highest_education"),
                        rs.getString("resume_summary")
                );
                c.setCreatedAt(rs.getString("created_at"));
                candidates.add(c);
            }
        }

        for (Candidate c : candidates) {
            c.setSkills(getSkillsForCandidate(c.getCandidateId()));
        }
        return candidates;
    }

    public Candidate getCandidateById(int candidateId) throws SQLException {
        String sql = "SELECT candidate_id, full_name, email, phone, total_exp_years, highest_education, resume_summary, created_at FROM candidates WHERE candidate_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, candidateId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Candidate c = new Candidate(
                            rs.getInt("candidate_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getDouble("total_exp_years"),
                            rs.getString("highest_education"),
                            rs.getString("resume_summary")
                    );
                    c.setCreatedAt(rs.getString("created_at"));
                    c.setSkills(getSkillsForCandidate(candidateId));
                    return c;
                }
            }
        }
        return null;
    }

    public Candidate getCandidateByEmail(String email) throws SQLException {
        String sql = "SELECT candidate_id, full_name, email, phone, total_exp_years, highest_education, resume_summary, created_at FROM candidates WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Candidate c = new Candidate(
                            rs.getInt("candidate_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getDouble("total_exp_years"),
                            rs.getString("highest_education"),
                            rs.getString("resume_summary")
                    );
                    c.setCreatedAt(rs.getString("created_at"));
                    c.setSkills(getSkillsForCandidate(c.getCandidateId()));
                    return c;
                }
            }
        }
        return null;
    }

    public List<CandidateSkill> getSkillsForCandidate(int candidateId) throws SQLException {
        List<CandidateSkill> list = new ArrayList<>();
        String sql = "SELECT cs.candidate_id, cs.skill_id, s.skill_name, s.category, cs.proficiency_level, cs.years_experience " +
                "FROM candidate_skills cs " +
                "JOIN skills s ON cs.skill_id = s.skill_id " +
                "WHERE cs.candidate_id = ? " +
                "ORDER BY s.skill_name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, candidateId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new CandidateSkill(
                            rs.getInt("candidate_id"),
                            rs.getInt("skill_id"),
                            rs.getString("skill_name"),
                            rs.getString("category"),
                            rs.getInt("proficiency_level"),
                            rs.getDouble("years_experience")
                    ));
                }
            }
        }
        return list;
    }
}

