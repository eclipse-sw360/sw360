package org.eclipse.sw360.datahandler.postgresql;

import java.util.List;
import java.util.UUID;
import org.eclipse.sw360.datahandler.postgres.VendorPG;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VendorRepositoryPG {

    private static final Logger LOGGER = LogManager.getLogger(VendorRepositoryPG.class);

    public VendorPG saveVendor(VendorPG vendor) throws HibernateException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        VendorPG savedVendor = session.merge(vendor);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();
        return savedVendor;
    }

    public VendorPG getVendorByFullName(String fullname) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        LOGGER.info("Searching for vendor with fullname: {}", fullname);
        List<VendorPG> vendor = session.createQuery("WHERE fullname = :fullname", VendorPG.class)
                .setParameter("fullname", fullname).getResultList();
        if (vendor == null || vendor.isEmpty()) {
            LOGGER.error("Vendor not found");
        } else {
            LOGGER.info("Vendor found: " + vendor);

            session.close();
            return vendor.get(0);
        }

        session.close();
        return null;

    }

    public VendorPG getVendorById(UUID id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        VendorPG vendor = session.get(VendorPG.class, id);
        session.getTransaction().commit();
        session.close();
        return vendor;
    }

    public VendorPG updateVendor(VendorPG existingVendor) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        VendorPG updatedVendor = session.merge(existingVendor);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();
        return updatedVendor;
    }

    public void deleteVendor(VendorPG vendor) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        session.remove(vendor);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();
    }

    public List<VendorPG> getVendors() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        List<VendorPG> vendors = session.createQuery("FROM VendorPG", VendorPG.class).getResultList();
        session.getTransaction().commit();
        session.close();
        return vendors;
    }
}
