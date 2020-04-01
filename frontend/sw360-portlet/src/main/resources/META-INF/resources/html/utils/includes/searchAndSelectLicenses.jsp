<%--
  ~ Copyright Siemens AG, 2017-2019. Part of the SW360 Portal User.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<div class="dialogs">
	<div id="search-licenses-div" data-title="<liferay-ui:message key="search.licenses" />" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">
                    <form>
                        <div class="row form-group">
                            <div class="col-6">
                                <input type="text" name="search" id="search-licenses-text" placeholder="<liferay-ui:message key="enter.search.text" />" class="form-control" autofocus/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="search-licenses-button"><liferay-ui:message key="search" /></button>
                                <button type="button" class="btn btn-secondary" id="reset-licenses-button"><liferay-ui:message key="reset" /></button>
                            </div>
                        </div>

                        <div id="usersearchresults">
                            <div id="search-spinner" class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only"><liferay-ui:message key="loading" /></span>
                                </div>
                            </div>

                            <table id="search-licenses-result-table" class="table table-bordered">
                                <colgroup>
                                    <col style="width: 1.7rem" />
                                    <col />
                                </colgroup>
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th><liferay-ui:message key="license" /></th>
                                    </tr>
                                </thead>
                                <tbody id="search-licenses-result-table-body">
                                </tbody>
                            </table>
                        </div>
                    </form>
				</div>
			    <div class="modal-footer">
		        <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="close" /></button>
			        <button id="search-add-licenses-button" type="button" class="btn btn-primary"><liferay-ui:message key="select.licenses" /></button>
			    </div>
			</div>
		</div>
	</div>
</div>
