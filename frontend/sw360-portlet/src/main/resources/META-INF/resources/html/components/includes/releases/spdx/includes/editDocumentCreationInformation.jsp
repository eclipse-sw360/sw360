<core_rt:set var="creatorDataPerson" value="${release.spdx.spdxCreatedBy.person.entrySet()}"/>
<core_rt:set var="creatorDataOrganization" value="${release.spdx.spdxCreatedBy.organization.entrySet()}"/>
<core_rt:set var="creatorDataTool" value="${release.spdx.spdxCreatedBy.tool.entrySet()}"/>

<table class="table three-columns" id="editDocumentCreationInformation">
    <thead>
        <tr>
            <th colspan="3">
                <liferay-ui:message key="document.creation.information" />
            </th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="spdxVersion">
                        <liferay-ui:message key="spdx.version" />
                    </label>
                    <div style="display: flex">
                        <label style="font-weight: 400; margin-top: 0.5rem; font-size: 16px">SPDX-</label>
                        <input style="width: auto;" id="spdxVersion" class="form-control needs-validation"
                            rule="regex:^[0-9]+\.[0-9]+$"
                            name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.SPDX_VERSION%>"
                            type="text" class="form-control"
                            placeholder="<liferay-ui:message key="enter.spdx.version" />"
                            value="<sw360:out value="${release.spdx.spdxVersion}" />" />
                    </div>
                    <div id="spdxVersion-error-messages">
                        <div class="invalid-feedback" rule="regex">
                            <liferay-ui:message key="formatting.must.be.SPDX-M.N" />
                        </div>
                    </div>
                </div>
            </td>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="dataLicense">
                        <liferay-ui:message key="data.license" />
                    </label>
                    <input id="dataLicense" class="form-control needs-validation"
                        rule="regex:^[0-9a-zA-Z.-]+$"
                        name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.SPDX_DATA_LICENSE%>"
                        type="text" class="form-control col-8"
                        placeholder="<liferay-ui:message key="enter.data.license" />"
                        value="<sw360:out value="${release.spdx.spdxDataLicense}" />" />
                </div>
                <div id="dataLicense-error-messages">
                    <div class="invalid-feedback" rule="regex">
                        <liferay-ui:message key="string.containing.letters.numbers.and/or.-" />
                    </div>
                </div>
            </td>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="spdxIdentifier">
                        <liferay-ui:message key="spdx.identifier" />
                    </label>
                    <div style="display: flex">
                        <label style="font-weight: 400; margin-top: 0.5rem; font-size: 16px">SPDXRef-</label>
                        <input style="width: auto;" id="spdxIdentifier" class="form-control needs-validation"
                            rule="regex:^[0-9a-zA-Z.-]+$"
                            name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.SPDX_IDENTIFIER%>"
                            type="text" class="form-control col-8"
                            placeholder="<liferay-ui:message key="enter.spdx.identifier" />"
                            value="<sw360:out value="${release.spdx.spdxIdentifier}" />" />
                    </div>
                </div>
                <div id="spdxIdentifier-error-messages">
                    <div class="invalid-feedback" rule="regex">
                        <liferay-ui:message key="formatting.must.be.SPDXRef.document" />
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="spdxDocumentName">
                        <liferay-ui:message key="document.name" />
                    </label>
                    <input id="spdxDocumentName"
                        name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.SPDX_NAME%>"
                        type="text"
                        class="form-control"
                        placeholder="<liferay-ui:message key="enter.spdx.document.name" />"
                        value="<sw360:out value="${release.spdx.spdxName}" />" />
                </div>
            </td>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="spdxDocumentNamespace">
                        <liferay-ui:message key="spdx.document.namespace" />
                    </label>
                    <input id="spdxDocumentNamespace" class="form-control needs-validation"
                        rule="isUrl"
                        name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.SPDX_NAME_SPACE%>"
                        type="text" class="form-control"
                        placeholder="<liferay-ui:message key="enter.spdx.document.namespace" />"
                        value="<sw360:out value="${release.spdx.spdxNameSpace}" />" />
                </div>
                <div id="spdxDocumentNamespace-error-messages">
                    <div class="invalid-feedback" rule="isUrl">
                        <liferay-ui:message key="formatting.must.be.an.URI" />
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td  id="spdxCreator" style="display: flex; flex-direction: column; border-top-width: 0px;">
                <label class="mandatory" for="spdxCreatorType">
                    <liferay-ui:message key="creator" />
                </label>
                <div style="display: flex; flex-direction: row; margin-bottom: 12px;">
                    <label style="font-weight: 400; margin-top: 0.5rem; font-size: 16px; flex: 0 0 7rem;">
                        <liferay-ui:message key="anonymous" />
                    </label>
                    <input style="margin-left: 1rem !important; margin-top: 12px; width: 16px; height: 16px;"
                        id="spdxCreatorType-Anonymous" type="checkbox"
                        name="<portlet:namespace/><%=SPDX._Fields.SPDX_CREATED_BY%><%=SpdxCreatedBy._Fields.IS_ANONYMOUS%>"
                        value="true"
                        onclick="setCreatorInput();"
                        <core_rt:if test="${release.spdx.spdxCreatedBy.isAnonymous}"> checked="checked" </core_rt:if> />
                </div>
                <div class="actions" name="spdxCreatorType-Person" style="display: flex; flex-direction: row; margin-bottom: 12px;">
                    <label style="font-weight: 400; margin-top: 0.5rem; font-size: 16px; flex: 0 0 7rem;"><liferay-ui:message key="person" /></label>
                    <input style="margin-left: 1rem !important;"
                        type="text" class="form-control"
                        name="<portlet:namespace/><%=PortalConstants.SPDX_PERSON_NAME%>"
                        placeholder="<liferay-ui:message key="enter.name.of.person" />"
                        value="" />
                    <input style="margin-left: 1rem !important;"
                        type="text" class="form-control medium-textbox"
                        name="<portlet:namespace/><%=PortalConstants.SPDX_PERSON_EMAIL%>"
                        placeholder="<liferay-ui:message key="enter.email.of.person" />"
                        value="" />
                    <svg class="disabled lexicon-icon" name="delete-spdxCreatorType-Person" data-row-id="" style="margin-left: 0.5rem; margin-top: 1em; width: 80px" onclick="removeRow(this);">
                        <title><liferay-ui:message key="delete" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                    </svg>
                </div>
                <div class="actions">
                    <svg class="action lexicon-icon" name="add-spdxCreatorType-Person" style="margin-left: 0.5rem; margin-top: 1em; width: 80px" onclick="addRow('spdxCreatorType-Person', '', '', '');">
                        <title><liferay-ui:message key="add" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#add-row"/>
                    </svg>
                </div>
                <div class="actions" name="spdxCreatorType-Organization" style="display: flex; flex-direction: row; margin-bottom: 12px;">
                    <label style="font-weight: 400; margin-top: 0.5rem; font-size: 16px; flex: 0 0 7rem;">
                        <liferay-ui:message key="organization" />
                    </label>
                    <input style="margin-left: 1rem !important;"
                        type="text" class="form-control"
                        name="<portlet:namespace/><%=PortalConstants.SPDX_ORGANIZATION_NAME%>"
                        placeholder="<liferay-ui:message key="enter.name.of.organisation" />"
                        value="" />
                    <input style="margin-left: 1rem !important;"
                        type="text" class="form-control medium-textbox"
                        name="<portlet:namespace/><%=PortalConstants.SPDX_ORGANIZATION_EMAIL%>"
                        placeholder="<liferay-ui:message key="enter.email.of.organisation" />"
                        value="" />
                    <svg class="disabled lexicon-icon" name="delete-spdxCreatorType-Organization" data-row-id="" style="margin-left: 0.5rem; margin-top: 1em; width: 80px" onclick="removeRow(this);">
                        <title><liferay-ui:message key="delete" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                    </svg>
                </div>
                <div class="actions">
                    <svg class="action lexicon-icon" name="add-spdxCreatorType-Organization" data-row-id="" style="margin-left: 0.5rem; margin-top: 1em; width: 80px" onclick="addRow('spdxCreatorType-Organization', '', '', '');">
                        <title><liferay-ui:message key="add" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#add-row"/>
                    </svg>
                </div>
                <div class="actions" name="spdxCreatorType-Tool" style="display: flex; flex-direction: row; margin-bottom: 12px;">
                    <label style="font-weight: 400; margin-top: 0.5rem; font-size: 16px; flex: 0 0 7rem;">
                        <liferay-ui:message key="tool" />
                    </label>
                    <input style="margin-left: 1rem !important;"
                        type="text" class="form-control"
                        name="<portlet:namespace/><%=PortalConstants.SPDX_TOOL_NAME%>"
                        placeholder="<liferay-ui:message key="enter.name.of.tool" />"
                        value="" />
                    <input style="margin-left: 1rem !important;"
                        type="text" class="form-control medium-textbox"
                        name="<portlet:namespace/><%=PortalConstants.SPDX_TOOL_VERSION%>"
                        placeholder="<liferay-ui:message key="enter.version.of.tool" />"
                        value="" \>
                    <svg class="disabled lexicon-icon" name="delete-spdxCreatorType-Tool" data-row-id="" style="margin-left: 0.5rem; margin-top: 1em; width: 80px" onclick="removeRow(this);">
                        <title><liferay-ui:message key="delete" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                    </svg>
                </div>
                <div class="actions">
                    <svg class="action lexicon-icon" name="add-spdxCreatorType-Tool" data-row-id="" style="margin-left: 0.5rem; margin-top: 1em; width: 80px" onclick="addRow('spdxCreatorType-Tool', '', '', '');">
                        <title><liferay-ui:message key="add" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#add-row"/>
                    </svg>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column; border-top-width: 0px;">
                <label class="mandatory" for="created_date">
                    <liferay-ui:message key="created" />
                </label>
                <div style="display: flex; flex-direction: row; margin-bottom: 12px;">
                    <input id="created_date" style="width: 12rem; text-align: center;"
                        type="date" class="form-control"
                        name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.SPDX_CREATED_DATE%>"
                        placeholder="<liferay-ui:message key=" creation.date.yyyy.mm.dd" />"
                        value="<sw360:out value="${release.spdx.spdxCreatedDate}" />" />
                    <input id="created_time" style="width: 12rem; text-align: center; margin-left: 10px;"
                        type="time" step="1"   class="form-control"
                        name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.SPDX_CREATED_TIME%>"
                        placeholder="<liferay-ui:message key=" creation.time.hh.mm.ss" />"
                        value="<sw360:out value="${release.spdx.spdxCreatedTime}" />" />
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>

    generateCreatorTable();
    setCreatorInput();
    function setCreatorInput() {
        if (document.getElementById('spdxCreatorType-Anonymous').checked == true) {
            $('#spdxCreator > div[name="spdxCreatorType-Person"] > input').prop('disabled', true);
            $('#spdxCreator > div[name="spdxCreatorType-Organization"] > input').prop('disabled', true);
            disableAction('add-spdxCreatorType-Person');
            disableAction('add-spdxCreatorType-Organization');
            disableAction('delete-spdxCreatorType-Person');
            disableAction('delete-spdxCreatorType-Organization');
        } else {
            $('#spdxCreator > div[name="spdxCreatorType-Person"] > input').prop('disabled', false);
            $('#spdxCreator > div[name="spdxCreatorType-Organization"] > input').prop('disabled', false);
            enableAction('delete-spdxCreatorType-Person');
            enableAction('delete-spdxCreatorType-Organization');
            enableAction('add-spdxCreatorType-Person');
            enableAction('add-spdxCreatorType-Organization');
        }
    }

    autoHideString('spdxVersion', 'SPDX-');
    function autoHideString(id, string, value) {
        if ((value == null || value == '')) {
            value = document.getElementById(id).value;
        }
        var newString = value.replace(string, '');
        document.getElementById(id).value = newString;
    }

    function generateCreatorTable() {
        <core_rt:if test="${not creatorDataPerson.isEmpty()}">
            <core_rt:forEach items="${creatorDataPerson}" var="tableEntry" varStatus="loop">
                addRow("spdxCreatorType-Person", "${tableEntry.key}", "${tableEntry.value}", "");
            </core_rt:forEach>
            removeRow(document.getElementsByName('delete-spdxCreatorType-Person')[0]);
        </core_rt:if>

        <core_rt:if test="${not creatorDataOrganization.isEmpty()}">
            <core_rt:forEach items="${creatorDataOrganization}" var="tableEntry" varStatus="loop">
                addRow("spdxCreatorType-Organization", "${tableEntry.key}", "${tableEntry.value}", "");
            </core_rt:forEach>
            removeRow(document.getElementsByName('delete-spdxCreatorType-Organization')[0]);
        </core_rt:if>


        <core_rt:if test="${not creatorDataTool.isEmpty()}">
            <core_rt:forEach items="${creatorDataTool}" var="tableEntry" varStatus="loop">
                addRow("spdxCreatorType-Tool", "${tableEntry.key}", "${tableEntry.value}", "");
            </core_rt:forEach>
            removeRow(document.getElementsByName('delete-spdxCreatorType-Tool')[0]);
        </core_rt:if>

    }

    function addRow(name, value1, value2, lable) {
        if ($(document.getElementsByName('add-' + name)).hasClass('disabled')) {
            return;
        }
        var size = document.getElementsByName(name).length;
        var el = document.getElementsByName(name)[size - 1];

        var clone = el.cloneNode(true);
        clone.getElementsByTagName('input')[0].name = clone.getElementsByTagName('input')[0].name + Date.now();
        clone.getElementsByTagName('input')[1].name = clone.getElementsByTagName('input')[1].name + Date.now();
        clone.getElementsByTagName('input')[0].value = value1;
        clone.getElementsByTagName('input')[1].value = value2;
        clone.getElementsByTagName('label')[0].innerHTML = '<liferay-ui:message key="' + lable +'" />';
        $(clone).insertAfter(el);
        if (size == 1) {
            enableAction('delete-' + name);
        }
    }

    function removeRow(el) {
        if ($(el).hasClass('disabled')) {
            return;
        }
        var parent = $(el).parent();
        var label = $(parent)[0].getElementsByTagName('label')[0].innerHTML;
        var name = $(parent)[0].getAttribute("name");
        $(parent).remove();
        if (label) {
            document.getElementsByName(name)[0].getElementsByTagName('label')[0].innerHTML = label;
        }
        if (document.getElementsByName(name).length < 2) {
            disableAction('delete-' + name);
        }
    }

    function disableAction(name) {
        var el = document.getElementsByName(name);
        $(el).removeClass('action');
        $(el).addClass('disabled');
    }

    function enableAction(name) {
        var el = document.getElementsByName(name);
        var size = $(el).length;
        if (size > 1 || name.includes("add")) {
            $(el).removeClass('disabled');
            $(el).addClass('action');
        }
    }

</script>