package com.hiresphere;

import com.hiresphere.analytics.SkillMatchEngine;
import com.hiresphere.config.DatabaseConfig;
import com.hiresphere.dao.DatabaseInitializer;
import com.hiresphere.dao.SkillDAO;
import com.hiresphere.model.*;
import com.hiresphere.service.AnalyticsService;
import com.hiresphere.service.CandidateService;
import com.hiresphere.service.JobService;
import com.hiresphere.util.ConsoleColors;
import com.hiresphere.util.TableFormatter;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

/**
 * HireSphere ATS - Main Application Entrypoint and Interactive Terminal Dashboard.
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final JobService jobService = new JobService();
    private static final CandidateService candidateService = new CandidateService();
    private static final AnalyticsService analyticsService = new AnalyticsService();
    private static final SkillDAO skillDAO = new SkillDAO();

    public static void main(String[] args) {
        printBanner();

        com.hiresphere.api.ApiServer apiServer = new com.hiresphere.api.ApiServer(8080);

        // Initialize Database Schema & Seed initial data
        try {
            System.out.println(ConsoleColors.cyan("Connecting to database: " + DatabaseConfig.getActiveDatabaseInfo()));
            DatabaseInitializer.initialize();
            jobService.seedInitialJobsIfEmpty();

            // Auto-seed sample candidates if candidate table is empty
            List<Candidate> cands = candidateService.getAllCandidates();
            if (cands.isEmpty()) {
                File sampleCsv = new File("data/sample_candidates.csv");
                if (sampleCsv.exists()) {
                    int imported = candidateService.importFromCsv(sampleCsv.getPath());
                    System.out.println(ConsoleColors.green("[OK] Auto-loaded " + imported + " sample candidates from data/sample_candidates.csv"));
                }
            }

            // Auto-screen candidates across all active jobs
            List<Job> allJobs = jobService.getAllJobs();
            for (Job j : allJobs) {
                analyticsService.batchScreenAllCandidatesForJob(j.getJobId());
            }

            System.out.println(ConsoleColors.green("[OK] Database and master records initialized successfully."));

            // Start REST API & Web Portal
            apiServer.start();
            System.out.println(ConsoleColors.green(ConsoleColors.bold("\n>>> WEB PORTAL READY: Open http://localhost:8080 in your browser <<<\n")));
        } catch (Exception e) {
            System.err.println(ConsoleColors.red("Initialization error: " + e.getMessage()));
        }

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print(ConsoleColors.bold("Select Option [0-10]: "));
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        handleCreateJob();
                        break;
                    case "2":
                        handleViewJobs();
                        break;
                    case "3":
                        handleRegisterCandidate();
                        break;
                    case "4":
                        handleViewCandidates();
                        break;
                    case "5":
                        handleCsvBulkImport();
                        break;
                    case "6":
                        handleSkillMatchSimulator();
                        break;
                    case "7":
                        handleBatchScreenAndRanking();
                        break;
                    case "8":
                        handleUpdateApplicationStatus();
                        break;
                    case "9":
                        handleRecruiterAnalyticsDashboard();
                        break;
                    case "10":
                        handleDatabaseSettings();
                        break;
                    case "0":
                        apiServer.stop();
                        System.out.println(ConsoleColors.cyan("\nThank you for using HireSphere ATS. Goodbye!"));
                        running = false;
                        break;
                    default:
                        System.out.println(ConsoleColors.red("Invalid option. Please enter a number between 0 and 10."));
                }
            } catch (Exception e) {
                System.out.println(ConsoleColors.red("\n[ERROR] An error occurred: " + e.getMessage()));
            }

            if (running) {
                System.out.print("\nPress [Enter] to continue...");
                scanner.nextLine();
            }
        }
    }

    private static void printBanner() {
        System.out.println(ConsoleColors.CYAN + ConsoleColors.BOLD);
        System.out.println("================================================================================");
        System.out.println("  _   _ _            ____  ____  _   _ _____ ____  _____ ");
        System.out.println(" | | | (_)_ __ ___  / ___||  _ \\| | | | ____|  _ \\| ____|");
        System.out.println(" | |_| | | '__/ _ \\ \\___ \\| |_) | |_| |  _| | |_) |  _|  ");
        System.out.println(" |  _  | | | |  __/  ___) |  __/|  _  | |___|  _ <| |___ ");
        System.out.println(" |_| |_|_|_|  \\___| |____/|_|   |_| |_|_____|_| \\_\\_____|");
        System.out.println("                                                         ");
        System.out.println("   Applicant Tracking System (ATS) with Skill-Match Analytics   ");
        System.out.println("   Core Java + JDBC Architecture | Dynamic Weighted Scoring Engine");
        System.out.println("================================================================================" + ConsoleColors.RESET);
    }

    private static void printMenu() {
        System.out.println("\n" + ConsoleColors.bold("MAIN MENU"));
        System.out.println(ConsoleColors.cyan("--- Job Requisition Management ---"));
        System.out.println(" [1] Post a New Job Requisition (with Weighted Skill Requirements)");
        System.out.println(" [2] View All Open Job Requisitions");
        System.out.println(ConsoleColors.cyan("--- Candidate Management ---"));
        System.out.println(" [3] Register Candidate Profile (with Skill Proficiencies)");
        System.out.println(" [4] View All Registered Candidates");
        System.out.println(" [5] Bulk Ingest Candidates from CSV (Batch Processing)");
        System.out.println(ConsoleColors.cyan("--- Skill-Match Analytics & Hiring Funnel ---"));
        System.out.println(" [6] Interactive Skill-Match Simulator & Gap Diagnosis (Candidate vs Job)");
        System.out.println(" [7] Run ATS Batch-Screening & Dynamic Ranking Leaderboard for a Job");
        System.out.println(" [8] Update Candidate Application Stage (Shortlist / Reject / Hire)");
        System.out.println(" [9] Recruiter Analytics Dashboard (Funnel & Skill Demand Matrix)");
        System.out.println(ConsoleColors.cyan("--- System Settings ---"));
        System.out.println(" [10] Database Connection Settings (Switch SQLite / MySQL)");
        System.out.println(" [0] Exit Application");
        System.out.println("--------------------------------------------------------------------------------");
    }

    // 1. Post a New Job Requisition
    private static void handleCreateJob() throws SQLException {
        System.out.println("\n" + ConsoleColors.bold("=== POST NEW JOB REQUISITION ==="));
        System.out.print("Job Title (e.g. Backend Engineer): ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println(ConsoleColors.red("Job title cannot be empty."));
            return;
        }

        System.out.print("Department (e.g. Engineering, Product): ");
        String dept = scanner.nextLine().trim();

        System.out.print("Minimum Required Experience (years, e.g. 3.0): ");
        double minExp = parseDoubleDefault(scanner.nextLine(), 0.0);

        System.out.print("Minimum Education (e.g. Bachelor in CSE): ");
        String education = scanner.nextLine().trim();
        if (education.isEmpty()) education = "Bachelor";

        Job job = new Job(0, title, dept, minExp, education, "OPEN");

        List<JobSkill> skills = new ArrayList<>();
        System.out.println("\n" + ConsoleColors.cyan("Define Skill Requirements (Enter blank skill name to finish):"));
        while (true) {
            System.out.print("  Skill Name (e.g. Java, Docker, React): ");
            String sName = scanner.nextLine().trim();
            if (sName.isEmpty()) break;

            System.out.print("  Skill Weight [1: MANDATORY (3x), 2: PREFERRED (2x), 3: OPTIONAL (1x)]: ");
            String wChoice = scanner.nextLine().trim();
            JobSkill.WeightType wt = JobSkill.WeightType.MANDATORY;
            if ("2".equals(wChoice)) wt = JobSkill.WeightType.PREFERRED;
            else if ("3".equals(wChoice)) wt = JobSkill.WeightType.OPTIONAL;

            System.out.print("  Minimum Proficiency (1=Basic, 3=Intermediate, 5=Expert) [1-5]: ");
            int minProf = parseIntDefault(scanner.nextLine(), 3);
            if (minProf < 1) minProf = 1;
            if (minProf > 5) minProf = 5;

            Skill s = skillDAO.getOrCreateSkill(sName, "TECHNICAL");
            skills.add(new JobSkill(0, s.getSkillId(), s.getSkillName(), s.getCategory(), wt, minProf));
            System.out.println(ConsoleColors.green("  [+] Added " + s.getSkillName() + " [" + wt + ", Min Lvl: " + minProf + "/5]"));
        }

        int jobId = jobService.createJob(job, skills);
        System.out.println(ConsoleColors.green("\n[OK] Job Requisition #" + jobId + " created successfully with " + skills.size() + " skill constraints!"));
    }

    // 2. View All Open Job Requisitions
    private static void handleViewJobs() throws SQLException {
        System.out.println("\n" + ConsoleColors.bold("=== CURRENT JOB REQUISITIONS ==="));
        List<Job> jobs = jobService.getAllJobs();
        if (jobs.isEmpty()) {
            System.out.println("No job requisitions found.");
            return;
        }

        TableFormatter tf = new TableFormatter("ID", "Job Title", "Department", "Min Exp", "Education", "Status", "Required Skills");
        for (Job j : jobs) {
            StringBuilder skSb = new StringBuilder();
            for (JobSkill js : j.getRequiredSkills()) {
                if (skSb.length() > 0) skSb.append(", ");
                skSb.append(js.getSkillName()).append(" (").append(js.getWeightType().name().charAt(0)).append(":").append(js.getMinProficiency()).append(")");
            }
            tf.addRow(
                    String.valueOf(j.getJobId()),
                    j.getTitle(),
                    j.getDepartment(),
                    j.getMinExpYears() + " yrs",
                    j.getMinEducation(),
                    ConsoleColors.statusBadge(j.getStatus()),
                    skSb.length() > 0 ? skSb.toString() : "None"
            );
        }
        tf.print();
        System.out.println(ConsoleColors.yellow("Legend for Skills: Name (Weight[M/P/O]:MinProficiency[1-5])"));
    }

    // 3. Register a Candidate Profile
    private static void handleRegisterCandidate() throws SQLException {
        System.out.println("\n" + ConsoleColors.bold("=== REGISTER CANDIDATE PROFILE ==="));
        System.out.print("Full Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println(ConsoleColors.red("Name cannot be empty."));
            return;
        }

        System.out.print("Email Address: ");
        String email = scanner.nextLine().trim();
        if (email.isEmpty()) {
            System.out.println(ConsoleColors.red("Email cannot be empty."));
            return;
        }

        System.out.print("Phone Number: ");
        String phone = scanner.nextLine().trim();

        System.out.print("Total Experience (Years, e.g. 4.5): ");
        double exp = parseDoubleDefault(scanner.nextLine(), 0.0);

        System.out.print("Highest Education (e.g. B.Tech Computer Science): ");
        String education = scanner.nextLine().trim();
        if (education.isEmpty()) education = "Bachelor";

        System.out.print("Resume Summary / Bio: ");
        String summary = scanner.nextLine().trim();

        Candidate candidate = new Candidate(name, email, phone, exp, education, summary);

        List<CandidateSkill> skills = new ArrayList<>();
        System.out.println("\n" + ConsoleColors.cyan("Add Candidate Skills (Enter blank skill name to finish):"));
        while (true) {
            System.out.print("  Skill Name (e.g. Java, Python, AWS): ");
            String sName = scanner.nextLine().trim();
            if (sName.isEmpty()) break;

            System.out.print("  Candidate Proficiency Level (1=Basic, 3=Competent, 5=Expert) [1-5]: ");
            int prof = parseIntDefault(scanner.nextLine(), 3);
            if (prof < 1) prof = 1;
            if (prof > 5) prof = 5;

            System.out.print("  Years of Hands-on Experience with this skill: ");
            double skillExp = parseDoubleDefault(scanner.nextLine(), exp);

            Skill s = skillDAO.getOrCreateSkill(sName, "TECHNICAL");
            skills.add(new CandidateSkill(0, s.getSkillId(), s.getSkillName(), s.getCategory(), prof, skillExp));
            System.out.println(ConsoleColors.green("  [+] Added " + s.getSkillName() + " (" + prof + "/5, " + skillExp + " yrs)"));
        }

        int candId = candidateService.registerCandidate(candidate, skills);
        System.out.println(ConsoleColors.green("\n[OK] Candidate profile #" + candId + " registered successfully with " + skills.size() + " skills!"));
    }

    // 4. View All Registered Candidates
    private static void handleViewCandidates() throws SQLException {
        System.out.println("\n" + ConsoleColors.bold("=== REGISTERED CANDIDATE PROFILES ==="));
        List<Candidate> candidates = candidateService.getAllCandidates();
        if (candidates.isEmpty()) {
            System.out.println("No candidates registered.");
            return;
        }

        TableFormatter tf = new TableFormatter("ID", "Full Name", "Email", "Exp", "Education", "Skills (Proficiency)");
        for (Candidate c : candidates) {
            StringBuilder skSb = new StringBuilder();
            for (CandidateSkill cs : c.getSkills()) {
                if (skSb.length() > 0) skSb.append(", ");
                skSb.append(cs.getSkillName()).append("(").append(cs.getProficiencyLevel()).append("/5)");
            }
            tf.addRow(
                    String.valueOf(c.getCandidateId()),
                    c.getFullName(),
                    c.getEmail(),
                    c.getTotalExpYears() + " yrs",
                    c.getHighestEducation(),
                    skSb.length() > 0 ? skSb.toString() : "None"
            );
        }
        tf.print();
    }

    // 5. Bulk Ingest Candidates from CSV (Batch Processing)
    private static void handleCsvBulkImport() throws Exception {
        System.out.println("\n" + ConsoleColors.bold("=== BULK RESUME / CANDIDATE CSV INGESTION ==="));
        System.out.print("Enter CSV File Path [default: data/sample_candidates.csv]: ");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) {
            path = "data/sample_candidates.csv";
        }

        File file = new File(path);
        if (!file.exists()) {
            System.out.println(ConsoleColors.red("File not found at: " + file.getAbsolutePath()));
            return;
        }

        System.out.println(ConsoleColors.cyan("Starting batch ingestion via JDBC Batch Updates..."));
        long start = System.currentTimeMillis();
        int imported = candidateService.importFromCsv(path);
        long duration = System.currentTimeMillis() - start;

        System.out.println(ConsoleColors.green("[OK] Successfully processed and loaded " + imported + " candidates in " + duration + " ms."));
    }

    // 6. Interactive Skill-Match Simulator & Gap Diagnosis
    private static void handleSkillMatchSimulator() throws SQLException {
        System.out.println("\n" + ConsoleColors.bold("=== SKILL-MATCH ANALYTICS SIMULATOR ==="));
        handleViewJobs();

        System.out.print("\nEnter Job ID to evaluate against: ");
        int jobId = parseIntDefault(scanner.nextLine(), -1);
        Job job = jobService.getJobById(jobId);
        if (job == null) {
            System.out.println(ConsoleColors.red("Job not found."));
            return;
        }

        handleViewCandidates();
        System.out.print("\nEnter Candidate ID to analyze: ");
        int candId = parseIntDefault(scanner.nextLine(), -1);
        Candidate candidate = candidateService.getCandidateById(candId);
        if (candidate == null) {
            System.out.println(ConsoleColors.red("Candidate not found."));
            return;
        }

        System.out.println(ConsoleColors.cyan("\nExecuting SkillMatchEngine multi-factor analytics..."));
        MatchResult result = SkillMatchEngine.evaluate(job, candidate);

        System.out.println("\n================================================================================");
        System.out.println(ConsoleColors.bold("SKILL-MATCH ANALYTICS EVALUATION REPORT"));
        System.out.println("================================================================================");
        System.out.println(" Job Requisition : #" + job.getJobId() + " - " + job.getTitle() + " (" + job.getDepartment() + ")");
        System.out.println(" Candidate Name  : #" + candidate.getCandidateId() + " - " + candidate.getFullName() + " (" + candidate.getEmail() + ")");
        System.out.println(" Experience Check: Candidate has " + candidate.getTotalExpYears() + " yrs (Job requires: " + job.getMinExpYears() + " yrs)");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(" COMPOSITE MATCH SCORE : " + ConsoleColors.scoreBadge(result.getCompositeScore()));
        System.out.println(" TIER CLASSIFICATION   : " + ConsoleColors.bold(result.getMatchTier()));
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(" FACTOR BREAKDOWN:");
        System.out.println("  1. Weighted Skill Coverage (55% weight) : " + String.format("%.1f%%", result.getSkillCoverageScore()));
        System.out.println("  2. Experience Factor       (25% weight) : " + String.format("%.1f%%", result.getExperienceFactor()));
        System.out.println("  3. Proficiency Alignment   (20% weight) : " + String.format("%.1f%%", result.getProficiencyFactor()));
        System.out.println("--------------------------------------------------------------------------------");

        System.out.println(ConsoleColors.bold("MATCHED SKILLS:"));
        if (result.getMatchedSkills().isEmpty()) {
            System.out.println("  (None)");
        } else {
            for (String ms : result.getMatchedSkills()) {
                System.out.println(ConsoleColors.green("  [OK] " + ms));
            }
        }

        System.out.println("\n" + ConsoleColors.bold("SKILL & QUALIFICATION GAP DIAGNOSIS:"));
        if (result.getSkillGaps().isEmpty()) {
            System.out.println(ConsoleColors.green("  [OK] Zero critical gaps detected! Outstanding fit."));
        } else {
            for (SkillGap gap : result.getSkillGaps()) {
                if (gap.getGapType() == SkillGap.GapType.MISSING_MANDATORY_SKILL) {
                    System.out.println(ConsoleColors.red("  [!] [CRITICAL] " + gap.getDescription()));
                } else if (gap.getGapType() == SkillGap.GapType.INSUFFICIENT_EXPERIENCE) {
                    System.out.println(ConsoleColors.yellow("  [-] [EXPERIENCE] " + gap.getDescription()));
                } else {
                    System.out.println(ConsoleColors.yellow("  [-] [DEFICIT] " + gap.getDescription()));
                }
            }
        }
        System.out.println("================================================================================");

        System.out.print("\nDo you want to formally submit/record this application? [y/N]: ");
        String submitChoice = scanner.nextLine().trim();
        if (submitChoice.equalsIgnoreCase("y")) {
            try {
                int appId = analyticsService.submitApplication(jobId, candId, "Simulated match submission");
                System.out.println(ConsoleColors.green("[OK] Application #" + appId + " recorded in database with match score " + result.getCompositeScore() + "%"));
            } catch (IllegalStateException dup) {
                System.out.println(ConsoleColors.yellow("Notice: " + dup.getMessage()));
            }
        }
    }

    // 7. Run ATS Batch-Screening & Dynamic Ranking Leaderboard for a Job
    private static void handleBatchScreenAndRanking() throws SQLException {
        System.out.println("\n" + ConsoleColors.bold("=== ATS BATCH-SCREENING & RANKING LEADERBOARD ==="));
        handleViewJobs();

        System.out.print("\nSelect Job ID to run Batch-Screening & Ranking: ");
        int jobId = parseIntDefault(scanner.nextLine(), -1);
        Job job = jobService.getJobById(jobId);
        if (job == null) {
            System.out.println(ConsoleColors.red("Job not found."));
            return;
        }

        System.out.println(ConsoleColors.cyan("Screening all registered candidates against Job #" + jobId + " (" + job.getTitle() + ")..."));
        int newlyScreened = analyticsService.batchScreenAllCandidatesForJob(jobId);
        if (newlyScreened > 0) {
            System.out.println(ConsoleColors.green("[OK] Screened " + newlyScreened + " candidates into the recruitment pipeline."));
        }

        List<Application> rankedApps = analyticsService.getRankedApplicantsForJob(jobId);
        if (rankedApps.isEmpty()) {
            System.out.println("No applicants found for this job requisition.");
            return;
        }

        System.out.println("\n" + ConsoleColors.bold("DYNAMIC APPLICANT RANKING LEADERBOARD (Sorted by Match Score DESC)"));
        TableFormatter tf = new TableFormatter("Rank", "App ID", "Candidate Name", "Email", "Match Score", "Coverage", "Exp Factor", "Proficiency", "Status");
        int rank = 1;
        for (Application app : rankedApps) {
            tf.addRow(
                    "#" + rank++,
                    String.valueOf(app.getApplicationId()),
                    app.getCandidateName(),
                    app.getCandidateEmail(),
                    ConsoleColors.scoreBadge(app.getMatchScore()),
                    String.format("%.1f%%", app.getSkillCoverageScore()),
                    String.format("%.1f%%", app.getExperienceFactor()),
                    String.format("%.1f%%", app.getProficiencyFactor()),
                    ConsoleColors.statusBadge(app.getStatus())
            );
        }
        tf.print();
    }

    // 8. Update Candidate Application Stage
    private static void handleUpdateApplicationStatus() throws SQLException {
        System.out.println("\n" + ConsoleColors.bold("=== UPDATE APPLICATION HIRING STAGE ==="));
        List<Application> apps = analyticsService.getAllApplications();
        if (apps.isEmpty()) {
            System.out.println("No applications currently recorded.");
            return;
        }

        TableFormatter tf = new TableFormatter("App ID", "Job Title", "Candidate", "Score", "Current Status");
        for (Application a : apps) {
            tf.addRow(
                    String.valueOf(a.getApplicationId()),
                    a.getJobTitle(),
                    a.getCandidateName(),
                    ConsoleColors.scoreBadge(a.getMatchScore()),
                    ConsoleColors.statusBadge(a.getStatus())
            );
        }
        tf.print();

        System.out.print("\nEnter Application ID to update: ");
        int appId = parseIntDefault(scanner.nextLine(), -1);

        System.out.println("Select New Status:");
        System.out.println(" [1] SHORTLISTED");
        System.out.println(" [2] HIRED");
        System.out.println(" [3] REJECTED");
        System.out.println(" [4] SCREENED");
        System.out.print("Choice: ");
        String sc = scanner.nextLine().trim();

        String newStatus;
        switch (sc) {
            case "1": newStatus = "SHORTLISTED"; break;
            case "2": newStatus = "HIRED"; break;
            case "3": newStatus = "REJECTED"; break;
            case "4": newStatus = "SCREENED"; break;
            default:
                System.out.println(ConsoleColors.red("Invalid status choice."));
                return;
        }

        analyticsService.updateApplicationStatus(appId, newStatus);
        System.out.println(ConsoleColors.green("[OK] Application #" + appId + " status successfully updated to " + newStatus));
    }

    // 9. Recruiter Analytics Dashboard
    private static void handleRecruiterAnalyticsDashboard() throws SQLException {
        System.out.println("\n" + ConsoleColors.bold("================================================================================"));
        System.out.println(ConsoleColors.bold("                 RECRUITER EXECUTIVE ANALYTICS DASHBOARD"));
        System.out.println(ConsoleColors.bold("================================================================================"));

        Map<String, Object> summary = analyticsService.getRecruitmentFunnelSummary();
        System.out.println(ConsoleColors.cyan(ConsoleColors.bold("RECRUITMENT FUNNEL METRICS:")));
        System.out.println("  * Total Job Postings        : " + summary.get("totalJobs") + " (" + summary.get("openJobs") + " OPEN)");
        System.out.println("  * Total Registered Candidates: " + summary.get("totalCandidates"));
        System.out.println("  * Total Applications Ingested: " + summary.get("totalApplications"));
        System.out.println("  * Shortlisted Candidates     : " + ConsoleColors.green(String.valueOf(summary.get("shortlistedCount"))));
        System.out.println("  * Final Hires Made           : " + ConsoleColors.green(ConsoleColors.bold(String.valueOf(summary.get("hiredCount")))));
        System.out.println("  * Rejected Applicants        : " + ConsoleColors.red(String.valueOf(summary.get("rejectedCount"))));
        System.out.println("  * Average Match Score Across : " + ConsoleColors.yellow(summary.get("avgMatchScore") + "%"));

        System.out.println("\n" + ConsoleColors.cyan(ConsoleColors.bold("TOP IN-DEMAND SKILLS (Across All Open Jobs):")));
        Map<String, Integer> demand = analyticsService.getSkillDemandMatrix();
        if (demand.isEmpty()) {
            System.out.println("  No skill demand data available yet.");
        } else {
            TableFormatter tf = new TableFormatter("Skill Name", "Job Postings Requiring", "Demand Bar");
            for (Map.Entry<String, Integer> entry : demand.entrySet()) {
                String bar = "#".repeat(Math.max(1, entry.getValue() * 4));
                tf.addRow(entry.getKey(), String.valueOf(entry.getValue()), ConsoleColors.green(bar));
            }
            tf.print();
        }

        System.out.println("\n" + ConsoleColors.cyan(ConsoleColors.bold("APPLICATION STATUS DISTRIBUTION:")));
        List<Map<String, Object>> statusList = analyticsService.getApplicationStatusBreakdown();
        if (!statusList.isEmpty()) {
            TableFormatter stf = new TableFormatter("Status", "Candidate Count", "Avg Match Score");
            for (Map<String, Object> row : statusList) {
                stf.addRow(
                        ConsoleColors.statusBadge((String) row.get("status")),
                        String.valueOf(row.get("count")),
                        row.get("avgScore") + "%"
                );
            }
            stf.print();
        }
        System.out.println("================================================================================");
    }

    // 10. Database Settings
    private static void handleDatabaseSettings() {
        System.out.println("\n" + ConsoleColors.bold("=== DATABASE CONFIGURATION ==="));
        System.out.println("Active Database: " + ConsoleColors.green(DatabaseConfig.getActiveDatabaseInfo()));
        System.out.println("\nOptions:");
        System.out.println(" [1] Use SQLite Embedded (Default, zero setup, local file)");
        System.out.println(" [2] Use MySQL (Localhost XAMPP / Port 3306)");
        System.out.println(" [3] Back to Main Menu");
        System.out.print("Select: ");
        String c = scanner.nextLine().trim();

        if ("1".equals(c)) {
            DatabaseConfig.setDatabaseType(DatabaseConfig.DatabaseType.SQLITE);
            DatabaseInitializer.initialize();
            System.out.println(ConsoleColors.green("[OK] Switched to SQLite Embedded successfully."));
        } else if ("2".equals(c)) {
            DatabaseConfig.setDatabaseType(DatabaseConfig.DatabaseType.MYSQL);
            try {
                DatabaseInitializer.initialize();
                System.out.println(ConsoleColors.green("[OK] Successfully connected and initialized MySQL!"));
            } catch (Exception e) {
                System.out.println(ConsoleColors.red("Failed to connect to MySQL: " + e.getMessage()));
                System.out.println(ConsoleColors.yellow("Reverting back to SQLite Embedded."));
                DatabaseConfig.setDatabaseType(DatabaseConfig.DatabaseType.SQLITE);
            }
        }
    }

    private static double parseDoubleDefault(String val, double def) {
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int parseIntDefault(String val, int def) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
