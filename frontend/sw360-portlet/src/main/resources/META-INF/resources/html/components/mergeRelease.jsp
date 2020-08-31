<%--
  ~ Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ComponentType" %>
<%@ page import="org.eclipse.sw360.datahandler.common.ThriftEnumUtils" %>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="releaseMergeWizardStepUrl" name="releaseMergeWizardStep"/>

<div id="releaseMergeWizard" class="container" data-step-id="0" data-release-target-id="${release.id}" data-componentid="${release.componentId}">
    <div class="row portlet-toolbar">
        <div class="col portlet-title text-truncate" title="<liferay-ui:message key="merge.into" /> ${sw360:printReleaseName(release)}">
           <liferay-ui:message key="merge.into" /> <sw360:out value="${sw360:printReleaseName(release)}"/>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="wizardHeader">
                <ul>
                    <li class="active"><liferay-ui:message key="choose.source" /><br /><small><liferay-ui:message key="choose.a.release.that.should.be.merged.into.the.current.one" /></small></li>
                    <li><liferay-ui:message key="merge.data" /><br /><small><liferay-ui:message key="merge.data.from.source.into.target.release" /></small></li>
                    <li><liferay-ui:message key="confirm" /><br /><small><liferay-ui:message key="check.the.merged.version.and.confirm" /></small></li>
                </ul>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="merge wizardBody">
                <div class="step active" data-step-id="1">
                    <div class="spinner spinner-with-text">
                        <div class="spinner-border" role="status">
                            <span class="sr-only"><liferay-ui:message key="loading.data.for.step.1.please.wait" /></span>
                        </div>
                        <liferay-ui:message key="loading.data.for.step.1.please.wait" />
                    </div>
                </div>
                <div class="step" data-step-id="2">
                    <div class="spinner spinner-with-text">
                        <div class="spinner-border" role="status">
                            <span class="sr-only"><liferay-ui:message key="" /><liferay-ui:message key="loading.data.for.step.2.please.wait" /></span>
                        </div>
                        <liferay-ui:message key="loading.data.for.step.2.please.wait" />
                    </div>
                </div>
                <div class="step" data-step-id="3">
                    <div class="spinner spinner-with-text">
                        <div class="spinner-border" role="status">
                            <span class="sr-only"><liferay-ui:message key="" /><liferay-ui:message key="loading.data.for.step.3.please.wait" /></span>
                        </div>
                        <liferay-ui:message key="loading.data.for.step.3.please.wait" />
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables', 'modules/mergeWizard'], function($, datatables, wizard) {
        var mergeWizardStepUrl = '<%=releaseMergeWizardStepUrl%>',
            postParamsPrefix = '<portlet:namespace/>',
            $wizardRoot = $('#releaseMergeWizard');

        wizard({
            wizardRoot: $wizardRoot,
            postUrl: mergeWizardStepUrl,
            postParamsPrefix: postParamsPrefix,
            loadErrorHook: errorHook,

            steps: [
                {
                    renderHook: renderChooseRelease,
                    submitHook: submitChosenRelease,
                    errorHook: errorHook
                },
                {
                    renderHook: renderMergeRelease,
                    submitHook: submitMergedRelease,
                    errorHook: errorHook
                },
                {
                    renderHook: renderConfirmMergedRelease,
                    submitHook: submitConfirmedMergedRelease,
                    errorHook: errorHook
                }
            ],
            finishCb: function($stepElement, data) {
                if (data && data.error) {
                    let $error = $('<div/>', {
                        'class': 'alert alert-danger mt-3'
                    });
                    let $idList = $('<ul>');

                    $error.append($('<p/>').append($('<b/>').text('<liferay-ui:message key="could.not.merge.releases" />' + data.error)));
                    $error.append($('<p/>').text('<liferay-ui:message key="this.error.can.lead.to.inconsistencies.in.the.database.please.inform.the.administrator.with.the.following.information" />'));
                    $error.append($('<p>').append($idList));
                    
                    let releaseSourceId = $stepElement.data('releaseSourceId');
                    $idList.append($('<li>').text('<liferay-ui:message key="source.release" />' + releaseSourceId));
                    $idList.append($('<li>').text('<liferay-ui:message key="target.release" />' + $wizardRoot.data('releaseTargetId')));

                    $stepElement.html('').append($error);
                    return false;
                } else if(data && data.redirectUrl) {
                    window.location.href = data.redirectUrl;
                    return true;
                } else {
                    window.history.back();
                    return true;
                }
            }
        });

        function renderChooseRelease($stepElement, data) {
            $stepElement.html('' +
                    '<div class="stepFeedback"></div>' +
                    '<form>' +
                    '    <table id="releaseSourcesTable" class="table table-bordered" title="<liferay-ui:message key="source.release" />">' +
                    '        <colgroup>' +
                    '            <col style="width: 1.7rem;" />' +
                    '            <col style="width: 70%;" />' +
                    '            <col style="width: 10%;" />' +
                    '            <col style="width: 20%;" />' +
                    '        </colgroup>' +
                    '        <thead>' +
                    '            <tr>' +
                    '                <th></th>' +
                    '                <th><liferay-ui:message key="release.name" /></th>' +
                    '                <th><liferay-ui:message key="version" /></th>' + 
                    '                <th><liferay-ui:message key="created.by" /></th>' +
                    '            </tr>' +
                    '        </thead>' +
                    '        <tbody>' +
                    '        </tbody>' +
                    '    </table>' +
                    '</form>'
                    );

            var table = datatables.create($stepElement.find('#releaseSourcesTable'), {
                data: data.releases,
                columns: [
                    { data: "id", render: $.fn.dataTable.render.inputRadio('releaseChooser') },
                    { data: "name", render: $.fn.dataTable.render.text() } ,
                    { data: "version", render: $.fn.dataTable.render.text() },
                    { data: "createdBy" }
                ],
                language: {
                    select: {
                        style: 'single',
                        rows: "<liferay-ui:message key="x.rows.selected" />"
                    },
                    url: "<liferay-ui:message key="datatables.lang" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                order: [ [ 1, 'asc' ] ],
            }, undefined, [0], true);

            $("#releaseSourcesTable").on('init.dt', function() {
                datatables.enableCheckboxForSelection(table, 0);
            });
        }

        function submitChosenRelease($stepElement) {
            var checkedList = $stepElement.find('input:checked');
            if (checkedList.length !== 1 || $(checkedList.get(0)).val() ===  $wizardRoot.data('releaseTargetId')) {
                $stepElement.find('.stepFeedback').html('<div class="alert alert-danger"><liferay-ui:message key="please.choose.exactly.one.release.which.is.not.the.release.itself" /></div>');
                $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
                setTimeout(function() {
                    $stepElement.find('.stepFeedback').html('');
                }, 5000);
                return false;
            }
            $stepElement.data('releaseTargetId', $wizardRoot.data('releaseTargetId'));
            $stepElement.data('releaseSourceId', $(checkedList.get(0)).val());
        }

        function renderMergeRelease($stepElement, data) {
            $stepElement.html('<div class="stepFeedback"></div>');
            $stepElement.data('releaseSourceId', data.releaseSource.id);

            $stepElement.append(renderNotice(data.usageInformation));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="general" />'));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="vendor" />', 
                data.releaseTarget.vendor ? data.releaseTarget.vendor.id : null, 
                data.releaseSource.vendor ? data.releaseSource.vendor.id : null, 
                vendorFormatter(data.releaseTarget.vendor, data.releaseSource.vendor)
            ));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="name" />', data.releaseTarget.name, data.releaseSource.name));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="version" />', data.releaseTarget.version, data.releaseSource.version));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="programming.languages" />', data.releaseTarget.languages, data.releaseSource.languages));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="operating.systems" />', data.releaseTarget.operatingSystems, data.releaseSource.operatingSystems));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="cpe.id3" />', data.releaseTarget.cpeid, data.releaseSource.cpeid));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="software.platforms" />', data.releaseTarget.softwarePlatforms, data.releaseSource.softwarePlatforms));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="release.date" />', data.releaseTarget.releaseDate, data.releaseSource.releaseDate));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="licenses" />', data.releaseTarget.mainLicenseIds, data.releaseSource.mainLicenseIds));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="source.code.download.url" />', data.releaseTarget.sourceCodeDownloadurl, data.releaseSource.sourceCodeDownloadurl));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="binary.download.url" />', data.releaseTarget.binaryDownloadurl, data.releaseSource.binaryDownloadurl));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="release.mainline.state" />', data.releaseTarget.mainlineState, data.releaseSource.mainlineState, mapFormatter(data.displayInformation, 'mainlineState')));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="created.on" />', data.releaseTarget.createdOn, data.releaseSource.createdOn));
            $stepElement.append(
                renderCreatedBy(
                    wizard.createSingleMergeLine('<liferay-ui:message key="created.by" />', data.releaseTarget.createdBy, data.releaseSource.createdBy),
                    data.releaseSource.createdBy != data.releaseTarget.createdBy,
                    data.releaseSource.createdBy,
                    'text-center'
                )
            );
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="contributors" />', data.releaseTarget.contributors, data.releaseSource.contributors));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="moderators" />', data.releaseTarget.moderators, data.releaseSource.moderators));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="subscribers" />', data.releaseTarget.subscribers, data.releaseSource.subscribers));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="repository" />', data.releaseTarget.repository, data.releaseSource.repository, repositoryFormatter(data.displayInformation)));
            $stepElement.append(wizard.createMultiMapMergeLine('<liferay-ui:message key="additional.roles" />', data.releaseTarget.roles, data.releaseSource.roles));
            $stepElement.append(wizard.createMapMergeLine('<liferay-ui:message key="external.ids" />', data.releaseTarget.externalIds, data.releaseSource.externalIds));
            $stepElement.append(wizard.createMapMergeLine('<liferay-ui:message key="additional.data" />', data.releaseTarget.additionalData, data.releaseSource.additionalData));
            
            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="linked.releases" />'));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="linked.releases" />', Object.keys(data.releaseTarget.releaseIdToRelationship || {}), Object.keys(data.releaseSource.releaseIdToRelationship || {}), mapFormatter(data.displayInformation, 'release')));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="clearing.details" />'));
            data.releaseTarget.clearingInformation = data.releaseTarget.clearingInformation || {};
            data.releaseSource.clearingInformation = data.releaseSource.clearingInformation || {};
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="binaries.original.from.community" />', data.releaseTarget.clearingInformation.binariesOriginalFromCommunity, data.releaseSource.clearingInformation.binariesOriginalFromCommunity, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="binaries.self.made" />', data.releaseTarget.clearingInformation.binariesSelfMade, data.releaseSource.clearingInformation.binariesSelfMade, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="component.license.information" />', data.releaseTarget.clearingInformation.componentLicenseInformation, data.releaseSource.clearingInformation.componentLicenseInformation, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="source.code.delivery" />', data.releaseTarget.clearingInformation.sourceCodeDelivery, data.releaseSource.clearingInformation.sourceCodeDelivery, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="source.code.original.from.community" />', data.releaseTarget.clearingInformation.sourceCodeOriginalFromCommunity, data.releaseSource.clearingInformation.sourceCodeOriginalFromCommunity, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="source.code.tool.made" />', data.releaseTarget.clearingInformation.sourceCodeToolMade, data.releaseSource.clearingInformation.sourceCodeToolMade, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="source.code.self.made" />', data.releaseTarget.clearingInformation.sourceCodeSelfMade, data.releaseSource.clearingInformation.sourceCodeSelfMade, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="screenshot.of.website" />', data.releaseTarget.clearingInformation.screenshotOfWebSite, data.releaseSource.clearingInformation.screenshotOfWebSite, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="finalized.license.scan.report" />', data.releaseTarget.clearingInformation.finalizedLicenseScanReport, data.releaseSource.clearingInformation.finalizedLicenseScanReport, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="license.scan.report.result" />', data.releaseTarget.clearingInformation.licenseScanReportResult, data.releaseSource.clearingInformation.licenseScanReportResult, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="legal.evaluation" />', data.releaseTarget.clearingInformation.legalEvaluation, data.releaseSource.clearingInformation.legalEvaluation, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="license.agreement" />', data.releaseTarget.clearingInformation.licenseAgreement, data.releaseSource.clearingInformation.licenseAgreement, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="scanned" />', data.releaseTarget.clearingInformation.scanned, data.releaseSource.clearingInformation.scanned));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="component.clearing.report" />', data.releaseTarget.clearingInformation.componentClearingReport, data.releaseSource.clearingInformation.componentClearingReport, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="clearing.standard" />', data.releaseTarget.clearingInformation.clearingStandard, data.releaseSource.clearingInformation.clearingStandard));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="external.url" />', data.releaseTarget.clearingInformation.externalUrl, data.releaseSource.clearingInformation.externalUrl));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="comment" />', data.releaseTarget.clearingInformation.comment, data.releaseSource.clearingInformation.comment));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="request.information" />'));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="request.id" />', data.releaseTarget.clearingInformation.requestID, data.releaseSource.clearingInformation.requestID));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="additional.request.info" />', data.releaseTarget.clearingInformation.additionalRequestInfo, data.releaseSource.clearingInformation.additionalRequestInfo));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="evaluation.start" />', data.releaseTarget.clearingInformation.procStart, data.releaseSource.clearingInformation.procStart));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="evaluation.end" />', data.releaseTarget.clearingInformation.evaluated, data.releaseSource.clearingInformation.evaluated));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="supplemental.information" />'));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="external.supplier.id" />', data.releaseTarget.clearingInformation.externalSupplierID, data.releaseSource.clearingInformation.externalSupplierID));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="count.of.security.vulnerabilities" />', data.releaseTarget.clearingInformation.countOfSecurityVn, data.releaseSource.clearingInformation.countOfSecurityVn));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="ecc.information" />'));
            data.releaseTarget.eccInformation = data.releaseTarget.eccInformation || {};
            data.releaseSource.eccInformation = data.releaseSource.eccInformation || {};
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="ecc.status" />', data.releaseTarget.eccInformation.eccStatus, data.releaseSource.eccInformation.eccStatus, mapFormatter(data.displayInformation, 'eccStatus')));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="ecc.comment" />', data.releaseTarget.eccInformation.eccComment, data.releaseSource.eccInformation.eccComment));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="ausfuhrliste" />', data.releaseTarget.eccInformation.AL, data.releaseSource.eccInformation.AL));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="eccn" />', data.releaseTarget.eccInformation.ECCN, data.releaseSource.eccInformation.ECCN));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="material.index.number" />', data.releaseTarget.eccInformation.materialIndexNumber, data.releaseSource.eccInformation.materialIndexNumber));
            $stepElement.append(
                renderAssessorContactPerson(
                    wizard.createSingleMergeLine('<liferay-ui:message key="assessor.contact.person" />', data.releaseTarget.eccInformation.assessorContactPerson, data.releaseSource.eccInformation.assessorContactPerson),
                    true,
                    data.releaseSource.eccInformation.assessorContactPerson && data.releaseSource.eccInformation.assessorContactPerson != data.releaseTarget.eccInformation.assessorContactPerson,
                    data.releaseSource.eccInformation.assessorContactPerson,
                    'text-center'
                )
            );

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="commercial.details.administration" />'));
            data.releaseTarget.cotsDetails = data.releaseTarget.cotsDetails || {};
            data.releaseSource.cotsDetails = data.releaseSource.cotsDetails || {};
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="usage.right.available" />', data.releaseTarget.cotsDetails.usageRightAvailable, data.releaseSource.cotsDetails.usageRightAvailable, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="cots.responsible" />', data.releaseTarget.cotsDetails.cotsResponsible, data.releaseSource.cotsDetails.cotsResponsible));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="cots.clearing.deadline" />', data.releaseTarget.cotsDetails.clearingDeadline, data.releaseSource.cotsDetails.clearingDeadline));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="cots.clearing.report.url" />', data.releaseTarget.cotsDetails.licenseClearingReportURL, data.releaseSource.cotsDetails.licenseClearingReportURL));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="cots.oss.information" />'));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="used.license" />', data.releaseTarget.cotsDetails.usedLicense, data.releaseSource.cotsDetails.usedLicense));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="contains.oss" />', data.releaseTarget.cotsDetails.containsOSS, data.releaseSource.cotsDetails.containsOSS, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="oss.contract.signed" />', data.releaseTarget.cotsDetails.ossContractSigned, data.releaseSource.cotsDetails.ossContractSigned, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="oss.information.url" />', data.releaseTarget.cotsDetails.ossInformationURL, data.releaseSource.cotsDetails.ossInformationURL));
            $stepElement.append(wizard.createSingleMergeLineForHtml('<liferay-ui:message key="source.code.available" />', data.releaseTarget.cotsDetails.sourceCodeAvailable, data.releaseSource.cotsDetails.sourceCodeAvailable, flagFormatter));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="attachments" />'));
            var sourceAttachmentMerge = createSourceCodeAttachmentsMultiMergeLine(data.displayInformation, data.releaseTarget.attachments, data.releaseSource.attachments);
            $stepElement.append(sourceAttachmentMerge.$element);
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="other.attachments" />', 
                filterAttachments(data.releaseTarget.attachments, sourceAttachmentMerge.sourceCodeAttachmentIds), 
                filterAttachments(data.releaseSource.attachments, sourceAttachmentMerge.sourceCodeAttachmentIds),
                attachmentFormatter(data.displayInformation)
            ));


            wizard.registerClickHandlers({
                'Created_by': true,
                'Assessor_Contact_Person': true
            }, function(propName, copied, targetValue, sourceValue) {
                if(propName == 'Created_by') {
                    $stepElement.find('.merge-info-createdby .user').text(copied ? targetValue : sourceValue);
                } else if(propName == 'Assessor_Contact_Person') {
                    $stepElement.find('.merge-info-assessor').replaceWith(renderAssessorContactPersonInfo(!copied, copied ? sourceValue : targetValue, copied || sourceValue, 'text-center'));
                }
            });
            wizard.registerClickHandlersForIcons({
                'Binaries_Original_from_Community': true,
                'Binaries_Self_Made': true,
                'Component_License_Information': true,
                'Source_Code_Delivery': true,
                'Source_Code_Original_from_Community': true,
                'Source_Code_Tool_Made': true,
                'Source_Code_Self_Made': true,
                'Screenshot_of_Website': true,
                'Finalized_License_Scan_Report': true,
                'License_Scan_Report_Result': true,
                'Legal_Evaluation': true,
                'License_Agreement': true,
                'Component_Clearing_Report': true,
                'Source_Code_Self_Made': true,
                'Usage_Right_Available': true,
                'Contains_OSS': true,
                'OSS_Contract_Signed': true,
                'Source_Code_Available': true
            }, null);

            $wizardRoot.data('releaseSource', data.releaseSource);
            $wizardRoot.data('releaseTarget', data.releaseTarget);
            $wizardRoot.data('displayInformation', data.displayInformation);
            $wizardRoot.data('usageInformation', data.usageInformation);
        }

        function submitMergedRelease($stepElement) {
            var assessorResult,
                releaseSelection = {},
                attachments = [],
                sourceCodeAttachments = [],
                releaseSource = $wizardRoot.data('releaseSource'),
                releaseTarget = $wizardRoot.data('releaseTarget');

            releaseSelection.id = releaseTarget.id;
            releaseSelection.componentId = releaseTarget.componentId;

            releaseSelection.vendor = wizard.getEnhancedFinalSingleValue('<liferay-ui:message key="vendor" />').target ? releaseTarget.vendor : releaseSource.vendor;
            releaseSelection.vendorId = releaseSelection.vendor ? releaseSelection.vendorId : undefined;
            releaseSelection.name = wizard.getFinalSingleValue('<liferay-ui:message key="name" />');
            releaseSelection.version = wizard.getFinalSingleValue('<liferay-ui:message key="version" />');
            releaseSelection.languages = wizard.getFinalMultiValue('<liferay-ui:message key="programming.languages" />');
            releaseSelection.operatingSystems = wizard.getFinalMultiValue('<liferay-ui:message key="operating.systems" />');
            releaseSelection.cpeid = wizard.getFinalSingleValue('<liferay-ui:message key="cpe.id3" />');
            releaseSelection.softwarePlatforms = wizard.getFinalMultiValue('<liferay-ui:message key="software.platforms" />');
            releaseSelection.releaseDate = wizard.getFinalSingleValue('<liferay-ui:message key="release.date" />');
            releaseSelection.mainLicenseIds = wizard.getFinalMultiValue('<liferay-ui:message key="licenses" />');
            releaseSelection.sourceCodeDownloadurl = wizard.getFinalSingleValue('<liferay-ui:message key="source.code.download.url" />');
            releaseSelection.binaryDownloadurl = wizard.getFinalSingleValue('<liferay-ui:message key="binary.download.url" />');
            releaseSelection.mainlineState = wizard.getFinalSingleValue('<liferay-ui:message key="release.mainline.state" />');
            releaseSelection.createdOn = wizard.getFinalSingleValue('<liferay-ui:message key="created.on" />');
            releaseSelection.createdBy = wizard.getFinalSingleValue('<liferay-ui:message key="created.by" />');

            releaseSelection.contributors = wizard.getFinalMultiValue('<liferay-ui:message key="contributors" />');
            releaseSelection.moderators = wizard.getFinalMultiValue('<liferay-ui:message key="moderators" />');
            releaseSelection.subscribers = wizard.getFinalMultiValue('<liferay-ui:message key="subscribers" />');
            releaseSelection.repository = wizard.getFinalSingleValue('<liferay-ui:message key="repository" />');
            releaseSelection.roles = wizard.getFinalMultiMapValue('<liferay-ui:message key="additional.roles" />');
            releaseSelection.externalIds = wizard.getFinalMapValue('<liferay-ui:message key="external.ids" />');
            releaseSelection.additionalData = wizard.getFinalMapValue('<liferay-ui:message key="additional.data" />');
            
            releaseSelection.releaseIdToRelationship = {};
            wizard.getEnhancedFinalMultiValue('<liferay-ui:message key="linked.releases" />').forEach(function(result) {
                releaseSelection.releaseIdToRelationship[result.value] = result.target ? releaseTarget.releaseIdToRelationship[result.value] : releaseSource.releaseIdToRelationship[result.value];
            });

            releaseSelection.clearingInformation = {};
            releaseSelection.clearingInformation.binariesOriginalFromCommunity = wizard.getFinalSingleValue('<liferay-ui:message key="binaries.original.from.community" />');
            releaseSelection.clearingInformation.binariesSelfMade = wizard.getFinalSingleValue('<liferay-ui:message key="binaries.self.made" />');
            releaseSelection.clearingInformation.componentLicenseInformation = wizard.getFinalSingleValue('<liferay-ui:message key="component.license.information" />');
            releaseSelection.clearingInformation.sourceCodeDelivery = wizard.getFinalSingleValue('<liferay-ui:message key="source.code.delivery" />');
            releaseSelection.clearingInformation.sourceCodeOriginalFromCommunity = wizard.getFinalSingleValue('<liferay-ui:message key="source.code.original.from.community" />');
            releaseSelection.clearingInformation.sourceCodeToolMade = wizard.getFinalSingleValue('<liferay-ui:message key="source.code.tool.made" />');
            releaseSelection.clearingInformation.sourceCodeSelfMade = wizard.getFinalSingleValue('<liferay-ui:message key="source.code.self.made" />');
            releaseSelection.clearingInformation.screenshotOfWebSite = wizard.getFinalSingleValue('<liferay-ui:message key="screenshot.of.website" />');
            releaseSelection.clearingInformation.finalizedLicenseScanReport = wizard.getFinalSingleValue('<liferay-ui:message key="finalized.license.scan.report" />');
            releaseSelection.clearingInformation.licenseScanReportResult = wizard.getFinalSingleValue('<liferay-ui:message key="license.scan.report.result" />');
            releaseSelection.clearingInformation.legalEvaluation = wizard.getFinalSingleValue('<liferay-ui:message key="legal.evaluation" />');
            releaseSelection.clearingInformation.licenseAgreement = wizard.getFinalSingleValue('<liferay-ui:message key="license.agreement" />');
            releaseSelection.clearingInformation.scanned = wizard.getFinalSingleValue('<liferay-ui:message key="scanned" />');
            releaseSelection.clearingInformation.componentClearingReport = wizard.getFinalSingleValue('<liferay-ui:message key="component.clearing.report" />');
            releaseSelection.clearingInformation.clearingStandard = wizard.getFinalSingleValue('<liferay-ui:message key="clearing.standard" />');
            releaseSelection.clearingInformation.externalUrl = wizard.getFinalSingleValue('<liferay-ui:message key="external.url" />');
            releaseSelection.clearingInformation.comment = wizard.getFinalSingleValue('<liferay-ui:message key="comment" />');

            releaseSelection.clearingInformation.requestID = wizard.getFinalSingleValue('<liferay-ui:message key="request.id" />');
            releaseSelection.clearingInformation.additionalRequestInfo = wizard.getFinalSingleValue('<liferay-ui:message key="additional.request.info" />');
            releaseSelection.clearingInformation.procStart = wizard.getFinalSingleValue('<liferay-ui:message key="evaluation.start" />');
            releaseSelection.clearingInformation.evaluated = wizard.getFinalSingleValue('<liferay-ui:message key="evaluation.end" />');
            
            releaseSelection.clearingInformation.externalSupplierID = wizard.getFinalSingleValue('<liferay-ui:message key="external.supplier.id" />');
            releaseSelection.clearingInformation.countOfSecurityVn = wizard.getFinalSingleValue('<liferay-ui:message key="count.of.security.vulnerabilities" />');

            releaseSelection.eccInformation = {};
            releaseSelection.eccInformation.eccStatus = wizard.getFinalSingleValue('<liferay-ui:message key="ecc.status" />');
            releaseSelection.eccInformation.eccComment = wizard.getFinalSingleValue('<liferay-ui:message key="ecc.comment" />');
            releaseSelection.eccInformation.AL = wizard.getFinalSingleValue('<liferay-ui:message key="ausfuhrliste" />');
            releaseSelection.eccInformation.ECCN = wizard.getFinalSingleValue('<liferay-ui:message key="eccn" />');
            releaseSelection.eccInformation.materialIndexNumber = wizard.getFinalSingleValue('<liferay-ui:message key="material.index.number" />');
            assessorResult = wizard.getEnhancedFinalSingleValue('<liferay-ui:message key="assessor.contact.person" />');
            if(assessorResult.target) {
                releaseSelection.eccInformation.assessorContactPerson = releaseTarget.eccInformation.assessorContactPerson;
                releaseSelection.eccInformation.assessorDepartment = releaseTarget.eccInformation.assessorDepartment;
                releaseSelection.eccInformation.assessmentDate = releaseTarget.eccInformation.assessmentDate;
            } else {
                releaseSelection.eccInformation.assessorContactPerson = releaseSource.eccInformation.assessorContactPerson;
                releaseSelection.eccInformation.assessorDepartment = releaseSource.eccInformation.assessorDepartment;
                releaseSelection.eccInformation.assessmentDate = releaseSource.eccInformation.assessmentDate;
            }

            releaseSelection.cotsDetails = {};
            releaseSelection.cotsDetails.usageRightAvailable = wizard.getFinalSingleValue('<liferay-ui:message key="usage.right.available" />');
            releaseSelection.cotsDetails.cotsResponsible = wizard.getFinalSingleValue('<liferay-ui:message key="cots.responsible" />');
            releaseSelection.cotsDetails.clearingDeadline = wizard.getFinalSingleValue('<liferay-ui:message key="cots.clearing.deadline" />');
            releaseSelection.cotsDetails.licenseClearingReportURL = wizard.getFinalSingleValue('<liferay-ui:message key="cots.clearing.report.url" />');

            releaseSelection.cotsDetails.usedLicense = wizard.getFinalSingleValue('<liferay-ui:message key="used.license" />');
            releaseSelection.cotsDetails.containsOSS = wizard.getFinalSingleValue('<liferay-ui:message key="contains.oss" />');
            releaseSelection.cotsDetails.ossContractSigned = wizard.getFinalSingleValue('<liferay-ui:message key="oss.contract.signed" />');
            releaseSelection.cotsDetails.ossInformationURL = wizard.getFinalSingleValue('<liferay-ui:message key="oss.information.url" />');
            releaseSelection.cotsDetails.sourceCodeAvailable = wizard.getFinalSingleValue('<liferay-ui:message key="source.code.available" />');


            releaseSelection.attachments = [];
            sourceCodeAttachments = wizard.getFinalMultiValue('<liferay-ui:message key="matching.source.attachments" />');
            $.each(sourceCodeAttachments, function(index, value) {
                /* add just required fields for easy identification */
                releaseSelection.attachments.push(JSON.parse('{ "attachmentContentId": "' + value.attachmentContentId + '", "filename": "' + value.filename + '"}'));
            });
            attachments = wizard.getFinalMultiValue('<liferay-ui:message key="other.attachments" />');
            $.each(attachments, function(index, value) {
                /* add just required fields for easy identification */
                releaseSelection.attachments.push(JSON.parse('{ "attachmentContentId": "' + value.attachmentContentId + '", "filename": "' + value.filename + '"}'));
            });

            $stepElement.data('releaseSelection', releaseSelection);
            /* releaseSourceId still as data at stepElement */
        }

        function renderConfirmMergedRelease($stepElement, data) {
            var releaseSource = $wizardRoot.data('releaseSource'),
                releaseTarget = $wizardRoot.data('releaseTarget'),
                displayInformation = $wizardRoot.data('displayInformation'),
                usageInformation = $wizardRoot.data('usageInformation');

            $stepElement.data('releaseSourceId', data.releaseSourceId);
            $stepElement.data('releaseSelection', data.releaseSelection);

            $stepElement.html('<div class="stepFeedback"></div>');

            $stepElement.append(renderNotice(usageInformation));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="general" />'));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="vendor" />', 
                data.releaseSelection.vendor ? data.releaseSelection.vendor.id : null, 
                vendorFormatter(releaseTarget.vendor, releaseSource.vendor)
            ));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="name" />', data.releaseSelection.name));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="version" />', data.releaseSelection.version));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="programming.languages" />', data.releaseSelection.languages));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="operating.systems" />', data.releaseSelection.operatingSystems));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="cpe.id3" />', data.releaseSelection.cpeid));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="software.platforms" />', data.releaseSelection.softwarePlatforms));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="release.date" />', data.releaseSelection.releaseDate));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="licenses" />', data.releaseSelection.mainLicenseIds));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="source.code.download.url" />', data.releaseSelection.sourceCodeDownloadurl));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="binary.download.url" />', data.releaseSelection.binaryDownloadurl));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="release.mainline.state" />', data.releaseSelection.mainlineState, mapFormatter(displayInformation, 'mainlineState')));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="created.on" />', data.releaseSelection.createdOn));
            $stepElement.append(
                renderCreatedBy(
                    wizard.createSingleDisplayLine('<liferay-ui:message key="created.by" />', data.releaseSelection.createdBy),
                    releaseSource.createdBy != releaseTarget.createdBy,
                    data.releaseSelection.createdBy === releaseSource.createdBy ? releaseTarget.createdBy : releaseSource.createdBy,
                    'pl-3'
                )
            );
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="contributors" />', data.releaseSelection.contributors));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="moderators" />', data.releaseSelection.moderators));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="subscribers" />', data.releaseSelection.subscribers));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="repository" />', data.releaseSelection.repository, repositoryFormatter(displayInformation)));
            $stepElement.append(wizard.createMultiMapDisplayLine('<liferay-ui:message key="additional.roles" />', data.releaseSelection.roles));
            $stepElement.append(wizard.createMapDisplayLine('<liferay-ui:message key="external.ids" />', data.releaseSelection.externalIds));
            $stepElement.append(wizard.createMapDisplayLine('<liferay-ui:message key="additional.data" />', data.releaseSelection.additionalData));
            
            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="linked.releases" />'));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="linked.releases" />', Object.keys(data.releaseSelection.releaseIdToRelationship), mapFormatter(displayInformation, 'release')));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="clearing.details" />'));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="binaries.original.from.community" />', data.releaseSelection.clearingInformation.binariesOriginalFromCommunity, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="binaries.self.made" />', data.releaseSelection.clearingInformation.binariesSelfMade, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="component.license.information" />', data.releaseSelection.clearingInformation.componentLicenseInformation, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="source.code.delivery" />', data.releaseSelection.clearingInformation.sourceCodeDelivery, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="source.code.original.from.community" />', data.releaseSelection.clearingInformation.sourceCodeOriginalFromCommunity, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="source.code.tool.made" />', data.releaseSelection.clearingInformation.sourceCodeToolMade, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="source.code.self.made" />', data.releaseSelection.clearingInformation.sourceCodeSelfMade, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="screenshot.of.website" />', data.releaseSelection.clearingInformation.screenshotOfWebSite, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="finalized.license.scan.report" />', data.releaseSelection.clearingInformation.finalizedLicenseScanReport, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="license.scan.report.result" />', data.releaseSelection.clearingInformation.licenseScanReportResult, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="legal.evaluation" />', data.releaseSelection.clearingInformation.legalEvaluation, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="license.agreement" />', data.releaseSelection.clearingInformation.licenseAgreement, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="scanned" />', data.releaseSelection.clearingInformation.scanned));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="component.clearing.report" />', data.releaseSelection.clearingInformation.componentClearingReport, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="clearing.standard" />', data.releaseSelection.clearingInformation.clearingStandard));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="external.url" />', data.releaseSelection.clearingInformation.externalUrl));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="comment" />', data.releaseSelection.clearingInformation.comment));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="request.information" />'));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="request.id" />', data.releaseSelection.clearingInformation.requestID));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="additional.request.info" />', data.releaseSelection.clearingInformation.additionalRequestInfo));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="evaluation.start" />', data.releaseSelection.clearingInformation.procStart));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="evaluation.end" />', data.releaseSelection.clearingInformation.evaluated));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="supplemental.information" />'));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="external.supplier.id" />', data.releaseSelection.clearingInformation.externalSupplierID));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="count.of.security.vulnerabilities" />', data.releaseSelection.clearingInformation.countOfSecurityVn));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="ecc.information" />'));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="ecc.status" />', data.releaseSelection.eccInformation.eccStatus, mapFormatter(displayInformation, 'eccStatus')));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="ecc.comment" />', data.releaseSelection.eccInformation.eccComment));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="ausfuhrliste" />', data.releaseSelection.eccInformation.AL));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="eccn" />', data.releaseSelection.eccInformation.ECCN));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="material.index.number" />', data.releaseSelection.eccInformation.materialIndexNumber));
            $stepElement.append(
                renderAssessorContactPerson(
                    wizard.createSingleDisplayLine('<liferay-ui:message key="assessor.contact.person" />', data.releaseSelection.eccInformation.assessorContactPerson),
                    false,
                    !wizard.getEnhancedFinalSingleValue('<liferay-ui:message key="assessor.contact.person" />').target,
                    true,
                    'pl-3'
                )
            );

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="commercial.details.administration" />'));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="usage.right.available" />', data.releaseSelection.cotsDetails.usageRightAvailable, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="cots.responsible" />', data.releaseSelection.cotsDetails.cotsResponsible));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="cots.clearing.deadline" />', data.releaseSelection.cotsDetails.clearingDeadline));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="cots.clearing.report.url" />', data.releaseSelection.cotsDetails.licenseClearingReportURL));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="cots.oss.information" />'));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="used.license" />', data.releaseSelection.cotsDetails.usedLicense));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="contains.oss" />', data.releaseSelection.cotsDetails.containsOSS, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="oss.contract.signed" />', data.releaseSelection.cotsDetails.ossContractSigned, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="oss.information.url" />', data.releaseSelection.cotsDetails.ossInformationURL));
            $stepElement.append(wizard.createSingleDisplayLineForHtml('<liferay-ui:message key="source.code.available" />', data.releaseSelection.cotsDetails.sourceCodeAvailable, flagFormatter));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="attachments" />'));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="attachments" />', data.releaseSelection.attachments, attachmentFormatter(displayInformation)));
        }

        function submitConfirmedMergedRelease($stepElement) {
            /* componentSourceId still as data at stepElement */
            /* componentSelection still as data at stepElement */
        }

        function errorHook($stepElement, textStatus, error) {
            if($stepElement.find('.stepFeedback').length === 0) {
                // initial loading
                $stepElement.html('<div class="stepFeedback"></div>');
            }

            $stepElement.find('.stepFeedback').html('<div class="alert alert-danger"><liferay-ui:message key="cannot.continue.to.merge.of.releases" />' + textStatus + error + '</div>');
            $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
        }

        function mapFormatter(data, key) {
            return function(value) {
                return value ? data[key][value] : '';
            }
        }

        function flagFormatter(flag) {
            if(flag) {
                return  '<span class=\"text-success\">' +
                        '   <svg class=\"lexicon-icon\"><title><liferay-ui:message key="yes" /></title><use href=\"/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#check-circle\"/></svg>' +
                        '   &nbsp;<liferay-ui:message key="yes" />' +
                        '</span>';
            } else {
                return  '<span class=\"text-danger\">' +
                        '   <svg class=\"lexicon-icon\"><title><liferay-ui:message key="yes" /></title><use href=\"/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#times-circle\"/></svg>' +
                        '   &nbsp;<title><liferay-ui:message key="no" />' +
                        '</span>';
            }
        }

        function vendorFormatter(vendor1, vendor2) {
            return function(id) {
                if(vendor1 && id === vendor1.id) {
                    return vendor1.fullname + ' (' + vendor1.shortname + ')<br/>' + vendor1.url;
                }
                if(vendor2 && id === vendor2.id) {
                    return vendor2.fullname + ' (' + vendor2.shortname + ')<br/>' + vendor2.url;
                }
                return '';
            };
        }

        function repositoryFormatter(data) {
            return function(repository) {
                return repository ? repository.url + ' (' + data['repositorytype'][repository.repositorytype] + ')' : '';
            }
        }

        function attachmentFormatter(data) {
            return function(attachment) {
                if (!attachment) {
                    return '';
                }
                return (attachment.filename || '-no-filename-') + ' (' + matchAttachmentType(data, attachment.attachmentType) + ')';
            };
        }

        function renderCreatedBy($line, renderInfo, user, alignment) {
            var $info = "<small class='merge-info-createdby form-text mt-0 pb-2 " + alignment + "'>" + 
                "<svg class='lexicon-icon'><use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open' /></svg> " + 
                <liferay-ui:message key="the.user.x.or.will.be.added.to.the.list.of.moderators" /> +
                "</small>";

            if(renderInfo) {
                $line.append($info);
            }

            return $line;
        }

        function renderAssessorContactPerson($line, useTarget, renderInfo, user, alignment) {
            $line.append(renderAssessorContactPersonInfo(useTarget, user, renderInfo, alignment));
            return $line;
        }

        function renderAssessorContactPersonInfo(useTarget, user, renderInfo, alignment) {
            var message = '';

            if(!renderInfo) {
                message = '';
            } else {       
                message = "<svg class='lexicon-icon'><use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open' /></svg> ";
                if(useTarget) {
                    message += <liferay-ui:message key="changing.the.assessor.contact.person.to.x.or.will.change.the.fields.assessor.department.and.assessement.date.accordingly" />;
                } else {
                    message += <liferay-ui:message key="the.fields.assessor.department.and.assessement.date.will.be.taken.as.well" />;
                }
            }

            return "<small class='merge-info-assessor form-text mt-0 pb-2 " + alignment + "'>" + message + "</small>";
        }

        function createSourceCodeAttachmentsMultiMergeLine(data, targetAttachments, sourceAttachments) {
            var result = {
                    sourceCodeAttachmentIds: {}
                };

            targetAttachments = targetAttachments || [];
            sourceAttachments = sourceAttachments || [];

            result.$element = wizard.createCustomMergeLines('<liferay-ui:message key="matching.source.attachments" />', function(container, createSingleMergeContent) {
                var rowIndex = 0;

                targetAttachments.forEach(function(targetAttachment) {
                    if(!isSourceAttachment(targetAttachment)) {
                        return;
                    }
                    
                    var sourceAttachment = sourceAttachments.find(function(sourceAttachment) {
                        return isSourceAttachment(sourceAttachment) && targetAttachment.sha1 == sourceAttachment.sha1; 
                    });
                    if(sourceAttachment) {
                        result.sourceCodeAttachmentIds[targetAttachment.sha1] = true;
                        container.append(createSingleMergeContent(targetAttachment, sourceAttachment, rowIndex++, attachmentFormatter(data), true));
                    }
                });
            });

            result.$element.append('' +
                "<small class='merge-info-attachments form-text mt-0 pb-2 text-center'>" +
                    "<svg class='lexicon-icon'><use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open' /></svg> " +
                    "<liferay-ui:message key="source.code.attachments.are.always.taken.from.source.release.and.cannot.be.deselected" />" +
                "</small>"
            );

            return result;
        }

        function isSourceAttachment(attachment) {
            return attachment.attachmentType == 1 || attachment.attachmentType == 9;
        }

        function matchAttachmentType(displayInformation, attachmentType) {
            if(!attachmentType) {
                return '-no-type-';
            }
            return displayInformation.attachmentType[attachmentType];
        }

        function filterAttachments(attachments, ids) {
            attachments = attachments || [];
            return attachments.filter(function(attachment) {
                return !isSourceAttachment(attachment) || !ids[attachment.sha1];
            });
        }

        function renderNotice(usageInformation) {
            var $note = $(
                '<div class="alert mt-4">' +
                    '<liferay-ui:message key="the.following.documents.will.be.affected.by.this.merge.and.are.changed.accordingly" />' +
                    '<ul>' +
                    '</ul>' +
                '</div>'
            );
            if(usageInformation.projects == 0 && 
                usageInformation.releases == 0 &&
                usageInformation.releaseVulnerabilities == 0 &&
                usageInformation.attachmentUsages == 0 &&
                usageInformation.projectRatings == 0
            ) {
                return $();
            }
            
            if(usageInformation.projects > 0) {
                var $li = $('<li><b class="number"></b> <liferay-ui:message key="projects2" /></li>');
                $li.find('.number').text(usageInformation.projects)
                
                $note.find('ul').append($li);
            }
            if(usageInformation.releases > 0) {
                var $li = $('<li><b class="number"></b> <liferay-ui:message key="releases2" /></li>');
                $li.find('.number').text(usageInformation.releases)
                
                $note.find('ul').append($li);
            }
            if(usageInformation.releaseVulnerabilities > 0) {
                var $li = $('<li><b class="number"></b> <liferay-ui:message key="vulnerabilities2" /></li>');
                $li.find('.number').text(usageInformation.releaseVulnerabilities)
                
                $note.find('ul').append($li);
            }
            if(usageInformation.attachmentUsages > 0) {
                var $li = $('<li><b class="number"></b> <liferay-ui:message key="attachment.usages2" /></li>');
                $li.find('.number').text(usageInformation.attachmentUsages)
                
                $note.find('ul').append($li);
            }
            if(usageInformation.projectRatings > 0) {
                var $li = $('<li><b class="number"></b> <liferay-ui:message key="project.ratings" /></li>');
                $li.find('.number').text(usageInformation.projectRatings)
                
                $note.find('ul').append($li);
            }

            if(usageInformation.projects + usageInformation.releases + usageInformation.releaseVulnerabilities + usageInformation.attachmentUsages + usageInformation.projectRatings > 1000) {
                $note.append('<b><liferay-ui:message key="more.than.1000.documents.affected.the.merge.operation.might.time.out.in.consequence.only.some.of" /> ' +
                    '<liferay-ui:message key="the.documents.might.be.changed.accordingly.in.this.case.just.restart.the.operation.as.long.as.the.source.release" /> ' + 
                    '<liferay-ui:message key="continues.to.exist" />');
                $note.addClass('alert-warning');
            } else {
                $note.addClass('alert-info');
            }
            return $note;
        }
    });
</script>
