package org.eclipse.sw360.datahandler.postgres;

import jakarta.persistence.*;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;



@Entity
@JsonInclude(value = Include.NON_EMPTY, content = Include.NON_NULL)
@Table(name = "project")
public class ProjectReleaseRelationshipPG {
    public enum SW360ReleaseRelationship {
        CONTAINED(0), REFERRED(1), UNKNOWN(2), DYNAMICALLY_LINKED(3), STATICALLY_LINKED(
                4), SIDE_BY_SIDE(5), STANDALONE(6), INTERNAL_USE(7), OPTIONAL(8), TO_BE_REPLACED(9);

        private final int value;

        SW360ReleaseRelationship(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static SW360ReleaseRelationship findByValue(int value) {
            switch (value) {
                case 0:
                    return CONTAINED;
                case 1:
                    return REFERRED;
                case 2:
                    return UNKNOWN;
                case 3:
                    return DYNAMICALLY_LINKED;
                case 4:
                    return STATICALLY_LINKED;
                case 5:
                    return SIDE_BY_SIDE;
                case 6:
                    return STANDALONE;
                case 7:
                    return INTERNAL_USE;
                case 8:
                    return OPTIONAL;
                case 9:
                    return TO_BE_REPLACED;
                default:
                    return null;
            }
        }
    }

    public enum SW360MainlineState {
        OPEN(0), MAINLINE(1), SPECIFIC(2), PHASEOUT(3), DENIED(4);

        private final int value;

        SW360MainlineState(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static SW360MainlineState findByValue(int value) {
            switch (value) {
                case 0:
                    return OPEN;
                case 1:
                    return MAINLINE;
                case 2:
                    return SPECIFIC;
                case 3:
                    return PHASEOUT;
                case 4:
                    return DENIED;
                default:
                    return null;
            }
        }
    }

    private SW360ReleaseRelationship releaseRelation;
    private SW360MainlineState mainlineState;

    public ProjectReleaseRelationshipPG() {}

    public ProjectReleaseRelationshipPG(SW360ReleaseRelationship releaseRelation,
            SW360MainlineState mainlineState) {
        this.releaseRelation = releaseRelation;
        this.mainlineState = mainlineState;
    }

    public SW360ReleaseRelationship getReleaseRelation() {
        return this.releaseRelation;
    }

    public ProjectReleaseRelationshipPG setReleaseRelation(
            SW360ReleaseRelationship releaseRelation) {
        this.releaseRelation = releaseRelation;
        return this;
    }

    public SW360MainlineState getMainlineState() {
        return this.mainlineState;
    }

    public ProjectReleaseRelationshipPG setMainlineState(SW360MainlineState mainlineState) {
        this.mainlineState = mainlineState;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProjectReleaseRelationshipPG that = (ProjectReleaseRelationshipPG) o;
        return releaseRelation == that.releaseRelation && mainlineState == that.mainlineState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(releaseRelation, mainlineState);
    }
}
