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
	<div id="search-users-div" data-title="<liferay-ui:message key="search.users" />" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">
                    <div id="truncationAlerter" class="alert alert-warning" role="alert" style="display: none;">
                        <liferay-ui:message key="output.limited.to.100.results.please.narrow.your.search" />
                    </div>

                    <form>
                        <div class="row form-group">
                            <div class="col-6">
                                <input type="text" name="search" id="search-text" placeholder="<liferay-ui:message key="enter.search.text" />" class="form-control" autofocus/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="search-button"><liferay-ui:message key="search" /></button>
                                <button type="button" class="btn btn-secondary" id="reset-button"><liferay-ui:message key="reset" /></button>
                            </div>
                        </div>

                        <div id="usersearchresults">
                            <div id="search-spinner" class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only"><liferay-ui:message key="loading" /></span>
                                </div>
                            </div>

                            <table id="search-result-table" class="table table-bordered">
                                <colgroup>
                                    <col style="width: 1.7rem;" />
                                    <col style="width: 25;" />
                                    <col style="width: 25;" />
                                    <col style="width: 25;" />
                                    <col style="width: 25;" />
                                </colgroup>
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th><liferay-ui:message key="given.name" /></th>
                                        <th><liferay-ui:message key="last.name" /></th>
                                        <th><liferay-ui:message key="email" /></th>
                                        <th><liferay-ui:message key="department" /></th>
                                    </tr>
                                </thead>
                                <tbody id="search-result-table-body">
                                </tbody>
                            </table>
                        </div>
                    </form>
				</div>
			    <div class="modal-footer">
		        <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="close" /></button>
			        <button id="search-add-button" type="button" class="btn btn-primary"><liferay-ui:message key="select.users" /></button>
			    </div>
			</div>
		</div>
	</div>
</div>
