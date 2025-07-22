package org.eclipse.sw360.datahandler.postgresql;

import org.hibernate.HibernateError;
import org.hibernate.Session;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.sw360.datahandler.postgres.ProjectPG;
import org.eclipse.sw360.datahandler.thrift.users.User;

public class ProjectRepositoryPG {

    public ProjectPG saveProject(ProjectPG project) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        ProjectPG savedProject = session.merge(project);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();

        return savedProject;

    }

    public ProjectPG getProjectById(String id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        ProjectPG project = session.get(ProjectPG.class, UUID.fromString(id));
        session.close();
        return project;
    }

    public void deleteProject(ProjectPG project) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        try {
            session.remove(project);
            session.getTransaction().commit();
        } catch (HibernateError e) {
            session.getTransaction().rollback();
            throw e;
        } finally {
            session.clear();
            session.close();
        }
    }

    public List<ProjectPG> getProjects() {
        try {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                return session.createQuery("FROM ProjectPG", ProjectPG.class).getResultList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<ProjectPG> getProjectsForUser(User sw360User, Integer pagesize, Integer pagenumber,
            Map<String, String> params) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ProjectPG", ProjectPG.class)
                    .setFirstResult((int) pagenumber).setMaxResults(pagesize).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

}
