<%--
  ~ Copyright Siemens AG, 2017-2019. Part of the SW360 Portal User.
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

<div class="dialogs">
	<div id="search-licenses-div" data-title="Search Licenses" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">
                    <form>
                        <div class="row form-group">
                            <div class="col-6">
                                <input type="text" name="search" id="search-licenses-text" placeholder="Enter search text..." class="form-control" autofocus/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="search-licenses-button">Search</button>
                                <button type="button" class="btn btn-secondary" id="reset-licenses-button">Reset</button>
                            </div>
                        </div>

                        <div id="usersearchresults">
                            <div id="search-spinner" class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only">Loading...</span>
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
                                        <th>License</th>
                                    </tr>
                                </thead>
                                <tbody id="search-licenses-result-table-body">
                                </tbody>
                            </table>
                        </div>
                    </form>
				</div>
			    <div class="modal-footer">
		        <button type="button" class="btn btn-light" data-dismiss="modal">Close</button>
			        <button id="search-add-licenses-button" type="button" class="btn btn-primary">Select Licenses</button>
			    </div>
			</div>
		</div>
	</div>
</div>
