package com.hiresphere.model;

/**
 * Details a specific skill or qualification gap between a candidate and a job requisition.
 */
public class SkillGap {
    public enum GapType {
        MISSING_MANDATORY_SKILL,
        MISSING_PREFERRED_SKILL,
        DEFICIENT_PROFICIENCY,
        INSUFFICIENT_EXPERIENCE
    }

    private GapType gapType;
    private String skillName;
    private int requiredLevel;
    private int actualLevel;
    private String description;

    public SkillGap(GapType gapType, String skillName, int requiredLevel, int actualLevel, String description) {
        this.gapType = gapType;
        this.skillName = skillName;
        this.requiredLevel = requiredLevel;
        this.actualLevel = actualLevel;
        this.description = description;
    }

    public GapType getGapType() {
        return gapType;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getActualLevel() {
        return actualLevel;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "[" + gapType + "] " + description;
    }
}

