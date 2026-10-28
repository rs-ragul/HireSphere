package com.hiresphere.dao;

import com.hiresphere.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object providing aggregate analytics and recruitment metrics.
 * Demonstrates complex SQL aggregates, GROUP BY, and statistical summarization.
 */
public class AnalyticsDAO {

    public Map<String, Object> getRecruitmentFunnelSummary() throws SQLException {
        Map<String, Object> metrics = new HashMap<>();

        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM jobs) AS total_jobs, " +
                "(SELECT COUNT(*) FROM jobs WHERE status = 'OPEN') AS open_jobs, " +
                "(SELECT COUNT(*) FROM candidates) AS total_candidates, " +
                "(SELECT COUNT(*) FROM applications) AS total_applications, " +
                "(SELECT COUNT(*) FROM applications WHERE status = 'SHORTLISTED') AS shortlisted_count, " +
                "(SELECT COUNT(*) FROM applications WHERE status = 'HIRED') AS hired_count, " +
                "(SELECT COUNT(*) FROM applications WHERE status = 'REJECTED') AS rejected_count, " +
                "(SELECT COALESCE(AVG(match_score), 0.0) FROM applications) AS avg_match_score";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                metrics.put("totalJobs", rs.getInt("total_jobs"));
                metrics.put("openJobs", rs.getInt("open_jobs"));
                metrics.put("totalCandidates", rs.getInt("total_candidates"));
                metrics.put("totalApplications", rs.getInt("total_applications"));
                metrics.put("shortlistedCount", rs.getInt("shortlisted_count"));
                metrics.put("hiredCount", rs.getInt("hired_count"));
                metrics.put("rejectedCount", rs.getInt("rejected_count"));
                metrics.put("avgMatchScore", Math.round(rs.getDouble("avg_match_score") * 10.0) / 10.0);
            }
        }
        return metrics;
    }

    public Map<String, Integer> getSkillDemandMatrix() throws SQLException {
        Map<String, Integer> demand = new LinkedHashMap<>();
        String sql = "SELECT s.skill_name, COUNT(jrs.job_id) AS demand_count " +
                "FROM job_required_skills jrs " +
                "JOIN skills s ON jrs.skill_id = s.skill_id " +
                "JOIN jobs j ON jrs.job_id = j.job_id " +
                "WHERE j.status = 'OPEN' " +
                "GROUP BY s.skill_name " +
                "ORDER BY demand_count DESC, s.skill_name ASC " +
                "LIMIT 10";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                demand.put(rs.getString("skill_name"), rs.getInt("demand_count"));
            }
        }
        return demand;
    }

    public List<Map<String, Object>> getApplicationStatusBreakdown() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT status, COUNT(*) AS count, COALESCE(AVG(match_score), 0.0) AS avg_score " +
                "FROM applications GROUP BY status ORDER BY count DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("status", rs.getString("status"));
                row.put("count", rs.getInt("count"));
                row.put("avgScore", Math.round(rs.getDouble("avg_score") * 10.0) / 10.0);
                list.add(row);
            }
        }
        return list;
    }
}

