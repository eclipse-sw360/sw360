<%--
  ~ Copyright Siemens AG, 2017-2018. Part of the SW360 Portal User.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="search-licenses-div" title="Search" style="display: none; background-color: #ffffff;">
    <div style="display: inline-block">
        <input type="text" name="search" id="search-licenses-text" placeholder="search" class="searchbar"/>&nbsp;
        <input type="button" value="Search" id="search-licenses-button" class="searchbutton"/>
        <input type="button" value="Reset" id="reset-licenses-button" class="resetbutton"/>
    </div>

    <div class="usersearchresults">
        <table width="100%" id="search-licenses-result-table">
            <thead style="border-bottom: 2px solid #66c1c2;">
            <tr class="trheader" style="height: 30px;">
                <th width="10%">&nbsp;</th>
                <th width="90%" class="textlabel" align="left">License</th>
            </tr>
            </thead>
            <tbody id="search-licenses-result-table-body">
            <tr class="trbodyClass">
                <td></td><td></td>
            </tr>
            </tbody>
        </table>
    </div>
    <hr noshade size="1" style="background-color: #66c1c2; border-color: #59D1C4;"/>
    <br/>
    <div>
        <input type="button" value="Select" id="search-add-licenses-button" class="addButton"/>
    </div>
</div>
