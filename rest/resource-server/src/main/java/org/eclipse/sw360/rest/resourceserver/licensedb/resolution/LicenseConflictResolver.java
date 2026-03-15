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
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class LicenseConflictResolver implements ConflictResolver<License> {

    private final ConflictConfig config;

    @Override
    public ConflictResult<License> resolve(License incoming, License existing, ResolutionStrategy strategy) {
        ConflictResult<License> result = new ConflictResult<>();

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

        ResolutionStrategy effectiveStrategy = strategy != null ? strategy : config.getDefaultLicenseStrategy();
        result.setStrategy(effectiveStrategy);

        switch (effectiveStrategy) {
            case REPLACE:
                result.setResolvedData(applyStrategy(incoming, existing, ResolutionStrategy.REPLACE));
                log.info("License conflict resolved using REPLACE strategy for: {}", incoming.getShortname());
                break;
            case SKIP:
                result.setResolvedData(existing);
                log.info("License conflict resolved using SKIP strategy for: {}", incoming.getShortname());
                break;
            case MERGE:
                result.setResolvedData(applyStrategy(incoming, existing, ResolutionStrategy.MERGE));
                log.info("License conflict resolved using MERGE strategy for: {}", incoming.getShortname());
                break;
            case MANUAL:
                result.setResolvedData(existing);
                result.markForManualReview("License requires manual review due to conflicts");
                log.warn("License flagged for manual review: {}", incoming.getShortname());
                break;
        }

        if (config.isAuditEnabled()) {
            logAuditResolution(incoming.getShortname(), existing.getShortname(), result);
        }

        return result;
    }

    @Override
    public boolean hasConflict(License incoming, License existing) {
        if (incoming == null || existing == null) {
            return false;
        }

        if (!Objects.equals(incoming.getFullname(), existing.getFullname())) {
            return true;
        }
        if (!Objects.equals(incoming.getText(), existing.getText())) {
            return true;
        }
        if (!Objects.equals(incoming.getLicenseTypeDatabaseId(), existing.getLicenseTypeDatabaseId())) {
            return true;
        }
        if (incoming.getOsiApproved() != null && existing.getOsiApproved() != null
                && !incoming.getOsiApproved().equals(existing.getOsiApproved())) {
            return true;
        }

        return false;
    }

    @Override
    public License applyStrategy(License incoming, License existing, ResolutionStrategy strategy) {
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

    private License applyReplace(License incoming, License existing) {
        existing.setFullname(incoming.getFullname());
        existing.setText(incoming.getText());
        existing.setLicenseTypeDatabaseId(incoming.getLicenseTypeDatabaseId());
        existing.setOsiApproved(incoming.getOsiApproved());
        existing.setExternalIds(incoming.getExternalIds());
        existing.setAdditionalData(incoming.getAdditionalData());
        return existing;
    }

    private License applyMerge(License incoming, License existing) {
        if (incoming.getFullname() != null && !incoming.getFullname().isEmpty()) {
            existing.setFullname(incoming.getFullname());
        }
        if (incoming.getText() != null && !incoming.getText().isEmpty()) {
            existing.setText(incoming.getText());
        }
        if (incoming.getLicenseTypeDatabaseId() != null) {
            existing.setLicenseTypeDatabaseId(incoming.getLicenseTypeDatabaseId());
        }
        if (incoming.getOsiApproved() != null) {
            existing.setOsiApproved(incoming.getOsiApproved());
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

    private void logAuditResolution(String incomingShortname, String existingShortname, ConflictResult<License> result) {
        log.info("AUDIT: License conflict resolution - incoming: {}, existing: {}, strategy: {}, requiresReview: {}, conflicts: {}",
                incomingShortname, existingShortname, result.getStrategy(),
                result.isRequiresManualReview(), result.getConflicts());
    }
}
