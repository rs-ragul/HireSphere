package com.hiresphere.dao;

import com.hiresphere.config.DatabaseConfig;
import com.hiresphere.model.Skill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Skill master records.
 */
public class SkillDAO {

    public List<Skill> getAllSkills() throws SQLException {
        List<Skill> skills = new ArrayList<>();
        String sql = "SELECT skill_id, skill_name, category FROM skills ORDER BY category, skill_name";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                skills.add(new Skill(
                        rs.getInt("skill_id"),
                        rs.getString("skill_name"),
                        rs.getString("category")
                ));
            }
        }
        return skills;
    }

    public Skill getOrCreateSkill(String skillName, String defaultCategory) throws SQLException {
        String trimmed = skillName.trim();
        String selectSql = "SELECT skill_id, skill_name, category FROM skills WHERE LOWER(skill_name) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, trimmed);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return new Skill(rs.getInt("skill_id"), rs.getString("skill_name"), rs.getString("category"));
                }
            }

            // Not found, insert new skill
            String insertSql = "INSERT INTO skills (skill_name, category) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, trimmed);
                insertStmt.setString(2, defaultCategory != null ? defaultCategory : "GENERAL");
                insertStmt.executeUpdate();

                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return new Skill(generatedKeys.getInt(1), trimmed, defaultCategory);
                    }
                }
            }
        }
        throw new SQLException("Failed to retrieve or create skill: " + trimmed);
    }
}

