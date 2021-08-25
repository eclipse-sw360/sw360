<%--
  ~ Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
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
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.spdxVersion}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.2 Data License</div>
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.dataLicense}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.3 SPDX Indentifier</div>
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.SPDXID}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.4 Document Name</div>
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.name}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.5 SPDX Document Namespace</div>
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.documentNamespace}"/></div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.6 External Document References</div>
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.externalDocumentRefs}"/></div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.7 License List Version</div>
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.licenseListVersion}"/></div>
			</td>
		</tr>
		<core_rt:set var="creators" value="${spdxDocumentCreationInfo.creator}" />
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.8 Creator</div>
				<div class="spdx-col-2">
					<core_rt:forEach items="${creators}" var="creatorData" varStatus="loop">
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-key"><sw360:out value="${creatorData.type}"/></div>
							<div class="spdx-col-3"><sw360:out value="${creatorData.value}"/></div>
						</div>
					</core_rt:forEach>
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.9 Created</div>
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.created}"/></div>
			</td>
		</tr>
	</tbody>
</table>
<core_rt:if test="${not spdxPackageInfo.isEmpty()}">
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
				<div class="spdx-col-2"><sw360:out value="${package.name}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.2 Package SPDX Identifier</div>
				<div class="spdx-col-2"><sw360:out value="${package.SPDXID}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.3 Package Version</div>
				<div class="spdx-col-2"><sw360:out value="${package.versionInfo}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.4 Package File Name</div>
				<div class="spdx-col-2"><sw360:out value="${package.packageFileName}"/></div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.5 Package Supplier</div>
				<div class="spdx-col-2"><sw360:out value="${package.supplier}"/></div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.6 Package Originator</div>
				<div class="spdx-col-2"><sw360:out value="${package.originator}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.7 Package Download Location</div>
				<div class="spdx-col-2"><sw360:out value="${package.downloadLocation}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.8 File Analyzed</div>
				<div class="spdx-col-2"><sw360:out value="${package.filesAnalyzed}"/></div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.9 Package Verification Code</div>
				<div class="spdx-col-2 spdx-flex-col">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Value</div>
						<div class="spdx-col-3"><sw360:out value="${package.packageVerificationCode.value}"/></div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Excluded Files</div>
							<p class="spdx-col-3 spdx-p">
								<core_rt:forEach items="${package.packageVerificationCode.excludedFiles}" var="excludedFileData" varStatus="loop">
									<sw360:out value="${excludedFileData}"/> <br> 
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
				<div class="spdx-col-2">
					<core_rt:forEach items="${package.checksums}" var="checksumData" varStatus="loop">
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-key"><sw360:out value="${checksumData.algorithm}"/></div>
							<div class="spdx-col-3"><sw360:out value="${checksumData.checksumValue}"/></div>
						</div>
					</core_rt:forEach>
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.11 Package Home Page</div>
				<div class="spdx-col-2"><sw360:out value="${package.homepage}"/></div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.12 Source Information</div>
				<div class="spdx-col-2 spdx-p"><sw360:out value="${package.sourceInfo}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.13 Concluded License</div>
				<div class="spdx-col-2"><sw360:out value="${package.licenseConcluded}"/></div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.14 All Licenses Information from Files</div>
				<p class="spdx-col-2 spdx-p">
					<core_rt:forEach items="${package.licenseInfoFromFiles}" var="licenseInfoFromFileData" varStatus="loop">
						<sw360:out value="${licenseInfoFromFileData}, "/>
					</core_rt:forEach>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.15 Declared License</div>
				<div class="spdx-col-2"><sw360:out value="${package.licenseDeclared}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.16 Comments on License</div>
				<p class="spdx-col-2 spdx-p"><sw360:out value="${package.licenseComments}"/></p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.17 Copyright Text</div>
				<p class="spdx-col-2 spdx-p"><sw360:out value="${package.copyrightText}"/></p>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.18 Package Summary Description</div>
				<p class="spdx-col-2 spdx-p"><sw360:out value="${package.summary}"/></p>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.19 Package Detailed Description</div>
				<p class="spdx-col-2 spdx-p"><sw360:out value="${package.description}"/></p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.20 Package Comment</div>
				<p class="spdx-col-2 spdx-p"><sw360:out value="${package.packageComment}"/></p>
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
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-key">Category</div>
							<div class="spdx-col-3"><sw360:out value="${externalRefsData.referenceCategory}"/></div>
						</div>
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-key">Type</div>
							<div class="spdx-col-3"><sw360:out value="${externalRefsData.referenceType}"/></div>
						</div>
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-key">Locator</div>
							<div class="spdx-col-3"><sw360:out value="${externalRefsData.referenceLocator}"/></div>
						</div>
						<div class="spdx-flex-row">
							<div class="spdx-col-1 spdx-key">3.22 Comment</div>
							<div class="spdx-col-3"><sw360:out value="${externalRefsData.comment}"/></div>
						</div>
					</core_rt:forEach>
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.23 Package Attribution Text</div>
				<p class="spdx-col-2 spdx-p"><sw360:out value="${package.attributionText}"/></p>
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
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.1 Snippet SPDX Identifier</div>
					<div class="spdx-col-2"><sw360:out value="${snippetsData.SPDXID}"/></div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.2 Snippet from File SPDX Identifier</div>
					<div class="spdx-col-2"><sw360:out value="${snippetsData.snippetFromFile}"/></div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.3 & 5.4 Snippet Ranges</div>
						<div class="spdx-col-2 spdx-flex-col">
							<core_rt:forEach items="${snippetsData.snippetRanges}" var="snippetRangesData" varStatus="loop">
								<div class="spdx-flex-row">
									<div class="spdx-col-1 spdx-key"><sw360:out value="${snippetRangesData.rangeType}"/></div>
									<div class="spdx-col-1 spdx-flex-row">
										<div class="spdx-col-1"><sw360:out value="${snippetRangesData.startPointer}"/></div>
										<div class="spdx-col-1">~</div>
										<div class="spdx-col-1"><sw360:out value="${snippetRangesData.endPointer}"/></div>
									</div>
									<div class="spdx-col-3"><sw360:out value="${snippetRangesData.reference}"/></div>
								</div>
							</core_rt:forEach>
						</div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.5 Snippet Concluded License</div>
					<div class="spdx-col-2"><sw360:out value="${snippetsData.licenseConcluded}"/></div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.6 License Information in Snippet</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${snippetsData.licenseInfoInSnippets}"/></p>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.7 Snippet Comments on License</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${snippetsData.licenseComments}"/></p>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.8 Copyright Text</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${snippetsData.copyrightText}"/></p>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.9 Snippet Comments</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${snippetsData.comment}"/></p>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.10 Snippet Name</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${snippetsData.name}"/></p>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">5.11 Snippet Attribution Text</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${snippetsData.snippetAttributionText}"/></p>
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
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.1 License Identifier</div>
					<div class="spdx-col-2"><sw360:out value="${otherLicensingData.licenseId}"/></div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.2 Extracted Text</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${otherLicensingData.extractedText}"/></p>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.3 License Name</div>
					<div class="spdx-col-2"><sw360:out value="${otherLicensingData.licenseName}"/></div>
				</td>
			</tr>
			<tr class="spdx-full">
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.4 License Cross Reference</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${otherLicensingData.licenseCrossRefs}"/></p>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">6.5 License Comment</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${otherLicensingData.licenseComment}"/></p>
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
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.1 Relationship</div>
					<div class="spdx-col-2 spdx-flex-col">
						<div class="spdx-flex-row">
							<div class="spdx-col-1"><sw360:out value="${relationshipsData.spdxElementId}"/></div>
							<div class="spdx-col-1 spdx-flex-row"><sw360:out value="${relationshipsData.relationshipType}"/></div>
							<div class="spdx-col-3"><sw360:out value="${relationshipsData.relatedSpdxElement}"/></div>
						</div>
					</div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">7.2 Relationship Comment</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${relationshipsData.relationshipComment}"/></p>
				</td>
			</tr>
		</core_rt:forEach>
	</tbody>
</table>

<core_rt:set var="annotations" value="${spdxDocument.annotations}" />
<table class="table label-value-table spdx-table spdx-full" id="Annotations">
	<thead class="spdx-thead">
		<tr>
			<th>8. Annotations</th>
		</tr>
	</thead>
	<tbody class="section" data-size="5">
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1 spdx-label-index">Index</div>
					<select id="annotationSelect" class="spdx-col-2" onchange="displayIndex(this)"></select>
				</td>
			</tr>
		<core_rt:forEach items="${annotations}" var="annotationsData" varStatus="loop">
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.1 Annotator</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${annotationsData.annotator}"/></p>
					</div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.2 Annotation Date</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${annotationsData.annotationDate}"/></p>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.3 Annotation Type</div>
					<div class="spdx-col-2">
						<div class="spdx-flex-row">
							<!-- <div class="spdx-col-1 spdx-key">Organization</div> -->
							<div class="spdx-col-3"><sw360:out value="${annotationsData.annotationType}"/></div>
						</div>
					</div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.4 SPDX Identifier Reference</div>
					<div class="spdx-col-2"><sw360:out value="${annotationsData.spdxRef}"/></div>
				</td>
			</tr>
			<tr>
				<td class="spdx-flex-row">
					<div class="spdx-col-1">8.5 Annotation Comment</div>
					<p class="spdx-col-2 spdx-p"><sw360:out value="${annotationsData.annotationComment}"/></p>
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
		display:flex;
		flex-direction: row;
		overflow: auto;;
	}

	.spdx-flex-col {
		display:flex;
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
</style>

<script type="text/javascript">
	$(function() {
		$('#spdxFullMode').on('click', function(e) {
			e.preventDefault();

			$(this).addClass('btn-info');
			$(this).removeClass('btn-secondary');

			$('#spdxLiteMode').addClass('btn-secondary');
			$('#spdxLiteMode').removeClass('btn-info');

			$('.spdx-full').css('display', '');
		});

		$('#spdxLiteMode').on('click', function(e) {
			e.preventDefault();

			$(this).addClass('btn-info');
			$(this).removeClass('btn-secondary');

			$('#spdxFullMode').addClass('btn-secondary');
			$('#spdxFullMode').removeClass('btn-info');

			$('.spdx-full').css('display', 'none');
		});

		// Expand/collapse section when click on the header
		$('thead').on('click', function() {
			if ($(this).next().css('display') == 'none') {
				$(this).next().css('display', '');
			} else {
				$(this).next().css('display', 'none');
			}
		});

		$('.spdx-table select').change();
	});

	function generateSelecterOption(selectId, length) {
        for (var i = 1; i <= length; i++) {
            var option = document.createElement("option");
            option.text = i;
            document.getElementById(selectId).add(option);
        }
    }
	generateSelecterOption('snippetInfoSelect', "${snippets.size()}");
	generateSelecterOption('otherLicensingSelect', "${otherLicensing.size()}");
	generateSelecterOption('relationshipSelect', "${relationships.size()}");
	generateSelecterOption('annotationSelect', "${annotations.size()}");
	generateSelecterOption('externalReferenceSelect', "${package.externalRefs.size()}");

	function displayIndex(el) {
		var index = $(el).val();
		var section = $(el).closest('.section');
		var size = section.data()['size'];

		section.children().css('display', 'none');
		section.children().eq(0).css('display', '');

		for (var i = 0; i < size; i++) {
			section.children().eq(size * (index - 1) + i + 1).css('display', '');
		}
	}
</script>