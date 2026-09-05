/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.thriftbridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.Visibility;
import org.eclipse.sw360.datahandler.services.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * Minimal thrift ↔ service-api mapping for datahandler callers that cannot depend on
 * {@code backend-common} converters (would create a cycle).
 */
public final class ThriftPojoBridge {

    private ThriftPojoBridge() {}

    public static org.eclipse.sw360.datahandler.services.users.User toPojoUser(User thrift) {
        if (thrift == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.services.users.User pojo =
                new org.eclipse.sw360.datahandler.services.users.User();
        if (thrift.isSetEmail()) {
            pojo.setEmail(thrift.getEmail());
        }
        if (thrift.isSetDepartment()) {
            pojo.setDepartment(thrift.getDepartment());
        }
        if (thrift.isSetUserGroup()) {
            pojo.setUserGroup(UserGroup.valueOf(thrift.getUserGroup().name()));
        }
        return pojo;
    }

    /**
     * Maps thrift Project → POJO. Includes permission/visibility fields so
     * {@code PermissionUtils.makePermission} can accept thrift documents while
     * resource-server remains thrift-centric.
     */
    public static org.eclipse.sw360.datahandler.services.projects.Project toPojoProject(Project thrift) {
        if (thrift == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.services.projects.Project pojo =
                new org.eclipse.sw360.datahandler.services.projects.Project();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetProjectResponsible()) {
            pojo.setProjectResponsible(thrift.getProjectResponsible());
        }
        if (thrift.isSetLeadArchitect()) {
            pojo.setLeadArchitect(thrift.getLeadArchitect());
        }
        if (thrift.isSetBusinessUnit()) {
            pojo.setBusinessUnit(thrift.getBusinessUnit());
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(new HashSet<>(thrift.getModerators()));
        }
        if (thrift.isSetContributors()) {
            pojo.setContributors(new HashSet<>(thrift.getContributors()));
        }
        if (thrift.isSetSecurityResponsibles()) {
            pojo.setSecurityResponsibles(new HashSet<>(thrift.getSecurityResponsibles()));
        }
        if (thrift.isSetVisbility()) {
            pojo.setVisbility(Visibility.valueOf(thrift.getVisbility().name()));
        }
        if (thrift.isSetClearingState()) {
            pojo.setClearingState(ProjectClearingState.valueOf(thrift.getClearingState().name()));
        }
        if (thrift.isSetAttachments()) {
            Set<Attachment> attachments = new HashSet<>();
            for (org.eclipse.sw360.datahandler.thrift.attachments.Attachment a : thrift.getAttachments()) {
                if (a == null) {
                    continue;
                }
                Attachment mapped = new Attachment();
                if (a.isSetAttachmentContentId()) {
                    mapped.setAttachmentContentId(a.getAttachmentContentId());
                }
                if (a.isSetFilename()) {
                    mapped.setFilename(a.getFilename());
                }
                attachments.add(mapped);
            }
            pojo.setAttachments(attachments);
        }
        if (thrift.isSetLinkedProjects()) {
            Map<String, org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship> map =
                    new HashMap<>();
            for (Map.Entry<String, ProjectProjectRelationship> e : thrift.getLinkedProjects().entrySet()) {
                map.put(e.getKey(), toPojoPpr(e.getValue()));
            }
            pojo.setLinkedProjects(map);
        }
        if (thrift.isSetReleaseIdToUsage()) {
            pojo.setReleaseIdToUsage(toPojoProjectReleaseRelationshipMap(thrift.getReleaseIdToUsage()));
        }
        if (thrift.isSetReleaseRelationNetwork()) {
            pojo.setReleaseRelationNetwork(thrift.getReleaseRelationNetwork());
        }
        return pojo;
    }

    private static org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship toPojoPpr(
            ProjectProjectRelationship thrift) {
        if (thrift == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship pojo =
                new org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship();
        if (thrift.isSetProjectRelationship()) {
            pojo.setProjectRelationship(org.eclipse.sw360.datahandler.services.projects.ProjectRelationship
                    .valueOf(thrift.getProjectRelationship().name()));
        }
        if (thrift.isSetEnableSvm()) {
            pojo.setEnableSvm(thrift.isEnableSvm());
        }
        return pojo;
    }

    public static List<ProjectLink> toThriftProjectLinks(
            List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> pojos) {
        if (pojos == null) {
            return Collections.emptyList();
        }
        List<ProjectLink> out = new ArrayList<>(pojos.size());
        for (org.eclipse.sw360.datahandler.services.projects.ProjectLink pojo : pojos) {
            out.add(toThriftProjectLink(pojo));
        }
        return out;
    }

    public static ProjectLink toThriftProjectLink(org.eclipse.sw360.datahandler.services.projects.ProjectLink pojo) {
        if (pojo == null) {
            return null;
        }
        ProjectLink thrift = new ProjectLink();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        if (pojo.getNodeId() != null) {
            thrift.setNodeId(pojo.getNodeId());
        }
        if (pojo.getParentNodeId() != null) {
            thrift.setParentNodeId(pojo.getParentNodeId());
        }
        if (pojo.getRelation() != null) {
            thrift.setRelation(ProjectRelationship.valueOf(pojo.getRelation().name()));
        }
        if (pojo.getSubprojects() != null) {
            thrift.setSubprojects(toThriftProjectLinks(pojo.getSubprojects()));
        }
        return thrift;
    }

    public static List<Project> toThriftProjects(List<org.eclipse.sw360.datahandler.services.projects.Project> pojos) {
        if (pojos == null) {
            return Collections.emptyList();
        }
        List<Project> out = new ArrayList<>(pojos.size());
        for (org.eclipse.sw360.datahandler.services.projects.Project pojo : pojos) {
            out.add(toThriftProject(pojo));
        }
        return out;
    }

    public static Project toThriftProject(org.eclipse.sw360.datahandler.services.projects.Project pojo) {
        if (pojo == null) {
            return null;
        }
        Project thrift = new Project();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        return thrift;
    }

    public static List<org.eclipse.sw360.datahandler.thrift.licenses.License> toThriftLicenses(
            List<org.eclipse.sw360.datahandler.services.licenses.License> pojos) {
        if (pojos == null) {
            return Collections.emptyList();
        }
        List<org.eclipse.sw360.datahandler.thrift.licenses.License> out = new ArrayList<>(pojos.size());
        for (org.eclipse.sw360.datahandler.services.licenses.License pojo : pojos) {
            out.add(toThriftLicense(pojo));
        }
        return out;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenses.License toThriftLicense(
            org.eclipse.sw360.datahandler.services.licenses.License pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenses.License thrift =
                new org.eclipse.sw360.datahandler.thrift.licenses.License();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getFullname() != null) {
            thrift.setFullname(pojo.getFullname());
        }
        return thrift;
    }

    public static List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation> toThriftObligations(
            List<org.eclipse.sw360.datahandler.services.licenses.Obligation> pojos) {
        if (pojos == null) {
            return Collections.emptyList();
        }
        List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation> out = new ArrayList<>(pojos.size());
        for (org.eclipse.sw360.datahandler.services.licenses.Obligation pojo : pojos) {
            out.add(toThriftObligation(pojo));
        }
        return out;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenses.Obligation toThriftObligation(
            org.eclipse.sw360.datahandler.services.licenses.Obligation pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenses.Obligation thrift =
                new org.eclipse.sw360.datahandler.thrift.licenses.Obligation();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getTitle() != null) {
            thrift.setTitle(pojo.getTitle());
        }
        if (pojo.getText() != null) {
            thrift.setText(pojo.getText());
        }
        if (pojo.getComments() != null) {
            thrift.setComments(pojo.getComments());
        }
        if (pojo.getObligationLevel() != null) {
            thrift.setObligationLevel(org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel
                    .valueOf(pojo.getObligationLevel().name()));
        }
        if (pojo.getObligationType() != null) {
            thrift.setObligationType(org.eclipse.sw360.datahandler.thrift.licenses.ObligationType
                    .valueOf(pojo.getObligationType().name()));
        }
        return thrift;
    }

    public static Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship>
            toPojoProjectReleaseRelationshipMap(
                    Map<String, org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship> thriftMap) {
        if (thriftMap == null) {
            return Collections.emptyMap();
        }
        Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> out =
                new HashMap<>();
        for (Map.Entry<String, org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship> e
                : thriftMap.entrySet()) {
            out.put(e.getKey(), toPojoProjectReleaseRelationship(e.getValue()));
        }
        return out;
    }

    public static org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship
            toPojoProjectReleaseRelationship(
                    org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship thrift) {
        if (thrift == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship pojo =
                new org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship();
        if (thrift.isSetReleaseRelation()) {
            pojo.setReleaseRelation(org.eclipse.sw360.datahandler.services.common.ReleaseRelationship
                    .valueOf(thrift.getReleaseRelation().name()));
        }
        if (thrift.isSetMainlineState()) {
            pojo.setMainlineState(org.eclipse.sw360.datahandler.services.common.MainlineState
                    .valueOf(thrift.getMainlineState().name()));
        }
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        return pojo;
    }

    public static Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship>
            toPojoReleaseRelationshipMap(
                    Map<String, org.eclipse.sw360.datahandler.thrift.ReleaseRelationship> thriftMap) {
        if (thriftMap == null) {
            return Collections.emptyMap();
        }
        Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> out =
                new HashMap<>();
        for (Map.Entry<String, org.eclipse.sw360.datahandler.thrift.ReleaseRelationship> e
                : thriftMap.entrySet()) {
            if (e.getValue() != null) {
                out.put(e.getKey(), org.eclipse.sw360.datahandler.services.common.ReleaseRelationship
                        .valueOf(e.getValue().name()));
            }
        }
        return out;
    }

    public static List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink> toThriftReleaseLinks(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> pojos) {
        if (pojos == null) {
            return Collections.emptyList();
        }
        List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink> out =
                new ArrayList<>(pojos.size());
        for (org.eclipse.sw360.datahandler.services.components.ReleaseLink pojo : pojos) {
            out.add(toThriftReleaseLink(pojo));
        }
        return out;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ReleaseLink toThriftReleaseLink(
            org.eclipse.sw360.datahandler.services.components.ReleaseLink pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ReleaseLink thrift =
                new org.eclipse.sw360.datahandler.thrift.components.ReleaseLink();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getVendor() != null) {
            thrift.setVendor(pojo.getVendor());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        if (pojo.getLongName() != null) {
            thrift.setLongName(pojo.getLongName());
        }
        if (pojo.getReleaseRelationship() != null) {
            thrift.setReleaseRelationship(org.eclipse.sw360.datahandler.thrift.ReleaseRelationship
                    .valueOf(pojo.getReleaseRelationship().name()));
        }
        if (pojo.getMainlineState() != null) {
            thrift.setMainlineState(org.eclipse.sw360.datahandler.thrift.MainlineState
                    .valueOf(pojo.getMainlineState().name()));
        }
        if (pojo.getHasSubreleases() != null) {
            thrift.setHasSubreleases(pojo.getHasSubreleases());
        }
        if (pojo.getNodeId() != null) {
            thrift.setNodeId(pojo.getNodeId());
        }
        if (pojo.getParentNodeId() != null) {
            thrift.setParentNodeId(pojo.getParentNodeId());
        }
        if (pojo.getClearingState() != null) {
            thrift.setClearingState(org.eclipse.sw360.datahandler.thrift.components.ClearingState
                    .valueOf(pojo.getClearingState().name()));
        }
        if (pojo.getComponentId() != null) {
            thrift.setComponentId(pojo.getComponentId());
        }
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getLicenseIds() != null) {
            thrift.setLicenseIds(pojo.getLicenseIds());
        }
        if (pojo.getAccessible() != null) {
            thrift.setAccessible(pojo.getAccessible());
        }
        return thrift;
    }


    /**
     * Maps thrift Component → POJO for permission/visibility dual-stack.
     */
    public static org.eclipse.sw360.datahandler.services.components.Component toPojoComponent(
            org.eclipse.sw360.datahandler.thrift.components.Component thrift) {
        if (thrift == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.services.components.Component pojo =
                new org.eclipse.sw360.datahandler.services.components.Component();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetBusinessUnit()) {
            pojo.setBusinessUnit(thrift.getBusinessUnit());
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(new HashSet<>(thrift.getModerators()));
        }
        if (thrift.isSetVisbility()) {
            pojo.setVisbility(Visibility.valueOf(thrift.getVisbility().name()));
        }
        if (thrift.isSetAttachments()) {
            Set<Attachment> attachments = new HashSet<>();
            for (org.eclipse.sw360.datahandler.thrift.attachments.Attachment a : thrift.getAttachments()) {
                if (a == null) {
                    continue;
                }
                Attachment mapped = new Attachment();
                if (a.isSetAttachmentContentId()) {
                    mapped.setAttachmentContentId(a.getAttachmentContentId());
                }
                if (a.isSetFilename()) {
                    mapped.setFilename(a.getFilename());
                }
                attachments.add(mapped);
            }
            pojo.setAttachments(attachments);
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.services.components.Release toPojoRelease(
            org.eclipse.sw360.datahandler.thrift.components.Release thrift) {
        if (thrift == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.services.components.Release pojo =
                new org.eclipse.sw360.datahandler.services.components.Release();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        if (thrift.isSetComponentId()) {
            pojo.setComponentId(thrift.getComponentId());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(new HashSet<>(thrift.getModerators()));
        }
        if (thrift.isSetContributors()) {
            pojo.setContributors(new HashSet<>(thrift.getContributors()));
        }
        if (thrift.isSetAttachments()) {
            Set<Attachment> attachments = new HashSet<>();
            for (org.eclipse.sw360.datahandler.thrift.attachments.Attachment a : thrift.getAttachments()) {
                if (a == null) {
                    continue;
                }
                Attachment mapped = new Attachment();
                if (a.isSetAttachmentContentId()) {
                    mapped.setAttachmentContentId(a.getAttachmentContentId());
                }
                if (a.isSetFilename()) {
                    mapped.setFilename(a.getFilename());
                }
                attachments.add(mapped);
            }
            pojo.setAttachments(attachments);
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.Release toThriftRelease(
            org.eclipse.sw360.datahandler.services.components.Release pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.Release thrift =
                new org.eclipse.sw360.datahandler.thrift.components.Release();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        if (pojo.getComponentId() != null) {
            thrift.setComponentId(pojo.getComponentId());
        }
        return thrift;
    }

    public static List<org.eclipse.sw360.datahandler.thrift.components.ReleaseNode> toThriftReleaseNodes(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> pojos) {
        if (pojos == null) {
            return Collections.emptyList();
        }
        List<org.eclipse.sw360.datahandler.thrift.components.ReleaseNode> out =
                new ArrayList<>(pojos.size());
        for (org.eclipse.sw360.datahandler.services.components.ReleaseNode pojo : pojos) {
            out.add(toThriftReleaseNode(pojo));
        }
        return out;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ReleaseNode toThriftReleaseNode(
            org.eclipse.sw360.datahandler.services.components.ReleaseNode pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ReleaseNode thrift =
                new org.eclipse.sw360.datahandler.thrift.components.ReleaseNode();
        if (pojo.getReleaseId() != null) {
            thrift.setReleaseId(pojo.getReleaseId());
        }
        if (pojo.getReleaseRelationship() != null) {
            thrift.setReleaseRelationship(pojo.getReleaseRelationship());
        }
        if (pojo.getMainlineState() != null) {
            thrift.setMainlineState(pojo.getMainlineState());
        }
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getCreateOn() != null) {
            thrift.setCreateOn(pojo.getCreateOn());
        }
        if (pojo.getCreateBy() != null) {
            thrift.setCreateBy(pojo.getCreateBy());
        }
        if (pojo.getReleaseLink() != null) {
            thrift.setReleaseLink(toThriftReleaseNodes(pojo.getReleaseLink()));
        }
        if (pojo.getReleaseName() != null) {
            thrift.setReleaseName(pojo.getReleaseName());
        }
        if (pojo.getReleaseVersion() != null) {
            thrift.setReleaseVersion(pojo.getReleaseVersion());
        }
        if (pojo.getComponentId() != null) {
            thrift.setComponentId(pojo.getComponentId());
        }
        return thrift;
    }
}
