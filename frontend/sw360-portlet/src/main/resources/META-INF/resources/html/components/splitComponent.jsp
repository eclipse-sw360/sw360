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

<portlet:actionURL var="componentSplitWizardStepUrl" name="componentSplitWizardStep"/>

<div id="componentSplitWizard" class="container" data-step-id="0" data-component-source-id="${component.id}">
    <div class="row portlet-toolbar">
        <div class="col portlet-title text-truncate" title="<liferay-ui:message key="split.into" /> ${sw360:printComponentName(component)}">
            <liferay-ui:message key="split.into" /> <sw360:out value="${sw360:printComponentName(component)}"/>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="wizardHeader">
                <ul>
                    <li class="active"><liferay-ui:message key="choose.target" /><br /><small><liferay-ui:message key="choose.a.component.into.which.current.one.should.be.split" /></small></li>
                    <li><liferay-ui:message key="split.data" /><br /><small><liferay-ui:message key="split.data.from.current.component.to.target.component" /></small></li>
                    <li><liferay-ui:message key="confirm" /><br /><small><liferay-ui:message key="check.the.split.version.and.confirm" /></small></li>
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
    require(['jquery', 'bridges/datatables', 'modules/mergeWizard' ], function($, datatables, wizard) {
        let releaseTooltipFormatter = function(release) {
            if (!release) {
                return '';
            }
            return '<liferay-ui:message key="source.code.download.url" /> - ' + (release.sourceCodeDownloadurl || '') + '\n'
                 + '<liferay-ui:message key="binary.download.url" /> - ' + (release.binaryDownloadurl || '');
        }

        let releaseDetailFormatter = function(release) {
            if (!release) {
                return '';
            }
            return (release.name || '-no-name-') + ' ' + (release.version || '-no-version-');
        }

        let attachmentDetailFormatter = function(attachment) {
            if (!attachment) {
                return '';
            }
            return (attachment.filename || '-no-filename-') + ' (' + (attachment.attachementType || '-no-type-') + ')';
        }

        var mergeWizardStepUrl = '<%=componentSplitWizardStepUrl%>',
            postParamsPrefix = '<portlet:namespace/>',
            $wizardRoot = $('#componentSplitWizard');
            wizard({
                wizardRoot: $wizardRoot,
                postUrl: mergeWizardStepUrl,
                postParamsPrefix: postParamsPrefix,
                loadErrorHook: errorHook,
                steps: [
                    {
                        renderHook: renderChooseComponent,
                        submitHook: submitChosenComponent,
                        errorHook: errorHook
                    },
                    {
                        renderHook: renderSplitComponent,
                        submitHook: submitSplitComponent,
                        errorHook: errorHook
                    },
                    {
                        renderHook: renderConfirmSplitComponent,
                        submitHook: submitConfirmedSplitComponent,
                        errorHook: errorHook
                    }
            ],
            finishCb: function($stepElement, data) {
                if (data && data.error) {
                    let $error = $('<div/>', {
                        'class': 'alert alert-danger mt-3'
                    });
                    let $idList = $('<ul>');

                    $error.append($('<p/>').append($('<b/>').text('<liferay-ui:message key="could.not.merge.components" /> ' + data.error)));
                    $error.append($('<p/>').text('<liferay-ui:message key="this.error.can.lead.to.inconsistencies.in.the.database.please.inform.the.administrator.with.the.following.information" />'));
                    $error.append($('<p>').append($idList));
                    
                    let componentSourceId = $stepElement.data('componentSourceId');
                    $idList.append($('<li>').text('<liferay-ui:message key="source.component" />: ' + componentSourceId));
                    $idList.append($('<li>').text('<liferay-ui:message key="target.component" />: ' + $wizardRoot.data('componentTargetId')));
                    $stepElement.data('componentSelection').releases.forEach( function(release) {
                        if(release.componentId == componentSourceId) {
                            $idList.append($('<li>').text('<liferay-ui:message key="release" />: ' + release.id));
                        }
                    });

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

        function renderChooseComponent($stepElement, data) {
            $stepElement.html('' +
                    '<div class="stepFeedback"></div>' +
                    '<form>' +
                    '    <table id="componentTargetsTable" class="table table-bordered" title="<liferay-ui:message key="source.component" />">' +
                    '        <colgroup>' +
                    '            <col style="width: 1.7rem;" />' +
                    '            <col style="width: 50%;" />' +
                    '            <col style="width: 30%;" />' +
                    '            <col style="width: 15%;" />' +
                    '        </colgroup>' +
                    '        <thead>' +
                    '            <tr>' +
                    '                <th></th>' +
                    '                <th><liferay-ui:message key="component.name" /></th>' +
                    '                <th><liferay-ui:message key="created.by" /></th>' +
                    '                <th><liferay-ui:message key="releases" /></th>' +
                    '            </tr>' +
                    '        </thead>' +
                    '        <tbody>' +
                    '        </tbody>' +
                    '    </table>' +
                    '</form>'
                    );

            var table = datatables.create($stepElement.find('#componentTargetsTable'), {
                data: data.components,
                columns: [
                    { data: "id", render: $.fn.dataTable.render.inputRadio('componentChooser') },
                    { data: "name", render: $.fn.dataTable.render.text()  },
                    { data: "createdBy" },
                    { data: "releases" }
                ],
                language: {
                    url: "<liferay-ui:message key="datatables.lang" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                order: [ [ 1, 'asc' ] ],
                select: 'single'
            }, undefined, [0], true);

            $("#componentTargetsTable").on('init.dt', function() {
                datatables.enableCheckboxForSelection(table, 0);
            });
        }

        function submitChosenComponent($stepElement) {
            var checkedList = $stepElement.find('input:checked');
            if (checkedList.length !== 1 || $(checkedList.get(0)).val() ===  $wizardRoot.data('componentTargetId')) {
                $stepElement.find('.stepFeedback').html('<div class="alert alert-danger"><liferay-ui:message key="please.choose.exactly.one.component.which.is.not.the.component.itself" /></div>');
                $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
                setTimeout(function() {
                    $stepElement.find('.stepFeedback').html('');
                }, 5000);
                return false;
            }
            $stepElement.data('componentTargetId', $(checkedList.get(0)).val());
            $stepElement.data('componentSourceId', $wizardRoot.data('componentSourceId'));
        }

        function renderSplitComponent($stepElement, data) {
            let releases, midElement = '<input class="btn btn-secondary" type="button" value="&Longrightarrow;" />';
            $stepElement.html('<div class="stepFeedback"></div>');
            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="general" />'));
            $stepElement.append(wizard.createSingleSplitLine('<liferay-ui:message key="name" />', data.componentSource.name, data.componentTarget.name));
            $stepElement.append(wizard.createSingleSplitLine('<liferay-ui:message key="created.on" />', data.componentSource.createdOn, data.componentTarget.createdOn));
            $stepElement.append(wizard.createSingleSplitLine('<liferay-ui:message key="created.by" />', data.componentSource.createdBy, data.componentTarget.createdBy));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="releases" />'));
            $stepElement.append(wizard.createMultiSplitLine('Releases', data.componentSource.releases, data.componentTarget.releases, 
                    releaseDetailFormatter, midElement, true, releaseTooltipFormatter));
            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="attachments" />'));
            $stepElement.append(wizard.createMultiSplitLine('<liferay-ui:message key="attachments" />', data.componentSource.attachments, 
                    data.componentTarget.attachments, attachmentDetailFormatter, midElement, false, null));

            $wizardRoot.find('fieldset div.mid input').each(function(index, element) {
                $(element).off('click.merge');
                $(element).on('click.merge', function(event) {
                let propName = $(element).parent().parent().parent().attr('id'),
                    rowIndex = $(element).parent().data('rowIndex'),
                    $fieldset = $('#' + propName),
                    targetNode = $('.right[data-row-index="' + rowIndex + '"]', $fieldset);
                    sourceNode = $('.left[data-row-index="' + rowIndex + '"]', $fieldset);
                    buttonNode = $('.mid[data-row-index="' + rowIndex + '"] input', $fieldset);
                if($(event.currentTarget).hasClass('undo')) {
                    $row.removeClass('modified');
                    buttonNode.removeClass('undo');
                    targetNode.removeData('newVal');
                    targetNode.find('span:first').text($row.data('detailFormatter')(targetNode.data('origVal')));
                    sourceNode.find('span:first').text($row.data('detailFormatter')(sourceNode.data('origVal')));
                    sourceNode.data('newVal', sourceNode.data('origVal'));
                    buttonNode.val($('<div/>').html('&Longrightarrow;').text());
                } else {
                    $row = sourceNode.parent();
                    $row.addClass('modified');
                    buttonNode.val($('<div/>').html('&#8631;').text());
                    buttonNode.addClass('undo');
                    targetNode.data('newVal', sourceNode.data('origVal'));
                    sourceNode.data('newVal', '');
                    targetNode.find('span:first').text($row.data('detailFormatter')(sourceNode.data('origVal')))
                       .attr("title", sourceNode.find('span:first').attr("title"))
                       .attr("data-html", "true");
                    sourceNode.find('span:first').text("");
                }
               });
            });

            $wizardRoot.data('componentSource', data.componentSource);
            $wizardRoot.data('componentTarget', data.componentTarget);
        }

        function submitSplitComponent($stepElement) {
            let srcComponent = {},
                srcReleases = [],
                srcAttachments = [],
                targetComponent = {},
                targetReleases = [],
                targetAttachments = [];
    
            srcComponent.id = $wizardRoot.data('componentSourceId');
            srcComponent.name = wizard.getFinalSingleValue('<liferay-ui:message key="name" />');
            srcComponent.createdOn = wizard.getFinalSingleValue('<liferay-ui:message key="created.on" />');
            srcComponent.createdBy = wizard.getFinalSingleValue('<liferay-ui:message key="created.by" />');
            srcReleases = wizard.getFinalMultiValue('Releases');
            srcComponent.releases = [];
            $.each(srcReleases, function(index, value) {
                srcComponent.releases.push({ "id": value.id , "name": value.name , "version": value.version , "componentId": value.componentId ,
                    "sourceCodeDownloadurl": value.sourceCodeDownloadurl , "binaryDownloadurl": value.binaryDownloadurl});
            });
            srcAttachments = wizard.getFinalMultiValue('<liferay-ui:message key="attachments" />');
            srcComponent.attachments = [];
            $.each(srcAttachments, function(index, value) {
                srcComponent.attachments.push(JSON.parse('{ "attachmentContentId": "' + value.attachmentContentId + '", "filename": "' + value.filename + '"}'));
            });
    
            targetComponent.id = $wizardRoot.data('componentTarget').id;
            targetComponent.name = wizard.getFinalSingleValueTarget('<liferay-ui:message key="name" />');
            targetComponent.createdOn = wizard.getFinalSingleValueTarget('<liferay-ui:message key="created.on" />');
            targetComponent.createdBy = wizard.getFinalSingleValueTarget('<liferay-ui:message key="created.by" />');
            targetReleases = wizard.getFinalMultiValueTarget('Releases');
            targetComponent.releases = [];
            $.each(targetReleases, function(index, value) {
                targetComponent.releases.push({ "id": value.id , "name": value.name , "version": value.version , "componentId": value.componentId ,
                    "sourceCodeDownloadurl": value.sourceCodeDownloadurl , "binaryDownloadurl": value.binaryDownloadurl});
            });
            targetAttachments = wizard.getFinalMultiValueTarget('<liferay-ui:message key="attachments" />');
            targetComponent.attachments = [];
            $.each(targetAttachments, function(index, value) {
                targetComponent.attachments.push(JSON.parse('{ "attachmentContentId": "' + value.attachmentContentId + '", "filename": "' + value.filename + '"}'));
            })

            $stepElement.data('targetComponent', targetComponent);
            $stepElement.data('srcComponent', srcComponent);
        }

        function renderConfirmSplitComponent($stepElement, data) {
            $stepElement.data('targetComponent', data.targetComponent);
            $stepElement.data('srcComponent', data.srcComponent);
            $stepElement.html('<div class="stepFeedback"></div>');
            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="general" />'));
            $stepElement.append(wizard.createSingleSplitLine('<liferay-ui:message key="name" />', data.srcComponent.name, data.targetComponent.name));
            $stepElement.append(wizard.createSingleSplitLine('<liferay-ui:message key="created.on" />', data.srcComponent.createdOn, data.targetComponent.createdOn));
            $stepElement.append(
                    wizard.createSingleSplitLine('<liferay-ui:message key="created.by" />', data.srcComponent.createdBy, data.targetComponent.createdBy),
            );
            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="releases" />'));
            $stepElement.append(wizard.createMultiSplitLine('Releases', data.srcComponent.releases, data.targetComponent.releases,
                    releaseDetailFormatter, '', true, releaseTooltipFormatter));
            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="attachments" />'));
            $stepElement.append(wizard.createMultiSplitLine('<liferay-ui:message key="attachments" />', data.srcComponent.attachments,
                    data.targetComponent.attachments, attachmentDetailFormatter, '', false, null));
        }

        function submitConfirmedSplitComponent($stepElement) {
            /* componentSourceId still as data at stepElement */
            /* componentSelection still as data at stepElement */
        }

        function errorHook($stepElement, textStatus, error) {
            if($stepElement.find('.stepFeedback').length === 0) {
                // initial loading
                $stepElement.html('<div class="stepFeedback"></div>');
            }

            $stepElement.find('.stepFeedback').html('<div class="alert alert-danger"><liferay-ui:message key="an.error.happened.while.communicating.with.the.server" />' + ':'  + textStatus + error + '</div>');
            $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
            setTimeout(function() {
                $stepElement.find('.stepFeedback').html('');
            }, 5000);
        }
    });
</script>