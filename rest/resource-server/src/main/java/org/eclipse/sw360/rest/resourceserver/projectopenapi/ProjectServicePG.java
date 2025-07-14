package org.eclipse.sw360.rest.resourceserver.projectopenapi;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.sw360.datahandler.componentsApi.model.ComponentAPI;
import org.eclipse.sw360.datahandler.componentsApi.model.ProjectAPI;
import org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI;
import org.eclipse.sw360.datahandler.postgres.ComponentPG;
import org.eclipse.sw360.datahandler.postgres.ProjectPG;
import org.eclipse.sw360.datahandler.postgresql.ProjectRepositoryPG;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.data.domain.Pageable;

public class ProjectServicePG {

    private ProjectRepositoryPG projectRepository = new ProjectRepositoryPG();

    public ProjectPG createProject(ProjectPG project, User user) {

        ProjectPG internalProject = new ProjectPG(project);
        internalProject.setCreatedBy(user != null ? user.getEmail() : "system");
        internalProject.setCreatedOn(project.getCreatedOn());

        if (project.getComponents() != null) {
            List<ComponentAPI> components = project.getComponents().stream().toList();
            internalProject.setComponents(components);
        } else {
            internalProject.setComponents(List.of());
        }

        return projectRepository.saveProject(internalProject);

    }

    public RequestStatus deleteProject(String id, User sw360User) {
        try {
            ProjectPG project = projectRepository.getProjectById(id);
            if (project != null) {
                projectRepository.deleteProject(project);
                return RequestStatus.SUCCESS;
            } else {
                return RequestStatus.FAILURE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RequestStatus.FAILURE;
        }
    }

    public List<ProjectPG> getRecentProjects(int i) {
        return projectRepository.getProjects();

    }

    public ProjectPG getProjectForUserById(String id, User user) {
        ProjectPG project = projectRepository.getProjectById(id);
        if (project != null) {
            return project;
        } else {
            return null;
        }
    }

    public ProjectPG updateProject(String id, ProjectAPI sw360Project, User user) {
        ProjectPG project = projectRepository.getProjectById(id);
        if (project != null) {
            ProjectPG updatedProject = new ProjectPG(sw360Project);
            updatedProject.setId(project.getId());
            updatedProject.setComponents(sw360Project.getComponents());
            return projectRepository.saveProject(updatedProject);
        } else {
            return null;
        }
    }

    public List<ProjectPG> getProjectsForUser(User sw360User, Pageable pageable,
            Map<String, String> params) {
        return projectRepository.getProjectsForUser(sw360User, pageable.getPageSize(),
                pageable.getPageNumber(), params);
    }

    public Object linkReleases(String id, List<ReleaseAPI> releasesInRequestBody, String comment) {
        ProjectPG project = projectRepository.getProjectById(id);
        if (project != null) {
            // project.setrele
            projectRepository.saveProject(project);
            return RequestStatus.SUCCESS;
        } else {
            return RequestStatus.FAILURE;
        }
    }

    public List<ComponentPG> getComponentsForProject(UUID id, User sw360User) {
        ProjectPG project = projectRepository.getProjectById(id.toString());
        if (project != null && project.getComponents() != null) {
            return project.getComponents().stream().map(component -> {
                ComponentPG componentPG = new ComponentPG();
                componentPG.setId(component.getId());
                componentPG.setName(component.getName());
                return componentPG;
            }).toList();
        } else {
            return List.of();
        }
    }

    public Object linkComponents(String id, List<ComponentAPI> components, String comment) {
        ProjectPG project = projectRepository.getProjectById(id);
        if (project != null) {
            project.setComponents(components);
            projectRepository.saveProject(project);
            return RequestStatus.SUCCESS;
        } else {
            return RequestStatus.FAILURE;
        }
    }

}
