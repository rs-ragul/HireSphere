package com.hiresphere.api;

import com.hiresphere.analytics.SkillMatchEngine;
import com.hiresphere.config.DatabaseConfig;
import com.hiresphere.dao.SkillDAO;
import com.hiresphere.model.*;
import com.hiresphere.service.AnalyticsService;
import com.hiresphere.service.CandidateService;
import com.hiresphere.service.JobService;
import com.hiresphere.util.ResumeParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Built-in Lightweight HTTP & REST API Server for the React Frontend.
 * Uses standard JDK HttpServer - zero external frameworks or heavy dependencies required.
 */
public class ApiServer {

    private final int port;
    private HttpServer server;
    private final JobService jobService = new JobService();
    private final CandidateService candidateService = new CandidateService();
    private final AnalyticsService analyticsService = new AnalyticsService();
    private final SkillDAO skillDAO = new SkillDAO();
    private final ResumeParser resumeParser = new ResumeParser();

    public ApiServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        // API Contexts
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/jobs", new JobsHandler());
        server.createContext("/api/candidates", new CandidatesHandler());
        server.createContext("/api/skills", new SkillsHandler());
        server.createContext("/api/match/evaluate", new EvaluateHandler());
        server.createContext("/api/match/batch-screen", new BatchScreenHandler());
        server.createContext("/api/applications", new ApplicationsHandler());
        server.createContext("/api/analytics/summary", new FunnelHandler());
        server.createContext("/api/analytics/skill-demand", new SkillDemandHandler());
        server.createContext("/api/resumes/parse", new ResumeParseHandler());
        server.createContext("/api/resumes/import", new ResumeImportHandler());

        // Static files handler for React web build
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("[OK] HireSphere REST API & Web Portal listening on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- Helper Methods ---

    private static void sendCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        sendCors(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int read;
            while ((read = is.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    // --- Handlers ---

    private static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String json = "{\"status\":\"UP\",\"database\":" + JsonUtil.quote(DatabaseConfig.getActiveDatabaseInfo()) +
                    ",\"databaseType\":" + JsonUtil.quote(DatabaseConfig.getCurrentType().name()) + "}";
            sendJson(exchange, 200, json);
        }
    }

    private class JobsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    List<Job> jobs = jobService.getAllJobs();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < jobs.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(jobToJson(jobs.get(i)));
                    }
                    sb.append("]");
                    sendJson(exchange, 200, sb.toString());
                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> data = JsonUtil.parseSimpleJson(body);

                    String title = data.getOrDefault("title", "New Position");
                    String dept = data.getOrDefault("department", "Engineering");
                    double minExp = parseDouble(data.get("minExpYears"), 0.0);
                    String edu = data.getOrDefault("minEducation", "Bachelor");

                    Job job = new Job(0, title, dept, minExp, edu, "OPEN");
                    List<JobSkill> skills = new ArrayList<>();

                    // Parse skills if provided in skillNames string or format "Java:MANDATORY:4;Spring:PREFERRED:3"
                    String skillsStr = data.get("skills");
                    if (skillsStr != null && !skillsStr.isEmpty()) {
                        String[] parts = skillsStr.split(";");
                        for (String p : parts) {
                            String[] sp = p.split(":");
                            if (sp.length >= 1 && !sp[0].trim().isEmpty()) {
                                Skill s = skillDAO.getOrCreateSkill(sp[0].trim(), "TECHNICAL");
                                JobSkill.WeightType wt = sp.length > 1 ? JobSkill.WeightType.fromString(sp[1]) : JobSkill.WeightType.MANDATORY;
                                int minProf = sp.length > 2 ? parseInt(sp[2], 3) : 3;
                                skills.add(new JobSkill(0, s.getSkillId(), s.getSkillName(), s.getCategory(), wt, minProf));
                            }
                        }
                    }

                    int jobId = jobService.createJob(job, skills);
                    Job created = jobService.getJobById(jobId);
                    sendJson(exchange, 201, jobToJson(created));
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class CandidatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    List<Candidate> candidates = candidateService.getAllCandidates();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < candidates.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(candidateToJson(candidates.get(i)));
                    }
                    sb.append("]");
                    sendJson(exchange, 200, sb.toString());
                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> data = JsonUtil.parseSimpleJson(body);

                    String name = data.getOrDefault("fullName", "Applicant");
                    String email = data.getOrDefault("email", "candidate" + System.currentTimeMillis() + "@hiresphere.local");
                    String phone = data.getOrDefault("phone", "");
                    double exp = parseDouble(data.get("totalExpYears"), 0.0);
                    String edu = data.getOrDefault("highestEducation", "Bachelor");
                    String summary = data.getOrDefault("resumeSummary", "");

                    Candidate c = new Candidate(name, email, phone, exp, edu, summary);
                    List<CandidateSkill> skills = new ArrayList<>();

                    String skillsStr = data.get("skills");
                    if (skillsStr != null && !skillsStr.isEmpty()) {
                        String[] parts = skillsStr.split(";");
                        for (String p : parts) {
                            String[] sp = p.split(":");
                            if (sp.length >= 1 && !sp[0].trim().isEmpty()) {
                                Skill s = skillDAO.getOrCreateSkill(sp[0].trim(), "TECHNICAL");
                                int prof = sp.length > 1 ? parseInt(sp[1], 3) : 3;
                                double sExp = sp.length > 2 ? parseDouble(sp[2], exp) : exp;
                                skills.add(new CandidateSkill(0, s.getSkillId(), s.getSkillName(), s.getCategory(), prof, sExp));
                            }
                        }
                    }

                    int candId = candidateService.registerCandidate(c, skills);
                    Candidate created = candidateService.getCandidateById(candId);
                    sendJson(exchange, 201, candidateToJson(created));
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class SkillsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                List<Skill> list = skillDAO.getAllSkills();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    Skill s = list.get(i);
                    sb.append("{\"skillId\":").append(s.getSkillId())
                            .append(",\"skillName\":").append(JsonUtil.quote(s.getSkillName()))
                            .append(",\"category\":").append(JsonUtil.quote(s.getCategory())).append("}");
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class EvaluateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, String> data = JsonUtil.parseSimpleJson(body);
                int jobId = parseInt(data.get("jobId"), -1);
                int candId = parseInt(data.get("candidateId"), -1);

                if (jobId <= 0 || candId <= 0) {
                    sendJson(exchange, 400, "{\"error\":\"jobId and candidateId are required\"}");
                    return;
                }

                Job job = jobService.getJobById(jobId);
                Candidate candidate = candidateService.getCandidateById(candId);
                if (job == null || candidate == null) {
                    sendJson(exchange, 404, "{\"error\":\"Job or Candidate not found\"}");
                    return;
                }

                MatchResult result = SkillMatchEngine.evaluate(job, candidate);
                sendJson(exchange, 200, matchResultToJson(result, job, candidate));
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class BatchScreenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");
                int jobId = -1;
                if (parts.length > 0) {
                    jobId = parseInt(parts[parts.length - 1], -1);
                }

                if (jobId <= 0) {
                    sendJson(exchange, 400, "{\"error\":\"Job ID must be specified in URL: /api/match/batch-screen/{jobId}\"}");
                    return;
                }

                int screened = analyticsService.batchScreenAllCandidatesForJob(jobId);
                List<Application> ranked = analyticsService.getRankedApplicantsForJob(jobId);

                StringBuilder sb = new StringBuilder("{\"screenedCount\":").append(screened).append(",\"applicants\":[");
                for (int i = 0; i < ranked.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(applicationToJson(ranked.get(i)));
                }
                sb.append("]}");
                sendJson(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class ApplicationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getQuery();

                if (path.contains("/notes")) {
                    String[] parts = path.split("/");
                    int appId = -1;
                    for (int i = 0; i < parts.length; i++) {
                        if ("applications".equals(parts[i]) && i + 1 < parts.length) {
                            appId = parseInt(parts[i + 1], -1);
                            break;
                        }
                    }
                    if ("GET".equals(method)) {
                        List<Map<String, Object>> notes = analyticsService.getApplicationNotes(appId);
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < notes.size(); i++) {
                            if (i > 0) sb.append(",");
                            Map<String, Object> n = notes.get(i);
                            sb.append("{\"noteId\":").append(n.get("noteId"))
                                    .append(",\"author\":").append(JsonUtil.quote((String) n.get("author")))
                                    .append(",\"noteText\":").append(JsonUtil.quote((String) n.get("noteText")))
                                    .append(",\"rating\":").append(n.get("rating"))
                                    .append(",\"createdAt\":").append(JsonUtil.quote((String) n.get("createdAt")))
                                    .append("}");
                        }
                        sb.append("]");
                        sendJson(exchange, 200, sb.toString());
                        return;
                    } else if ("POST".equals(method)) {
                        String body = readBody(exchange);
                        Map<String, String> data = JsonUtil.parseSimpleJson(body);
                        String author = data.getOrDefault("author", "Recruiter");
                        String noteText = data.getOrDefault("noteText", "");
                        int rating = parseInt(data.get("rating"), 4);
                        analyticsService.addApplicationNote(appId, author, noteText, rating);
                        sendJson(exchange, 201, "{\"success\":true}");
                        return;
                    }
                }

                if ("GET".equals(method)) {
                    List<Application> apps;
                    if (query != null && query.contains("jobId=")) {
                        int jId = parseInt(query.replace("jobId=", "").trim(), -1);
                        apps = analyticsService.getRankedApplicantsForJob(jId);
                    } else {
                        apps = analyticsService.getAllApplications();
                    }

                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < apps.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(applicationToJson(apps.get(i)));
                    }
                    sb.append("]");
                    sendJson(exchange, 200, sb.toString());
                } else if ("PUT".equals(method)) {
                    // Expect /api/applications/{id}/status
                    String[] parts = path.split("/");
                    int appId = -1;
                    if (parts.length >= 4) {
                        appId = parseInt(parts[parts.length - 2], -1);
                    } else if (parts.length >= 3) {
                        appId = parseInt(parts[parts.length - 1], -1);
                    }

                    String body = readBody(exchange);
                    Map<String, String> data = JsonUtil.parseSimpleJson(body);
                    String status = data.getOrDefault("status", "SCREENED");

                    if (appId > 0) {
                        analyticsService.updateApplicationStatus(appId, status);
                        sendJson(exchange, 200, "{\"success\":true,\"applicationId\":" + appId + ",\"status\":" + JsonUtil.quote(status) + "}");
                    } else {
                        sendJson(exchange, 400, "{\"error\":\"Invalid application ID\"}");
                    }
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class FunnelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                Map<String, Object> funnel = analyticsService.getRecruitmentFunnelSummary();
                StringBuilder sb = new StringBuilder("{");
                int i = 0;
                for (Map.Entry<String, Object> e : funnel.entrySet()) {
                    if (i++ > 0) sb.append(",");
                    sb.append(JsonUtil.quote(e.getKey())).append(":").append(e.getValue());
                }
                sb.append("}");
                sendJson(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class SkillDemandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                Map<String, Integer> demand = analyticsService.getSkillDemandMatrix();
                StringBuilder sb = new StringBuilder("[");
                int i = 0;
                for (Map.Entry<String, Integer> e : demand.entrySet()) {
                    if (i++ > 0) sb.append(",");
                    sb.append("{\"skill\":").append(JsonUtil.quote(e.getKey()))
                            .append(",\"demand\":").append(e.getValue()).append("}");
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class ResumeParseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, String> data = JsonUtil.parseSimpleJson(body);
                String raw = data.getOrDefault("resumeText", "");
                if (raw.trim().isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"resumeText is required\"}");
                    return;
                }

                ResumeParser.ParseResult res = resumeParser.parse(raw);
                sendJson(exchange, 200, candidateToJson(res.candidate));
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class ResumeImportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, String> data = JsonUtil.parseSimpleJson(body);
                String raw = data.getOrDefault("resumeText", "");
                int targetJobId = parseInt(data.get("jobId"), -1);

                if (raw.trim().isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"resumeText is required\"}");
                    return;
                }

                ResumeParser.ParseResult res = resumeParser.parse(raw);
                int candId = candidateService.registerCandidate(res.candidate, res.skills);
                res.candidate.setCandidateId(candId);

                int appId = -1;
                MatchResult match = null;
                if (targetJobId > 0) {
                    Job targetJob = jobService.getJobById(targetJobId);
                    if (targetJob != null) {
                        match = SkillMatchEngine.evaluate(targetJob, res.candidate);
                        appId = analyticsService.submitApplication(targetJobId, candId, "Pasted resume parse import");
                    }
                }

                StringBuilder sb = new StringBuilder("{");
                sb.append("\"candidateId\":").append(candId).append(",");
                sb.append("\"applicationId\":").append(appId).append(",");
                sb.append("\"candidate\":").append(candidateToJson(res.candidate));
                if (match != null) {
                    sb.append(",\"matchScore\":").append(match.getCompositeScore());
                    sb.append(",\"matchTier\":").append(JsonUtil.quote(match.getMatchTier()));
                }
                sb.append("}");
                sendJson(exchange, 201, sb.toString());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Check frontend/dist or web
            File distDir = new File("frontend/dist");
            File file = new File(distDir, path.substring(1));

            // SPA Fallback: If not an asset, fallback to index.html
            if (!file.exists() || file.isDirectory()) {
                file = new File(distDir, "index.html");
            }

            if (!file.exists()) {
                // If dist not built yet, return simple informational message
                String msg = "HireSphere ATS API is running. Build frontend with 'npm run build' or access React at Vite dev server (e.g. http://localhost:5173).";
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, msg.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(msg.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String contentType = "text/html; charset=UTF-8";
            if (path.endsWith(".js")) contentType = "application/javascript";
            else if (path.endsWith(".css")) contentType = "text/css";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";
            else if (path.endsWith(".json")) contentType = "application/json";
            else if (path.endsWith(".png")) contentType = "image/png";

            exchange.getResponseHeaders().set("Content-Type", contentType);
            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // --- JSON Serialization Helpers ---

    private static String jobToJson(Job j) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"jobId\":").append(j.getJobId()).append(",");
        sb.append("\"title\":").append(JsonUtil.quote(j.getTitle())).append(",");
        sb.append("\"department\":").append(JsonUtil.quote(j.getDepartment())).append(",");
        sb.append("\"minExpYears\":").append(j.getMinExpYears()).append(",");
        sb.append("\"minEducation\":").append(JsonUtil.quote(j.getMinEducation())).append(",");
        sb.append("\"status\":").append(JsonUtil.quote(j.getStatus())).append(",");
        sb.append("\"requiredSkills\":[");
        List<JobSkill> skills = j.getRequiredSkills();
        for (int i = 0; i < skills.size(); i++) {
            if (i > 0) sb.append(",");
            JobSkill js = skills.get(i);
            sb.append("{\"skillId\":").append(js.getSkillId())
                    .append(",\"skillName\":").append(JsonUtil.quote(js.getSkillName()))
                    .append(",\"category\":").append(JsonUtil.quote(js.getCategory()))
                    .append(",\"weightType\":").append(JsonUtil.quote(js.getWeightType().name()))
                    .append(",\"minProficiency\":").append(js.getMinProficiency())
                    .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String candidateToJson(Candidate c) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"candidateId\":").append(c.getCandidateId()).append(",");
        sb.append("\"fullName\":").append(JsonUtil.quote(c.getFullName())).append(",");
        sb.append("\"email\":").append(JsonUtil.quote(c.getEmail())).append(",");
        sb.append("\"phone\":").append(JsonUtil.quote(c.getPhone())).append(",");
        sb.append("\"totalExpYears\":").append(c.getTotalExpYears()).append(",");
        sb.append("\"highestEducation\":").append(JsonUtil.quote(c.getHighestEducation())).append(",");
        sb.append("\"resumeSummary\":").append(JsonUtil.quote(c.getResumeSummary())).append(",");
        sb.append("\"skills\":[");
        List<CandidateSkill> skills = c.getSkills();
        for (int i = 0; i < skills.size(); i++) {
            if (i > 0) sb.append(",");
            CandidateSkill cs = skills.get(i);
            sb.append("{\"skillId\":").append(cs.getSkillId())
                    .append(",\"skillName\":").append(JsonUtil.quote(cs.getSkillName()))
                    .append(",\"proficiencyLevel\":").append(cs.getProficiencyLevel())
                    .append(",\"yearsExperience\":").append(cs.getYearsExperience())
                    .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String applicationToJson(Application a) {
        return "{\"applicationId\":" + a.getApplicationId() +
                ",\"jobId\":" + a.getJobId() +
                ",\"jobTitle\":" + JsonUtil.quote(a.getJobTitle()) +
                ",\"candidateId\":" + a.getCandidateId() +
                ",\"candidateName\":" + JsonUtil.quote(a.getCandidateName()) +
                ",\"candidateEmail\":" + JsonUtil.quote(a.getCandidateEmail()) +
                ",\"appliedAt\":" + JsonUtil.quote(a.getAppliedAt()) +
                ",\"status\":" + JsonUtil.quote(a.getStatus()) +
                ",\"matchScore\":" + a.getMatchScore() +
                ",\"skillCoverageScore\":" + a.getSkillCoverageScore() +
                ",\"experienceFactor\":" + a.getExperienceFactor() +
                ",\"proficiencyFactor\":" + a.getProficiencyFactor() +
                ",\"notes\":" + JsonUtil.quote(a.getNotes()) +
                "}";
    }

    private static String matchResultToJson(MatchResult r, Job job, Candidate candidate) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"jobId\":").append(r.getJobId()).append(",");
        sb.append("\"jobTitle\":").append(JsonUtil.quote(job.getTitle())).append(",");
        sb.append("\"candidateId\":").append(r.getCandidateId()).append(",");
        sb.append("\"candidateName\":").append(JsonUtil.quote(candidate.getFullName())).append(",");
        sb.append("\"compositeScore\":").append(r.getCompositeScore()).append(",");
        sb.append("\"skillCoverageScore\":").append(r.getSkillCoverageScore()).append(",");
        sb.append("\"experienceFactor\":").append(r.getExperienceFactor()).append(",");
        sb.append("\"proficiencyFactor\":").append(r.getProficiencyFactor()).append(",");
        sb.append("\"matchTier\":").append(JsonUtil.quote(r.getMatchTier())).append(",");

        sb.append("\"matchedSkills\":[");
        for (int i = 0; i < r.getMatchedSkills().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(JsonUtil.quote(r.getMatchedSkills().get(i)));
        }
        sb.append("],");

        sb.append("\"skillGaps\":[");
        for (int i = 0; i < r.getSkillGaps().size(); i++) {
            if (i > 0) sb.append(",");
            SkillGap g = r.getSkillGaps().get(i);
            sb.append("{\"gapType\":").append(JsonUtil.quote(g.getGapType().name()))
                    .append(",\"skillName\":").append(JsonUtil.quote(g.getSkillName()))
                    .append(",\"requiredLevel\":").append(g.getRequiredLevel())
                    .append(",\"actualLevel\":").append(g.getActualLevel())
                    .append(",\"description\":").append(JsonUtil.quote(g.getDescription()))
                    .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static double parseDouble(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
