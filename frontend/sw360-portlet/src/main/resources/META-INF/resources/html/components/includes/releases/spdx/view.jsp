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
			<th>2. Document Creation Information</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.1 SPDX Version</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.spdxVersion}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.2 Data License</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.dataLicense}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.3 SPDX Indentifier</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.SPDXID}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.4 Document Name</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.name}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.5 SPDX Document Namespace</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.documentNamespace}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.6 External Document References</div>
				<div class="spdx-col-2 section" data-size="3">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-label-index">Index</div>
						<select class="spdx-col-3" id="externalDocumentRefs" onchange="displayIndex(this)">
						</select>
					</div>
					<core_rt:forEach items="${spdxDocumentCreationInfo.externalDocumentRefs}"
						var="externalDocumentRefeData" varStatus="loop">
						<div class="spdx-flex-row" data-index="${externalDocumentRefeData.index}">
							<div class="spdx-col-1 spdx-key">External Document ID</div>
							<div class="spdx-col-3">
								<sw360:out value="${externalDocumentRefeData.externalDocumentId}" />
							</div>
						</div>
						<div class="spdx-flex-row" data-index="${externalDocumentRefeData.index}">
							<div class="spdx-col-1 spdx-key">External Document</div>
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
				<div class="spdx-col-1">2.7 License List Version</div>
				<div class="spdx-col-2">
					<sw360:out value="${spdxDocumentCreationInfo.licenseListVersion}" />
				</div>
			</td>
		</tr>
		<core_rt:set var="creators" value="${spdxDocumentCreationInfo.creator}" />
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.8 Creator</div>
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
				<div class="spdx-col-1">2.9 Created</div>
				<div class="spdx-col-2" id="createdDateTime">
					<sw360:out value="${spdxDocumentCreationInfo.created}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.10 Creator Comment</div>
				<p class="spdx-col-2" id="creatorComment">
					<sw360:out value="${spdxDocumentCreationInfo.creatorComment}" />
				</p>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.11 Document Comment</div>
				<p class="spdx-col-2" id="documentComment">
					<sw360:out value="${spdxDocumentCreationInfo.documentComment}" />
				</p>
			</td>
		</tr>
	</tbody>
</table>
<core_rt:if test="${not empty spdxPackageInfo}">
	<core_rt:set var="package" value="${spdxPackageInfo.iterator().next()}" />
</core_rt:if>
<table class="table label-value-table spdx-table" id="PackageInformation">
	<thead class="spdx-thead">
		<tr>
			<th>3. Package Information</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.1 Package Name</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.name}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.2 Package SPDX Identifier</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.SPDXID}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.3 Package Version</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.versionInfo}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.4 Package File Name</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.packageFileName}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.5 Package Supplier</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.supplier}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.6 Package Originator</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.originator}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.7 Package Download Location</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.downloadLocation}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.8 File Analyzed</div>
				<div class="spdx-col-2 spdx-uppercase">
					<sw360:out value="${package.filesAnalyzed}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.9 Package Verification Code</div>
				<div class="spdx-col-2 spdx-flex-col">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Value</div>
						<div class="spdx-col-3">
							<sw360:out value="${package.packageVerificationCode.value}" />
						</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Excluded Files</div>
						<p class="spdx-col-3 spdx-p" id="excludedFiles">
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
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.10 Package Checksum</div>
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
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.11 Package Home Page</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.homepage}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.12 Source Information</div>
				<p class="spdx-col-2 spdx-p" id="sourceInfo">
					<sw360:out value="${package.sourceInfo}" />
				</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.13 Concluded License</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.licenseConcluded}" />
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.14 All Licenses Information from Files</div>
				<p class="spdx-col-2 spdx-p" id="licenseInfoFromFile">
					<core_rt:forEach items="${package.licenseInfoFromFiles}" var="licenseInfoFromFileData"
						varStatus="loop">
						<sw360:out value="${licenseInfoFromFileData} " /> <br>
					</core_rt:forEach>
				</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.15 Declared License</div>
				<div class="spdx-col-2">
					<sw360:out value="${package.licenseDeclared}" />
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.16 Comments on License</div>
				<p class="spdx-col-2 spdx-p" id="licenseComments">
					<sw360:out value="${package.licenseComments}" stripNewlines="false" />
				</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.17 Copyright Text</div>
				<p class="spdx-col-2 spdx-p" id="copyrightText">
					<sw360:out value="${package.copyrightText}" stripNewlines="false" />
				</p>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.18 Package Summary Description</div>
				<p class="spdx-col-2 spdx-p" id="summary">
					<sw360:out value="${package.summary}" stripNewlines="false" />
				</p>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.19 Package Detailed Description</div>
				<p class="spdx-col-2 spdx-p" id="description">
					<sw360:out value="${package.description}" stripNewlines="false" />
				</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.20 Package Comment</div>
				<p class="spdx-col-2 spdx-p" id="packageComment">
					<sw360:out value="${package.packageComment}" stripNewlines="false" />
				</p>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.21 External Reference</div>
				<div class="spdx-col-2 section" data-size="4">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-label-index">Index</div>
						<select id="externalReferenceSelect" class="spdx-col-3" onchange="displayIndex(this)">
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
							<div class="spdx-col-1 spdx-key">3.22 Comment</div>
							<p class="spdx-col-3" id="externalRefComment-${externalRefsData.index}">
								<sw360:out value="${externalRefsData.comment}" />
							</p>
						</div>
					</core_rt:forEach>
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.23 Package Attribution Text</div>
				<p class="spdx-col-2 spdx-p" id="attributionText">
					<core_rt:forEach items="${package.attributionText}" var="attributionTextData" varStatus="loop">
						<sw360:out value="${attributionTextData}"/><br>
					</core_rt:forEach>
				</p>
			</td>
		</tr>
	</tbody>
</table>

<core_rt:set var="snippets" value="${spdxDocument.snippets}" />
<table class="table label-value-table spdx-table spdx-full" id="SnippetInformation">
	<thead class="spdx-thead">
		<tr>
			<th>5. Snippet Information</th>
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
					<div class="spdx-col-1">5.1 Snippet SPDX Identifier</div>
					<div class="spdx-col-2">
						<sw360:out value="${snippetsData.SPDXID}" />
					</div>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.2 Snippet from File SPDX Identifier</div>
					<div class="spdx-col-2">
						<sw360:out value="${snippetsData.snippetFromFile}" />
					</div>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.3 & 5.4 Snippet Ranges</div>
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
					<div class="spdx-col-1">5.5 Snippet Concluded License</div>
					<div class="spdx-col-2">
						<sw360:out value="${snippetsData.licenseConcluded}" />
					</div>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.6 License Information in Snippet</div>
					<p class="spdx-col-2 spdx-p">
						<core_rt:forEach items="${snippetsData.licenseInfoInSnippets}" var="licenseInfoInSnippetData"
							varStatus="loop">
							<sw360:out value="${licenseInfoInSnippetData} " /> <br>
						</core_rt:forEach>
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.7 Snippet Comments on License</div>
					<p class="spdx-col-2 spdx-p" id="snippetLicenseComments-${snippetsData.index}">
						<sw360:out value="${snippetsData.licenseComments}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.8 Copyright Text</div>
					<p class="spdx-col-2 spdx-p" id="snippetCopyrightText-${snippetsData.index}">
						<sw360:out value="${snippetsData.copyrightText}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.9 Snippet Comment</div>
					<p class="spdx-col-2 spdx-p" id="snippetComment-${snippetsData.index}">
						<sw360:out value="${snippetsData.comment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.10 Snippet Name</div>
					<p class="spdx-col-2 spdx-p">
						<sw360:out value="${snippetsData.name}" />
					</p>
				</td>
			</tr>
			<tr data-index="${snippetsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.11 Snippet Attribution Text</div>
					<p class="spdx-col-2 spdx-p" id="snippetAttributionText-${snippetsData.index}">
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
			<th>6. Other Licensing Information Detected</th>
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
					<div class="spdx-col-1">6.1 License Identifier</div>
					<div class="spdx-col-2">
						<sw360:out value="${otherLicensingData.licenseId}" />
					</div>
				</td>
			</tr>
			<tr data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.2 Extracted Text</div>
					<p class="spdx-col-2 spdx-p" id="extractedText-${otherLicensingData.index}">
						<sw360:out value="${otherLicensingData.extractedText}" stripNewlines="false" />
					</p>
				</td>
			</tr>
			<tr data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.3 License Name</div>
					<div class="spdx-col-2">
						<sw360:out value="${otherLicensingData.licenseName}" />
					</div>
				</td>
			</tr>
			<tr class="spdx-full" data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.4 License Cross Reference</div>
					<p class="spdx-col-2 spdx-p" id="licenseCrossRefs-${otherLicensingData.index}">
						<core_rt:forEach items="${otherLicensingData.licenseCrossRefs}" var="licenseCrossRefsData" varStatus="loop">
							<sw360:out value="${licenseCrossRefsData}"/><br>
						</core_rt:forEach>
					</p>
				</td>
			</tr>
			<tr data-index="${otherLicensingData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.5 License Comment</div>
					<p class="spdx-col-2 spdx-p" id="otherLicenseComment-${otherLicensingData.index}">
						<sw360:out value="${otherLicensingData.licenseComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>
	</tbody>
</table>

<core_rt:set var="relationships" value="${spdxDocument.relationships}" />
<table class="table label-value-table spdx-table spdx-full" id="RelationshipsbetweenSPDXElements">
	<thead class="spdx-thead">
		<tr>
			<th>7. Relationships between SPDX Elements</th>
		</tr>
	</thead>
	<tbody class="section" data-size="2">
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select id="relationshipSelect" class="spdx-col-2" onchange="displayIndex(this)"></select>
			</td>
		</tr>
		<core_rt:forEach items="${relationships}" var="relationshipsData" varStatus="loop">
			<tr data-index="${relationshipsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.1 Relationship</div>
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
			<tr data-index="${relationshipsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.2 Relationship Comment</div>
					<p class="spdx-col-2 spdx-p" id="relationshipComment-${relationshipsData.index}">
						<sw360:out value="${relationshipsData.relationshipComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>
	</tbody>
</table>

<core_rt:set var="documentAnnotations" value="${spdxDocument.annotations}" />
<core_rt:if test="${not empty spdxPackageInfo}">
	<core_rt:set var="packageAnnotations" value="${spdxPackageInfo.iterator().next().annotations}" />
</core_rt:if>
<table class="table label-value-table spdx-table spdx-full" id="Annotations">
	<thead class="spdx-thead">
		<tr>
			<th>8. Annotations</th>
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
					<div class="spdx-col-1">8.1 Annotator</div>
					<p class="spdx-col-2 spdx-p">
						<sw360:out value="${annotationsData.annotator}" />
					</p>
					</div>
				</td>
			</tr>
			<tr class="annotation-document" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.2 Annotation Date</div>
					<p class="spdx-col-2 spdx-p" id="annotation-document-date-${loop.count}">
						<sw360:out value="${annotationsData.annotationDate}" />
					</p>
				</td>
			</tr>
			<tr class="annotation-document" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.3 Annotation Type</div>
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
					<div class="spdx-col-1">8.4 SPDX Identifier Reference</div>
					<div class="spdx-col-2">
						<sw360:out value="${annotationsData.spdxIdRef}" />
					</div>
				</td>
			</tr>
			<tr class="annotation-document" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.5 Annotation Comment</div>
					<p class="spdx-col-2 spdx-p" id="documentAnnotationComment-${annotationsData.index}">
						<sw360:out value="${annotationsData.annotationComment}" stripNewlines="false" />
					</p>
				</td>
			</tr>
		</core_rt:forEach>
		<core_rt:forEach items="${packageAnnotations}" var="annotationsData" varStatus="loop">
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.1 Annotator</div>
					<p class="spdx-col-2 spdx-p">
						<sw360:out value="${annotationsData.annotator}" />
					</p>
					</div>
				</td>
			</tr>
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.2 Annotation Date</div>
					<p class="spdx-col-2 spdx-p" id="annotation-package-date-${loop.count}">
						<sw360:out value="${annotationsData.annotationDate}" />
					</p>
				</td>
			</tr>
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.3 Annotation Type</div>
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
					<div class="spdx-col-1">8.4 SPDX Identifier Reference</div>
					<div class="spdx-col-2">
						<sw360:out value="${annotationsData.spdxIdRef}" />
					</div>
				</td>
			</tr>
			<tr class="annotation-package" data-index="${annotationsData.index}">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.5 Annotation Comment</div>
					<p class="spdx-col-2 spdx-p" id="packageAnnotationComment-${annotationsData.index}">
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

	.spdx-p {
		margin-bottom: 0;
	}

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
			$(tag).append(value[i].replaceAll('<', '&lt;').replaceAll('>', '&gt;'));
			$(tag).append('<br>');
		}
	}

	function formatArrayParagraph(tag) {
		fillArray(tag, readArray(tag));
	}

	let spdxDocumentObj = jQuery.parseJSON(JSON.stringify(${ spdxDocumentJson }));
	let documentCreationInformationObj = jQuery.parseJSON(JSON.stringify(${ documentCreationInfoJson }));
	let packageInformationObj = jQuery.parseJSON(JSON.stringify(${ packageInfoJson }));

	$(function () {
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
		let snippetIndex = $('#snippetInfoSelect').val() - 1;
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
	generateSelecterOption('externalReferenceSelect', "${package.externalRefs.size()}");
	generateSelecterOption('externalDocumentRefs', "${spdxDocumentCreationInfo.externalDocumentRefs.size()}");

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
</script>
