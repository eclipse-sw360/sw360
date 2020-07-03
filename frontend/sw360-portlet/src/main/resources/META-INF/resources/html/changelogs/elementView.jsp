<!--
  ~ Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
-->
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:resourceURL var="changelogslisturl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.LOAD_CHANGE_LOGS%>"/>
    <portlet:param name="<%=PortalConstants.DOCUMENT_ID%>" value="${docid}"/>
</portlet:resourceURL>

<portlet:resourceURL var="changelogsviewurl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_CHANGE_LOGS%>"/>
</portlet:resourceURL>

<div class="tab-content" id="pills-changelogstab">
    <div class="tab-pane fade show active" id="pills-changelogslist"
        role="tabpanel" aria-labelledby="pills-changelogs-list-tab">
        <table id="changeLogsTable" class="table table-bordered"></table>
    </div>
    <div class="tab-pane fade" id="pills-changelogsView" role="tabpanel"
        aria-labelledby="pills-changelogs-view-tab">
        <div id="changelogspinner">
            <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
        </div>
        <div id="cardScreen"></div>
    </div>
</div>

<div id="template" class="d-none">
    <div class="card border-info">
        <div class="card-header cardHeader text-white p-1"
            id="headingOne" data-toggle="collapse"
            data-target="#collapseOne" aria-expanded="true"
            aria-controls="collapseOne">
            <h3 class="mb-0 p-1"></h3>
        </div>

        <div id="collapseOne" class="collapse show"
            aria-labelledby="headingOne" data-parent="#template">
            <div class="p-0 border border-info" id="tableContainer">

            </div>
        </div>

    </div>
</div>
<table id="templateTable" cellspacing="10" class="d-none"
    style="width: 100%;table-layout: fixed;">
    <tr id="templateRow">
        <td
            class="text-danger text-justify align-top w-50 p-3 font-italic oldValue"
            style="background-color: #ffeef0; border: 1px solid red; border-radius: 5px">

        </td>
        <td
            class="text-success text-justify align-top w-50 p-3 font-italic newValue"
            style="background-color: #e6ffed; border: 1px solid green; border-radius: 5px">

        </td>
    </tr>
</table>

<table id="basicInfoTemplateTable" cellspacing="10" class="d-none table"
    style="width: 100%;table-layout: fixed;">
    <tr id="templateRow">
        <td class="text-justify align-top w-50 p-3">
        </td>
        <td class="text-justify align-top w-50 p-3 border-right">
        </td>
        <td class="text-justify align-top w-50 p-3">
        </td>
        <td class="text-justify align-top w-50 p-3">
        </td>
    </tr>
</table>
<script>
    AUI().use('liferay-portlet-url', function () {
        require(['jquery', 'modules/autocomplete', 'modules/dialog', 'modules/validation', 'bridges/datatables', 'utils/render', 'bridges/jquery-ui'], function($, autocomplete, dialog, validation, datatables, render) {
            var changeLogTable;
            var cardsScreen = $("#cardScreen");
            var cardId = 0;
            function getChangeLogsData(changelogId)
            {
                cardsScreen.html("");
                $("#changelogspinner").removeClass("d-none");
                $.ajax({
                    url: '<%=changelogsviewurl%>',
                    type: "GET",
                    data: {"<portlet:namespace/>changelogid":changelogId},
                    success: function(result){
                        createCards(result);
                        $("#changelogspinner").addClass("d-none");
                  }});
                $("#pills-changelogs-view-tab").attr("data-toggle","pill");
                $("#pills-changelogs-view-tab").trigger("click");
            }

            function createCards(result)
            {
                createBasicInfoCard(result);
                createChangesCards(result.changes);
            }

            function createChangesCards(changes)
            {
                if(changes !== null && changes !== undefined && changes.length > 0)
                {
                    for(let i=0;i<changes.length;i++)
                    {
                        if(changes[i].fieldName === "revision"||changes[i].fieldName === "documentState")
                        {
                            continue;
                        }
                        cardId++;
                        let template=$("#template").clone(true,true);
                        template.removeClass("d-none").attr("id","template-"+cardId);

                        template.find("h3").html('<liferay-ui:message key="field.name"/> - <i>'+changes[i].fieldName+"</i>").parent().attr("id","heading-"+cardId)
                        .attr("aria-controls","collapse-"+cardId).attr("data-target","#collapse-"+cardId);
                        changesTable=$("#templateTable").clone(true,true).removeClass("d-none").removeAttr("id");
                        changesTable.find("td:eq(0)").html(getFieldValueInPrettyFormat(changes[i].fieldValueOld,"text-danger"));
                        changesTable.find("td:eq(1)").html(getFieldValueInPrettyFormat(changes[i].fieldValueNew,"text-success"));

                        template.find("#tableContainer").append(changesTable).removeAttr("id").parent().attr("id","collapse-"+cardId)
                        .attr("aria-labelledby","heading-"+cardId).attr("data-parent","#template-"+cardId);;
                        cardsScreen.append(template);
                    }
                }
            }

            function createBasicInfoCard(result)
            {
                cardId++;
                let basicInfo=$("#template").clone(true,true);
                basicInfo.removeClass("d-none").attr("id","template-"+cardId);;

                basicInfo.find("h3").html('<liferay-ui:message key="basic.info"/>').parent().attr("id","heading-"+cardId)
                .attr("aria-controls","collapse-"+cardId).attr("data-target","#collapse-"+cardId);
                let basicInfoTable=$("#basicInfoTemplateTable").clone(true,true).removeClass("d-none").removeAttr("id");
                basicInfoTable.find("td:eq(0)").html('<liferay-ui:message key="user"/> : ');
                basicInfoTable.find("td:eq(1)").html(result.userEdited);
                basicInfoTable.find("td:eq(2)").html('<liferay-ui:message key="document.id"/> : ');
                basicInfoTable.find("td:eq(3)").html(result.documentId);

                basicInfoTableTr1= basicInfoTable.find("tr").clone(true,true);
                basicInfoTableTr1.find("td:eq(0)").html('<liferay-ui:message key="date"/> : ');
                basicInfoTableTr1.find("td:eq(1)").html(result.changeTimestamp);
                basicInfoTableTr1.find("td:eq(2)").html('<liferay-ui:message key="document.type"/> : ');
                basicInfoTableTr1.find("td:eq(3)").html(result.documentType);

                basicInfoTableTr2= basicInfoTable.find("tr").clone(true,true);
                basicInfoTableTr2.find("td:eq(0)").html('<liferay-ui:message key="operation"/> : ');
                basicInfoTableTr2.find("td:eq(1)").html(result.operation);
                let basicInfoTableTr3 = null;
                let secondRowThirdCol = basicInfoTableTr2.find("td:eq(2)");
                let secondRowFourthCol = basicInfoTableTr2.find("td:eq(3)");
                if(isNullOrUndefined(result.referenceDoc)&&isNullOrUndefined(result.info))
                {
                    secondRowThirdCol.addClass("d-none");
                    secondRowFourthCol.addClass("d-none");
                }
                else if(isNullOrUndefined(result.info))
                {
                    secondRowThirdCol.html('<liferay-ui:message key="reference.doc"/> : ');
                    secondRowFourthCol.html(getJsonArrPrettyFormat(result.referenceDoc));
                }
                else if(isNullOrUndefined(result.referenceDoc))
                {
                    secondRowThirdCol.html('<liferay-ui:message key="info"/> : ');
                    secondRowFourthCol.html(getFieldValueInPrettyFormat(result.info,""));
                }
                else
                {
                    secondRowThirdCol.html('<liferay-ui:message key="info"/> : ');
                    secondRowFourthCol.html(getFieldValueInPrettyFormat(result.info,""));
                    basicInfoTableTr3= basicInfoTable.find("tr").clone(true,true);
                    basicInfoTableTr3.find("td:eq(0)").html('<liferay-ui:message key="reference.doc"/> : ');
                    basicInfoTableTr3.find("td:eq(1)").html(getJsonArrPrettyFormat(result.referenceDoc));
                    basicInfoTableTr3.find("td:eq(2)").addClass("d-none");
                    basicInfoTableTr3.find("td:eq(3)").addClass("d-none");
                }

                basicInfoTable.append(basicInfoTableTr1);
                basicInfoTable.append(basicInfoTableTr2);
                if(!isNullOrUndefined(basicInfoTableTr3))
                {
                    basicInfoTable.append(basicInfoTableTr3);
                }

                basicInfo.find("#tableContainer").append(basicInfoTable).removeAttr("id").parent().attr("id","collapse-"+cardId)
                .attr("aria-labelledby","heading-"+cardId).attr("data-parent","#template-"+cardId);
                cardsScreen.append(basicInfo);
            }

            function isNullOrUndefined(obj)
            {
                if(obj === null || obj === undefined)
                {
                    return true;
                }
                return false;
            }

            function getJsonArrPrettyFormat(referenceDoc)
            {
                let jsonStr = JSON.stringify(referenceDoc, undefined, 5);
                if(referenceDoc !== null && referenceDoc !== undefined && referenceDoc.length>0)
                {
                    jsonStr = '<pre style="white-space: pre-wrap;word-break: break-all;">' + jsonStr + '</pre>';
                }
                return jsonStr;
            }

            function getFieldValueInPrettyFormat(fieldValue,textclass)
            {
                let fieldJsonObj = null;
                if(fieldValue !== null && fieldValue !== undefined )
                {
                    if(typeof fieldValue === 'object')
                    {
                        fieldJsonObj = fieldValue;
                    }
                    else
                    {
                        fieldJsonObj = JSON.parse(fieldValue);
                    }
                }
                let jsonStr = JSON.stringify(fieldJsonObj, undefined, 5);
                jsonStr = $($.parseHTML('<pre class="' + textclass + '" style="white-space: pre-wrap;word-break: break-all;"></pre>')).text(jsonStr)[0].outerHTML;
                return jsonStr;
            }

            changeLogTable = createChangeLogTable();

            // create and render datatable
            function createChangeLogTable() {
                var changeLogTable;

                changeLogTable = datatables.create('#changeLogsTable', {
                    bServerSide: true,
                    sAjaxSource: '<%=changelogslisturl%>',

                    columns: [
                        {title: '<liferay-ui:message key="date" />', width: "22%", data: "changeTimestamp"},
                        {title: '<liferay-ui:message key="change.log" /> <liferay-ui:message key="id" />', width: "22%", data: "id"},
                        {title: '<liferay-ui:message key="change.type" />', width: "23%", data: {"documentType":"documentType","documentId":"documentId"}, render: {display: renderChangeType} },
                        {title: '<liferay-ui:message key="user" />', width: "22%", data: "user" },
                        {title: '<liferay-ui:message key="actions" />',"orderable": false,"searchable": false, width: "11%", data: {"id":"id","modReqUrl":"moderationUrl"}, render: {display: renderChangeLogActions} }
                    ],
                    "order": [[ 0, "desc" ]],
                    fnDrawCallback: datatableLoaded
                }, [0, 1, 2, 3, 4], 4);

                return changeLogTable;
            }

            function renderChangeLogActions(data, type, row) {
                var $actions = $('<div style="width: fit-content">', {
                        'class': 'actions'
                    }),
                    $viewAction=$('<a>', {
                        'class': 'view p-1',
                        'data-changelogid': data.id
                    });

                $viewAction.append($('<span class="glyphicon glyphicon-open-file" style="font-size: 20px;" title="<liferay-ui:message key="view.change.logs" />"></span>'));
                $actions.append($viewAction);

                let moderationUrl = data.moderationUrl
                if(moderationUrl !== null && moderationUrl !== undefined)
                {
                    $modAction=$('<a>', {
                        'href': moderationUrl
                    });

                    $modSpan=$('<span>', {
                        'class': 'font-weight-bold text-white bg-secondary rounded',
                        'href': moderationUrl,
                        'style': 'padding: 2px;font-size: initial;',
                        'title': '<liferay-ui:message key="moderation.request" />'
                    });
                    $modSpan.append("M");
                    $modAction.append($modSpan);
                    $actions.append($modAction);
                }
                return $actions[0].outerHTML;
            }

            function renderChangeType(documentData, type, row) {
                let changeType = '<span><liferay-ui:message key="reference.doc"/> <liferay-ui:message key="changes" />  : ' + documentData.documentType + '</span>';
                if(documentData.documentId === '<core_rt:out value = "${docid}"/>')
                {
                    changeType = '<span"><liferay-ui:message key="attribute.changes"/></span>'
                }
                return changeType;
            }

            function datatableLoaded()
            {
                datatables.showPageContainer;
                $(".view").each(function(){
                    $(this).click(function(){
                        getChangeLogsData($(this).data("changelogid"));
                    });
                });
            }
    });
    });
</script>