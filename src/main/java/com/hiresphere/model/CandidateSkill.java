package com.hiresphere.model;

/**
 * Represents a skill possessed by a candidate, along with their self/assessed proficiency
 * and years of hands-on experience.
 */
public class CandidateSkill {
    private int candidateId;
    private int skillId;
    private String skillName;
    private String category;
    private int proficiencyLevel; // 1 to 5 scale
    private double yearsExperience;

    public CandidateSkill() {
        this.proficiencyLevel = 1;
        this.yearsExperience = 0.0;
    }

    public CandidateSkill(int candidateId, int skillId, String skillName, String category, int proficiencyLevel, double yearsExperience) {
        this.candidateId = candidateId;
        this.skillId = skillId;
        this.skillName = skillName;
        this.category = category;
        this.proficiencyLevel = proficiencyLevel;
        this.yearsExperience = yearsExperience;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(int proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public double getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(double yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    @Override
    public String toString() {
        return skillName + " (" + proficiencyLevel + "/5, " + yearsExperience + " yrs)";
    }
}

