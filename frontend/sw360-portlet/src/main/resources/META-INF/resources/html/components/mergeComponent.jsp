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

<portlet:actionURL var="componentMergeWizardStepUrl" name="componentMergeWizardStep"/>

<div id="componentMergeWizard" class="container" data-step-id="0" data-component-target-id="${component.id}">
    <div class="row portlet-toolbar">
        <div class="col portlet-title text-truncate" title="<liferay-ui:message key="merge.into" />${sw360:printComponentName(component)}">
            <liferay-ui:message key="merge.into" />  <sw360:out value="${sw360:printComponentName(component)}"/>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="wizardHeader">
                <ul>
                    <li class="active"><liferay-ui:message key="choose.source" /><br /><small><liferay-ui:message key="choose.a.component.that.should.be.merged.into.the.current.one" /></small></li>
                    <li><liferay-ui:message key="merge.data" /><br /><small><liferay-ui:message key="merge.data.from.source.into.target.component" /></small></li>
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
    require(['jquery', 'bridges/datatables', 'modules/mergeWizard' ], function($, datatables, wizard) {
        var mergeWizardStepUrl = '<%=componentMergeWizardStepUrl%>',
            postParamsPrefix = '<portlet:namespace/>',
            $wizardRoot = $('#componentMergeWizard');

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
                    renderHook: renderMergeComponent,
                    submitHook: submitMergedComponent,
                    errorHook: errorHook
                },
                {
                    renderHook: renderConfirmMergedComponent,
                    submitHook: submitConfirmedMergedComponent,
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
                    '    <table id="componentSourcesTable" class="table table-bordered" title="<liferay-ui:message key="source.component" />">' +
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

            var table = datatables.create($stepElement.find('#componentSourcesTable'), {
                data: data.components,
                columns: [
                    { data: "id", render: $.fn.dataTable.render.inputRadio('componentChooser') },
                    { data: "name", render: $.fn.dataTable.render.text() },
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

            $("#componentSourcesTable").on('init.dt', function() {
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
            $stepElement.data('componentTargetId', $wizardRoot.data('componentTargetId'));
            $stepElement.data('componentSourceId', $(checkedList.get(0)).val());
        }

        function renderMergeComponent($stepElement, data) {
            var releases,
                releaseMap;

            $stepElement.html('<div class="stepFeedback"></div>');
            $stepElement.data('componentSourceId', data.componentSource.id);
            $stepElement.data('releaseCount', (data.componentTarget.releases.length || 0) + (data.componentSource.releases.length || 0));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="general" />'));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="name" />', data.componentTarget.name, data.componentSource.name));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="created.on" />', data.componentTarget.createdOn, data.componentSource.createdOn));
            $stepElement.append(
                renderCreatedBy(
                    wizard.createSingleMergeLine('<liferay-ui:message key="created.by" />', data.componentTarget.createdBy, data.componentSource.createdBy),
                    data.componentSource.createdBy != data.componentTarget.createdBy,
                    data.componentSource.createdBy,
                    'text-center'
                )
            );
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="categories" />', data.componentTarget.categories, data.componentSource.categories));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="component.type" />', data.componentTarget.componentType, data.componentSource.componentType, getComponentTypeDisplayString));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="default.vendor" />', data.componentTarget.defaultVendor, data.componentSource.defaultVendor, getDefaultVendorDisplayString));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="homepage" />', data.componentTarget.homepage, data.componentSource.homepage));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="blog" />', data.componentTarget.blog, data.componentSource.blog));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="wiki" />', data.componentTarget.wiki, data.componentSource.wiki));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="mailing.list" />', data.componentTarget.mailinglist, data.componentSource.mailinglist));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="description" />', data.componentTarget.description, data.componentSource.description));
            $stepElement.append(wizard.createMapMergeLine('<liferay-ui:message key="external.ids" />', data.componentTarget.externalIds, data.componentSource.externalIds));
            $stepElement.append(wizard.createMapMergeLine('<liferay-ui:message key="additional.data" />', data.componentTarget.additionalData, data.componentSource.additionalData));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="roles" />'));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="component.owner" />', data.componentTarget.componentOwner, data.componentSource.componentOwner));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="owner.accounting.unit" />', data.componentTarget.ownerAccountingUnit, data.componentSource.ownerAccountingUnit));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="owner.billing.group" />', data.componentTarget.ownerGroup, data.componentSource.ownerGroup));
            $stepElement.append(wizard.createSingleMergeLine('<liferay-ui:message key="owner.country" />', data.componentTarget.ownerCountry, data.componentSource.ownerCountry));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="moderators" />', data.componentTarget.moderators, data.componentSource.moderators));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="subscribers" />', data.componentTarget.subscribers, data.componentSource.subscribers));
            $stepElement.append(wizard.createMultiMapMergeLine('<liferay-ui:message key="additional.roles" />', data.componentTarget.roles, data.componentSource.roles));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="releases" />'));
            releases = wizard.createMultiMergeLine('Releases', data.componentTarget.releases, data.componentSource.releases, function(release) {
                if (!release) {
                    return '';
                }
                return (release.name || '-no-name-') + ' ' + (release.version || '-no-version-');
            });
            $stepElement.append(releases);

            releaseMap = {};
            data.componentSource.releases.forEach(function(release) {
                releaseMap[release.id] = release;
            });
            releases.find('.merge.single.right').each(function(index, element) {
                var $row = $(element);
                if(releaseMap[$row.data().origVal.id]) {
                    wizard.mergeByDefault('Releases', index);
                }
                wizard.lockRow('<liferay-ui:message key="releases" />', index, true);
            });

            $stepElement.append(wizard.createCategoryLine(''));
            $stepElement.append(wizard.createMultiMergeLine('<liferay-ui:message key="attachments" />', data.componentTarget.attachments, data.componentSource.attachments, function(attachment) {
                if (!attachment) {
                    return '';
                }
                return (attachment.filename || '-no-filename-') + ' (' + (attachment.attachementType || '-no-type-') + ')';
            }));

            wizard.registerClickHandlers({
                'Createdby': true
            }, function(propName, copied, targetValue, sourceValue) {
                $stepElement.find('.merge-info-createdby .user').text(copied ? targetValue : sourceValue);
            });

            $wizardRoot.data('componentSource', data.componentSource);
            $wizardRoot.data('componentTarget', data.componentTarget);
        }

        function submitMergedComponent($stepElement) {
            var componentSelection = {},
                releases = [],
                attachments = [];

            componentSelection.id = $wizardRoot.data('componentTargetId');

            componentSelection.name = wizard.getFinalSingleValue('<liferay-ui:message key="name" />');
            componentSelection.createdOn = wizard.getFinalSingleValue('<liferay-ui:message key="created.on" />');
            componentSelection.createdBy = wizard.getFinalSingleValue('<liferay-ui:message key="created.by" />');
            componentSelection.categories = wizard.getFinalMultiValue('<liferay-ui:message key="categories" />');
            componentSelection.componentType = wizard.getFinalSingleValue('<liferay-ui:message key="component.type" />');
            componentSelection.defaultVendor = wizard.getFinalSingleValue('<liferay-ui:message key="default.vendor" />');
            componentSelection.defaultVendorId = componentSelection.defaultVendor ? componentSelection.defaultVendor.id : undefined;
            componentSelection.homepage = wizard.getFinalSingleValue('<liferay-ui:message key="homepage" />');
            componentSelection.blog = wizard.getFinalSingleValue('<liferay-ui:message key="blog" />');
            componentSelection.wiki = wizard.getFinalSingleValue('<liferay-ui:message key="wiki" />');
            componentSelection.mailinglist = wizard.getFinalSingleValue('<liferay-ui:message key="mailing.list" />');
            componentSelection.description = wizard.getFinalSingleValue('<liferay-ui:message key="description" />');
            componentSelection.externalIds = wizard.getFinalMapValue('<liferay-ui:message key="external.ids" />');
            componentSelection.additionalData = wizard.getFinalMapValue('<liferay-ui:message key="additional.data" />');

            componentSelection.componentOwner = wizard.getFinalSingleValue('<liferay-ui:message key="component.owner" />');
            componentSelection.ownerAccountingUnit = wizard.getFinalSingleValue('<liferay-ui:message key="owner.accounting.unit" />');
            componentSelection.ownerGroup = wizard.getFinalSingleValue('<liferay-ui:message key="owner.billing.group" />');
            componentSelection.ownerCountry = wizard.getFinalSingleValue('<liferay-ui:message key="owner.country" />');
            componentSelection.moderators = wizard.getFinalMultiValue('<liferay-ui:message key="moderators" />');
            componentSelection.subscribers = wizard.getFinalMultiValue('<liferay-ui:message key="subscribers" />');
            componentSelection.roles = wizard.getFinalMultiMapValue('<liferay-ui:message key="additional.roles" />');

            releases = wizard.getFinalMultiValue('Releases');
            componentSelection.releases = [];
            $.each(releases, function(index, value) {
                /* add just required fields for easy identification */
                componentSelection.releases.push({ "id": value.id , "name": value.name , "version": value.version , "componentId": value.componentId });
            });

            if ((componentSelection.releases.length || 0) < $stepElement.data('releaseCount')) {
                $stepElement.find('.stepFeedback').html('<div class="alert alert-danger"><liferay-ui:message key="please.migrate.all.releases.and.keep.the.existing.ones" /></div>');
                $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
                setTimeout(function() {
                    $stepElement.find('.stepFeedback').html('');
                }, 5000);
                return false;
            }

            attachments = wizard.getFinalMultiValue('<liferay-ui:message key="attachments" />');
            componentSelection.attachments = [];
            $.each(attachments, function(index, value) {
                /* add just required fields for easy identification */
                componentSelection.attachments.push(JSON.parse('{ "attachmentContentId": "' + value.attachmentContentId + '", "filename": "' + value.filename + '"}'));
            })

            $stepElement.data('componentSelection', componentSelection);
            /* componentSourceId still as data at stepElement */
        }

        function renderConfirmMergedComponent($stepElement, data) {
            var componentSource = $wizardRoot.data('componentSource'),
                componentTarget = $wizardRoot.data('componentTarget');

            $stepElement.data('componentSourceId', data.componentSourceId);
            $stepElement.data('componentSelection', data.componentSelection);

            $stepElement.html('<div class="stepFeedback"></div>');

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="general" />'));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="name" />', data.componentSelection.name));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="created.on" />', data.componentSelection.createdOn));
            $stepElement.append(
                renderCreatedBy(
                    wizard.createSingleDisplayLine('<liferay-ui:message key="created.by" />', data.componentSelection.createdBy),
                    componentSource.createdBy != componentTarget.createdBy,
                    data.componentSelection.createdBy === componentSource.createdBy ? componentTarget.createdBy : componentSource.createdBy,
                    'pl-3'
                )
            );
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="categories" />', data.componentSelection.categories));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="component.type" />', data.componentSelection.componentType, getComponentTypeDisplayString));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="default.vendor" />', data.componentSelection.defaultVendor, getDefaultVendorDisplayString));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="homepage" />', data.componentSelection.homepage));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="blog" />', data.componentSelection.blog));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="wiki" />', data.componentSelection.wiki));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="mailing.list" />', data.componentSelection.mailinglist));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="description" />', data.componentSelection.description));
            $stepElement.append(wizard.createMapDisplayLine('<liferay-ui:message key="external.ids" />', data.componentSelection.externalIds));
            $stepElement.append(wizard.createMapDisplayLine('<liferay-ui:message key="additional.data" />', data.componentSelection.additionalData));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="roles" />'));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="component.owner" />', data.componentSelection.componentOwner));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="owner.accounting.unit" />', data.componentSelection.ownerAccountingUnit));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="owner.billing.group" />', data.componentSelection.ownerGroup));
            $stepElement.append(wizard.createSingleDisplayLine('<liferay-ui:message key="owner.country" />', data.componentSelection.ownerCountry));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="moderators" />', data.componentSelection.moderators));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="subscribers" />', data.componentSelection.subscribers));
            $stepElement.append(wizard.createMultiMapDisplayLine('<liferay-ui:message key="additional.roles" />', data.componentSelection.roles));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="releases" />'));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="releases" />', data.componentSelection.releases, function(release) {
                if (!release) {
                    return '';
                }
                return (release.name || '-no-name-') + ' ' + (release.version || '-no-version-');
            }));

            $stepElement.append(wizard.createCategoryLine('<liferay-ui:message key="attachments" />'));
            $stepElement.append(wizard.createMultiDisplayLine('<liferay-ui:message key="attachments" />', data.componentSelection.attachments, function(attachment) {
                if (!attachment) {
                    return '';
                }
                return (attachment.filename || '-no-filename-') + ' (' + (attachment.attachmentType || '-no-type-') + ')';
            }));
        }

        function renderCreatedBy($line, renderInfo, user, alignment) {
            var $info = "<small class='merge-info-createdby form-text mt-0 pb-2 " + alignment + "'>" + 
                "<svg class='lexicon-icon'><use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open' /></svg> " + 
                <liferay-ui:message key="the.user.x.will.be.added.to.the.list.of.moderators" /> +
                "</small>";

            if(renderInfo) {
                $line.append($info);
            }

            return $line;
        }

        function submitConfirmedMergedComponent($stepElement) {
            /* componentSourceId still as data at stepElement */
            /* componentSelection still as data at stepElement */
        }

        function errorHook($stepElement, textStatus, error) {
            if($stepElement.find('.stepFeedback').length === 0) {
                // initial loading
                $stepElement.html('<div class="stepFeedback"></div>');
            }

            $stepElement.find('.stepFeedback').html('<div class="alert alert-danger"><liferay-ui:message key="an.error.happened.while.communicating.with.the.server" />' + textStatus + error + '</div>');
            $('html, body').stop().animate({ scrollTop: 0 }, 300, 'swing');
            setTimeout(function() {
                $stepElement.find('.stepFeedback').html('');
            }, 5000);
        }

        var componentTypeDisplayStrings = { '': '' };
        <core_rt:forEach items="<%=ComponentType.values()%>" var="ct">
            componentTypeDisplayStrings[${ct.value}] = '${sw360:enumToString(ct)}';
        </core_rt:forEach>

        function getComponentTypeDisplayString(componentType) {
            return componentTypeDisplayStrings[componentType];
        }

        function getDefaultVendorDisplayString(defaultVendor) {
            return defaultVendor ? defaultVendor.fullname : '';
        }
    });
</script>