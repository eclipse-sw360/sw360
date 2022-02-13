-- Copyright BMW CarIT GmbH, 2021.
-- 
-- This program and the accompanying materials are made
-- available under the terms of the Eclipse Public License 2.0
-- which is available at https://www.eclipse.org/legal/epl-2.0/
-- 
-- SPDX-License-Identifier: EPL-2.0

CREATE USER fossy;
ALTER USER fossy WITH ENCRYPTED PASSWORD 'fossy';
CREATE DATABASE fossology;
GRANT ALL PRIVILEGES ON DATABASE fossology TO fossy;
