/**
 * @license
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 * Copyright (c) Bosch.IO GmbH 2022.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import { Component, Input } from '@angular/core';

@Component({
    selector: 'sw-project-users',
    templateUrl: './project-users.component.html',
    styles: [``]
})
export class ProjectUsersComponent {
    @Input() element: any;
}
