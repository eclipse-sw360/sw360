<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="updateComponentsURL" name="updateComponents">
</portlet:actionURL>

<portlet:actionURL var="updateAttachmentsURL" name="updateComponentAttachments">
</portlet:actionURL>

<portlet:actionURL var="updateReleaseLinksURL" name="updateReleaseLinks">
</portlet:actionURL>

<portlet:actionURL var="updateLicenseArchiveURL" name="updateLicenses">
</portlet:actionURL>

<div class="container">
	<div class="row portlet-toolbar">
        <div class="col portlet-title text-truncate" title="<liferay-ui:message key="import.export" />">
            <liferay-ui:message key="import.export" />
        </div>
    </div>
    <div class="row">
        <div class="col" data-section="export">
            <h4><liferay-ui:message key="export" /></h4>
            <div class="row mb-2">
                <div class="col">
			        <button type="button" class="btn btn-secondary btn-sm text-left" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD%>'/></portlet:resourceURL>'">
                        <clay:icon symbol="download"/><liferay-ui:message key="download.component.csv" /> 
                    </button>
                </div>
            </div>
            <div class="row mb-2">
                <div class="col">
			        <button type="button" class="btn btn-secondary btn-sm text-left" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_SAMPLE%>'/></portlet:resourceURL>'">
                        <clay:icon symbol="download"/><liferay-ui:message key="download.csv.template.for.component.upload" /> 
                    </button>
                </div>
            </div>
            <div class="row mb-2">
                <div class="col">
			        <button type="button" class="btn btn-secondary btn-sm text-left" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_SAMPLE_ATTACHMENT_INFO%>'/></portlet:resourceURL>'">
                        <clay:icon symbol="download"/><liferay-ui:message key="download.attachment.sample.information" /> 
                    </button>
                </div>
            </div>
            <div class="row mb-2">
                <div class="col">
			        <button type="button" class="btn btn-secondary btn-sm text-left" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_ATTACHMENT_INFO%>'/></portlet:resourceURL>'">
                        <clay:icon symbol="download"/><liferay-ui:message key="download.attachment.information" />
                    </button>
                </div>
            </div>
            <div class="row mb-2">
                <div class="col">
			        <button type="button" class="btn btn-secondary btn-sm text-left" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_SAMPLE_RELEASE_LINK_INFO%>'/></portlet:resourceURL>'">
                        <clay:icon symbol="download"/><liferay-ui:message key="download.release.link.sample.information" />
                    </button>
                </div>
            </div>
            <div class="row mb-2">
                <div class="col">
			        <button type="button" class="btn btn-secondary btn-sm text-left" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_RELEASE_LINK_INFO%>'/></portlet:resourceURL>'">
                        <clay:icon symbol="download"/><liferay-ui:message key="download.release.link.information" />
                    </button>
                </div>
            </div>
            <div class="row">
                <div class="col">
			        <button type="button" class="btn btn-secondary btn-sm text-left" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_LICENSE_BACKUP%>'/></portlet:resourceURL>'">
                        <clay:icon symbol="download"/><liferay-ui:message key="download.license.archive" />
                    </button>
                </div>
            </div>
        </div>

        <div class="col" data-section="import">
            <h4><liferay-ui:message key="import" /></h4>
            <form name="uploadForm" action="<%=updateComponentsURL%>" class="form needs-validation mb-2" method="POST" enctype="multipart/form-data" novalidate>
                <div class="form-row">
                    <div class="col">
                        <input type="file" class="form-control-file" name="<portlet:namespace/>file" required>
                        <div class="invalid-feedback">
                            <liferay-ui:message key="please.select.a.file" />
                        </div>
                    </div>
                    <div class="col content-right">
                        <button type="submit" class="btn btn-secondary btn-sm text-left"><clay:icon symbol="upload"/><liferay-ui:message key="upload.component.csv" /></button>
                    </div>
                </div>
            </form>
            <form name="uploadAttachmentsForm" action="<%=updateAttachmentsURL%>" class="form needs-validation mb-2" method="POST" enctype="multipart/form-data" novalidate>
                <div class="form-row">
                    <div class="col">
                        <input type="file" class="form-control-file" name="<portlet:namespace/>file" required>
                        <div class="invalid-feedback">
                            <liferay-ui:message key="please.select.a.file" />
                        </div>
                    </div>
                    <div class="col content-right">
                        <button type="submit" class="btn btn-secondary btn-sm text-left"><clay:icon symbol="upload"/><liferay-ui:message key="upload.component.attachments" /></button>
                    </div>
                </div>
            </form>
            <form name="uploadRelaseLinksForm" action="<%=updateReleaseLinksURL%>" class="form needs-validation mb-2" method="POST" enctype="multipart/form-data" novalidate>
                <div class="form-row">
                    <div class="col">
                        <input type="file" class="form-control-file" name="<portlet:namespace/>file" required>
                        <div class="invalid-feedback">
                            <liferay-ui:message key="please.select.a.file" />
                        </div>
                    </div>
                    <div class="col content-right">
                        <button type="submit" class="btn btn-secondary btn-sm text-left"><clay:icon symbol="upload"/><liferay-ui:message key="upload.release.links" /></button>
                    </div>
                </div>
            </form>
            <form name="uploadLicenseArchiveForm" action="<%=updateLicenseArchiveURL%>" class="form needs-validation mb-2" method="POST" enctype="multipart/form-data" novalidate>
                <div class="form-row">
                    <div class="col">
                        <input type="file" class="form-control-file" name="<portlet:namespace/>file" required>
                        <div class="invalid-feedback">
                            <liferay-ui:message key="please.select.a.file" />
                        </div>
                    </div>
                    <div class="col content-right">
                        <button type="submit" class="btn btn-secondary btn-sm text-left ml-2"><clay:icon symbol="upload"/><liferay-ui:message key="upload.license.archive" /></button>
                    </div>
                </div>
            </form>
        </div>
	</div>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['modules/validation' ], function(validation) {
        validation.enableForm('form[name="uploadForm"]');
        validation.enableForm('form[name="uploadAttachmentsForm"]');
        validation.enableForm('form[name="uploadRelaseLinksForm"]');
        validation.enableForm('form[name="uploadLicenseArchiveForm"]');
    });
</script>
