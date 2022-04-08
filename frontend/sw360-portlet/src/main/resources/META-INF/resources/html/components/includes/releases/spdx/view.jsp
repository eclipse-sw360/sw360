<%--
  ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>

<div class="form-group btn-group">
	<button id="spdxFullMode" class="btn btn-info">SPDX Full</button>
	<button id="spdxLiteMode" class="btn btn-secondary">SPDX Lite</button>
</div>
<table class="table label-value-table spdx-table" id="DocumentCreationInformation">
	<thead class="spdx-thead">
		<tr>
			<th>6. Document Creation Information</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.1 SPDX version</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.spdxVersion}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.2 Data license</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.dataLicense}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.3 SPDX identifier</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.SPDXID}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.4 Document name</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.name}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.5 SPDX document namespace</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.documentNamespace}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.6 External document references</div>
				<div class="spdx-col-2 section" data-size="3">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-label-index">Index</div>
						<select class="spdx-col-3" id="externalDocumentRefs" onchange="displayIndex(this)">
						</select>
					</div>
					<core_rt:forEach items="${spdxDocumentCreationInfo.externalDocumentRefs}"
						var="externalDocumentRefeData" varStatus="loop">
						<div class="spdx-flex-row" data-index="${externalDocumentRefeData.index}">
							<div class="spdx-col-1 spdx-key">External document ID</div>
							<div class="spdx-col-3">
								<sw360:out value="${externalDocumentRefeData.externalDocumentId}" />
							</div>
						</div>
						<div class="spdx-flex-row" data-index="${externalDocumentRefeData.index}">
							<div class="spdx-col-1 spdx-key">External document</div>
							<div class="spdx-col-3">
								<sw360:out value="${externalDocumentRefeData.spdxDocument}" />
							</div>
						</div>
						<div class="spdx-flex-row" data-index="${externalDocumentRefeData.index}">
							<div class="spdx-col-2 spdx-key">Checksum</div>
							<div class="spdx-col-3">
								<sw360:out
									value="${externalDocumentRefeData.checksum.algorithm}" />
								:
								<sw360:out value="${externalDocumentRefeData.checksum.checksumValue}" />
							</div>
						</div>
					</core_rt:forEach>
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.7 License list version</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.licenseListVersion}" />
				</div>
			</td>
		</tr>
		<core_rt:set var="creators" value="${spdxDocumentCreationInfo.creator}" />
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.8 Creators</div>
				<div class="spdx-col-2" id="creators">
					<core_rt:forEach items="${creators}" var="creatorData" varStatus="loop">
						<div class="spdx-flex-row creator" data-index="${creatorData.index}">
							<div class="spdx-col-1 spdx-key">
								<sw360:out value="${creatorData.type}" />
							</div>
							<div class="spdx-col-3">
								<sw360:out value="${creatorData.value}" />
							</div>
						</div>
					</core_rt:forEach>
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.9 Created</div>
				<div class="spdx-col-2" id="createdDateTime">
					<sw360:out value="${spdxDocumentCreationInfo.created}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.10 Creator comment</div>
				<div class="spdx-col-2" id="creatorComment">
					<sw360:out value="${spdxDocumentCreationInfo.creatorComment}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.11 Document comment</div>
				<div class="spdx-col-2" id="documentComment">
					<sw360:out value="${spdxDocumentCreationInfo.documentComment}" />
				</div>
			</td>
		</tr>
	</tbody>
</table>
<!-- <core_rt:if test="${not empty spdxPackageInfo}">
	<core_rt:set var="package" value="${spdxPackageInfo.iterator().next()}" />
</core_rt:if> -->
<table class="table label-value-table spdx-table" id="PackageInformation">
	<thead class="spdx-thead">
		<tr>
			<th>7. Package Information</th>
		</tr>
	</thead>
	<tbody class="section" data-size="23">
		<tr>
			<td class="spdx-flex-row" style="display:none;">
					<div class="spdx-col-1 spdx-label-index">Index</div>
                     <select id="packageInfoSelect" class="spdx-col-2" onchange="changePackageIndex(this)"></select>
			</td>
		</tr>

		<core_rt:forEach items="${spdxPackageInfo}" var="package" varStatus="loop">
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.1 Package name</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.name}" />
					</div>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.2 Package SPDX identifier</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.SPDXID}" />
					</div>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.3 Package version</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.versionInfo}" />
					</div>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.4 Package file name</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.packageFileName}" />
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.5 Package supplier</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.supplier}" />
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.6 Package originator</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.originator}" />
					</div>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.7 Package download location</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.downloadLocation}" />
					</div>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.8 Files analyzed</div>
					<div class="spdx-col-2 spdx-uppercase">
						<sw360:out value="${package.filesAnalyzed}" />
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.9 Package verification code</div>
					<div class="spdx-col-2 spdx-flex-col">
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-key">Value</div>
							<div class="spdx-col-3">
								<sw360:out value="${package.packageVerificationCode.value}" />
							</div>
						</div>
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-key">Excluded files</div>
							<p class="spdx-col-3 " id="excludedFiles">
								<core_rt:forEach items="${package.packageVerificationCode.excludedFiles}"
									var="excludedFileData" varStatus="loop">
									<sw360:out value="${excludedFileData}" /> <br>
								</core_rt:forEach>
							</p>
						</div>
					</div>
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.10 Package checksum</div>
					<div class="spdx-col-2" id="checksums">
						<core_rt:forEach items="${package.checksums}" var="checksumData" varStatus="loop">
							<div class="spdx-flex-row checksum" data-index="${checksumData.index}">
								<div class="spdx-col-1 spdx-key">
									<sw360:out value="${checksumData.algorithm}" />
								</div>
								<div class="spdx-col-3">
									<sw360:out value="${checksumData.checksumValue}" />
								</div>
							</div>
						</core_rt:forEach>
					</div>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.11 Package home page</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.homepage}" />
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.12 Source information</div>
					<div class="spdx-col-2 " id="sourceInfo">
						<sw360:out value="${package.sourceInfo}" />
					</div>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.13 Concluded license</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.licenseConcluded}" />
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.14 All licenses information from files</div>
					<p class="spdx-col-2 " id="licenseInfoFromFile">
						<core_rt:forEach items="${package.licenseInfoFromFiles}" var="licenseInfoFromFileData"
							varStatus="loop">
							<sw360:out value="${licenseInfoFromFileData} " />
						</core_rt:forEach>
					</p>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.15 Declared license</div>
					<div class="spdx-col-2">
						<sw360:out value="${package.licenseDeclared}" />
					</div>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.16 Comments on license</div>
					<p class="spdx-col-2 " id="licenseComments">
						<sw360:out value="${package.licenseComments}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.17 Copyright text</div>
					<p class="spdx-col-2 " id="copyrightText">
						<sw360:out value="${package.copyrightText}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.18 Package summary description</div>
					<p class="spdx-col-2 " id="summary" >
						<sw360:out value="${package.summary}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.19 Package detailed description</div>
					<p class="spdx-col-2 " id="description">
						<sw360:out value="${package.description}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.20 Package comment</div>
					<p class="spdx-col-2 " id="packageComment">
						<sw360:out value="${package.packageComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.21 External references </div>
					<core_rt:if test="${package.externalRefs.size() gt 0}">
					<div class="spdx-col-2 section" data-size="4">
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-label-index">Index</div>
							<select id="externalReferenceSelect${package.index}" class="spdx-col-3" onchange="displayIndex(this)">
							</select>
						</div>
						<core_rt:forEach items="${package.externalRefs}" var="externalRefsData" varStatus="loop">
							<div class="spdx-flex-row" data-index="${externalRefsData.index}">
								<div class="spdx-col-1 spdx-key">Category</div>
								<div class="spdx-col-3 spdx-uppercase">
									<sw360:out value="${externalRefsData.referenceCategory}" />
								</div>
							</div>
							<div class="spdx-flex-row" data-index="${externalRefsData.index}">
								<div class="spdx-col-1 spdx-key">Type</div>
								<div class="spdx-col-3">
									<sw360:out value="${externalRefsData.referenceType}" />
								</div>
							</div>
							<div class="spdx-flex-row" data-index="${externalRefsData.index}">
								<div class="spdx-col-1 spdx-key">Locator</div>
								<div class="spdx-col-3">
									<sw360:out value="${externalRefsData.referenceLocator}" />
								</div>
							</div>
							<div class="spdx-flex-row" data-index="${externalRefsData.index}">
								<div class="spdx-col-1 spdx-key">7.22 Comment</div>
								<p class="spdx-col-3" id="externalRefComment-${externalRefsData.index}">
									<sw360:out value="${externalRefsData.comment}" />
								</p>
							</div>
						</core_rt:forEach>
					</core_rt:if>
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${package.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.23 Package attribution text</div>
					<p class="spdx-col-2 " id="attributionText">
						<core_rt:forEach items="${package.attributionText}" var="attributionTextData" varStatus="loop">
							<sw360:out value="${attributionTextData}"/><br>
						</core_rt:forEach>
					</p>
				</td>
			</tr>
		</core_rt:forEach>
	</tbody>
</table>

<core_rt:set var="snippets" value="${spdxDocument.snippets}" />
<table class="table label-value-table spdx-table spdx-full" id="SnippetInformation">
	<thead class="spdx-thead">
		<tr>
			<th>9. Snippet Information</th>
		</tr>
	</thead>
	<tbody class="section" data-size="10">
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select id="snippetInfoSelect" class="spdx-col-2" onchange="displayIndex(this)"></select>
			</td>
		</tr>
		<core_rt:forEach items="${snippets}" var="snippetsData" varStatus="loop">
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.1 Snippet SPDX identifier</div>
					<div class="spdx-col-2">
						<sw360:out value="${snippetsData.SPDXID}" />
					</div>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.2 Snippet from file SPDX identifier</div>
					<div class="spdx-col-2">
						<sw360:out value="${snippetsData.snippetFromFile}" />
					</div>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.3 & 9.4 Snippet ranges</div>
					<div class="spdx-col-2 spdx-flex-col" id="snippetRanges-${snippetsData.index}">
						<core_rt:forEach items="${snippetsData.snippetRanges}" var="snippetRangeData" varStatus="loop">
							<div class="spdx-flex-row snippetRange-${snippetsData.index}" data-index="${snippetRangeData.index}">
								<div class="spdx-col-1 spdx-key">
									<sw360:out value="${snippetRangeData.rangeType}" />
								</div>
								<div class="spdx-col-1 spdx-flex-row">
									<div class="spdx-col-1">
										<sw360:out value="${snippetRangeData.startPointer}" />
									</div>
									<div class="spdx-col-1">~</div>
									<div class="spdx-col-1">
										<sw360:out value="${snippetRangeData.endPointer}" />
									</div>
								</div>
								<div class="spdx-col-3">
									<sw360:out value="${snippetRangeData.reference}" />
								</div>
							</div>
						</core_rt:forEach>
					</div>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.5 Snippet concluded license</div>
					<div class="spdx-col-2">
						<sw360:out value="${snippetsData.licenseConcluded}" />
					</div>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.6 License information in snippet</div>
					<p class="spdx-col-2 ">
						<core_rt:forEach items="${snippetsData.licenseInfoInSnippets}" var="licenseInfoInSnippetData"
							varStatus="loop">
							<sw360:out value="${licenseInfoInSnippetData} " /> <br>
						</core_rt:forEach>
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.7 Snippet comments on license</div>
					<p class="spdx-col-2 " id="snippetLicenseComments-${snippetsData.index}">
						<sw360:out value="${snippetsData.licenseComments}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.8 Snippet copyright text</div>
					<p class="spdx-col-2 " id="snippetCopyrightText-${snippetsData.index}">
						<sw360:out value="${snippetsData.copyrightText}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.9 Snippet comment</div>
					<p class="spdx-col-2 " id="snippetComment-${snippetsData.index}">
						<sw360:out value="${snippetsData.comment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.10 Snippet name</div>
					<p class="spdx-col-2 ">
						<sw360:out value="${snippetsData.name}" />
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">9.11 Snippet attribution text</div>
					<p class="spdx-col-2 " id="snippetAttributionText-${snippetsData.index}">
						<sw360:out value="${snippetsData.snippetAttributionText}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>
	</tbody>
</table>

<core_rt:set var="otherLicensing" value="${spdxDocument.otherLicensingInformationDetecteds}" />
<table class="table label-value-table spdx-table" id="OtherLicensingInformationDetected">
	<thead class="spdx-thead">
		<tr>
			<th>10. Other Licensing Information Detected</th>
		</tr>
	</thead>
	<tbody class="section" data-size="5">
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select id="otherLicensingSelect" class="spdx-col-2" onchange="displayIndex(this)"></select>
			</td>
		</tr>
		<core_rt:forEach items="${otherLicensing}" var="otherLicensingData" varStatus="loop">
			<tr data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">10.1 License identifier</div>
					<div class="spdx-col-2">
						<sw360:out value="${otherLicensingData.licenseId}" />
					</div>
				</td>
			</tr>
			<tr data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">10.2 Extracted text</div>
					<p class="spdx-col-2 " id="extractedText-${otherLicensingData.index}">
						<sw360:out value="${otherLicensingData.extractedText}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">10.3 License name</div>
					<div class="spdx-col-2">
						<sw360:out value="${otherLicensingData.licenseName}" />
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">10.4 License cross reference</div>
					<p class="spdx-col-2 " id="licenseCrossRefs-${otherLicensingData.index}">
						<core_rt:forEach items="${otherLicensingData.licenseCrossRefs}" var="licenseCrossRefsData" varStatus="loop">
							<sw360:out value="${licenseCrossRefsData}"/><br>
						</core_rt:forEach>
					</p>
				</td>
			</tr>
			<tr data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">10.5 License comment</div>
					<p class="spdx-col-2 " id="otherLicenseComment-${otherLicensingData.index}">
						<sw360:out value="${otherLicensingData.licenseComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>
	</tbody>
</table>

<core_rt:set var="relationships" value="${spdxDocument.relationships}" />
<core_rt:forEach items="${spdxPackageInfo}" var="spdxPackage" varStatus="loop">
	<core_rt:if test="${spdxPackage.index eq 0}">
		<core_rt:set var="packageRelationships" value="${spdxPackage.relationships}" />
	</core_rt:if>
</core_rt:forEach>
<table class="table label-value-table spdx-table spdx-full" id="RelationshipsbetweenSPDXElements">
	<thead class="spdx-thead">
		<tr>
			<th>11. Relationship between SPDX Elements Information</th>
		</tr>
	</thead>
	<tbody class="section" data-size="3">
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Source</div>
				<select id="relationshipSourceSelect" class="spdx-col-2" onchange="changeRelationshipSource(this)">
					<option>SPDX Document</option>
					<option>Package</option>
				</select>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select id="relationshipSelect" class="spdx-col-2" onchange="displayRelationshipIndex(this)"></select>
			</td>
		</tr>
		<core_rt:forEach items="${relationships}" var="relationshipsData" varStatus="loop">
			<tr class="relationship-document" data-index="${relationshipsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">11.1 Relationship</div>
					<div class="spdx-col-2 spdx-flex-col">
						<div class="spdx-flex-row">
							<div class="spdx-col-1">
								<sw360:out value="${relationshipsData.spdxElementId}" />
							</div>
							<div class="spdx-col-1 spdx-flex-row">
								<sw360:out
									value="${relationshipsData.relationshipType.replace('relationshipType_', '')}" />
							</div>
							<div class="spdx-col-3">
								<sw360:out value="${relationshipsData.relatedSpdxElement}" />
							</div>
						</div>
					</div>
				</td>
			</tr>
			<tr class="relationship-document" data-index="${relationshipsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">11.2 Relationship comment</div>
					<p class="spdx-col-2 " id="relationshipComment-${relationshipsData.index}">
						<sw360:out value="${relationshipsData.relationshipComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>


		<core_rt:forEach items="${packageRelationships}" var="relationshipsData" varStatus="loop">
			<tr class="relationship-package" data-index="${relationshipsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">11.1 Relationship</div>
					<div class="spdx-col-2 spdx-flex-col">
						<div class="spdx-flex-row">
							<div class="spdx-col-1">
								<sw360:out value="${relationshipsData.spdxElementId}" />
							</div>
							<div class="spdx-col-1 spdx-flex-row">
								<sw360:out
									value="${relationshipsData.relationshipType.replace('relationshipType_', '')}" />
							</div>
							<div class="spdx-col-3">
								<sw360:out value="${relationshipsData.relatedSpdxElement}" />
							</div>
						</div>
					</div>
				</td>
			</tr>
			<tr class="relationship-package" data-index="${relationshipsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">11.2 Relationship comment</div>
					<p class="spdx-col-2 " id="relationshipComment-${relationshipsData.index}">
						<sw360:out value="${relationshipsData.relationshipComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>
	</tbody>
</table>

<core_rt:set var="documentAnnotations" value="${spdxDocument.annotations}" />
<core_rt:forEach items="${spdxPackageInfo}" var="spdxPackage" varStatus="loop">
	<core_rt:if test="${spdxPackage.index eq 0}">
		<core_rt:set var="packageAnnotations" value="${spdxPackage.annotations}" />
	</core_rt:if>
</core_rt:forEach>
<table class="table label-value-table spdx-table spdx-full" id="Annotations">
	<thead class="spdx-thead">
		<tr>
			<th>12. Annotation Information</th>
		</tr>
	</thead>
	<tbody class="section" data-size="5">
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Source</div>
				<select id="annotationSourceSelect" class="spdx-col-2" onchange="changeAnnotationSource(this)">
					<option>SPDX Document</option>
					<option>Package</option>
				</select>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select id="annotationSelect" class="spdx-col-2" onchange="displayAnnotationIndex(this)"></select>
			</td>
		</tr>
		<core_rt:forEach items="${documentAnnotations}" var="annotationsData" varStatus="loop">
			<tr class="annotation-document" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.1 Annotator</div>
					<p class="spdx-col-2 ">
						<sw360:out value="${annotationsData.annotator}" />
					</p>
					</div>
				</td>
			</tr>
			<tr class="annotation-document" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.2 Annotation date</div>
					<p class="spdx-col-2 " id="annotation-document-date-${loop.count}">
						<sw360:out value="${annotationsData.annotationDate}" />
					</p>
				</td>
			</tr>
			<tr class="annotation-document" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.3 Annotation type</div>
					<div class="spdx-col-2">
						<div class="spdx-flex-row">
							<div class="spdx-col-3">
								<sw360:out value="${annotationsData.annotationType}" />
							</div>
						</div>
					</div>
				</td>
			</tr>
			<tr class="annotation-document" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.4 SPDX identifier reference</div>
					<div class="spdx-col-2">
						<sw360:out value="${annotationsData.spdxIdRef}" />
					</div>
				</td>
			</tr>
			<tr class="annotation-document" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.5 Annotation comment</div>
					<p class="spdx-col-2 " id="documentAnnotationComment-${annotationsData.index}">
						<sw360:out value="${annotationsData.annotationComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>
		<core_rt:forEach items="${packageAnnotations}" var="annotationsData" varStatus="loop">
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.1 Annotator</div>
					<p class="spdx-col-2 ">
						<sw360:out value="${annotationsData.annotator}" />
					</p>
					</div>
				</td>
			</tr>
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.2 Annotation date</div>
					<p class="spdx-col-2 " id="annotation-package-date-${loop.count}">
						<sw360:out value="${annotationsData.annotationDate}" />
					</p>
				</td>
			</tr>
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.3 Annotation type</div>
					<div class="spdx-col-2">
						<div class="spdx-flex-row">
							<div class="spdx-col-3">
								<sw360:out value="${annotationsData.annotationType}" />
							</div>
						</div>
					</div>
				</td>
			</tr>
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.4 SPDX identifier reference</div>
					<div class="spdx-col-2">
						<sw360:out value="${annotationsData.spdxIdRef}" />
					</div>
				</td>
			</tr>
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">12.5 Annotation comment</div>
					<p class="spdx-col-2 " id="packageAnnotationComment-${annotationsData.index}">
						<sw360:out value="${annotationsData.annotationComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>
	</tbody>
</table>

<style>
	.spdx-col-1 {
		flex: 1;
	}

	.spdx-col-2 {
		flex: 2;
	}

	.spdx-col-3 {
		flex: 3;
		max-width: 60%;
	}

	.spdx-flex-row {
		display: flex;
		flex-direction: row;
		overflow: auto;
		;
	}

	.spdx-flex-col {
		display: flex;
		flex-direction: column;
	}

	/*. {*/
	/*	margin-bottom: 0;*/
	/*}*/

	.spdx-key {
		font-weight: bold;
	}

	.spdx-label-index {
		text-decoration: underline;
	}

	.spdx-table td:first-child {
		width: 100% !important;
	}

	.spdx-thead {
		cursor: pointer;
	}

	.spdx-uppercase {
		text-transform: uppercase;
	}
</style>

<script type="text/javascript">
	function sortElements(parent, elements) {
		for (let i = 0; i < elements.length; i++) {
			$(parent).find('[data-index=' + i + ']').appendTo($(parent));
		}
	}

	function readArray(tag) {
		let arr = $(tag).text();
		arr = arr.replaceAll('\t', '').split('\n').map(str => str.trim()).filter(function(str) {
			return str != '';
		}).sort();
		return arr;
	}

	function fillArray(tag, value) {
		$(tag).text('');

		for (let i = 0; i< value.length; i++) {
		    let temp = value[i].replaceAll('&','&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
			$(tag).append(temp);
			$(tag).append('<br>');
		}
	}

	function formatArrayParagraph(tag) {
		fillArray(tag, readArray(tag));
	}

	function dynamicSort(property, type) {
		var sortOrder = 1;

		if(property[0] === "-") {
			sortOrder = -1;

			property = property.substr(1);
		}

		return function (a,b) {
			var result;

			switch (type) {
				case 'int':
					result = (parseInt(a[property]) < parseInt(b[property])) ? -1 : (parseInt(a[property]) > (b[property])) ? 1 : 0;
					break;
				case 'string':
				default:
					result = (a[property] < b[property]) ? -1 : (a[property] > b[property]) ? 1 : 0;
			}

			return  result * sortOrder;
		}
	}

	$(function () {
		var spdxDocumentObj = jQuery.parseJSON(JSON.stringify(${ spdxDocumentJson }));
		var documentCreationInformationObj = jQuery.parseJSON(JSON.stringify(${ documentCreationInfoJson }));
		var packagesInformationObj = jQuery.parseJSON(JSON.stringify(${ packageInfoJson }));
		packagesInformationObj.sort(dynamicSort('index', 'int'));
		var packageInformationObj = packagesInformationObj[0];
		formatArrayParagraph('#excludedFiles');
		formatArrayParagraph('#licenseInfoFromFile');
		formatArrayParagraph('#attributionText');
		formatArrayParagraph('#creatorComment');

		fillArray('#creatorComment', documentCreationInformationObj.creatorComment.split('\n'));
		fillArray('#documentComment', documentCreationInformationObj.documentComment.split('\n'));

		fillArray('#sourceInfo', packageInformationObj.sourceInfo.split('\n'));
		fillArray('#licenseComments', packageInformationObj.licenseComments.split('\n'));
		fillArray('#copyrightText', packageInformationObj.copyrightText.split('\n'));
		fillArray('#summary', packageInformationObj.summary.split('\n'));
		fillArray('#description', packageInformationObj.description.split('\n'));
		fillArray('#packageComment', packageInformationObj.packageComment.split('\n'));

		for (let i = 0; i < packageInformationObj.externalRefs.length; i++) {
			for (let j = 0; j < packageInformationObj.externalRefs.length; j++) {
				if (packageInformationObj.externalRefs[j].index == i) {
					fillArray('#externalRefComment-' + i, packageInformationObj.externalRefs[j].comment.split('\n'));
				}
			}
		}

		for (let i = 0; i < spdxDocumentObj.snippets.length; i++) {
			for (let j = 0; j < spdxDocumentObj.snippets.length; j++) {
				if (spdxDocumentObj.snippets[j].index == i) {
					fillArray('#snippetLicenseComments-' + i, spdxDocumentObj.snippets[j].licenseComments.split('\n'));
					fillArray('#snippetCopyrightText-' + i, spdxDocumentObj.snippets[j].copyrightText.split('\n'));
					fillArray('#snippetComment-' + i, spdxDocumentObj.snippets[j].comment.split('\n'));
					fillArray('#snippetAttributionText-' + i, spdxDocumentObj.snippets[j].snippetAttributionText.split('\n'));
				}
			}
		}

		for (let i = 0; i < spdxDocumentObj.otherLicensingInformationDetecteds.length; i++) {
			for (let j = 0; j < spdxDocumentObj.otherLicensingInformationDetecteds.length; j++) {
				if (spdxDocumentObj.otherLicensingInformationDetecteds[j].index == i) {
					fillArray('#extractedText-' + i, spdxDocumentObj.otherLicensingInformationDetecteds[j].extractedText.split('\n'));
					fillArray('#otherLicenseComment-' + i, spdxDocumentObj.otherLicensingInformationDetecteds[j].licenseComment.split('\n'));
				}
			}
		}

		for (let i = 0; i < spdxDocumentObj.relationships.length; i++) {
			for (let j = 0; j < spdxDocumentObj.relationships.length; j++) {
				if (spdxDocumentObj.relationships[j].index == i) {
					fillArray('#relationshipComment-' + i, spdxDocumentObj.relationships[j].relationshipComment.split('\n'));
				}
			}
		}

		for (let i = 0; i < spdxDocumentObj.annotations.length; i++) {
			for (let j = 0; j < spdxDocumentObj.annotations.length; j++) {
				if (spdxDocumentObj.annotations[j].index == i) {
					fillArray('#documentAnnotationComment-' + i, spdxDocumentObj.annotations[j].annotationComment.split('\n'));
				}
			}
		}

		for (let i = 0; i < packageInformationObj.annotations.length; i++) {
			for (let j = 0; j < packageInformationObj.annotations.length; j++) {
				if (packageInformationObj.annotations[j].index == i) {
					fillArray('#packageAnnotationComment-' + i, packageInformationObj.annotations[j].annotationComment.split('\n'));
				}
			}
		}

		for (let i = 0; i < $('#otherLicensingSelect').find('option').last().text(); i++) {
			formatArrayParagraph('#licenseCrossRefs-' + i);
		}

		$('#spdxFullMode').on('click', function (e) {
			e.preventDefault();

			$(this).addClass('btn-info');
			$(this).removeClass('btn-secondary');

			$('#spdxLiteMode').addClass('btn-secondary');
			$('#spdxLiteMode').removeClass('btn-info');

			$('.spdx-full').css('display', '');
		});

		$('#spdxLiteMode').on('click', function (e) {
			e.preventDefault();

			$(this).addClass('btn-info');
			$(this).removeClass('btn-secondary');

			$('#spdxFullMode').addClass('btn-secondary');
			$('#spdxFullMode').removeClass('btn-info');

			$('.spdx-full').css('display', 'none');
		});

		// Expand/collapse section when click on the header
		$('thead').on('click', function () {
			if ($(this).next().css('display') == 'none') {
				$(this).next().css('display', '');
			} else {
				$(this).next().css('display', 'none');
			}
		});

		$('.spdx-table select').each(function () {
			if ($(this).children().length == 0) {
				$(this).attr('disabled', 'true');
			}
		});

		sortElements('#creators', $('.creator').toArray());
		sortElements('#checksums', $('.checksum').toArray());
		var snippetIndex = $('#snippetInfoSelect').val() - 1;
		sortElements('#snippetRanges-' + snippetIndex, $('.snippetRange-' + snippetIndex).toArray());

		$('.spdx-table select').change();
	});

	function generateSelecterOption(selectId, length) {
		$('#' + selectId).find('option').remove();
		for (var i = 1; i <= length; i++) {
			var option = document.createElement("option");
			option.text = i;
			$('#' + selectId).append(option);
		}
		if (length == 0) {
			$('#' + selectId).attr('disabled', 'disabled');
		} else {
			$('#' + selectId).removeAttr('disabled', 'disabled');
		}
	}
	generateSelecterOption('snippetInfoSelect', "${snippets.size()}");
	generateSelecterOption('otherLicensingSelect', "${otherLicensing.size()}");
	generateSelecterOption('relationshipSelect', "${relationships.size()}");
	generateSelecterOption('annotationSelect', "${documentAnnotations.size()}");
	generateSelecterOption('externalDocumentRefs', "${spdxDocumentCreationInfo.externalDocumentRefs.size()}");
	generateSelecterOption('packageInfoSelect', "${spdxPackageInfo.size()}");

	var packageIndex =  $('#packageInfoSelect')[0].selectedIndex;
	var packagesInformationObj = jQuery.parseJSON(JSON.stringify(${ packageInfoJson }));
	packagesInformationObj.sort(dynamicSort('index', 'int'));
	generateSelecterOption('externalReferenceSelect'+packagesInformationObj[packageIndex].externalRefs.length, packagesInformationObj[packageIndex].externalRefs.length);

	function displayIndex(el) {
		var index = $(el).val();
		var section = $(el).closest('.section');
		var size = section.data()['size'];

		section.children().css('display', 'none');
		section.children().eq(0).css('display', '');

		section.find('[data-index=' + (index - 1).toString() + ']').css('display', '');

		if ($(el).attr('id') == 'snippetInfoSelect') {
			sortElements('#snippetRanges-' + (index - 1), $('.snippetRange-' + (index - 1)).toArray());
		}
	}

	function changePackageIndex(el) {
		if ($(el).attr('id') == 'packageInfoSelect') {
			var index = $(el).val();
			var section = $(el).closest('.section');
			var size = section.data()['size'];

			section.children().css('display', 'none');
			section.children().eq(0).css('display', '');

			section.find('[data-index=' + (index - 1).toString() + ']').css('display', '');

			var packagesInformationObj = jQuery.parseJSON(JSON.stringify(${ packageInfoJson }));
			packagesInformationObj.sort(dynamicSort('index', 'int'));
			var packageIndex =  $('#packageInfoSelect')[0].selectedIndex;
			generateSelecterOption('externalReferenceSelect'+(index-1), packagesInformationObj[packageIndex].externalRefs.length);
			$('#externalReferenceSelect'+(index-1)).change();
		}
	}

	function displayAnnotationIndex(el) {
		var index = $(el).val();
		var section = $(el).closest('.section');
		var size = section.data()['size'];

		section.children().css('display', 'none');
		section.children().eq(0).css('display', '');
		section.children().eq(1).css('display', '');

		if ($('#annotationSourceSelect').val() == 'SPDX Document') {
			$('.annotation-document[data-index=' + (index - 1).toString() + ']').css('display', 'table-row');
		} else {
			$('.annotation-package[data-index=' + (index - 1).toString() + ']').css('display', 'table-row');
		}
	}

	function changeAnnotationSource(el) {
		if ($('#annotationSourceSelect').val() == 'Package') {
			generateSelecterOption('annotationSelect', '${packageAnnotations.size()}');
		} else {
			generateSelecterOption('annotationSelect', '${documentAnnotations.size()}');
		}
		$('#annotationSelect').change();
	}

	<core_rt:forEach items="${documentAnnotations}" var="documentAnnotationData" varStatus="loop">
		displayDateTime('annotation-document-date-${loop.count}', '${documentAnnotationData.annotationDate}');
	</core_rt:forEach>

	displayDateTime('createdDateTime', "${spdxDocumentCreationInfo.created}");

	<core_rt:forEach items="${packageAnnotations}" var="packageAnnotationData" varStatus="loop">
		displayDateTime('annotation-package-date-${loop.count}', '${packageAnnotationData.annotationDate}');
	</core_rt:forEach>

	function displayDateTime(id, value) {
		if (value == '') {
			return;
		}

        let timeStamp = Date.parse(value);

        let date = new Date(timeStamp);

        let localTimeStamp = timeStamp - date.getTimezoneOffset();

        let localDate = new Date(localTimeStamp);

		let dateTime = localDate.getFullYear()
					+ '-' + (localDate.getMonth() + 1).toString().padStart(2, '0')
					+ '-' + localDate.getDate().toString().padStart(2, '0')
					+ ' ' + date.getHours().toString().padStart(2, '0')
					+ ':' + date.getMinutes().toString().padStart(2, '0')
					+ ':' + date.getSeconds().toString().padStart(2, '0');

		document.getElementById(id).innerHTML = dateTime;
	}

	function changeRelationshipSource(el) {
		if ($('#relationshipSourceSelect').val() == 'Package') {
			generateSelecterOption('relationshipSelect', '${packageRelationships.size()}');
		} else {
			generateSelecterOption('relationshipSelect', '${relationships.size()}');
		}
		$('#relationshipSelect').change();
	}

	function displayRelationshipIndex(el) {
		var index = $(el).val();
		var section = $(el).closest('.section');
		var size = section.data()['size'];

		section.children().css('display', 'none');
		section.children().eq(0).css('display', '');
		section.children().eq(1).css('display', '');

		if ($('#relationshipSourceSelect').val() == 'SPDX Document') {
			$('.relationship-document[data-index=' + (index - 1).toString() + ']').css('display', 'table-row');
		} else {
			$('.relationship-package[data-index=' + (index - 1).toString() + ']').css('display', 'table-row');
		}
	}
</script>
