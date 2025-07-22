package org.eclipse.sw360.datahandler.postgresql;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.sw360.datahandler.postgres.ComponentPG;
import org.eclipse.sw360.datahandler.postgres.ReleasePG;
import org.hibernate.Session;

public class ComponentRepositoryPG {

    public ComponentPG saveComponent(ComponentPG component) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        ComponentPG savedComponent = session.merge(component);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();
        return savedComponent;
    }

    public List<ComponentPG> getComponents(Map<String, String> params) {
        String psqlString = "SELECT * FROM Component";
        Set<String> keys = params.keySet();
        if (!keys.isEmpty()) {
            StringBuilder queryBuilder = new StringBuilder(psqlString);
            queryBuilder.append(" WHERE ");
            for (String key : keys) {
                if (!key.equals("number") && !key.equals("size")) {
                    queryBuilder.append(key).append(" = '").append(params.get(key))
                            .append("' AND ");
                }
            }
            queryBuilder.setLength(queryBuilder.length() - 5);
            psqlString = queryBuilder.toString();
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNativeQuery(psqlString, ComponentPG.class).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public ComponentPG getComponentById(String id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(ComponentPG.class, UUID.fromString(id));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteComponent(ComponentPG component) {
        try {
            for (org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI release : component
                    .getReleases()) {

                try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                    ReleasePG dbrelease = session.find(ReleasePG.class, release.getId());
                    if (dbrelease != null) {
                        dbrelease.setComponent(null);
                        session.beginTransaction();
                        session.merge(dbrelease);
                        session.flush();
                        session.getTransaction().commit();
                        session.clear();
                    }
                }
            }
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                component.setReleases(List.of());
                session.beginTransaction();
                session.merge(component);
                session.flush();
                session.clear();
                session.remove(component);
                session.flush();
                session.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ComponentPG updateComponent(ComponentPG internalComponent) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            ComponentPG updatedComponent = session.merge(internalComponent);
            session.flush();
            session.getTransaction().commit();
            session.clear();
            return updatedComponent;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
