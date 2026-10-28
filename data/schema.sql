-- ========================================================
-- HireSphere: Applicant Tracking System (ATS)
-- Database Schema Definition (Compatible with SQLite & MySQL)
-- ========================================================

-- 1. Skills Master Table
CREATE TABLE IF NOT EXISTS skills (
    skill_id INTEGER PRIMARY KEY AUTOINCREMENT,
    skill_name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL -- e.g. BACKEND, FRONTEND, DATABASE, CLOUD, DEVOPS, AI_ML
);

-- 2. Job Postings Table
CREATE TABLE IF NOT EXISTS jobs (
    job_id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(150) NOT NULL,
    department VARCHAR(100) NOT NULL,
    min_exp_years REAL NOT NULL DEFAULT 0,
    min_education VARCHAR(100) NOT NULL DEFAULT 'Bachelor',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, CLOSED, ON_HOLD
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Job Required Skills (Many-to-Many with Weight & Proficiency)
CREATE TABLE IF NOT EXISTS job_required_skills (
    job_id INTEGER NOT NULL,
    skill_id INTEGER NOT NULL,
    weight_type VARCHAR(20) NOT NULL DEFAULT 'MANDATORY', -- MANDATORY (3), PREFERRED (2), OPTIONAL (1)
    min_proficiency INTEGER NOT NULL DEFAULT 1,            -- 1 to 5 scale
    PRIMARY KEY (job_id, skill_id),
    FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE
);

-- 4. Candidates Profile Table
CREATE TABLE IF NOT EXISTS candidates (
    candidate_id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30),
    total_exp_years REAL NOT NULL DEFAULT 0,
    highest_education VARCHAR(100) NOT NULL,
    resume_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Candidate Skills (Many-to-Many with Proficiency & Experience)
CREATE TABLE IF NOT EXISTS candidate_skills (
    candidate_id INTEGER NOT NULL,
    skill_id INTEGER NOT NULL,
    proficiency_level INTEGER NOT NULL DEFAULT 1, -- 1 to 5 scale
    years_experience REAL NOT NULL DEFAULT 0,
    PRIMARY KEY (candidate_id, skill_id),
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE
);

-- 6. Job Applications & Calculated Match Scores
CREATE TABLE IF NOT EXISTS applications (
    application_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    candidate_id INTEGER NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'APPLIED', -- APPLIED, SCREENED, SHORTLISTED, REJECTED, HIRED
    match_score REAL NOT NULL DEFAULT 0.0,
    skill_coverage_score REAL NOT NULL DEFAULT 0.0,
    experience_factor REAL NOT NULL DEFAULT 0.0,
    proficiency_factor REAL NOT NULL DEFAULT 0.0,
    notes TEXT,
    UNIQUE(job_id, candidate_id),
    FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE
);

-- 7. Analytics Audit / Score Logs
CREATE TABLE IF NOT EXISTS match_audit_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER NOT NULL,
    missing_mandatory_skills TEXT,
    gap_summary TEXT,
    evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_job_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_app_job_score ON applications(job_id, match_score DESC);
CREATE INDEX IF NOT EXISTS idx_candidate_email ON candidates(email);

