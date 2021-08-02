<table class="table three-columns" id="editOtherLicensingInformationDetected">
<thead>
    <tr>
        <th colspan="3"><liferay-ui:message key="other.licensing.information.detected" /></th>
    </tr>
</thead>
<tbody>
<tr>
    <td>
        <div class="form-group">
            <label for="spdxLicenseIdentifier"><liferay-ui:message key="license.identifier" /></label>
            <div style="display: flex">
                <label style="font-weight: 400; margin-top: 0.5rem; font-size: 16px">LicenseRef-</label>
                <input style="margin-left: 4px; width: 30rem;" id="spdxLicenseIdentifier"
                    class="form-control needs-validation"
                    rule="regex:^[0-9a-zA-Z-.]+$"
                    type="text"
                    name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.OTHER_LICENSE_ID%>"
                    placeholder="<liferay-ui:message key="enter.license.identifier" />"
                    value="<sw360:out value="${release.spdx.otherLicenseId}"/>"/>
            </div>
            <div id="spdxLicenseIdentifier-error-messages">
                <div class="invalid-feedback" rule="regex">
                    <liferay-ui:message key="string.containing.letters.numbers.and/or.-" />
                </div>
            </div>
        </div>
    </td>
</tr>
<tr>
    <td>
        <div class="form-group">
            <label for="spdxExtractedText"><liferay-ui:message key="extracted.text" /></label>
            <textarea class="form-control" id="spdxExtractedText" rows="4"
                name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.OTHER_LICENSE_TEXT%>"
                placeholder="<liferay-ui:message key="enter.extracted.text" />"><sw360:out value="${release.spdx.otherLicenseText}"/></textarea>
        </div>
    </td>
</tr>
<tr>
    <td style="display: flex; flex-direction: column;">
        <label><liferay-ui:message key="license.name" /></label>
        <div style="display: flex; flex-direction: row; width: 50rem;">
            <div style="display: inline-flex;">
                <input style="margin-top: 12px;" id="spdxLicenseNameExist"
                    type="radio"
                    name="<portlet:namespace/><%=SPDX._Fields.OTHER_LICENSE_NAME%><%=OtherLicenseName._Fields.TYPE%>"
                    value="exist"
                    onchange="setInputValue('spdxLicenseName', value);"
                />
                <input style="margin-left: 4px; width: 30rem;" id="spdxLicenseName"
                    class="form-control" type="text"
                    name="<portlet:namespace/><%=SPDX._Fields.OTHER_LICENSE_NAME%><%=OtherLicenseName._Fields.VALUE%>"
                    placeholder="<liferay-ui:message key="enter.license.name" />"
                    value="<sw360:out value="${release.spdx.otherLicenseName.value}" />" />
            </div>
            <div style="margin-left: 2rem;">
                <input style="margin-top: 12px;" id="spdxLicenseNameNoassertion"
                type="radio"
                name="<portlet:namespace/><%=SPDX._Fields.OTHER_LICENSE_NAME%><%=OtherLicenseName._Fields.TYPE%>"
                value="noassertion"
                onchange="setInputValue('spdxLicenseName', value);" />
                <label class="form-check-label radio-label" for="spdxLicenseNameNoassertion"><liferay-ui:message key="noassertion" /></label>
            </div>
        </div>
    </td>
</tr>
<tr>
    <td>
        <div class="form-group">
            <label for="spdxLicenseComment"><liferay-ui:message key="license.comment" /></label>
            <textarea class="form-control" id="spdxLicenseComment" rows="4"
                name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.OTHER_LICENSE_COMMENT%>"
                placeholder="<liferay-ui:message key="enter.license.comment" />"><sw360:out value="${release.spdx.otherLicenseComment}"/></textarea>
        </div>
    </td>
</tr>
</tbody>
</table>

<script>
    setInputValue('spdxLicenseName', "${release.spdx.otherLicenseName.type}");
</script>