/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

namespace java org.eclipse.sw360.testthrift

enum Status {
  ACTIVE,
  DELETED
}

struct TestObject {
	1: string id,
	2: string revision,
	3: string name,
	4: string text
}

service TestService {

   TestObject test(1:TestObject user)
}