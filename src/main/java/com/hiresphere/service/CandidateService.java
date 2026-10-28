package com.hiresphere.service;

import com.hiresphere.dao.CandidateDAO;
import com.hiresphere.model.Candidate;
import com.hiresphere.model.CandidateSkill;
import com.hiresphere.util.CsvParser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service managing candidate registrations, lookups, and bulk CSV ingestion.
 */
public class CandidateService {

    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final CsvParser csvParser = new CsvParser();

    public int registerCandidate(Candidate candidate, List<CandidateSkill> skills) throws SQLException {
        return candidateDAO.createCandidate(candidate, skills);
    }

    public List<Candidate> getAllCandidates() throws SQLException {
        return candidateDAO.getAllCandidates();
    }

    public Candidate getCandidateById(int candidateId) throws SQLException {
        return candidateDAO.getCandidateById(candidateId);
    }

    public Candidate getCandidateByEmail(String email) throws SQLException {
        return candidateDAO.getCandidateByEmail(email);
    }

    public int importFromCsv(String filePath) throws IOException, SQLException {
        return csvParser.importCandidatesFromCsv(filePath);
    }
}

