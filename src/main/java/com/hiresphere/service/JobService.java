package com.hiresphere.service;

import com.hiresphere.dao.JobDAO;
import com.hiresphere.dao.SkillDAO;
import com.hiresphere.model.Job;
import com.hiresphere.model.JobSkill;
import com.hiresphere.model.Skill;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing job requisition lifecycle and sample requisition bootstrapping.
 */
public class JobService {

    private final JobDAO jobDAO = new JobDAO();
    private final SkillDAO skillDAO = new SkillDAO();

    public int createJob(Job job, List<JobSkill> requiredSkills) throws SQLException {
        return jobDAO.createJob(job, requiredSkills);
    }

    public List<Job> getAllJobs() throws SQLException {
        return jobDAO.getAllJobs();
    }

    public Job getJobById(int jobId) throws SQLException {
        return jobDAO.getJobById(jobId);
    }

    public void updateJobStatus(int jobId, String status) throws SQLException {
        jobDAO.updateJobStatus(jobId, status);
    }

    /**
     * Seeds initial real-world job postings if the database is currently empty.
     */
    public void seedInitialJobsIfEmpty() {
        try {
            List<Job> existing = jobDAO.getAllJobs();
            if (!existing.isEmpty()) {
                return; // already has jobs
            }

            // Job 1: Senior Java Backend Developer
            Job job1 = new Job(0, "Senior Java Backend Engineer", "Core Platform", 3.5, "Bachelor in CSE / IT", "OPEN");
            List<JobSkill> skills1 = new ArrayList<>();

            Skill java = skillDAO.getOrCreateSkill("Java", "BACKEND");
            Skill spring = skillDAO.getOrCreateSkill("Spring Boot", "BACKEND");
            Skill mysql = skillDAO.getOrCreateSkill("MySQL", "DATABASE");
            Skill docker = skillDAO.getOrCreateSkill("Docker", "DEVOPS");
            Skill rest = skillDAO.getOrCreateSkill("REST APIs", "BACKEND");

            skills1.add(new JobSkill(0, java.getSkillId(), java.getSkillName(), java.getCategory(), JobSkill.WeightType.MANDATORY, 4));
            skills1.add(new JobSkill(0, spring.getSkillId(), spring.getSkillName(), spring.getCategory(), JobSkill.WeightType.MANDATORY, 3));
            skills1.add(new JobSkill(0, mysql.getSkillId(), mysql.getSkillName(), mysql.getCategory(), JobSkill.WeightType.PREFERRED, 3));
            skills1.add(new JobSkill(0, rest.getSkillId(), rest.getSkillName(), rest.getCategory(), JobSkill.WeightType.PREFERRED, 3));
            skills1.add(new JobSkill(0, docker.getSkillId(), docker.getSkillName(), docker.getCategory(), JobSkill.WeightType.OPTIONAL, 2));

            jobDAO.createJob(job1, skills1);

            // Job 2: Cloud Native DevOps / Backend Architect
            Job job2 = new Job(0, "Cloud Microservices Specialist", "Infrastructure", 4.0, "Bachelor or Master", "OPEN");
            List<JobSkill> skills2 = new ArrayList<>();

            Skill k8s = skillDAO.getOrCreateSkill("Kubernetes", "DEVOPS");
            Skill aws = skillDAO.getOrCreateSkill("AWS", "CLOUD");
            Skill micro = skillDAO.getOrCreateSkill("Microservices", "BACKEND");

            skills2.add(new JobSkill(0, java.getSkillId(), java.getSkillName(), java.getCategory(), JobSkill.WeightType.MANDATORY, 4));
            skills2.add(new JobSkill(0, docker.getSkillId(), docker.getSkillName(), docker.getCategory(), JobSkill.WeightType.MANDATORY, 4));
            skills2.add(new JobSkill(0, k8s.getSkillId(), k8s.getSkillName(), k8s.getCategory(), JobSkill.WeightType.PREFERRED, 3));
            skills2.add(new JobSkill(0, aws.getSkillId(), aws.getSkillName(), aws.getCategory(), JobSkill.WeightType.PREFERRED, 3));
            skills2.add(new JobSkill(0, micro.getSkillId(), micro.getSkillName(), micro.getCategory(), JobSkill.WeightType.MANDATORY, 4));

            jobDAO.createJob(job2, skills2);

        } catch (SQLException e) {
            System.err.println("[WARN] Initial jobs seed note: " + e.getMessage());
        }
    }
}

