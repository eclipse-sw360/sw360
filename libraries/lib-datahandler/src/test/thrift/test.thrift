/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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