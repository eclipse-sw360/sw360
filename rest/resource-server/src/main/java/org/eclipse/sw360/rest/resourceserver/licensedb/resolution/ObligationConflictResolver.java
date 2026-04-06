/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb.resolution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class ObligationConflictResolver implements ConflictResolver<Obligation> {

    private final ConflictConfig config;

    @Override
    public ConflictResult<Obligation> resolve(Obligation incoming, Obligation existing, ResolutionStrategy strategy) {
        ConflictResult<Obligation> result = new ConflictResult<>();

        if (existing == null) {
            result.setResolvedData(incoming);
            result.setStrategy(ResolutionStrategy.REPLACE);
            return result;
        }

        boolean hasConflict = hasConflict(incoming, existing);
        result.setHasConflict(hasConflict);

        if (!hasConflict) {
            result.setResolvedData(incoming);
            result.setStrategy(ResolutionStrategy.REPLACE);
            return result;
        }

        ResolutionStrategy effectiveStrategy = strategy != null ? strategy : config.getDefaultObligationStrategy();
        result.setStrategy(effectiveStrategy);

        switch (effectiveStrategy) {
            case REPLACE:
                result.setResolvedData(applyStrategy(incoming, existing, ResolutionStrategy.REPLACE));
                log.info("Obligation conflict resolved using REPLACE strategy for: {}", incoming.getTitle());
                break;
            case SKIP:
                result.setResolvedData(existing);
                log.info("Obligation conflict resolved using SKIP strategy for: {}", incoming.getTitle());
                break;
            case MERGE:
                result.setResolvedData(applyStrategy(incoming, existing, ResolutionStrategy.MERGE));
                log.info("Obligation conflict resolved using MERGE strategy for: {}", incoming.getTitle());
                break;
            case MANUAL:
                result.setResolvedData(existing);
                result.markForManualReview("Obligation requires manual review due to conflicts");
                log.warn("Obligation flagged for manual review: {}", incoming.getTitle());
                break;
        }

        if (config.isAuditEnabled()) {
            logAuditResolution(incoming.getTitle(), existing.getTitle(), result);
        }

        return result;
    }

    @Override
    public boolean hasConflict(Obligation incoming, Obligation existing) {
        if (incoming == null || existing == null) {
            return false;
        }

        if (!Objects.equals(incoming.getText(), existing.getText())) {
            return true;
        }
        if (incoming.getObligationType() != null && existing.getObligationType() != null
                && !incoming.getObligationType().equals(existing.getObligationType())) {
            return true;
        }
        if (incoming.getObligationLevel() != null && existing.getObligationLevel() != null
                && !incoming.getObligationLevel().equals(existing.getObligationLevel())) {
            return true;
        }
        if (!Objects.equals(incoming.isDevelopment(), existing.isDevelopment())) {
            return true;
        }
        if (!Objects.equals(incoming.isDistribution(), existing.isDistribution())){
            return true;
        }

        return false;
    }

    @Override
    public Obligation applyStrategy(Obligation incoming, Obligation existing, ResolutionStrategy strategy) {
        if (existing == null) {
            return incoming;
        }

        switch (strategy) {
            case REPLACE:
                return applyReplace(incoming, existing);
            case SKIP:
                return existing;
            case MERGE:
                return applyMerge(incoming, existing);
            case MANUAL:
                return existing;
            default:
                return existing;
        }
    }

    private Obligation applyReplace(Obligation incoming, Obligation existing) {
        existing.setText(incoming.getText());
        existing.setObligationType(incoming.getObligationType());
        existing.setObligationLevel(incoming.getObligationLevel());
        existing.setDevelopment(incoming.isDevelopment());
        existing.setDistribution(incoming.isDistribution());
        existing.setExternalIds(incoming.getExternalIds());
        existing.setAdditionalData(incoming.getAdditionalData());
        return existing;
    }

    private Obligation applyMerge(Obligation incoming, Obligation existing) {
        if (incoming.getText() != null && !incoming.getText().isEmpty()) {
            existing.setText(incoming.getText());
        }
        if (incoming.getObligationType() != null) {
            existing.setObligationType(incoming.getObligationType());
        }
        if (incoming.getObligationLevel() != null) {
            existing.setObligationLevel(incoming.getObligationLevel());
        }
        if (incoming.isDevelopment()) {
            existing.setDevelopment(true);
        } else {
            existing.setDevelopment(false);
        }
        if (incoming.isDistribution()) {
            existing.setDistribution(true);
        } else {
            existing.setDistribution(false);
        }
        if (incoming.getExternalIds() != null && !incoming.getExternalIds().isEmpty()) {
            if (existing.getExternalIds() == null) {
                existing.setExternalIds(incoming.getExternalIds());
            } else {
                existing.getExternalIds().putAll(incoming.getExternalIds());
            }
        }
        if (incoming.getAdditionalData() != null && !incoming.getAdditionalData().isEmpty()) {
            if (existing.getAdditionalData() == null) {
                existing.setAdditionalData(incoming.getAdditionalData());
            } else {
                existing.getAdditionalData().putAll(incoming.getAdditionalData());
            }
        }
        return existing;
    }

    private void logAuditResolution(String incomingTitle, String existingTitle, ConflictResult<Obligation> result) {
        log.info("AUDIT: Obligation conflict resolution - incoming: {}, existing: {}, strategy: {}, requiresReview: {}, conflicts: {}",
                incomingTitle, existingTitle, result.getStrategy(),
                result.isRequiresManualReview(), result.getConflicts());
    }
}
