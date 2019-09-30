<%--
  ~ Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
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
        <div class="col portlet-title text-truncate" title="Merge into ${sw360:printReleaseName(release)}">
            Merge into ${sw360:printReleaseName(release)}
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="wizardHeader">
                <ul>
                    <li class="active">1. Choose source<br /><small>Choose a release that should be merged into the current one</small></li>
                    <li>2. Merge data<br /><small>Merge data from source into target release</small></li>
                    <li>3. Confirm<br /><small>Check the merged version and confirm</small></li>
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
                            <span class="sr-only">Loading data for step 1, please wait...</span>
                        </div>
                        Loading data for step 1, please wait...
                    </div>
                </div>
                <div class="step" data-step-id="2">
                    <div class="spinner spinner-with-text">
                        <div class="spinner-border" role="status">
                            <span class="sr-only">Loading data for step 2, please wait...</span>
                        </div>
                        Loading data for step 2, please wait...
                    </div>
                </div>
                <div class="step" data-step-id="3">
                    <div class="spinner spinner-with-text">
                        <div class="spinner-border" role="status">
                            <span class="sr-only">Loading data for step 3, please wait...</span>
                        </div>
                        Loading data for step 3, please wait...
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

                    $error.append($('<p/>').append($('<b/>').text('Could not merge releases: ' + data.error)));
                    $error.append($('<p/>').text('This error can lead to inconsistencies in the database. Please inform the administrator with the following information:'));
                    $error.append($('<p>').append($idList));
                    
                    let releaseSourceId = $stepElement.data('releaseSourceId');
                    $idList.append($('<li>').text('Source release: ' + releaseSourceId));
                    $idList.append($('<li>').text('Target release: ' + $wizardRoot.data('releaseTargetId')));

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
                    '    <table id="releaseSourcesTable" class="table table-bordered" title="Source release">' +
                    '        <colgroup>' +
                    '            <col style="width: 1.7rem;" />' +
                    '            <col style="width: 70%;" />' +
                    '            <col style="width: 10%;" />' +
                    '            <col style="width: 20%;" />' +
                    '        </colgroup>' +
                    '        <thead>' +
                    '            <tr>' +
                    '                <th></th>' +
                    '                <th>Release name</th>' +
                    '                <th>Version</th>' + 
                    '                <th>Created by</th>' +
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
                    { data: "name" },
                    { data: "version" },
                    { data: "createdBy" }
                ],
                order: [ [ 1, 'asc' ] ],
                select: 'single'
            }, undefined, [0], true);
            datatables.enableCheckboxForSelection(table, 0);
        }

        function submitChosenRelease($stepElement) {
            var checkedList = $stepElement.find('input:checked');
            if (checkedList.length !== 1 || $(checkedList.get(0)).val() ===  $wizardRoot.data('releaseTargetId')) {
                $stepElement.find('.stepFeedback').html('<div class="alert alert-danger">Please choose exactly one release, which is not the release itself!</div>');
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

            $stepElement.append(wizard.createCategoryLine('General'));
            $stepElement.append(wizard.createSingleMergeLine('Vendor', 
                data.releaseTarget.vendor ? data.releaseTarget.vendor.id : null, 
                data.releaseSource.vendor ? data.releaseSource.vendor.id : null, 
                vendorFormatter(data.releaseTarget.vendor, data.releaseSource.vendor)
            ));
            $stepElement.append(wizard.createSingleMergeLine('Name', data.releaseTarget.name, data.releaseSource.name));
            $stepElement.append(wizard.createSingleMergeLine('Version', data.releaseTarget.version, data.releaseSource.version));
            $stepElement.append(wizard.createMultiMergeLine('Programming Languages', data.releaseTarget.languages, data.releaseSource.languages));
            $stepElement.append(wizard.createMultiMergeLine('Operating Systems', data.releaseTarget.operatingSystems, data.releaseSource.operatingSystems));
            $stepElement.append(wizard.createSingleMergeLine('CPE ID', data.releaseTarget.cpeid, data.releaseSource.cpeid));
            $stepElement.append(wizard.createMultiMergeLine('Software Platforms', data.releaseTarget.softwarePlatforms, data.releaseSource.softwarePlatforms));
            $stepElement.append(wizard.createSingleMergeLine('Release Date', data.releaseTarget.releaseDate, data.releaseSource.releaseDate));
            $stepElement.append(wizard.createMultiMergeLine('Licenses', data.releaseTarget.mainLicenseIds, data.releaseSource.mainLicenseIds));
            $stepElement.append(wizard.createSingleMergeLine('Download URL', data.releaseTarget.downloadurl, data.releaseSource.downloadurl));
            $stepElement.append(wizard.createSingleMergeLine('Release Mainline State', data.releaseTarget.mainlineState, data.releaseSource.mainlineState, mapFormatter(data.displayInformation, 'mainlineState')));
            $stepElement.append(wizard.createSingleMergeLine('Created on', data.releaseTarget.createdOn, data.releaseSource.createdOn));
            $stepElement.append(
                renderCreatedBy(
                    wizard.createSingleMergeLine('Created by', data.releaseTarget.createdBy, data.releaseSource.createdBy),
                    data.releaseSource.createdBy != data.releaseTarget.createdBy,
                    data.releaseSource.createdBy,
                    'text-center'
                )
            );
            $stepElement.append(wizard.createMultiMergeLine('Contributors', data.releaseTarget.contributors, data.releaseSource.contributors));
            $stepElement.append(wizard.createMultiMergeLine('Moderators', data.releaseTarget.moderators, data.releaseSource.moderators));
            $stepElement.append(wizard.createMultiMergeLine('Subscribers', data.releaseTarget.subscribers, data.releaseSource.subscribers));
            $stepElement.append(wizard.createSingleMergeLine('Repository', data.releaseTarget.repository, data.releaseSource.repository, repositoryFormatter(data.displayInformation)));
            $stepElement.append(wizard.createMultiMapMergeLine('Additional Roles', data.releaseTarget.roles, data.releaseSource.roles));
            $stepElement.append(wizard.createMapMergeLine('External ids', data.releaseTarget.externalIds, data.releaseSource.externalIds));
            $stepElement.append(wizard.createMapMergeLine('Additional Data', data.releaseTarget.additionalData, data.releaseSource.additionalData));
            
            $stepElement.append(wizard.createCategoryLine('Linked Releases'));
            $stepElement.append(wizard.createMultiMergeLine('Linked Releases', Object.keys(data.releaseTarget.releaseIdToRelationship || {}), Object.keys(data.releaseSource.releaseIdToRelationship || {}), mapFormatter(data.displayInformation, 'release')));

            $stepElement.append(wizard.createCategoryLine('Clearing Details'));
            data.releaseTarget.clearingInformation = data.releaseTarget.clearingInformation || {};
            data.releaseSource.clearingInformation = data.releaseSource.clearingInformation || {};
            $stepElement.append(wizard.createSingleMergeLine('Binaries Original from Community', data.releaseTarget.clearingInformation.binariesOriginalFromCommunity, data.releaseSource.clearingInformation.binariesOriginalFromCommunity, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Binaries Self-Made', data.releaseTarget.clearingInformation.binariesSelfMade, data.releaseSource.clearingInformation.binariesSelfMade, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Component License Information', data.releaseTarget.clearingInformation.componentLicenseInformation, data.releaseSource.clearingInformation.componentLicenseInformation, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Source Code Delivery', data.releaseTarget.clearingInformation.sourceCodeDelivery, data.releaseSource.clearingInformation.sourceCodeDelivery, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Source Code Original from Community', data.releaseTarget.clearingInformation.sourceCodeOriginalFromCommunity, data.releaseSource.clearingInformation.sourceCodeOriginalFromCommunity, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Source Code Tool-Made', data.releaseTarget.clearingInformation.sourceCodeToolMade, data.releaseSource.clearingInformation.sourceCodeToolMade, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Source Code Self-Made', data.releaseTarget.clearingInformation.sourceCodeSelfMade, data.releaseSource.clearingInformation.sourceCodeSelfMade, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Screenshot of Website', data.releaseTarget.clearingInformation.screenshotOfWebSite, data.releaseSource.clearingInformation.screenshotOfWebSite, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Finalized License Scan Report', data.releaseTarget.clearingInformation.finalizedLicenseScanReport, data.releaseSource.clearingInformation.finalizedLicenseScanReport, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('License Scan Report Result', data.releaseTarget.clearingInformation.licenseScanReportResult, data.releaseSource.clearingInformation.licenseScanReportResult, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Legal Evaluation', data.releaseTarget.clearingInformation.legalEvaluation, data.releaseSource.clearingInformation.legalEvaluation, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('License Agreement', data.releaseTarget.clearingInformation.licenseAgreement, data.releaseSource.clearingInformation.licenseAgreement, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Scanned', data.releaseTarget.clearingInformation.scanned, data.releaseSource.clearingInformation.scanned));
            $stepElement.append(wizard.createSingleMergeLine('Component Clearing Report', data.releaseTarget.clearingInformation.componentClearingReport, data.releaseSource.clearingInformation.componentClearingReport, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('Clearing Standard', data.releaseTarget.clearingInformation.clearingStandard, data.releaseSource.clearingInformation.clearingStandard));
            $stepElement.append(wizard.createSingleMergeLine('External URL', data.releaseTarget.clearingInformation.externalUrl, data.releaseSource.clearingInformation.externalUrl));
            $stepElement.append(wizard.createSingleMergeLine('Comment', data.releaseTarget.clearingInformation.comment, data.releaseSource.clearingInformation.comment));

            $stepElement.append(wizard.createCategoryLine('Request Information'));
            $stepElement.append(wizard.createSingleMergeLine('Request ID', data.releaseTarget.clearingInformation.requestID, data.releaseSource.clearingInformation.requestID));
            $stepElement.append(wizard.createSingleMergeLine('Additional Request Info', data.releaseTarget.clearingInformation.additionalRequestInfo, data.releaseSource.clearingInformation.additionalRequestInfo));
            $stepElement.append(wizard.createSingleMergeLine('Evaluation Start', data.releaseTarget.clearingInformation.procStart, data.releaseSource.clearingInformation.procStart));
            $stepElement.append(wizard.createSingleMergeLine('Evalutation End', data.releaseTarget.clearingInformation.evaluated, data.releaseSource.clearingInformation.evaluated));

            $stepElement.append(wizard.createCategoryLine('Supplemental Information'));
            $stepElement.append(wizard.createSingleMergeLine('External Supplier ID', data.releaseTarget.clearingInformation.externalSupplierID, data.releaseSource.clearingInformation.externalSupplierID));
            $stepElement.append(wizard.createSingleMergeLine('Count of Security Vulnerabilities', data.releaseTarget.clearingInformation.countOfSecurityVn, data.releaseSource.clearingInformation.countOfSecurityVn));

            $stepElement.append(wizard.createCategoryLine('ECC Information'));
            data.releaseTarget.eccInformation = data.releaseTarget.eccInformation || {};
            data.releaseSource.eccInformation = data.releaseSource.eccInformation || {};
            $stepElement.append(wizard.createSingleMergeLine('ECC Status', data.releaseTarget.eccInformation.eccStatus, data.releaseSource.eccInformation.eccStatus, mapFormatter(data.displayInformation, 'eccStatus')));
            $stepElement.append(wizard.createSingleMergeLine('ECC Comment', data.releaseTarget.eccInformation.eccComment, data.releaseSource.eccInformation.eccComment));
            $stepElement.append(wizard.createSingleMergeLine('Ausfuhrliste', data.releaseTarget.eccInformation.AL, data.releaseSource.eccInformation.AL));
            $stepElement.append(wizard.createSingleMergeLine('ECCN', data.releaseTarget.eccInformation.ECCN, data.releaseSource.eccInformation.ECCN));
            $stepElement.append(wizard.createSingleMergeLine('Material Index Number', data.releaseTarget.eccInformation.materialIndexNumber, data.releaseSource.eccInformation.materialIndexNumber));
            $stepElement.append(
                renderAssessorContactPerson(
                    wizard.createSingleMergeLine('Assessor Contact Person', data.releaseTarget.eccInformation.assessorContactPerson, data.releaseSource.eccInformation.assessorContactPerson),
                    true,
                    data.releaseSource.eccInformation.assessorContactPerson && data.releaseSource.eccInformation.assessorContactPerson != data.releaseTarget.eccInformation.assessorContactPerson,
                    data.releaseSource.eccInformation.assessorContactPerson,
                    'text-center'
                )
            );

            $stepElement.append(wizard.createCategoryLine('Commercial Details Administration'));
            data.releaseTarget.cotsDetails = data.releaseTarget.cotsDetails || {};
            data.releaseSource.cotsDetails = data.releaseSource.cotsDetails || {};
            $stepElement.append(wizard.createSingleMergeLine('Usage Right Available', data.releaseTarget.cotsDetails.usageRightAvailable, data.releaseSource.cotsDetails.usageRightAvailable, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('COTS Responsible', data.releaseTarget.cotsDetails.cotsResponsible, data.releaseSource.cotsDetails.cotsResponsible));
            $stepElement.append(wizard.createSingleMergeLine('COTS Clearing Deadline', data.releaseTarget.cotsDetails.clearingDeadline, data.releaseSource.cotsDetails.clearingDeadline));
            $stepElement.append(wizard.createSingleMergeLine('COTS Clearing Report URL', data.releaseTarget.cotsDetails.licenseClearingReportURL, data.releaseSource.cotsDetails.licenseClearingReportURL));

            $stepElement.append(wizard.createCategoryLine('COTS OSS Information'));
            $stepElement.append(wizard.createSingleMergeLine('Used License', data.releaseTarget.cotsDetails.usedLicense, data.releaseSource.cotsDetails.usedLicense));
            $stepElement.append(wizard.createSingleMergeLine('Contains OSS', data.releaseTarget.cotsDetails.containsOSS, data.releaseSource.cotsDetails.containsOSS, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('OSS contract signed', data.releaseTarget.cotsDetails.ossContractSigned, data.releaseSource.cotsDetails.ossContractSigned, flagFormatter));
            $stepElement.append(wizard.createSingleMergeLine('OSS Information URL', data.releaseTarget.cotsDetails.ossInformationURL, data.releaseSource.cotsDetails.ossInformationURL));
            $stepElement.append(wizard.createSingleMergeLine('Source Code Available', data.releaseTarget.cotsDetails.sourceCodeAvailable, data.releaseSource.cotsDetails.sourceCodeAvailable, flagFormatter));

            $stepElement.append(wizard.createCategoryLine('Attachments'));
            var sourceAttachmentMerge = createSourceCodeAttachmentsMultiMergeLine(data.displayInformation, data.releaseTarget.attachments, data.releaseSource.attachments);
            $stepElement.append(sourceAttachmentMerge.$element);
            $stepElement.append(wizard.createMultiMergeLine('Other Attachments', 
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

            releaseSelection.vendor = wizard.getEnhancedFinalSingleValue('Vendor').target ? releaseTarget.vendor : releaseSource.vendor;
            releaseSelection.vendorId = releaseSelection.vendor ? releaseSelection.vendorId : undefined;
            releaseSelection.name = wizard.getFinalSingleValue('Name');
            releaseSelection.version = wizard.getFinalSingleValue('Version');
            releaseSelection.languages = wizard.getFinalMultiValue('Programming Languages');
            releaseSelection.operatingSystems = wizard.getFinalMultiValue('Operating Systems');
            releaseSelection.cpeid = wizard.getFinalSingleValue('CPE ID');
            releaseSelection.softwarePlatforms = wizard.getFinalMultiValue('Software Platforms');
            releaseSelection.releaseDate = wizard.getFinalSingleValue('Release Date');
            releaseSelection.mainLicenseIds = wizard.getFinalMultiValue('Licenses');
            releaseSelection.downloadurl = wizard.getFinalSingleValue('Download URL');
            releaseSelection.mainlineState = wizard.getFinalSingleValue('Release Mainline State');
            releaseSelection.createdOn = wizard.getFinalSingleValue('Created on');
            releaseSelection.createdBy = wizard.getFinalSingleValue('Created by');

            releaseSelection.contributors = wizard.getFinalMultiValue('Contributors');
            releaseSelection.moderators = wizard.getFinalMultiValue('Moderators');
            releaseSelection.subscribers = wizard.getFinalMultiValue('Subscribers');
            releaseSelection.repository = wizard.getFinalSingleValue('Repository');
            releaseSelection.roles = wizard.getFinalMultiMapValue('Additional Roles');
            releaseSelection.externalIds = wizard.getFinalMapValue('External ids');
            releaseSelection.additionalData = wizard.getFinalMapValue('Additional Data');
            
            releaseSelection.releaseIdToRelationship = {};
            wizard.getEnhancedFinalMultiValue('Linked Releases').forEach(function(result) {
                releaseSelection.releaseIdToRelationship[result.value] = result.target ? releaseTarget.releaseIdToRelationship[result.value] : releaseSource.releaseIdToRelationship[result.value];
            });

            releaseSelection.clearingInformation = {};
            releaseSelection.clearingInformation.binariesOriginalFromCommunity = wizard.getFinalSingleValue('Binaries Original from Community');
            releaseSelection.clearingInformation.binariesSelfMade = wizard.getFinalSingleValue('Binaries Self-Made');
            releaseSelection.clearingInformation.componentLicenseInformation = wizard.getFinalSingleValue('Component License Information');
            releaseSelection.clearingInformation.sourceCodeDelivery = wizard.getFinalSingleValue('Source Code Delivery');
            releaseSelection.clearingInformation.sourceCodeOriginalFromCommunity = wizard.getFinalSingleValue('Source Code Original from Community');
            releaseSelection.clearingInformation.sourceCodeToolMade = wizard.getFinalSingleValue('Source Code Tool-Made');
            releaseSelection.clearingInformation.sourceCodeSelfMade = wizard.getFinalSingleValue('Source Code Self-Made');
            releaseSelection.clearingInformation.screenshotOfWebSite = wizard.getFinalSingleValue('Screenshot of Website');
            releaseSelection.clearingInformation.finalizedLicenseScanReport = wizard.getFinalSingleValue('Finalized License Scan Report');
            releaseSelection.clearingInformation.licenseScanReportResult = wizard.getFinalSingleValue('License Scan Report Result');
            releaseSelection.clearingInformation.legalEvaluation = wizard.getFinalSingleValue('Legal Evaluation');
            releaseSelection.clearingInformation.licenseAgreement = wizard.getFinalSingleValue('License Agreement');
            releaseSelection.clearingInformation.scanned = wizard.getFinalSingleValue('Scanned');
            releaseSelection.clearingInformation.componentClearingReport = wizard.getFinalSingleValue('Component Clearing Report');
            releaseSelection.clearingInformation.clearingStandard = wizard.getFinalSingleValue('Clearing Standard');
            releaseSelection.clearingInformation.externalUrl = wizard.getFinalSingleValue('External URL');
            releaseSelection.clearingInformation.comment = wizard.getFinalSingleValue('Comment');

            releaseSelection.clearingInformation.requestID = wizard.getFinalSingleValue('Request ID');
            releaseSelection.clearingInformation.additionalRequestInfo = wizard.getFinalSingleValue('Additional Request Info');
            releaseSelection.clearingInformation.procStart = wizard.getFinalSingleValue('Evaluation Start');
            releaseSelection.clearingInformation.evaluated = wizard.getFinalSingleValue('Evalutation End');
            
            releaseSelection.clearingInformation.externalSupplierID = wizard.getFinalSingleValue('External Supplier ID');
            releaseSelection.clearingInformation.countOfSecurityVn = wizard.getFinalSingleValue('Count of Security Vulnerabilities');

            releaseSelection.eccInformation = {};
            releaseSelection.eccInformation.eccStatus = wizard.getFinalSingleValue('ECC Status');
            releaseSelection.eccInformation.eccComment = wizard.getFinalSingleValue('ECC Comment');
            releaseSelection.eccInformation.AL = wizard.getFinalSingleValue('Ausfuhrliste');
            releaseSelection.eccInformation.ECCN = wizard.getFinalSingleValue('ECCN');
            releaseSelection.eccInformation.materialIndexNumber = wizard.getFinalSingleValue('Material Index Number');
            assessorResult = wizard.getEnhancedFinalSingleValue('Assessor Contact Person');
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
            releaseSelection.cotsDetails.usageRightAvailable = wizard.getFinalSingleValue('Usage Right Available');
            releaseSelection.cotsDetails.cotsResponsible = wizard.getFinalSingleValue('COTS Responsible');
            releaseSelection.cotsDetails.clearingDeadline = wizard.getFinalSingleValue('COTS Clearing Deadline');
            releaseSelection.cotsDetails.licenseClearingReportURL = wizard.getFinalSingleValue('COTS Clearing Report URL');

            releaseSelection.cotsDetails.usedLicense = wizard.getFinalSingleValue('Used License');
            releaseSelection.cotsDetails.containsOSS = wizard.getFinalSingleValue('Contains OSS');
            releaseSelection.cotsDetails.ossContractSigned = wizard.getFinalSingleValue('OSS contract signed');
            releaseSelection.cotsDetails.ossInformationURL = wizard.getFinalSingleValue('OSS Information URL');
            releaseSelection.cotsDetails.sourceCodeAvailable = wizard.getFinalSingleValue('Source Code Available');


            releaseSelection.attachments = [];
            sourceCodeAttachments = wizard.getFinalMultiValue('Matching Source Attachments');
            $.each(sourceCodeAttachments, function(index, value) {
                /* add just required fields for easy identification */
                releaseSelection.attachments.push(JSON.parse('{ "attachmentContentId": "' + value.attachmentContentId + '", "filename": "' + value.filename + '"}'));
            });
            attachments = wizard.getFinalMultiValue('Other Attachments');
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

            $stepElement.append(wizard.createCategoryLine('General'));
            $stepElement.append(wizard.createSingleDisplayLine('Vendor', 
                data.releaseSelection.vendor ? data.releaseSelection.vendor.id : null, 
                vendorFormatter(releaseTarget.vendor, releaseSource.vendor)
            ));
            $stepElement.append(wizard.createSingleDisplayLine('Name', data.releaseSelection.name));
            $stepElement.append(wizard.createSingleDisplayLine('Version', data.releaseSelection.version));
            $stepElement.append(wizard.createMultiDisplayLine('Programming Languages', data.releaseSelection.languages));
            $stepElement.append(wizard.createMultiDisplayLine('Operating Systems', data.releaseSelection.operatingSystems));
            $stepElement.append(wizard.createSingleDisplayLine('CPE ID', data.releaseSelection.cpeid));
            $stepElement.append(wizard.createMultiDisplayLine('Software Platforms', data.releaseSelection.softwarePlatforms));
            $stepElement.append(wizard.createSingleDisplayLine('Release Date', data.releaseSelection.releaseDate));
            $stepElement.append(wizard.createMultiDisplayLine('Licenses', data.releaseSelection.mainLicenseIds));
            $stepElement.append(wizard.createSingleDisplayLine('Download URL', data.releaseSelection.downloadurl));
            $stepElement.append(wizard.createSingleDisplayLine('Release Mainline State', data.releaseSelection.mainlineState, mapFormatter(displayInformation, 'mainlineState')));
            $stepElement.append(wizard.createSingleDisplayLine('Created on', data.releaseSelection.createdOn));
            $stepElement.append(
                renderCreatedBy(
                    wizard.createSingleDisplayLine('Created by', data.releaseSelection.createdBy),
                    releaseSource.createdBy != releaseTarget.createdBy,
                    data.releaseSelection.createdBy === releaseSource.createdBy ? releaseTarget.createdBy : releaseSource.createdBy,
                    'pl-3'
                )
            );
            $stepElement.append(wizard.createMultiDisplayLine('Contributors', data.releaseSelection.contributors));
            $stepElement.append(wizard.createMultiDisplayLine('Moderators', data.releaseSelection.moderators));
            $stepElement.append(wizard.createMultiDisplayLine('Subscribers', data.releaseSelection.subscribers));
            $stepElement.append(wizard.createSingleDisplayLine('Repository', data.releaseSelection.repository, repositoryFormatter(displayInformation)));
            $stepElement.append(wizard.createMultiMapDisplayLine('Additional Roles', data.releaseSelection.roles));
            $stepElement.append(wizard.createMapDisplayLine('External ids', data.releaseSelection.externalIds));
            $stepElement.append(wizard.createMapDisplayLine('Additional Data', data.releaseSelection.additionalData));
            
            $stepElement.append(wizard.createCategoryLine('Linked Releases'));
            $stepElement.append(wizard.createMultiDisplayLine('Linked Releases', Object.keys(data.releaseSelection.releaseIdToRelationship), mapFormatter(displayInformation, 'release')));

            $stepElement.append(wizard.createCategoryLine('Clearing Details'));
            $stepElement.append(wizard.createSingleDisplayLine('Binaries Original from Community', data.releaseSelection.clearingInformation.binariesOriginalFromCommunity, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Binaries Self-Made', data.releaseSelection.clearingInformation.binariesSelfMade, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Component License Information', data.releaseSelection.clearingInformation.componentLicenseInformation, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Source Code Delivery', data.releaseSelection.clearingInformation.sourceCodeDelivery, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Source Code Original from Community', data.releaseSelection.clearingInformation.sourceCodeOriginalFromCommunity, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Source Code Tool-Made', data.releaseSelection.clearingInformation.sourceCodeToolMade, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Source Code Self-Made', data.releaseSelection.clearingInformation.sourceCodeSelfMade, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Screenshot of Website', data.releaseSelection.clearingInformation.screenshotOfWebSite, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Finalized License Scan Report', data.releaseSelection.clearingInformation.finalizedLicenseScanReport, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('License Scan Report Result', data.releaseSelection.clearingInformation.licenseScanReportResult, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Legal Evaluation', data.releaseSelection.clearingInformation.legalEvaluation, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('License Agreement', data.releaseSelection.clearingInformation.licenseAgreement, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Scanned', data.releaseSelection.clearingInformation.scanned));
            $stepElement.append(wizard.createSingleDisplayLine('Component Clearing Report', data.releaseSelection.clearingInformation.componentClearingReport, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('Clearing Standard', data.releaseSelection.clearingInformation.clearingStandard));
            $stepElement.append(wizard.createSingleDisplayLine('External URL', data.releaseSelection.clearingInformation.externalUrl));
            $stepElement.append(wizard.createSingleDisplayLine('Comment', data.releaseSelection.clearingInformation.comment));

            $stepElement.append(wizard.createCategoryLine('Request Information'));
            $stepElement.append(wizard.createSingleDisplayLine('Request ID', data.releaseSelection.clearingInformation.requestID));
            $stepElement.append(wizard.createSingleDisplayLine('Additional Request Info', data.releaseSelection.clearingInformation.additionalRequestInfo));
            $stepElement.append(wizard.createSingleDisplayLine('Evaluation Start', data.releaseSelection.clearingInformation.procStart));
            $stepElement.append(wizard.createSingleDisplayLine('Evalutation End', data.releaseSelection.clearingInformation.evaluated));

            $stepElement.append(wizard.createCategoryLine('Supplemental Information'));
            $stepElement.append(wizard.createSingleDisplayLine('External Supplier ID', data.releaseSelection.clearingInformation.externalSupplierID));
            $stepElement.append(wizard.createSingleDisplayLine('Count of Security Vulnerabilities', data.releaseSelection.clearingInformation.countOfSecurityVn));

            $stepElement.append(wizard.createCategoryLine('ECC Information'));
            $stepElement.append(wizard.createSingleDisplayLine('ECC Status', data.releaseSelection.eccInformation.eccStatus, mapFormatter(displayInformation, 'eccStatus')));
            $stepElement.append(wizard.createSingleDisplayLine('ECC Comment', data.releaseSelection.eccInformation.eccComment));
            $stepElement.append(wizard.createSingleDisplayLine('Ausfuhrliste', data.releaseSelection.eccInformation.AL));
            $stepElement.append(wizard.createSingleDisplayLine('ECCN', data.releaseSelection.eccInformation.ECCN));
            $stepElement.append(wizard.createSingleDisplayLine('Material Index Number', data.releaseSelection.eccInformation.materialIndexNumber));
            $stepElement.append(
                renderAssessorContactPerson(
                    wizard.createSingleDisplayLine('Assessor Contact Person', data.releaseSelection.eccInformation.assessorContactPerson),
                    false,
                    !wizard.getEnhancedFinalSingleValue('Assessor Contact Person').target,
                    true,
                    'pl-3'
                )
            );

            $stepElement.append(wizard.createCategoryLine('Commercial Details Administration'));
            $stepElement.append(wizard.createSingleDisplayLine('Usage Right Available', data.releaseSelection.cotsDetails.usageRightAvailable, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('COTS Responsible', data.releaseSelection.cotsDetails.cotsResponsible));
            $stepElement.append(wizard.createSingleDisplayLine('COTS Clearing Deadline', data.releaseSelection.cotsDetails.clearingDeadline));
            $stepElement.append(wizard.createSingleDisplayLine('COTS Clearing Report URL', data.releaseSelection.cotsDetails.licenseClearingReportURL));

            $stepElement.append(wizard.createCategoryLine('COTS OSS Information'));
            $stepElement.append(wizard.createSingleDisplayLine('Used License', data.releaseSelection.cotsDetails.usedLicense));
            $stepElement.append(wizard.createSingleDisplayLine('Contains OSS', data.releaseSelection.cotsDetails.containsOSS, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('OSS contract signed', data.releaseSelection.cotsDetails.ossContractSigned, flagFormatter));
            $stepElement.append(wizard.createSingleDisplayLine('OSS Information URL', data.releaseSelection.cotsDetails.ossInformationURL));
            $stepElement.append(wizard.createSingleDisplayLine('Source Code Available', data.releaseSelection.cotsDetails.sourceCodeAvailable, flagFormatter));

            $stepElement.append(wizard.createCategoryLine('Attachments'));
            $stepElement.append(wizard.createMultiDisplayLine('Attachments', data.releaseSelection.attachments, attachmentFormatter(displayInformation)));
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

            $stepElement.find('.stepFeedback').html('<div class="alert alert-danger">Cannot continue to merge of releases: ' + textStatus + error + '</div>');
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
                        '   <svg class=\"lexicon-icon\"><title>Yes</title><use href=\"/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#check-circle\"/></svg>' +
                        '   &nbsp;Yes' +
                        '</span>';
            } else {
                return  '<span class=\"text-danger\">' +
                        '   <svg class=\"lexicon-icon\"><title>Yes</title><use href=\"/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#times-circle\"/></svg>' +
                        '   &nbsp;No' +
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
                "The user <b class='user'>" + (user || '') + "</b> will be added to the list of moderators." +
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
                    message += "Changing the assessor contact person to <b>" + (user || '') + "</b> will change the fields <b>Assessor Department</b> and <b>Assessement Date</b> accordingly.";
                } else {
                    message += "The fields <b>Assessor Department</b> and <b>Assessement Date</b> will be taken as well.";
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

            result.$element = wizard.createCustomMergeLines('Matching Source Attachments', function(container, createSingleMergeContent) {
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
                    "Source Code Attachments are always taken from source release and cannot be deselected." +
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
                    'The following documents will be affected by this merge and are changed accordingly:' +
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
                var $li = $('<li><b class="number"></b> project(s)</li>');
                $li.find('.number').text(usageInformation.projects)
                
                $note.find('ul').append($li);
            }
            if(usageInformation.releases > 0) {
                var $li = $('<li><b class="number"></b> release(s)</li>');
                $li.find('.number').text(usageInformation.releases)
                
                $note.find('ul').append($li);
            }
            if(usageInformation.releaseVulnerabilities > 0) {
                var $li = $('<li><b class="number"></b> vulnerabilities</li>');
                $li.find('.number').text(usageInformation.releaseVulnerabilities)
                
                $note.find('ul').append($li);
            }
            if(usageInformation.attachmentUsages > 0) {
                var $li = $('<li><b class="number"></b> attachment usage(s)</li>');
                $li.find('.number').text(usageInformation.attachmentUsages)
                
                $note.find('ul').append($li);
            }
            if(usageInformation.projectRatings > 0) {
                var $li = $('<li><b class="number"></b> project rating(s)</li>');
                $li.find('.number').text(usageInformation.projectRatings)
                
                $note.find('ul').append($li);
            }

            if(usageInformation.projects + usageInformation.releases + usageInformation.releaseVulnerabilities + usageInformation.attachmentUsages + usageInformation.projectRatings > 1000) {
                $note.append('<b>More than 1000 documents affected. The merge operation might time out. In consequence only some of ' +
                    'the documents might be changed accordingly. In this case just restart the operation as long as the source release ' + 
                    'continues to exist.');
                $note.addClass('alert-warning');
            } else {
                $note.addClass('alert-info');
            }
            return $note;
        }
    });
</script>
