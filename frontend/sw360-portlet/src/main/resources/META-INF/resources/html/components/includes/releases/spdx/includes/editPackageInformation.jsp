<table class="table three-columns" id="editPackageInformation">
    <thead>
        <tr>
            <th colspan="3"><liferay-ui:message key="package.information" /></th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="spdxPackageName"><liferay-ui:message key="package.name" /></label>
                    <input id="spdxPackageName"
                    type="text" class="form-control"
                    name="<portlet:namespace/><%=Release._Fields.SPDX%><%=Release._Fields.NAME%>"
                    value="<sw360:out value="${component.name}"/>" />
                </div>
            </td>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="packageSpdxIdentifier"><liferay-ui:message key="package.spdx.identifier" /></label>
                    <div style="display: flex">
                        <label style="font-weight: 400; margin-top: 0.5rem; font-size: 16px">SPDXRef-</label>
                        <input id="packageSpdxIdentifier"
                            class="form-control needs-validation"
                            rule="regex:^[0-9a-zA-Z.-]+$"
                            type="text"
                            name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.PACKAGE_SPDX_ID%>"
                            placeholder="<liferay-ui:message key="enter.package.spdx.identifier" />"
                            value="<sw360:out value="${release.spdx.packageSpdxId}" />" />
                    </div>
                    <div id="packageSpdxIdentifier-error-messages">
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
                    <label for="spdxPackageVersion"><liferay-ui:message key="package.version" /></label>
                    <input id="spdxPackageVersion"
                        type="text" class="form-control"
                        name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.PACKAGE_VERSION%>"
                        placeholder="<liferay-ui:message key="enter.package.version" />"
                        value="<sw360:out value="${release.spdx.spdxVersion}" />" />
                </div>
            </td>
            <td>
                <div class="form-group">
                    <label for="spdxPackageFileName"><liferay-ui:message key="package.file.name" /></label>
                    <input id="spdxPackageFileName"
                        type="text" class="form-control"
                        name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.PACKAGE_FILENAME%>"
                        placeholder="<liferay-ui:message key="enter.package.file.name" />"
                        value="<sw360:out value="${release.spdx.packageFilename}" />" />
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <label class="mandatory"><liferay-ui:message key="package.download.location" /></label>
                <div style="display: flex; flex-direction: row; width: 50rem;">
                    <div style="display: inline-flex;">
                        <input style="margin-top: 12px;" id="spdxPackageDownloadLocationExist"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.PACKAGE_DOWNLOAD%><%=PackageDownload._Fields.TYPE%>"
                            value="exist"
                            onchange="setInputValue('spdxPackageDownloadLocation', value);" />
                        <input style="margin-left: 4px; width: 30rem;" id="spdxPackageDownloadLocation"
                            class="form-control needs-validation"
                            rule="isDownloadUrl"
                            type="text" required=""
                            name="<portlet:namespace/><%=SPDX._Fields.PACKAGE_DOWNLOAD%><%=PackageDownload._Fields.VALUE%>"
                            placeholder="<liferay-ui:message key="enter.url" />"
                            value="<sw360:out value="${release.spdx.packageDownload.value}" />" />
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxPackageDownloadLocationNone"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.PACKAGE_DOWNLOAD%><%=PackageDownload._Fields.TYPE%>"
                            value="none"
                            onchange="setInputValue('spdxPackageDownloadLocation', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxPackageDownloadLocationNone"><liferay-ui:message key="none" /></label>
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxPackageDownloadLocationNoassertion"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.PACKAGE_DOWNLOAD%><%=PackageDownload._Fields.TYPE%>"
                            value="noassertion"
                            onchange="setInputValue('spdxPackageDownloadLocation', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxPackageDownloadLocationNoassertion"><liferay-ui:message key="noassertion" /></label>
                    </div>
                </div>
                <div id="spdxPackageDownloadLocation-error-messages">
                    <div class="invalid-feedback" rule="isDownloadUrl">
                        <liferay-ui:message key="formatting.must.be.an.download.url" />
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <label for="spdxFileAnalyzedTrue"><liferay-ui:message key="file.analyzed" /></label>
                <div style="display: flex; flex-direction: row; width: 50rem;">
                    <div>
                        <input style="margin-top: 12px;" id="spdxFileAnalyzedTrue"
                            type="radio"
                            name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.FILE_ANALYZED%>"
                            value="true"
                            <core_rt:if test="${release.spdx.fileAnalyzed == 'true'}"> checked="checked" </core_rt:if>
                        />
                        <label class="form-check-label radio-label" for="spdxFileAnalyzedTrue"><liferay-ui:message key="true" /></label>
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxFileAnalyzedFalse"
                            type="radio"
                            name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.FILE_ANALYZED%>"
                            value="false"
                            <core_rt:if test="${release.spdx.fileAnalyzed == 'false'}"> checked="checked" </core_rt:if>
                        />
                        <label class="form-check-label radio-label" for="spdxFileAnalyzedFalse"><liferay-ui:message key="false" /></label>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <label class="mandatory"><liferay-ui:message key="package.homepage" /></label>
                <div style="display: flex; flex-direction: row; width: 50rem;">
                    <div style="display: inline-flex;">
                        <input style="margin-top: 12px;" id="spdxPackageHomepageExist"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.PACKAGE_HOME_PAGE%><%=PackageHomePage._Fields.TYPE%>"
                            value="exist"
                            onchange="setInputValue('spdxPackageHomepage', value);"
                        />
                        <input style="margin-left: 4px; width: 30rem;" id="spdxPackageHomepage"
                            class="form-control needs-validation"
                            rule="isUrl"
                            type="text" required=""
                            name="<portlet:namespace/><%=SPDX._Fields.PACKAGE_HOME_PAGE%><%=PackageHomePage._Fields.VALUE%>"
                            placeholder="<liferay-ui:message key="enter.url" />"
                            value="<sw360:out value="${release.spdx.packageHomePage.value}" />" />
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxPackageHomepageNone"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.PACKAGE_HOME_PAGE%><%=PackageHomePage._Fields.TYPE%>"
                            value="none"
                            onchange="setInputValue('spdxPackageHomepage', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxPackageHomepageNone"><liferay-ui:message key="none" /></label>
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxPackageHomepageNoassertion"
                        type="radio"
                        name="<portlet:namespace/><%=SPDX._Fields.PACKAGE_HOME_PAGE%><%=PackageHomePage._Fields.TYPE%>"
                        value="noassertion"
                        onchange="setInputValue('spdxPackageHomepage', value);" />
                        <label class="form-check-label radio-label" for="spdxPackageHomepageNoassertion"><liferay-ui:message key="noassertion" /></label>
                    </div>
                </div>
                <div id="spdxPackageHomepage-error-messages">
                    <div class="invalid-feedback" rule="isUrl">
                        <liferay-ui:message key="formatting.must.be.an.URI" />
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <label class="mandatory"><liferay-ui:message key="concluded.license" /></label>
                <div style="display: flex; flex-direction: row; width: 50rem;">
                    <div style="display: inline-flex;">
                        <input style="margin-top: 12px;" id="spdxConcludedLicenseExist"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.CONCLUDED_LICENSE%><%=ConcludedLicense._Fields.TYPE%>"
                            value="exist"
                            onchange="setInputValue('spdxConcludedLicense', value);"
                        />
                        <input style="margin-left: 4px; width: 30rem;" id="spdxConcludedLicense"
                            class="form-control needs-validation"
                            rule="regex:^[0-9a-zA-Z-.+]+$"
                            type="text" required=""
                            name="<portlet:namespace/><%=SPDX._Fields.CONCLUDED_LICENSE%><%=ConcludedLicense._Fields.VALUE%>"
                            placeholder="<liferay-ui:message key="enter.concluded.license" />"
                            value="<sw360:out value="${release.spdx.concludedLicense.value}" />" />
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxConcludedLicenseNone"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.CONCLUDED_LICENSE%><%=ConcludedLicense._Fields.TYPE%>"
                            value="none"
                            onchange="setInputValue('spdxConcludedLicense', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxConcludedLicenseNone"><liferay-ui:message key="none" /></label>
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxConcludedLicenseNoassertion"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.CONCLUDED_LICENSE%><%=ConcludedLicense._Fields.TYPE%>"
                            value="noassertion"
                            onchange="setInputValue('spdxConcludedLicense', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxConcludedLicenseNoassertion"><liferay-ui:message key="noassertion" /></label>
                    </div>
                </div>
                <div id="spdxConcludedLicense-error-messages">
                    <div class="invalid-feedback" rule="regex">
                        <liferay-ui:message key="string.containing.letters.numbers.and/or.-" />
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <label class="mandatory"><liferay-ui:message key="declared.license" /></label>
                <div style="display: flex; flex-direction: row; width: 50rem;">
                    <div style="display: inline-flex;">
                        <input style="margin-top: 12px;" id="spdxDeclaredLicenseExist"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.DECLARED_LICENSE%><%=DeclaredLicense._Fields.TYPE%>"
                            value="exist"
                            onchange="setInputValue('spdxDeclaredLicense', value);"
                        />
                        <input style="margin-left: 4px; width: 30rem;" id="spdxDeclaredLicense"
                            class="form-control needs-validation"
                            rule="regex:^[0-9a-zA-Z-.+]+$"
                            type="text" required=""
                            name="<portlet:namespace/><%=SPDX._Fields.DECLARED_LICENSE%><%=DeclaredLicense._Fields.VALUE%>"
                            placeholder="<liferay-ui:message key="enter.declared.license" />"
                            value="<sw360:out value="${release.spdx.declaredLicense.value}" />" />
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxDeclaredLicenseNone"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.DECLARED_LICENSE%><%=DeclaredLicense._Fields.TYPE%>"
                            value="none"
                            onchange="setInputValue('spdxDeclaredLicense', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxDeclaredLicenseNone"><liferay-ui:message key="none" /></label>
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxDeclaredLicenseNoassertion"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.DECLARED_LICENSE%><%=DeclaredLicense._Fields.TYPE%>"
                            value="noassertion"
                            onchange="setInputValue('spdxDeclaredLicense', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxDeclaredLicenseNoassertion"><liferay-ui:message key="noassertion" /></label>
                    </div>
                </div>
                <div id="spdxDeclaredLicense-error-messages">
                    <div class="invalid-feedback" rule="regex">
                        <liferay-ui:message key="string.containing.letters.numbers.and/or.-" />
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="spdxPackageLicenseComments"><liferay-ui:message key="comments.on.license" /></label>
                    <textarea class="form-control" id="spdxPackageLicenseComments" rows="4"
                        name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.PACKAGE_LICENSE_COMMENTS%>"
                        placeholder="<liferay-ui:message key="enter.comments" />"><sw360:out value="${release.spdx.packageLicenseComments}"/></textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <label class="mandatory"><liferay-ui:message key="copyright.text" /></label>
                <div style="display: flex; flex-direction: row; width: 60rem;">
                    <div style="display: inline-flex;">
                        <input style="margin-top: 12px;" id="spdxPackageCopyrightExist"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.COPYRIGHT%><%=Copyright._Fields.TYPE%>"
                            value="exist"
                            onchange="setInputValue('spdxPackageCopyright', value);"
                        />
                        <textarea style="margin-left: 4px; width: 40rem" id="spdxPackageCopyright"
                            required="" class="form-control" rows="4"
                            name="<portlet:namespace/><%=SPDX._Fields.COPYRIGHT%><%=Copyright._Fields.VALUE%>"
                            placeholder="<liferay-ui:message key="enter.copyright" />"><sw360:out value="${release.spdx.copyright.value}"/></textarea>
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxPackageCopyrightNone"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.COPYRIGHT%><%=Copyright._Fields.TYPE%>"
                            value="none"
                            onchange="setInputValue('spdxPackageCopyright', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxPackageCopyrightNone"><liferay-ui:message key="none" /></label>
                    </div>
                    <div style="margin-left: 2rem;">
                        <input style="margin-top: 12px;" id="spdxPackageCopyrightNoassertion"
                            type="radio"
                            name="<portlet:namespace/><%=SPDX._Fields.COPYRIGHT%><%=Copyright._Fields.TYPE%>"
                            value="noassertion"
                            onchange="setInputValue('spdxPackageCopyright', value);"
                        />
                        <label class="form-check-label radio-label" for="spdxPackageCopyrightNoassertion"><liferay-ui:message key="noassertion" /></label>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="spdxPackageComment"><liferay-ui:message key="package.comment" /></label>
                    <textarea class="form-control" id="spdxPackageComment" rows="4"
                    name="<portlet:namespace/><%=Release._Fields.SPDX%><%=SPDX._Fields.PACKAGE_COMMENT%>"
                    placeholder="<liferay-ui:message key="enter.comments" />"><sw360:out value="${release.spdx.packageComment}"/></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>

    setInputValue('spdxPackageDownloadLocation', "${release.spdx.packageDownload.type}");
    setInputValue('spdxPackageHomepage', "${release.spdx.packageDownload.type}");
    setInputValue('spdxConcludedLicense', "${release.spdx.concludedLicense.type}");
    setInputValue('spdxDeclaredLicense', "${release.spdx.declaredLicense.type}");
    setInputValue('spdxPackageCopyright', "${release.spdx.copyright.type}");

    function setInputValue(id, type) {
        switch(type) {
            case 'exist':
                document.getElementById(id + 'Exist').checked = 'true';
                setEnabled(id);
                break;
            case 'none':
                document.getElementById(id + 'None').checked = 'true';
                setDisabled(id);
                break;
            case 'noassertion':
                document.getElementById(id + 'Noassertion').checked = 'true';
                setDisabled(id);
                break;
            default:
                setDisabled(id);
                break;
        }
    }

    function setDisabled(id){
        $('#' + id).prop('disabled', true);
    }

    function setEnabled(id){
        $('#' + id).prop('disabled', false);
    }

</script>