<!--
  ~ Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->

<div class="columns-2-sidebar" id="main-content" role="main">
	<div class="portlet-layout row">
		<div class="col-10">
			<div class="row">
				<div id="column-1" class="col-6 portlet-column">
					${processor.processColumn("column-1", "portlet-column-content")}
				</div>
				<div id="column-2" class="col-6 portlet-column">
					${processor.processColumn("column-2", "portlet-column-content")}
				</div>
			</div>
			<div class="row">
				<div id="column-3" class="col-6 portlet-column">
					${processor.processColumn("column-3", "portlet-column-content")}
				</div>
				<div id="column-4" class="col-6 portlet-column">
					${processor.processColumn("column-4", "portlet-column-content")}
				</div>
			</div>
		</div>
	    <div id="column-5" class="col-2 portlet-column">
	        ${processor.processColumn("column-5", "portlet-column-content")}
	    </div>
	</div>
</div>
