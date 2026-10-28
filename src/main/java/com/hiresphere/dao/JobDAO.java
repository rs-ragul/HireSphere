package com.hiresphere.dao;

import com.hiresphere.config.DatabaseConfig;
import com.hiresphere.model.Job;
import com.hiresphere.model.JobSkill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Jobs and Job Skill requirements.
 * Demonstrates JDBC Transaction management (commit/rollback).
 */
public class JobDAO {

    public int createJob(Job job, List<JobSkill> requiredSkills) throws SQLException {
        String insertJobSql = "INSERT INTO jobs (title, department, min_exp_years, min_education, status) VALUES (?, ?, ?, ?, ?)";
        String insertSkillSql = "INSERT INTO job_required_skills (job_id, skill_id, weight_type, min_proficiency) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Begin JDBC Transaction

            int generatedJobId = -1;
            try (PreparedStatement jobStmt = conn.prepareStatement(insertJobSql, Statement.RETURN_GENERATED_KEYS)) {
                jobStmt.setString(1, job.getTitle());
                jobStmt.setString(2, job.getDepartment());
                jobStmt.setDouble(3, job.getMinExpYears());
                jobStmt.setString(4, job.getMinEducation());
                jobStmt.setString(5, job.getStatus());
                jobStmt.executeUpdate();

                try (ResultSet keys = jobStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedJobId = keys.getInt(1);
                    }
                }
            }

            if (generatedJobId == -1) {
                throw new SQLException("Failed to retrieve generated job ID.");
            }

            if (requiredSkills != null && !requiredSkills.isEmpty()) {
                try (PreparedStatement skillStmt = conn.prepareStatement(insertSkillSql)) {
                    for (JobSkill js : requiredSkills) {
                        skillStmt.setInt(1, generatedJobId);
                        skillStmt.setInt(2, js.getSkillId());
                        skillStmt.setString(3, js.getWeightType().name());
                        skillStmt.setInt(4, js.getMinProficiency());
                        skillStmt.addBatch();
                    }
                    skillStmt.executeBatch();
                }
            }

            conn.commit(); // Commit Transaction
            job.setJobId(generatedJobId);
            return generatedJobId;

        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback Transaction on error
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

    public List<Job> getAllJobs() throws SQLException {
        List<Job> jobs = new ArrayList<>();
        String sql = "SELECT job_id, title, department, min_exp_years, min_education, status, created_at FROM jobs ORDER BY job_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Job job = new Job(
                        rs.getInt("job_id"),
                        rs.getString("title"),
                        rs.getString("department"),
                        rs.getDouble("min_exp_years"),
                        rs.getString("min_education"),
                        rs.getString("status")
                );
                job.setCreatedAt(rs.getString("created_at"));
                jobs.add(job);
            }
        }

        // Populate skills for each job
        for (Job j : jobs) {
            j.setRequiredSkills(getSkillsForJob(j.getJobId()));
        }
        return jobs;
    }

    public Job getJobById(int jobId) throws SQLException {
        String sql = "SELECT job_id, title, department, min_exp_years, min_education, status, created_at FROM jobs WHERE job_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Job job = new Job(
                            rs.getInt("job_id"),
                            rs.getString("title"),
                            rs.getString("department"),
                            rs.getDouble("min_exp_years"),
                            rs.getString("min_education"),
                            rs.getString("status")
                    );
                    job.setCreatedAt(rs.getString("created_at"));
                    job.setRequiredSkills(getSkillsForJob(jobId));
                    return job;
                }
            }
        }
        return null;
    }

    public List<JobSkill> getSkillsForJob(int jobId) throws SQLException {
        List<JobSkill> list = new ArrayList<>();
        String sql = "SELECT jrs.job_id, jrs.skill_id, s.skill_name, s.category, jrs.weight_type, jrs.min_proficiency " +
                "FROM job_required_skills jrs " +
                "JOIN skills s ON jrs.skill_id = s.skill_id " +
                "WHERE jrs.job_id = ? " +
                "ORDER BY " +
                "  CASE jrs.weight_type WHEN 'MANDATORY' THEN 1 WHEN 'PREFERRED' THEN 2 ELSE 3 END, " +
                "  s.skill_name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new JobSkill(
                            rs.getInt("job_id"),
                            rs.getInt("skill_id"),
                            rs.getString("skill_name"),
                            rs.getString("category"),
                            JobSkill.WeightType.fromString(rs.getString("weight_type")),
                            rs.getInt("min_proficiency")
                    ));
                }
            }
        }
        return list;
    }

    public void updateJobStatus(int jobId, String status) throws SQLException {
        String sql = "UPDATE jobs SET status = ? WHERE job_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, jobId);
            stmt.executeUpdate();
        }
    }
}

