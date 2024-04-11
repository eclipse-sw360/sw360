/*
  ~ Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
 */
define('modules/expandCollapse', [ 'jquery' ], function($) {
    return {
        toggleIcon: function toggleIcon(element){

            if (element.closest('thead').hasClass('collapsed')) {
                element.find('.lexicon-icon-caret-bottom').hide();
                element.find('.lexicon-icon-caret-bottom').parent().hide();
                element.find('.lexicon-icon-caret-top').show();
                element.find('.lexicon-icon-caret-top').parent().show();
            }else{
                element.find('.lexicon-icon-caret-bottom').show();
                element.find('.lexicon-icon-caret-bottom').parent().show();
                element.find('.lexicon-icon-caret-top').hide();
                element.find('.lexicon-icon-caret-top').parent().hide();
            }
        }
    }
});
