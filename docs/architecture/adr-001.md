[//]: <> (Copyright Siemens AG, 2017.)
[//]: <> (Part of the SW360 Portal Project.)
[//]: <> ()
[//]: <> (SPDX-License-Identifier: EPL-1.0)
[//]: <> ()
[//]: <> (All rights reserved. This program and the accompanying materials)
[//]: <> (are made available under the terms of the Eclipse Public License v1.0)
[//]: <> (which accompanies this distribution, and is available at)
[//]: <> (http://www.eclipse.org/legal/epl-v10.html)

## ADR 1: Pagination in Backend

### Context
  Components and Projects pages display entire contents of the database in one table. Reading data, generating HTML 
  page, and rendering the table (which is paginated by datatables on the client) all take a long time with thousands of 
  entities.
  
  Datatables supports server-side processing for displaying and paginating data. For that, the server must be able to
  tell datatables how many rows are in the table in total and to load a page of data starting from given index.
  
  However, CouchDB cookbook strongly discourages loading of data starting from some index because of performance 
  concerns. Instead, loading data starting from a specific key should be used. This is incompatible with what datatables
  requires and also makes going to previous pages highly complicated.
  
  To support sorting of the table by multiple columns would require creating a CouchDB view per column.
  
  In addition, projects are filtered by visibility in backend after loading from CouchDB. This filtering cannot be
  implemented in CouchDB. This complicates the matters even further with regards to pagination of projects table in
  backend.

### Decision
  We will not use datatables' server-side processing as it's not worth the effort.
  
  We will load only some number of latest components by default and let the user increase that number up to all
  available components. We will make this choice sticky between sessions.
  
  We will not change the projects table for now as the users have the option of loading only the projects from their
  group and are not disturbed much by the performance of the page when all projects are displayed.
 
### Status
  Accepted

### Consequences
  Users will not see some components that are already created in the system and may try to create the "missing"
  components. Quick Filter will not help them find the component as it works on client-side only.
  To really make sure that a component is not in the system, users will have to use Advanced Search.
  
  Loading time of the components page (with default settings) will improve dramatically and will be independent of
  the total number of components in the system.
  
  Loading time of unfiltered projects table will still be slow with thousands of projects.
