package org.eclipse.sw360.datahandler.postgresql;

import org.eclipse.sw360.datahandler.postgres.LicensePG;
import org.hibernate.HibernateError;
import org.hibernate.Session;
import io.swagger.v3.oas.models.info.License;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LicenseRepositoryPG {

    public LicensePG saveLicense(LicensePG license) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        LicensePG savedLicense = session.merge(license);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();

        return savedLicense;
    }

    public LicensePG getLicenseById(String id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        LicensePG license = session.get(LicensePG.class, UUID.fromString(id));
        session.close();
        return license;
    }

    public void deleteLicense(LicensePG license) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        try {
            session.remove(license);
            session.getTransaction().commit();
        } catch (HibernateError e) {
            session.getTransaction().rollback();
            throw e;
        } finally {
            session.clear();
            session.close();
        }
    }

    public List<LicensePG> getLicenses() {
        try {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                return session.createQuery("FROM LicensePG", LicensePG.class).getResultList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
