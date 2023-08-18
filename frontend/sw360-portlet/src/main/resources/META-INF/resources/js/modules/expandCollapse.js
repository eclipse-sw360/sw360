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
                element.find("div").eq(3).children().css('display', 'none');
                element.find("div").eq(2).children().css('display', 'inline');
            }else{
                element.find("div").eq(3).children().css('display', 'inline');
                element.find("div").eq(2).children().css('display', 'none');
            }
        }
    }
});
