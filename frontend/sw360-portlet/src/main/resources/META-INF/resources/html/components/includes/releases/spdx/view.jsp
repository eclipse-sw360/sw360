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

<table class="table label-value-table" id="DocumentCreationInformation">
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
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">2.8 Creator</div>
				<div class="spdx-col-2">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Organization</div>
						<div class="spdx-col-3">TSDV</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Person</div>
						<div class="spdx-col-3">QuanTV</div>
					</div>
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
<table class="table label-value-table" id="PackageInformation">
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
						<div class="spdx-col-3">111</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Excluded Files</div>
						<p class="spdx-col-3 spdx-p">File 1 <br> File 2</p>
					</div>
				</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">3.10 Package Checksum</div>
				<div class="spdx-col-2">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Algorithm 1</div>
						<div class="spdx-col-3">Value 1</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Algorithm 2</div>
						<div class="spdx-col-3">Value 2</div>
					</div>
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
				<p class="spdx-col-2 spdx-p">source_information_1<br>source_information_2</p>
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
				<p class="spdx-col-2 spdx-p"><sw360:out value="${package.licenseInfoFromFiles}"/></p>
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
				<div class="spdx-col-2">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-label-index">Index</div>
						<select class="spdx-col-3">
							<option>1</option>
							<option>2</option>
						</select>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Category</div>
						<div class="spdx-col-3">category</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Type</div>
						<div class="spdx-col-3">type</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Locator</div>
						<div class="spdx-col-3">locator</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">3.22 Comment</div>
						<div class="spdx-col-3">comment</div>
					</div>
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
<table class="table label-value-table spdx-full" id="SnippetInformation">
	<thead class="spdx-thead">
		<tr>
			<th>5. Snippet Information</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select class="spdx-col-2">
					<option>1</option>
					<option>2</option>
				</select>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.1 Snippet SPDX Identifier</div>
				<div class="spdx-col-2"><sw360:out value="${spdxDocumentCreationInfo.snippets}"/></div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.2 Snippet from File SPDX Identifier</div>
				<div class="spdx-col-2">SPDXRef-xxx</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.3 & 5.4 Snippet Ranges</div>
				<div class="spdx-col-2 spdx-flex-col">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Byte</div>
						<div class="spdx-col-1 spdx-flex-row">
							<div class="spdx-col-1">310</div>
							<div class="spdx-col-1">~</div>
							<div class="spdx-col-1">420</div>
						</div>
						<div class="spdx-col-3">./src/org/spdx/parser/DOAPProject.java</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Line</div>
						<div class="spdx-col-1 spdx-flex-row">
							<div class="spdx-col-1">5</div>
							<div class="spdx-col-1">~</div>
							<div class="spdx-col-1">25</div>
						</div>
						<div class="spdx-col-3">./src/org/spdx/parser/DOAPProject.java</div>
					</div>
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.5 Snippet Concluded License</div>
				<div class="spdx-col-2">snippet_concluded_license</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.6 License Information in Snippet</div>
				<p class="spdx-col-2 spdx-p">license_information_in_snippet</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.7 Snippet Comments on License</div>
				<p class="spdx-col-2 spdx-p">snippet_comments_on_license</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.8 Copyright Text</div>
				<p class="spdx-col-2 spdx-p">copyright_text</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.9 Snippet Comments</div>
				<p class="spdx-col-2 spdx-p">snippet_comments</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.10 License List Version</div>
				<div class="spdx-col-2">license_list_version</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">5.11 License List Version</div>
				<p class="spdx-col-2 spdx-p">license_list_version</p>
			</td>
		</tr>
	</tbody>
</table>
<table class="table label-value-table" id="OtherLicensingInformationDetected">
	<thead class="spdx-thead">
		<tr>
			<th>6. Other Licensing Information Detected</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select class="spdx-col-2">
					<option>1</option>
					<option>2</option>
				</select>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.1 License Identifier</div>
				<div class="spdx-col-2">LicenseRef-xxx</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.2 Extracted Text</div>
				<p class="spdx-col-2 spdx-p">extracted_text</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.3 License Name</div>
				<div class="spdx-col-2">license_name</div>
			</td>
		</tr>
		<tr class="spdx-full">
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.4 License Cross Reference</div>
				<p class="spdx-col-2 spdx-p">license_cross_reference</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">6.5 License Comment</div>
				<p class="spdx-col-2 spdx-p">license_comment</p>
			</td>
		</tr>
	</tbody>
</table>
<table class="table label-value-table spdx-full" id="RelationshipsbetweenSPDXElements">
	<thead class="spdx-thead">
		<tr>
			<th>7. Other Licensing Information Detected</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select class="spdx-col-2">
					<option>1</option>
					<option>2</option>
				</select>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">7.1 Relationship</div>
				<div class="spdx-col-2 spdx-flex-col">
					<div class="spdx-flex-row">
						<div class="spdx-col-1">SPDX_element</div>
						<div class="spdx-col-1 spdx-flex-row">relationship_type</div>
						<div class="spdx-col-3">related_SPDX_element</div>
					</div>
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">7.2 Relationship Comment</div>
				<p class="spdx-col-2 spdx-p">license_comment</p>
			</td>
		</tr>
	</tbody>
</table>
<table class="table label-value-table spdx-full" id="Annotations">
	<thead class="spdx-thead">
		<tr>
			<th>8. Annotations</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1 spdx-label-index">Index</div>
				<select class="spdx-col-2">
					<option>1</option>
					<option>2</option>
				</select>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">8.1 Annotator</div>
				<div class="spdx-col-2 spdx-flex-col">
					<div class="spdx-flex-row">
						<div class="spdx-col-1">SPDX_element</div>
						<div class="spdx-col-1 spdx-flex-row">relationship_type</div>
						<div class="spdx-col-3">related_SPDX_element</div>
					</div>
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">8.2 Annotation Date</div>
				<p class="spdx-col-2 spdx-p">2021-08-24 12:00:00</p>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">8.3 Annotation Type</div>
				<div class="spdx-col-2">
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Organization</div>
						<div class="spdx-col-3">TSDV</div>
					</div>
					<div class="spdx-flex-row">
						<div class="spdx-col-1 spdx-key">Person</div>
						<div class="spdx-col-3">QuanTV</div>
					</div>
				</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">8.4 SPDX Identifier Reference</div>
				<div class="spdx-col-2">SPDX_identifier_reference</div>
			</td>
		</tr>
		<tr>
			<td class="spdx-flex-row">
				<div class="spdx-col-1">8.5 Annotation Comment</div>
				<p class="spdx-col-2 spdx-p">annotation_comment</p>
			</td>
		</tr>
	</tbody>
</table>

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
		})
	});
</script>