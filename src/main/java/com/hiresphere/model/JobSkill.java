package com.hiresphere.model;

/**
 * Represents a skill requirement for a specific job, including importance weighting
 * and minimum proficiency requirements.
 */
public class JobSkill {
    public enum WeightType {
        MANDATORY(3.0),
        PREFERRED(2.0),
        OPTIONAL(1.0);

        private final double weightMultiplier;

        WeightType(double weightMultiplier) {
            this.weightMultiplier = weightMultiplier;
        }

        public double getWeightMultiplier() {
            return weightMultiplier;
        }

        public static WeightType fromString(String val) {
            if (val == null) return OPTIONAL;
            try {
                return WeightType.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return OPTIONAL;
            }
        }
    }

    private int jobId;
    private int skillId;
    private String skillName;
    private String category;
    private WeightType weightType;
    private int minProficiency; // 1 to 5 scale

    public JobSkill() {
        this.weightType = WeightType.MANDATORY;
        this.minProficiency = 1;
    }

    public JobSkill(int jobId, int skillId, String skillName, String category, WeightType weightType, int minProficiency) {
        this.jobId = jobId;
        this.skillId = skillId;
        this.skillName = skillName;
        this.category = category;
        this.weightType = weightType;
        this.minProficiency = minProficiency;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
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

    public WeightType getWeightType() {
        return weightType;
    }

    public void setWeightType(WeightType weightType) {
        this.weightType = weightType;
    }

    public int getMinProficiency() {
        return minProficiency;
    }

    public void setMinProficiency(int minProficiency) {
        this.minProficiency = minProficiency;
    }

    @Override
    public String toString() {
        return skillName + " [" + weightType + ", Min Lvl: " + minProficiency + "/5]";
    }
}

