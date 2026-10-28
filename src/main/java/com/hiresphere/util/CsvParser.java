package com.hiresphere.util;

import com.hiresphere.dao.CandidateDAO;
import com.hiresphere.dao.SkillDAO;
import com.hiresphere.model.Candidate;
import com.hiresphere.model.CandidateSkill;
import com.hiresphere.model.Skill;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk ingestion parser for resume/applicant CSV data.
 */
public class CsvParser {

    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final SkillDAO skillDAO = new SkillDAO();

    public int importCandidatesFromCsv(String filePath) throws IOException, SQLException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Read header line
            if (line == null) return 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                List<String> tokens = parseCsvLine(line);
                if (tokens.size() < 6) continue;

                String fullName = tokens.get(0).trim();
                String email = tokens.get(1).trim();
                String phone = tokens.get(2).trim();
                double expYears = 0.0;
                try {
                    expYears = Double.parseDouble(tokens.get(3).trim());
                } catch (NumberFormatException ignored) {}

                String education = tokens.get(4).trim();
                String skillsRaw = tokens.get(5).trim();
                String summary = tokens.size() > 6 ? tokens.get(6).trim() : "";

                // Check if candidate already exists
                Candidate existing = candidateDAO.getCandidateByEmail(email);
                if (existing != null) {
                    continue; // Skip duplicate email
                }

                Candidate candidate = new Candidate(fullName, email, phone, expYears, education, summary);

                // Parse candidate skills
                List<CandidateSkill> skills = new ArrayList<>();
                if (!skillsRaw.isEmpty()) {
                    String[] skillTokens = skillsRaw.split(";");
                    for (String st : skillTokens) {
                        String[] parts = st.split(":");
                        if (parts.length >= 1) {
                            String skillName = parts[0].trim();
                            int proficiency = 3;
                            double years = expYears;
                            if (parts.length >= 2) {
                                try { proficiency = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                            }
                            if (parts.length >= 3) {
                                try { years = Double.parseDouble(parts[2].trim()); } catch (Exception ignored) {}
                            }

                            Skill s = skillDAO.getOrCreateSkill(skillName, "TECHNICAL");
                            skills.add(new CandidateSkill(0, s.getSkillId(), s.getSkillName(), s.getCategory(), proficiency, years));
                        }
                    }
                }

                candidateDAO.createCandidate(candidate, skills);
                count++;
            }
        }
        return count;
    }

    private List<String> parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                list.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        list.add(cur.toString());
        return list;
    }
}

