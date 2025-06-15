package org.eclipse.sw360.rest.resourceserver.release;

import java.util.ArrayList;
import java.util.List;

public class ReleaseMergeService {

    public void mergeReleases(String targetReleaseId, List<String> mergeFromReleaseIds) {
        // 1. Fetch Target Release
        Release target = getReleaseById(targetReleaseId);

        // 2. Fetch Releases to Merge
        List<Release> sources = getReleasesByIds(mergeFromReleaseIds);

        // 3. Validate: All releases must belong to the same component
        for (Release r : sources) {
            if (!r.getComponentId().equals(target.getComponentId())) {
                throw new RuntimeException("All releases must belong to the same component");
            }
        }

        // 4. Merge Fields (you can expand this logic)
        for (Release r : sources) {
            // Merge description (prefer longer one)
            if (r.getDescription() != null && r.getDescription().length() > target.getDescription().length()) {
                target.setDescription(r.getDescription());
            }

            // Merge license (if target license is missing)
            if (target.getLicense() == null && r.getLicense() != null) {
                target.setLicense(r.getLicense());
            }

            // TODO: Merge other fields like external IDs, attachments, etc.
        }

        // 5. Update the target release
        updateRelease(target);

        // 6. Delete/Archive the merged-from releases
        deleteReleases(mergeFromReleaseIds);

        System.out.println("Merged releases: " + mergeFromReleaseIds + " into target: " + targetReleaseId);
    }

    // ---------------- Helper Methods ----------------

    private Release getReleaseById(String id) {
        // TODO: Replace with actual DB/service call
        System.out.println("Fetching release with ID: " + id);
        return new Release(id, "component123", "Sample description", "MIT");
    }

    private List<Release> getReleasesByIds(List<String> ids) {
        List<Release> releases = new ArrayList<>();
        for (String id : ids) {
            releases.add(getReleaseById(id));
        }
        return releases;
    }

    private void updateRelease(Release release) {
        // TODO: Replace with actual update logic (DB or API call)
        System.out.println("Updating release: " + release.getId());
    }

    private void deleteReleases(List<String> releaseIds) {
        // TODO: Replace with actual deletion logic
        for (String id : releaseIds) {
            System.out.println("Deleting release: " + id);
        }
    }

    // ---------------- Dummy Release Class ----------------

    // This is a dummy class. Replace with the actual SW360 Release model.
    private static class Release {
        private String id;
        private String componentId;
        private String description;
        private String license;

        public Release() {}

        public Release(String id, String componentId, String description, String license) {
            this.id = id;
            this.componentId = componentId;
            this.description = description;
            this.license = license;
        }

        public String getId() {
            return id;
        }

        public String getComponentId() {
            return componentId;
        }

        public String getDescription() {
            return description == null ? "" : description;
        }

        public String getLicense() {
            return license;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setLicense(String license) {
            this.license = license;
        }
    }
}
