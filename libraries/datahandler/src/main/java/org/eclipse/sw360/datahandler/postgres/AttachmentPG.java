package org.eclipse.sw360.datahandler.postgres;

import jakarta.persistence.Table;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Table(name = "attachment")
public class AttachmentPG {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public String filename;

}
