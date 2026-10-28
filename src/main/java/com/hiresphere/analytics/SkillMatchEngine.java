package com.hiresphere.analytics;

import com.hiresphere.model.Candidate;
import com.hiresphere.model.CandidateSkill;
import com.hiresphere.model.Job;
import com.hiresphere.model.JobSkill;
import com.hiresphere.model.MatchResult;
import com.hiresphere.model.SkillGap;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill-Match Analytics Engine.
 * 
 * Implements multi-factor weighted matching algorithm:
 * 1. Skill Coverage Score (55% weight): Evaluates presence of mandatory (3x), preferred (2x), and optional (1x) skills.
 * 2. Experience Alignment Factor (25% weight): Evaluates candidate's total experience against job requisites.
 * 3. Proficiency Alignment Factor (20% weight): Assesses candidate competency level against required benchmark.
 * 4. Skill Gap Diagnostics: Pinpoints specific deficits for recruiter insights.
 */
public class SkillMatchEngine {

    private static final double WEIGHT_SKILL_COVERAGE = 0.55;
    private static final double WEIGHT_EXPERIENCE = 0.25;
    private static final double WEIGHT_PROFICIENCY = 0.20;

    public static MatchResult evaluate(Job job, Candidate candidate) {
        MatchResult result = new MatchResult();
        result.setJobId(job.getJobId());
        result.setCandidateId(candidate.getCandidateId());

        // Map candidate skills for fast O(1) lookup by normalized lowercase name
        Map<String, CandidateSkill> candSkillMap = new HashMap<>();
        if (candidate.getSkills() != null) {
            for (CandidateSkill cs : candidate.getSkills()) {
                if (cs.getSkillName() != null) {
                    candSkillMap.put(cs.getSkillName().trim().toLowerCase(), cs);
                }
            }
        }

        // 1. Skill Coverage Calculation
        double totalPossibleSkillWeight = 0.0;
        double earnedSkillWeight = 0.0;
        double totalProficiencyScore = 0.0;
        int evaluatedProficiencySkillsCount = 0;
        int missingMandatoryCount = 0;

        if (job.getRequiredSkills() == null || job.getRequiredSkills().isEmpty()) {
            // If job specifies no skill constraints
            totalPossibleSkillWeight = 1.0;
            earnedSkillWeight = 1.0;
        } else {
            for (JobSkill req : job.getRequiredSkills()) {
                double weightMultiplier = req.getWeightType().getWeightMultiplier();
                totalPossibleSkillWeight += weightMultiplier;

                String normName = req.getSkillName() != null ? req.getSkillName().trim().toLowerCase() : "";
                CandidateSkill candSkill = candSkillMap.get(normName);

                if (candSkill != null) {
                    // Skill is present
                    earnedSkillWeight += weightMultiplier;
                    result.addMatchedSkill(req.getSkillName() + " (" + candSkill.getProficiencyLevel() + "/5)");

                    // Proficiency alignment for this skill
                    double profRatio = Math.min(1.0, (double) candSkill.getProficiencyLevel() / (double) req.getMinProficiency());
                    totalProficiencyScore += (profRatio * 100.0);
                    evaluatedProficiencySkillsCount++;

                    if (candSkill.getProficiencyLevel() < req.getMinProficiency()) {
                        result.addSkillGap(new SkillGap(
                                SkillGap.GapType.DEFICIENT_PROFICIENCY,
                                req.getSkillName(),
                                req.getMinProficiency(),
                                candSkill.getProficiencyLevel(),
                                req.getSkillName() + ": candidate level " + candSkill.getProficiencyLevel() +
                                        "/5 is below required " + req.getMinProficiency() + "/5"
                        ));
                    }
                } else {
                    // Skill is missing
                    if (req.getWeightType() == JobSkill.WeightType.MANDATORY) {
                        missingMandatoryCount++;
                        result.addSkillGap(new SkillGap(
                                SkillGap.GapType.MISSING_MANDATORY_SKILL,
                                req.getSkillName(),
                                req.getMinProficiency(),
                                0,
                                "Missing mandatory skill: " + req.getSkillName() + " (min level " + req.getMinProficiency() + "/5)"
                        ));
                    } else if (req.getWeightType() == JobSkill.WeightType.PREFERRED) {
                        result.addSkillGap(new SkillGap(
                                SkillGap.GapType.MISSING_PREFERRED_SKILL,
                                req.getSkillName(),
                                req.getMinProficiency(),
                                0,
                                "Missing preferred skill: " + req.getSkillName()
                        ));
                    }
                }
            }
        }

        double skillCoverage = totalPossibleSkillWeight > 0
                ? (earnedSkillWeight / totalPossibleSkillWeight) * 100.0
                : 100.0;

        // Apply mandatory skill deficit penalty (each missing mandatory skill docks 10% from coverage)
        if (missingMandatoryCount > 0) {
            double penalty = missingMandatoryCount * 12.0;
            skillCoverage = Math.max(0.0, skillCoverage - penalty);
        }

        // 2. Proficiency Factor Calculation
        double proficiencyFactor;
        if (evaluatedProficiencySkillsCount > 0) {
            proficiencyFactor = totalProficiencyScore / evaluatedProficiencySkillsCount;
        } else if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            proficiencyFactor = 0.0;
        } else {
            proficiencyFactor = 100.0;
        }

        // 3. Experience Factor Calculation
        double expFactor;
        if (job.getMinExpYears() <= 0.0) {
            expFactor = 100.0;
        } else {
            double ratio = candidate.getTotalExpYears() / job.getMinExpYears();
            if (ratio >= 1.0) {
                // Meets or exceeds requirement
                expFactor = Math.min(100.0, 100.0 + (ratio - 1.0) * 5.0); // max 100%
            } else {
                expFactor = Math.max(0.0, ratio * 100.0);
                double shortfall = job.getMinExpYears() - candidate.getTotalExpYears();
                result.addSkillGap(new SkillGap(
                        SkillGap.GapType.INSUFFICIENT_EXPERIENCE,
                        "Overall Experience",
                        (int) Math.ceil(job.getMinExpYears()),
                        (int) Math.floor(candidate.getTotalExpYears()),
                        "Experience shortfall: " + String.format("%.1f", candidate.getTotalExpYears()) +
                                " yrs vs required " + String.format("%.1f", job.getMinExpYears()) + " yrs (" +
                                String.format("%.1f", shortfall) + " yrs gap)"
                ));
            }
        }

        // 4. Composite Score Aggregation
        double composite = (skillCoverage * WEIGHT_SKILL_COVERAGE) +
                           (expFactor * WEIGHT_EXPERIENCE) +
                           (proficiencyFactor * WEIGHT_PROFICIENCY);

        composite = Math.max(0.0, Math.min(100.0, composite));

        // Format to 1 decimal point
        result.setSkillCoverageScore(Math.round(skillCoverage * 10.0) / 10.0);
        result.setExperienceFactor(Math.round(expFactor * 10.0) / 10.0);
        result.setProficiencyFactor(Math.round(proficiencyFactor * 10.0) / 10.0);
        result.setCompositeScore(Math.round(composite * 10.0) / 10.0);

        return result;
    }
}

