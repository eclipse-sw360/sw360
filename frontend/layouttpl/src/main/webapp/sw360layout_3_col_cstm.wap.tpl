<!-- 
  Copyright Siemens AG, 2013-2014. Part of the SW360 Portal Project.
 
  SPDX-License-Identifier: EPL-1.0

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 
  Authors: nunifar.ms@siemens.com, puspa.panda@siemens.com, 
    cedric.bodet.ext@siemens.com, johannes.najjar.ext@siemens.com
  
  Description: Page layout file for custom 3-column layout
-->
<style type="text/css">
#layout-column_column-3 {
	border-left: 2px solid #cccccc;
	padding-left:25px;
	margin-left:20px;
}
</style>

<div class="sw360layout_3_col_cstm" id="main-content" role="main">
   <div class="portlet-layout row-fluid">
      <div class="portlet-column portlet-column-first span5" id="column-1">
         $processor.processColumn("column-1", "portlet-column-content portlet-column-content-first")
      </div>

      <div class="portlet-column span5" id="column-2">
         $processor.processColumn("column-2", "portlet-column-content")
      </div>

      <div class="portlet-column portlet-column-last span2" id="column-3">
         $processor.processColumn("column-3", "portlet-column-content portlet-column-content-last")
      </div>
   </div>
</div>