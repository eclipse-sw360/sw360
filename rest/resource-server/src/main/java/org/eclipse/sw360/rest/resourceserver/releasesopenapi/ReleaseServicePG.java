package org.eclipse.sw360.rest.resourceserver.releasesopenapi;

import org.eclipse.sw360.datahandler.postgres.ReleasePG;
import java.util.List;
import org.eclipse.sw360.datahandler.postgresql.ComponentRepositoryPG;
import org.eclipse.sw360.datahandler.postgresql.ReleaseRepositoryPG;
import org.eclipse.sw360.datahandler.thrift.users.User;

public class ReleaseServicePG {
        private final ReleaseRepositoryPG releaseRepository = new ReleaseRepositoryPG();
        private final ComponentRepositoryPG componentRepository = new ComponentRepositoryPG();

        public ReleasePG getReleaseForUserById(String id, User user) {
                ReleasePG release = releaseRepository.getReleaseById(id);
                if (release == null) {
                        throw new IllegalArgumentException("Release not found");
                }

                return release;
        }

        public ReleasePG createRelease(ReleasePG release, User user) {
                if (release.getName() == null || release.getName().isEmpty()) {
                        throw new IllegalArgumentException("Release name is required");
                }

                org.eclipse.sw360.datahandler.postgres.ReleasePG internalRelease =
                                new org.eclipse.sw360.datahandler.postgres.ReleasePG(release);
                org.eclipse.sw360.datahandler.postgres.ComponentPG component =
                                componentRepository.getComponentById(release.getComponentId());

                if (component == null) {
                        throw new IllegalArgumentException("Component not found");
                }
                internalRelease.setComponent(component);
                internalRelease.setComponentId(component.getId().toString());

                org.eclipse.sw360.datahandler.postgres.ReleasePG savedRelease =
                                releaseRepository.saveRelease(internalRelease);

                ReleasePG createdRelease = new ReleasePG(savedRelease);
                createdRelease.setName(savedRelease.getName());
                createdRelease.setVersion(savedRelease.getVersion());
                createdRelease.setId(savedRelease.getId());
                // createdRelease.setComponent(component != null
                // ? new org.eclipse.sw360.datahandler.postgres.ComponentPG(component)
                // : null);
                createdRelease.setComponentId(savedRelease.getComponent() != null
                                ? savedRelease.getComponent().getId().toString()
                                : null);

                return createdRelease;
        }

        public List<ReleasePG> getReleasesByComponentId(String id, User sw360User) {
                List<org.eclipse.sw360.datahandler.postgres.ReleasePG> releases =
                                releaseRepository.getReleasesByComponentId(id);
                if (releases == null || releases.isEmpty()) {
                        throw new IllegalArgumentException("No releases found for component");
                }
                List<ReleasePG> openApiReleases = releases.stream().map(release -> {
                        ReleasePG openApiRelease = new ReleasePG(release);
                        return openApiRelease;
                }).toList();

                return openApiReleases;
        }

        public List<ReleasePG> getAllReleases(User sw360User, int page, int size) {
                List<org.eclipse.sw360.datahandler.postgres.ReleasePG> releases =
                                releaseRepository.getAllReleases(page, size);
                if (releases == null) {
                        throw new IllegalArgumentException("No releases found");
                }
                List<ReleasePG> openApiReleases = releases.stream().map(ReleasePG::new).toList();

                return openApiReleases;
        }

        public List<ReleasePG> getReleasesForUser(User sw360User, int page, int size) {
                List<org.eclipse.sw360.datahandler.postgres.ReleasePG> releases =
                                releaseRepository.getAllReleases(page, size);
                if (releases == null || releases.isEmpty()) {
                        throw new IllegalArgumentException("No releases found");
                }
                return releases;
        }

        public ReleasePG updateRelease(String id, ReleasePG release, User sw360User) {
                ReleasePG existingRelease = getReleaseForUserById(id, sw360User);
                if (existingRelease == null) {
                        throw new IllegalArgumentException("Release not found");
                }

                if (release.getName() != null && !release.getName().isEmpty()) {
                        existingRelease.setName(release.getName());
                }
                if (release.getVersion() != null && !release.getVersion().isEmpty()) {
                        existingRelease.setVersion(release.getVersion());
                }
                if (release.getComponentId() != null && !release.getComponentId().isEmpty()) {
                        org.eclipse.sw360.datahandler.postgres.ComponentPG component =
                                        componentRepository
                                                        .getComponentById(release.getComponentId());
                        if (component == null) {
                                throw new IllegalArgumentException("Component not found");
                        }
                        existingRelease.setComponent(component);
                        existingRelease.setComponentId(component.getId().toString());
                }

                org.eclipse.sw360.datahandler.postgres.ReleasePG updatedRelease = releaseRepository
                                .saveRelease(new org.eclipse.sw360.datahandler.postgres.ReleasePG(
                                                existingRelease));

                return new ReleasePG(updatedRelease);
        }
}
