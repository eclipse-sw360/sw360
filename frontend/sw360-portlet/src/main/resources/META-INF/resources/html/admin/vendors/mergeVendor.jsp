<%--
  ~ Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="vendorMergeWizardStepUrl" name="vendorMergeWizardStep"/>

<div id="vendorMergeWizard" class="container" data-step-id="0" data-vendor-target-id="${vendor.id}">
    <div class="row portlet-toolbar">
        <div class="col portlet-title text-truncate" title="<liferay-ui:message key="merge.into" /> <sw360:out value='${vendor.fullname}'/>">
            <liferay-ui:message key="merge.into" /> <sw360:out value='${vendor.fullname}'/>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="wizardHeader">
                <ul>
                    <li class="active"><liferay-ui:message key="choose.source" /><br /><small><liferay-ui:message key="choose.a.vendor.that.should.be.merged.into.the.current.one" /></small></li>
                    <li><liferay-ui:message key="merge.data" /><br /><small><liferay-ui:message key="merge.data.from.source.into.target.vendor" /></small></li>
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
                            <span class="sr-only"><liferay-ui:message key="loading.data.for.step.2.please.wait" /></span>
                        </div>
                        <liferay-ui:message key="loading.data.for.step.2.please.wait" />
                    </div>
                </div>
                <div class="step" data-step-id="3">
                    <div class="spinner spinner-with-text">
                        <div class="spinner-border" role="status">
                            <span class="sr-only"><liferay-ui:message key="loading.data.for.step.3.please.wait" /></span>
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

                    $error.append($('<p/>').append($('<b/>').text('<liferay-ui:message key="could.not.merge.vendors" /> ' + data.error)));
                    $error.append($('<p/>').text('<liferay-ui:message key="this.error.can.lead.to.inconsistencies.in.the.database.please.inform.the.administrator.that.the.following.vendors.could.not.be.merged" />'));
                    $error.append($('<p>').append($idList));
                    
                    let releaseSourceId = $stepElement.data('releaseSourceId');
                    $idList.append($('<li>').text('<liferay-ui:message key="source.vendor" />: ' + releaseSourceId));
                    $idList.append($('<li>').text('<liferay-ui:message key="target.vendor" />: ' + $wizardRoot.data('releaseTargetId')));
                    
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
                    '    <table id="vendorSourcesTable" class="table table-bordered" title="<liferay-ui:message key="source.vendor" />">' +
                    '        <colgroup>' +
                    '            <col style="width: 1.7rem;" />' +
                    '            <col style="width: 50%;" />' +
                    '            <col style="width: 20%;" />' +
                    '            <col style="width: 30%;" />' +
                    '        </colgroup>' +
                    '        <thead>' +
                    '            <tr>' +
                    '                <th></th>' +
                    '                <th><liferay-ui:message key="vendor.full.name" /></th>' +
                    '                <th><liferay-ui:message key="vendor.short.name" /></th>' +
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
                language: {
                    select: {
                        style: 'single',
                        rows: "<liferay-ui:message key="x.rows.selected" />"
                    },
                    url: "<liferay-ui:message key="datatables.lang" />"
                },
            });

            $("#vendorSourcesTable").on('init.dt', function() {
                datatables.enableCheckboxForSelection(table, 0);
            });
        }

        function submitChosenVendor($stepElement) {
            var checkedList = $stepElement.find('input:checked');
            if (checkedList.length !== 1 || $(checkedList.get(0)).val() ===  $wizardRoot.data('vendorTargetId')) {
                $stepElement.find('.stepFeedback').html('<div class="alert alert-danger"><liferay-ui:message key="please.choose.exactly.one.vendor.which.is.not.the.target.vendor.itself" /></div>');
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
            
            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="vendor" />'));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="full.name" />', data.vendorTarget.fullname, data.vendorSource.fullname));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="short.name" />', data.vendorTarget.shortname, data.vendorSource.shortname));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="url" />', data.vendorTarget.url, data.vendorSource.url));
            
            wizard.registerClickHandlers();
        }

        function submitMergedVendor($stepElement) {
            var vendorSelection = {};

            vendorSelection.id = $wizardRoot.data('vendorTargetId');

            vendorSelection.fullname = wizard.getFinalSingleValue('<liferay-ui:message key="full.name" />');
            vendorSelection.shortname = wizard.getFinalSingleValue('<liferay-ui:message key="short.name" />');
            vendorSelection.url = wizard.getFinalSingleValue('<liferay-ui:message key="url" />');

            $stepElement.data('vendorSelection', vendorSelection);
        }

        function renderConfirmMergedVendor($stepElement, data) {
            $stepElement.data('vendorSourceId', data.vendorSourceId);
            $stepElement.data('vendorSelection', data.vendorSelection);

            $stepElement.html('<div class="stepFeedback"></div>');

            $stepElement.append(renderNotice($wizardRoot.data('affectedComponents'), $wizardRoot.data('affectedReleases')));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="vendor" />'));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="full.name" />', data.vendorSelection.fullname));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="short.name" />', data.vendorSelection.shortname));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="url" />', data.vendorSelection.url));
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

            $wizardRoot.find('.active.step .stepFeedback').html('<div class="alert alert-danger"><liferay-ui:message key="an.error.happened.while.communicating.with.the.server" />: ' + textStatus + error + '</div>');
            $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
            setTimeout(function() {
                $wizardRoot.find('.active.step .stepFeedback').html('');
            }, 5000);
        }

        function renderNotice(affectedComponents, affectedReleases) {
            var $note = $(
                '<div class="alert mt-4">' +
                    '<liferay-ui:message key="the.following.documents.will.be.affected.by.this.merge.and.are.changed.accordingly" />' +
                    '<ul>' +
                    '</ul>' +
                '</div>'
            );

            if(affectedReleases == 0 && affectedComponents == 0) {
                return $();
            }

            if(affectedComponents > 0) {
                var $li = $('<li><b class="number"></b> <liferay-ui:message key="components2" /></li>');
                $li.find('.number').text(affectedComponents)
                
                $note.find('ul').append($li);
            }
            if(affectedReleases > 0) {
                var $li = $('<li><b class="number"></b> <liferay-ui:message key="releases2" /></li>');
                $li.find('.number').text(affectedReleases)
                
                $note.find('ul').append($li);
            }
            if(affectedComponents + affectedReleases > 1000) {
                $note.append('<b><liferay-ui:message key="more.than.1000.documents.affected.the.merge.operation.might.time.out.in.consequence.only.some.of" /> ' +
                    '<liferay-ui:message key="the.documents.might.be.changed.accordingly.in.this.case.just.restart.the.operation.as.long.as.the.source.vendor" /> ' + 
                    '<liferay-ui:message key="continues.to.exist" />');
                $note.addClass('alert-warning');
            } else {
                $note.addClass('alert-info');
            }

            return $note;
        }
    });
</script>
