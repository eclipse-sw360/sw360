package org.eclipse.sw360.datahandler.postgresql;

import java.util.List;
import java.util.UUID;
import org.eclipse.sw360.datahandler.postgres.ReleasePG;
import org.hibernate.Session;

public class ReleaseRepositoryPG {

    public ReleasePG saveRelease(ReleasePG release) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        ReleasePG savedRelease = session.merge(release);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();
        return savedRelease;
    }

    public ReleasePG getReleaseById(String id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        ReleasePG release = session.get(ReleasePG.class, UUID.fromString(id));
        session.getTransaction().commit();
        session.close();
        return release;
    }

    public List<ReleasePG> getReleasesByComponentId(String id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        List<ReleasePG> releases = session.createQuery("FROM Release WHERE component.id = :id", ReleasePG.class)
                .setParameter("id", UUID.fromString(id)).getResultList();
        session.getTransaction().commit();
        session.close();

        return releases;
    }

    public void deleteRelease(ReleasePG release) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        session.remove(release);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();
    }

    public List<ReleasePG> getAllReleases(int page, int size) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        List<ReleasePG> releases = session.createQuery("FROM ReleasePG", ReleasePG.class)
                .setFirstResult(page * size).setMaxResults(size).getResultList();

        session.flush();
        session.getTransaction().commit();
        session.close();

        return releases;
    }
}
