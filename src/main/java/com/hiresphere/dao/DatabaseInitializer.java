package com.hiresphere.dao;

import com.hiresphere.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes database schemas, indexes, and initial seeds for skills and demo jobs.
 * Ensures the project runs out of the box on first launch.
 */
public class DatabaseInitializer {

    public static void initialize() {
        boolean isMySQL = DatabaseConfig.getCurrentType() == DatabaseConfig.DatabaseType.MYSQL;
        String autoInc = isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String pkInt = isMySQL ? "INT NOT NULL AUTO_INCREMENT" : "INTEGER PRIMARY KEY AUTOINCREMENT";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Skills table
            if (isMySQL) {
                stmt.execute("CREATE TABLE IF NOT EXISTS skills (" +
                        "skill_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "skill_name VARCHAR(100) NOT NULL UNIQUE," +
                        "category VARCHAR(50) NOT NULL" +
                        ") ENGINE=InnoDB;");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS skills (" +
                        "skill_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "skill_name VARCHAR(100) NOT NULL UNIQUE," +
                        "category VARCHAR(50) NOT NULL" +
                        ");");
            }

            // 2. Jobs table
            if (isMySQL) {
                stmt.execute("CREATE TABLE IF NOT EXISTS jobs (" +
                        "job_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "title VARCHAR(150) NOT NULL," +
                        "department VARCHAR(100) NOT NULL," +
                        "min_exp_years DOUBLE NOT NULL DEFAULT 0," +
                        "min_education VARCHAR(100) NOT NULL DEFAULT 'Bachelor'," +
                        "status VARCHAR(20) NOT NULL DEFAULT 'OPEN'," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB;");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS jobs (" +
                        "job_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "title VARCHAR(150) NOT NULL," +
                        "department VARCHAR(100) NOT NULL," +
                        "min_exp_years REAL NOT NULL DEFAULT 0," +
                        "min_education VARCHAR(100) NOT NULL DEFAULT 'Bachelor'," +
                        "status VARCHAR(20) NOT NULL DEFAULT 'OPEN'," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");");
            }

            // 3. Job Required Skills
            if (isMySQL) {
                stmt.execute("CREATE TABLE IF NOT EXISTS job_required_skills (" +
                        "job_id INT NOT NULL," +
                        "skill_id INT NOT NULL," +
                        "weight_type VARCHAR(20) NOT NULL DEFAULT 'MANDATORY'," +
                        "min_proficiency INT NOT NULL DEFAULT 1," +
                        "PRIMARY KEY (job_id, skill_id)," +
                        "FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB;");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS job_required_skills (" +
                        "job_id INTEGER NOT NULL," +
                        "skill_id INTEGER NOT NULL," +
                        "weight_type VARCHAR(20) NOT NULL DEFAULT 'MANDATORY'," +
                        "min_proficiency INTEGER NOT NULL DEFAULT 1," +
                        "PRIMARY KEY (job_id, skill_id)," +
                        "FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE" +
                        ");");
            }

            // 4. Candidates
            if (isMySQL) {
                stmt.execute("CREATE TABLE IF NOT EXISTS candidates (" +
                        "candidate_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "full_name VARCHAR(150) NOT NULL," +
                        "email VARCHAR(150) NOT NULL UNIQUE," +
                        "phone VARCHAR(30)," +
                        "total_exp_years DOUBLE NOT NULL DEFAULT 0," +
                        "highest_education VARCHAR(100) NOT NULL," +
                        "resume_summary TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB;");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS candidates (" +
                        "candidate_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "full_name VARCHAR(150) NOT NULL," +
                        "email VARCHAR(150) NOT NULL UNIQUE," +
                        "phone VARCHAR(30)," +
                        "total_exp_years REAL NOT NULL DEFAULT 0," +
                        "highest_education VARCHAR(100) NOT NULL," +
                        "resume_summary TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");");
            }

            // 5. Candidate Skills
            if (isMySQL) {
                stmt.execute("CREATE TABLE IF NOT EXISTS candidate_skills (" +
                        "candidate_id INT NOT NULL," +
                        "skill_id INT NOT NULL," +
                        "proficiency_level INT NOT NULL DEFAULT 1," +
                        "years_experience DOUBLE NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (candidate_id, skill_id)," +
                        "FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB;");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS candidate_skills (" +
                        "candidate_id INTEGER NOT NULL," +
                        "skill_id INTEGER NOT NULL," +
                        "proficiency_level INTEGER NOT NULL DEFAULT 1," +
                        "years_experience REAL NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (candidate_id, skill_id)," +
                        "FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE" +
                        ");");
            }

            // 6. Applications
            if (isMySQL) {
                stmt.execute("CREATE TABLE IF NOT EXISTS applications (" +
                        "application_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "job_id INT NOT NULL," +
                        "candidate_id INT NOT NULL," +
                        "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "status VARCHAR(30) NOT NULL DEFAULT 'APPLIED'," +
                        "match_score DOUBLE NOT NULL DEFAULT 0.0," +
                        "skill_coverage_score DOUBLE NOT NULL DEFAULT 0.0," +
                        "experience_factor DOUBLE NOT NULL DEFAULT 0.0," +
                        "proficiency_factor DOUBLE NOT NULL DEFAULT 0.0," +
                        "notes TEXT," +
                        "UNIQUE KEY uq_job_candidate (job_id, candidate_id)," +
                        "FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB;");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS applications (" +
                        "application_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "job_id INTEGER NOT NULL," +
                        "candidate_id INTEGER NOT NULL," +
                        "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "status VARCHAR(30) NOT NULL DEFAULT 'APPLIED'," +
                        "match_score REAL NOT NULL DEFAULT 0.0," +
                        "skill_coverage_score REAL NOT NULL DEFAULT 0.0," +
                        "experience_factor REAL NOT NULL DEFAULT 0.0," +
                        "proficiency_factor REAL NOT NULL DEFAULT 0.0," +
                        "notes TEXT," +
                        "UNIQUE(job_id, candidate_id)," +
                        "FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE" +
                        ");");
            }

            // 7. Audit logs
            if (isMySQL) {
                stmt.execute("CREATE TABLE IF NOT EXISTS match_audit_logs (" +
                        "log_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "application_id INT NOT NULL," +
                        "missing_mandatory_skills TEXT," +
                        "gap_summary TEXT," +
                        "evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB;");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS match_audit_logs (" +
                        "log_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "application_id INTEGER NOT NULL," +
                        "missing_mandatory_skills TEXT," +
                        "gap_summary TEXT," +
                        "evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE" +
                        ");");
            }

            // 8. Application Recruiter Notes / Scorecards
            if (isMySQL) {
                stmt.execute("CREATE TABLE IF NOT EXISTS application_notes (" +
                        "note_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "application_id INT NOT NULL," +
                        "author VARCHAR(100) NOT NULL," +
                        "note_text TEXT NOT NULL," +
                        "rating INT DEFAULT 3," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB;");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS application_notes (" +
                        "note_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "application_id INTEGER NOT NULL," +
                        "author VARCHAR(100) NOT NULL," +
                        "note_text TEXT NOT NULL," +
                        "rating INTEGER DEFAULT 3," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE" +
                        ");");
            }

            // Seed master skills catalog if empty
            seedInitialSkills(conn);

        } catch (SQLException e) {
            System.err.println("[ERROR] Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedInitialSkills(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM skills")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // already seeded
            }
        }

        String sql = "INSERT INTO skills (skill_name, category) VALUES (?, ?)";
        String[][] initialSkills = {
                {"Java", "BACKEND"},
                {"Spring Boot", "BACKEND"},
                {"REST APIs", "BACKEND"},
                {"Microservices", "BACKEND"},
                {"Python", "BACKEND"},
                {"Django", "BACKEND"},
                {"MySQL", "DATABASE"},
                {"PostgreSQL", "DATABASE"},
                {"Redis", "DATABASE"},
                {"Docker", "DEVOPS"},
                {"Kubernetes", "DEVOPS"},
                {"AWS", "CLOUD"},
                {"Git", "TOOLS"},
                {"React", "FRONTEND"},
                {"JavaScript", "FRONTEND"},
                {"HTML/CSS", "FRONTEND"},
                {"Kafka", "BACKEND"}
        };

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String[] sk : initialSkills) {
                pstmt.setString(1, sk[0]);
                pstmt.setString(2, sk[1]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}

