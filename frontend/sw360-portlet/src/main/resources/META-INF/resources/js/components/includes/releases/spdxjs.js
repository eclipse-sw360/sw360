define('components/includes/releases/spdxjs', ['jquery'], function($) {
    function enableSection(section, state) {
        if (!state) {
          section.find('button').attr('disabled', 'disabled');
          section.find('select').attr('disabled', 'disabled');
          section.find('input').attr('disabled', 'disabled');
          section.find('textarea').attr('disabled', 'disabled');
    
          section.find('.spdx-delete-icon-main').css('cursor', 'not-allowed');
          section.find('.spdx-delete-icon-sub').css('cursor', 'not-allowed');
        } else {
          section.find('button').removeAttr('disabled');
          section.find('select').removeAttr('disabled');
          section.find('input').removeAttr('disabled');
          section.find('textarea').removeAttr('disabled');
    
          section.find('select').removeAttr('disabled');
    
          section.find('.spdx-delete-icon-main').css('cursor', 'pointer');
          section.find('.spdx-delete-icon-sub').css('cursor', 'pointer');
        }

        section.find('.always-enable').removeAttr('disabled');
    
        section.find('.spdx-radio').each(function () {
          $(this).parent().parent().find('input[type=text]').attr('disabled', 'true');
          $(this).parent().parent().find('select').attr('disabled', 'true');
          $(this).parent().parent().find('textarea').attr('disabled', 'true');
        });
        section.find('.spdx-add-button-main').removeAttr('disabled');
      }
    
      function clearSection(section) {
        section.find('input[type=text]').val('');
        section.find('textarea').val('');
        section.find('select').not('.spdx-select').prop("selectedIndex", 0).change();
        section.find('input[type=radio]').prop('checked', false);
      }
    
      function deleteMain(deleteBtn) {
        if ($(deleteBtn).css('cursor') == 'not-allowed') {
          return;
        }
        let selectbox = $(deleteBtn).prev('select');
        selectbox.find('option:selected').remove();
        if (selectbox.find('option').length == 0) {
          selectbox.attr('disabled', 'true');
          $(deleteBtn).css('cursor', 'not-allowed')
        }
        let newItem = selectbox.find('option:selected').val();
        if (typeof (newItem) == 'undefined') {
          section = selectbox.closest('.section');
          enableSection(section, false);
          clearSection(section);
        } else {
          selectbox.change();
        }
      }
    
      function deleteSub(deleteBtn) {
        if ($(deleteBtn).css('cursor') == 'not-allowed') {
          return;
        }
    
        let section = $(deleteBtn).parent().parent();
    
        if (section.find('.spdx-delete-icon-sub').length == 1) {
          $(deleteBtn).parent().css('display', 'none');
          $(deleteBtn).addClass('hidden');
        } else {
          $(deleteBtn).parent().remove();
        }
      }
    
      function addMain(addBtn) {
        let selectbox = $(addBtn).prev().find('select');
    
        let newIndex = parseInt(selectbox.find('option').last().val()) + 1;
    
        if (isNaN(newIndex)) {
          newIndex = 1;
        }
    
        selectbox.append('<option>' + newIndex + '</option>');
    
        section = selectbox.closest('.section');
    
        enableSection(section, true);
    
        clearSection(section);
    
        selectbox.val(newIndex);
      }
    
      function addSub(addBtn)  {
        if ($(addBtn).prev().css('display') == 'none') {
          $(addBtn).prev().css('display', 'flex');
          $(addBtn).prev().find('[name=delete-snippetRange]').removeClass('hidden');
          clearSection($(addBtn).prev());
          $(addBtn).prev().find('*').removeAttr('disabled');
          $(addBtn).prev().find('.spdx-delete-icon-sub').css('cursor', 'pointer');
    
          if ($(addBtn).hasClass('spdx-add-button-sub-creator')) {
            if ($('#creator-anonymous').is(':checked')) {
              $(addBtn).prev().find('.creator-type').val('Tool');
            } else {
              $(addBtn).prev().find('.creator-type').val('Organization');
            }
          }
        } else {
          let newItem = $(addBtn).prev().clone();
          clearSection(newItem)
          newItem.find('*').removeAttr('disabled');
          newItem.find('.spdx-delete-icon-sub').css('cursor', 'pointer');
    
          if ($(addBtn).hasClass('spdx-add-button-sub-creator')) {
            if ($('#creator-anonymous').is(':checked')) {
              newItem.find('.creator-type').val('Tool');
            } else {
              newItem.find('.creator-type').val('Organization');
            }
          }
    
          $(addBtn).before(newItem);
        }
      }
    
      function setAnonymous() {
        let selectboxes = $('#creator-anonymous').parent().next().find('select');
    
        if ($('#creator-anonymous').is(':checked')) {
          selectboxes.each(function (index) {
            if ($(this).val() == 'Organization' || $(this).val() == 'Person') {
              $(this).attr('disabled', 'true');
              $(this).next().attr('disabled', 'true');
              $(this).next().next().css('cursor', 'not-allowed');
            }
          });
        } else {
          selectboxes.each(function (index) {
            if ($(this).val() == 'Organization' || $(this).val() == 'Person') {
              $(this).removeAttr('disabled');
              $(this).next().removeAttr('disabled');
              $(this).next().next().css('cursor', 'pointer');
            }
          });
        }
      }
    
      function updateRadioButton(button) {
        if ($(button).attr('id') == 'FilesAnalyzedFalse' && $(button).is(':checked')) {
          $('#verificationCodeValue').attr('disabled', 'true');
          $('#excludedFiles').attr('disabled', 'true');
          return;
        }
    
        if ($(button).attr('id') == 'FilesAnalyzedTrue' && $(button).is(':checked')) {
          $('#verificationCodeValue').removeAttr('disabled');
          $('#excludedFiles').removeAttr('disabled');
          return;
        }
    
        if (button.val() == 'NONE' || button.val() == 'NOASSERTION') {
          button.parent().parent().find('input[type=text]').attr('disabled', 'true');
          button.parent().parent().find('select').attr('disabled', 'true');
          button.parent().parent().find('textarea').attr('disabled', 'true');
        } else {
          button.parent().parent().find('input[type=text]').removeAttr('disabled');
          button.parent().parent().find('select').removeAttr('disabled');
          button.parent().parent().find('textarea').removeAttr('disabled');
        }
      }
    
    
      function changeCreatorType(selectbox) {
        if ($('#creator-anonymous').is(':checked') &&
          ($(selectbox).val() == 'Organization' || $(selectbox).val() == 'Person')) {
          $(selectbox).attr('disabled', 'true');
          $(selectbox).next().attr('disabled', 'true');
          $(selectbox).next().next().css('cursor', 'not-allowed');
        }
      }
    
      function fillDateTime(datePicker, timePicker, value) {
        let timeStamp = Date.parse(value);
    
        let date = new Date(timeStamp);
    
        let localTimeStamp = timeStamp - date.getTimezoneOffset();
    
        let localDate = new Date(localTimeStamp);
    
        $(datePicker).val(localDate.getFullYear()
                  + '-' + (localDate.getMonth() + 1).toString().padStart(2, '0')
                  + '-' + localDate.getDate().toString().padStart(2, '0'));
    
        $(timePicker).val(date.getHours().toString().padStart(2, '0')
                  + ':' + date.getMinutes().toString().padStart(2, '0')
                  + ':' + date.getSeconds().toString().padStart(2, '0'));
      }
    
      function fillMultiOptionsField(inputTag, value, type = 'text') {
        if (!Array.isArray(value) && (value.toUpperCase() == 'NONE' || value.toUpperCase() == 'NOASSERTION')) {
          $(inputTag).parent().parent().find('input[value=' + value + ']').click();
        } else {
          switch (type) {
            case 'array':
              fillArray(inputTag, value);
              break;
            case 'annotator':
              fillAnnotator(inputTag, value);
              break;
            case 'text':
            default:
              $(inputTag).val(value);
              $(inputTag).prev().click();
          }
          $(inputTag).parent().parent().find('input[value=EXIST]').click();
        }
      }
    
      function fillArray(textarea, value) {
        if (Array.isArray(value)) {
          $(textarea).val(value.join('\n'));
        } else {
          $(textarea).val('');
        }
      }
    
      function fillAnnotator(typeTag, value) {
        if (value.startsWith('Organization: ')) {
          $(typeTag).val('Organization');
          $(typeTag).next().val(value.substr(14));
        } else if (value.startsWith('Person: ')) {
          $(typeTag).val('Person');
          $(typeTag).next().val(value.substr(8));
        } else if (value.startsWith('Tool: ')) {
          $(typeTag).val('Tool');
          $(typeTag).next().val(value.substr(6));
        } else {
          $(typeTag).val('Organization');
          $(typeTag).next().val('');
        }
      }
    
      function readMultiOptionField(inputTag, type = 'text') {
        if ($(inputTag).attr('disabled')) {
          return $(inputTag).parent().parent().find(':checked').val()
        } else {
          switch (type) {
            case 'array':
              return readArray(inputTag);
            case 'annotator':
              return readAnnotator(inputTag);
            case 'text':
            default:
              return $(inputTag).val().trim();
          }
        }
      }
    
      function readArray(textarea) {
        let result = $(textarea).val().split('\n');
    
        for (let i = 0; i < result.length; i++) {
          result[i] = result[i].trim();
        }
    
        return result.filter(function(e) { return e !== '' });
      }
    
      function readAnnotator(typeTag) {
        let val = $(typeTag).parent().parent().find('.spdx-radio:checked').val();
    
        if (val == 'NONE' || val == 'NOASSERTION') {
          return val;
        }
    
        if ($(typeTag).next().val().trim() != '') {
          val = $(typeTag).val() + ': ' + $(typeTag).next().val().trim();
        } else {
          val = '';
        }
    
        return val;
      }
    
      function readDateTime(datePicker, timePicker) {
        if ($(datePicker).val() == '' || $(timePicker).val() == '') {
          return '';
        }
    
        let localDate = new Date($(datePicker).val() + ' ' + $(timePicker).val());
    
        return localDate.toISOString();
      }
    
      function fillSelectbox(selectbox, num) {
        $(selectbox).find('option').remove();
    
        for (let i = 0; i < num; i++) {
          $(selectbox).append('<option>' + (i + 1).toString() + '</option>');
        }
    
        if (num > 0) {
          $(selectbox).val(1);
        }
      }

      function fillExternalRef(packageInformationObj, index) {
        let obj = packageInformationObj.externalRefs[index];

        $('#referenceCategory').val(obj['referenceCategory']);
        $('#referenceCategory').change();

        if (obj['referenceCategory'] == 'referenceCategory_security' || obj['referenceCategory'] == 'referenceCategory_packageManager') {
            $('#referenceType-1').val(obj['referenceType']);
        } else {
            $('#referenceType-2').val(obj['referenceType']);
        }

        $('#externalReferencesLocator').val(obj['referenceLocator']);
        $('#externalReferencesComment').val(obj['comment']);
      }

      function initPackageInfo(packageInformationObj) {
        if (packageInformationObj.SPDXID.startsWith('SPDXRef-')) {
          $('#packageSPDXId').val(packageInformationObj.SPDXID.substr(8));
        } else {
          $('#packageSPDXId').val('Package-' + packageInformationObj['name']);
        }
        fillMultiOptionsField('#supplierType', packageInformationObj.supplier, 'annotator');
        fillMultiOptionsField('#originatorType', packageInformationObj.originator, 'annotator');
        fillMultiOptionsField('#downloadLocationValue', packageInformationObj.downloadLocation);
        if (packageInformationObj.filesAnalyzed) {
          $('#FilesAnalyzedTrue').click();
          $('#verificationCodeValue').val(packageInformationObj.packageVerificationCode.value);
          fillArray('#excludedFiles', packageInformationObj.packageVerificationCode.excludedFiles);
        } else {
          $('#FilesAnalyzedFalse').click();
        }
        for (let i = 0; i < packageInformationObj.checksums.length; i++) {
          addSub($('.spdx-add-button-sub-checksum').first());
          $('.checksum-delete').last().bind('click', function() {
            deleteSub($(this));
          });

          let algorithm   = packageInformationObj.checksums[i].algorithm;
          let checksumValue = packageInformationObj.checksums[i].checksumValue;

          if (algorithm.startsWith('checksumAlgorithm_')) {
            $('.checksum-algorithm').last().val(algorithm.substr(18));
          } else {
            $('.checksum-algorithm').last().val('');
          }

          $('.checksum-value').last().val(packageInformationObj.checksums[i].checksumValue);
        }
        fillMultiOptionsField('#packageHomepageValue', packageInformationObj.homepage);
        fillMultiOptionsField('#licenseConcludedValue', packageInformationObj.licenseConcluded);
        fillMultiOptionsField('#licenseInfoFromFilesValue', packageInformationObj.licenseInfoFromFiles, 'array');
        fillMultiOptionsField('#licenseDeclaredValue', packageInformationObj.licenseDeclared);
        fillMultiOptionsField('#copyrightTextValue', packageInformationObj.copyrightText);
        if (packageInformationObj.externalRefs.length == 0) {
          enableSection($('.section-external-ref'), false);
        } else {
          fillSelectbox('#externalReferences', packageInformationObj.externalRefs.length);
          fillExternalRef(packageInformationObj, 0);
        }
      }

      function storePackageInfo(packageInformationObj) {
        packageInformationObj['name'] = $('#packageName').val().trim();
        if ($('#packageSPDXId').val().trim() == '') {
          packageInformationObj['SPDXID'] = 'SPDXRef-Package-' + packageInformationObj['name'];
        } else {
          packageInformationObj['SPDXID'] = 'SPDXRef-Package-' + $('#packageSPDXId').val().trim();
        }
        packageInformationObj['versionInfo'] = $('#versionInfo').val().trim();
        packageInformationObj['packageFileName'] = $('#packageFileName').val().trim();
        packageInformationObj['supplier'] = readMultiOptionField('#supplierType', 'annotator');
        packageInformationObj['originator'] = readMultiOptionField('#originatorType', 'annotator');
        packageInformationObj['downloadLocation'] = readMultiOptionField('#downloadLocationValue');
        packageInformationObj['filesAnalyzed'] = $('[name=_sw360_portlet_components_FILES_ANALYZED]:checked').val();
        if (packageInformationObj['filesAnalyzed']) {
          packageInformationObj['packageVerificationCode']['value'] = $('#verificationCodeValue').val().trim();
          packageInformationObj['packageVerificationCode']['excludedFiles'] = readArray('#excludedFiles');
        } else {
          packageInformationObj['packageVerificationCode']['value'] = '';
          packageInformationObj['packageVerificationCode']['excludedFiles'] = '';
        }
        packageInformationObj['checksums'] = [];
        $('[name=checksumRow]').each(function() {
          let algorithm = $(this).find('.checksum-algorithm').first().val().trim();
          let checksumValue = $(this).find('.checksum-value').first().val().trim();
    
          if (checksumValue != '') {
            packageInformationObj['checksums'].push({ 'algorithm': 'checksumAlgorithm_' + algorithm, 'checksumValue': checksumValue });
          }
        });
        packageInformationObj['homepage'] = readMultiOptionField('#packageHomepageValue');
        packageInformationObj['sourceInfo'] = $('#sourceInfo').val().trim();
        packageInformationObj['licenseConcluded'] = readMultiOptionField('#licenseConcludedValue');
        packageInformationObj['licenseInfoFromFiles'] = readMultiOptionField('#licenseInfoFromFilesValue', 'array');
        packageInformationObj['licenseDeclared'] = readMultiOptionField('#licenseDeclaredValue');
        packageInformationObj['licenseComments'] = $('#licenseComments').val().trim();
        packageInformationObj['copyrightText'] = $('#copyrightTextValue').val().trim();
        packageInformationObj['summary'] = $('#summary').val().trim();
        packageInformationObj['description'] = $('#description').val().trim();
        packageInformationObj['packageComment'] = $('#spdxPackageComment').val().trim();
        packageInformationObj['attributionText'] = $('#spdxPackageAttributionText').val().trim();
      }
    
      function storeExternalRef(packageInformationObj, index) {
        if (index < 0 || index > packageInformationObj.externalRefs.length - 1) {
          return;
        }

        let obj = packageInformationObj.externalRefs[index];
    
        obj['referenceCategory'] = $('#referenceCategory').val().trim();
    
        if (obj['referenceCategory'] == 'referenceCategory_security' || obj['referenceCategory'] == 'referenceCategory_packageManager') {
          obj['referenceType'] = $('#referenceType-1').val().trim();
        } else {
          obj['referenceType'] = $('#referenceType-2').val().trim();
        }
    
        obj['referenceLocator'] = $('#externalReferencesLocator').val().trim();
        obj['comment'] = $('#externalReferencesComment').val().trim();
      }

      return {
        enableSection:function (section, state) {
          return enableSection(section, state);
        },
        clearSection:function (section) {
          return clearSection(section);
        },
        deleteMain:function (deleteBtn) {
          return deleteMain(deleteBtn);
        },
        deleteSub:function (deleteBtn) {
          return deleteSub(deleteBtn);
        },
        addMain:function (addBtn) {
          return addMain(addBtn);
        },
        addSub:function (addBtn) {
          return addSub(addBtn);
        },
        updateRadioButton:function (button) {
          return updateRadioButton(button);
        },
        fillDateTime:function (datePicker, timePicker, value) {
          return fillDateTime(datePicker, timePicker, value);
        },
        fillMultiOptionsField:function (inputTag, value, type = 'text') {
          return fillMultiOptionsField(inputTag, value, type = 'text');
        },
        fillArray:function (textarea, value) {
          return fillArray(textarea, value);
        },
        fillAnnotator:function (typeTag, value) {
          return fillAnnotator(typeTag, value);
        },
        readMultiOptionField:function (inputTag, type = 'text') {
          return readMultiOptionField(inputTag, type = 'text');
        },
        readArray:function (textarea) {
          return readArray(textarea);
        },
        readAnnotator:function (typeTag) {
          return readAnnotator(typeTag);
        },
        readDateTime:function (datePicker, timePicker) {
          return readDateTime(datePicker, timePicker);
        },
        fillSelectbox:function (selectbox, num) {
          return fillSelectbox(selectbox, num);
        },
        fillExternalRef:function (packageInformationObj, index) {
          return fillExternalRef(packageInformationObj, index);
        },
        initPackageInfo:function (packageInformationObj) {
          return initPackageInfo(packageInformationObj);
        },
        storePackageInfo:function (packageInformationObj) {
          return storePackageInfo(packageInformationObj)
        },
        storeExternalRef:function (packageInformationObj, index) {
          return storeExternalRef(packageInformationObj, index)
        }
      };
});
