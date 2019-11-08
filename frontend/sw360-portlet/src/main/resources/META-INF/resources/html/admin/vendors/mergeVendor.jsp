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
<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="vendorMergeWizardStepUrl" name="vendorMergeWizardStep"/>

<div id="vendorMergeWizard" class="container" data-step-id="0" data-vendor-target-id="${vendor.id}">
    <div class="row portlet-toolbar">
        <div class="col portlet-title text-truncate" title="Merge into <sw360:out value='${vendor.fullname}'/>">
            Merge into <sw360:out value='${vendor.fullname}'/>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="wizardHeader">
                <ul>
                    <li class="active">1. Choose source<br /><small>Choose a vendor that should be merged into the current one</small></li>
                    <li>2. Merge data<br /><small>Merge data from source into target vendor</small></li>
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
        var mergeWizardStepUrl = '<%=vendorMergeWizardStepUrl%>',
            postParamsPrefix = '<portlet:namespace/>',
            $wizardRoot = $('#vendorMergeWizard');

        wizard({
            wizardRoot: $wizardRoot,
            postUrl: mergeWizardStepUrl,
            postParamsPrefix: postParamsPrefix,
            loadErrorHook: errorHook,

            steps: [
                {
                    renderHook: renderChooseVendor,
                    submitHook: submitChosenVendor,
                    errorHook: errorHook
                },
                {
                    renderHook: renderMergeVendor,
                    submitHook: submitMergedVendor,
                    errorHook: errorHook
                },
                {
                    renderHook: renderConfirmMergedVendor,
                    submitHook: submitConfirmedMergedVendor,
                    errorHook: errorHook
                }
            ],
            finishCb: function($stepElement, data) {
                if (data && data.error) {
                    let $error = $('<div/>', {
                        'class': 'alert alert-danger mt-3'
                    });
                    let $idList = $('<ul>');

                    $error.append($('<p/>').append($('<b/>').text('Could not merge vendors: ' + data.error)));
                    $error.append($('<p/>').text('This error can lead to inconsistencies in the database. Please inform the administrator that the following vendors could not be merged:'));
                    $error.append($('<p>').append($idList));
                    
                    let releaseSourceId = $stepElement.data('releaseSourceId');
                    $idList.append($('<li>').text('Source vendor: ' + releaseSourceId));
                    $idList.append($('<li>').text('Target vendor: ' + $wizardRoot.data('releaseTargetId')));
                    
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

        function renderChooseVendor($stepElement, data) {
            $stepElement.html('' +
                    '<div class="stepFeedback"></div>' +
                    '<form>' +
                    '    <table id="vendorSourcesTable" class="table table-bordered" title="Source vendor">' +
                    '        <colgroup>' +
                    '            <col style="width: 1.7rem;" />' +
                    '            <col style="width: 50%;" />' +
                    '            <col style="width: 20%;" />' +
                    '            <col style="width: 30%;" />' +
                    '        </colgroup>' +
                    '        <thead>' +
                    '            <tr>' +
                    '                <th></th>' +
                    '                <th>Vendor Full Name</th>' +
                    '                <th>Vendor Short Name</th>' +
                    '                <th>URL</th>' +
                    '            </tr>' +
                    '        </thead>' +
                    '        <tbody>' +
                    '        </tbody>' +
                    '    </table>' +
                    '</form>'
                    );

            var table = datatables.create($stepElement.find('#vendorSourcesTable'), {
                data: data.vendors,
                columns: [
                    { data: "id", render: $.fn.dataTable.render.inputRadio('vendorChooser'), orderable: false },
                    { data: "fullname" },
                    { data: "shortname" },
                    { data: "url" }
                ],
                order: [ [ 1, 'asc' ] ],
                select: 'single'
            });
            datatables.enableCheckboxForSelection(table, 0);
        }

        function submitChosenVendor($stepElement) {
            var checkedList = $stepElement.find('input:checked');
            if (checkedList.length !== 1 || $(checkedList.get(0)).val() ===  $wizardRoot.data('vendorTargetId')) {
                $stepElement.find('.stepFeedback').html('<div class="alert alert-danger">Please choose exactly one vendor, which is not the target vendor itself!</div>');
                $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
                setTimeout(function() {
                    $stepElement.find('.stepFeedback').html('');
                }, 5000);
                return false;
            }
            $stepElement.data('vendorTargetId', $wizardRoot.data('vendorTargetId'));
            $stepElement.data('vendorSourceId', $(checkedList.get(0)).val());
        }

        function renderMergeVendor($stepElement, data) {
            $stepElement.html('<div class="stepFeedback"></div>');
            $stepElement.data('vendorSourceId', data.vendorSource.id);
            $wizardRoot.data('affectedComponents', data.affectedComponents);
            $wizardRoot.data('affectedReleases', data.affectedReleases);

            $stepElement.append(renderNotice(data.affectedComponents, data.affectedReleases));
            
            $stepElement.append(wizard.createCategoryLine('Vendor'));
            $stepElement.append(wizard.createSingleMergeLine('Full Name', data.vendorTarget.fullname, data.vendorSource.fullname));
            $stepElement.append(wizard.createSingleMergeLine('Short Name', data.vendorTarget.shortname, data.vendorSource.shortname));
            $stepElement.append(wizard.createSingleMergeLine('URL', data.vendorTarget.url, data.vendorSource.url));
            
            wizard.registerClickHandlers();
        }

        function submitMergedVendor($stepElement) {
            var vendorSelection = {};

            vendorSelection.id = $wizardRoot.data('vendorTargetId');

            vendorSelection.fullname = wizard.getFinalSingleValue('Full Name');
            vendorSelection.shortname = wizard.getFinalSingleValue('Short Name');
            vendorSelection.url = wizard.getFinalSingleValue('URL');

            $stepElement.data('vendorSelection', vendorSelection);
        }

        function renderConfirmMergedVendor($stepElement, data) {
            $stepElement.data('vendorSourceId', data.vendorSourceId);
            $stepElement.data('vendorSelection', data.vendorSelection);

            $stepElement.html('<div class="stepFeedback"></div>');

            $stepElement.append(renderNotice($wizardRoot.data('affectedComponents'), $wizardRoot.data('affectedReleases')));

            $stepElement.append(wizard.createCategoryLine('Vendor'));
            $stepElement.append(wizard.createSingleDisplayLine('Full Name', data.vendorSelection.fullname));
            $stepElement.append(wizard.createSingleDisplayLine('Short Name', data.vendorSelection.shortname));
            $stepElement.append(wizard.createSingleDisplayLine('URL', data.vendorSelection.url));
        }

        function submitConfirmedMergedVendor($stepElement) {
            /* vendorSourceId still as data at stepElement */
            /* vendorSelection still as data at stepElement */
        }

        function errorHook($stepElement, textStatus, error) {
            if($stepElement.find('.stepFeedback').length === 0) {
                // initial loading
                $stepElement.html('<div class="stepFeedback"></div>');
            }

            $wizardRoot.find('.active.step .stepFeedback').html('<div class="alert alert-danger">An error happened while communicating with the server: ' + textStatus + error + '</div>');
            $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
            setTimeout(function() {
                $wizardRoot.find('.active.step .stepFeedback').html('');
            }, 5000);
        }

        function renderNotice(affectedComponents, affectedReleases) {
            var $note = $(
                '<div class="alert mt-4">' +
                    'The following documents will be affected by this merge and are changed accordingly:' +
                    '<ul>' +
                    '</ul>' +
                '</div>'
            );

            if(affectedReleases == 0 && affectedComponents == 0) {
                return $();
            }

            if(affectedComponents > 0) {
                var $li = $('<li><b class="number"></b> component(s)</li>');
                $li.find('.number').text(affectedComponents)
                
                $note.find('ul').append($li);
            }
            if(affectedReleases > 0) {
                var $li = $('<li><b class="number"></b> release(s)</li>');
                $li.find('.number').text(affectedReleases)
                
                $note.find('ul').append($li);
            }
            if(affectedComponents + affectedReleases > 1000) {
                $note.append('<b>More than 1000 documents affected. The merge operation might time out. In consequence only some of ' +
                    'the documents might be changed accordingly. In this case just restart the operation as long as the source vendor ' + 
                    'continues to exist.');
                $note.addClass('alert-warning');
            } else {
                $note.addClass('alert-info');
            }

            return $note;
        }
    });
</script>
