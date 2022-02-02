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
import { Component } from "@angular/core";
import { TitleService } from "../services/title.service";
import { AppState } from '../../../app.state';
import { selectIsAuth } from '../../auth/auth.state';
import { Store } from "@ngrx/store";

@Component({
    selector: 'sw-layout',
    templateUrl: './layout.component.html',
    styleUrls: ['./layout.component.scss']
})
export class LayoutComponent {
    
    title: string;
    showSidebar = true;
    isAuth = false;

    constructor(private store: Store<AppState>, private titleService: TitleService) {
        store.select(selectIsAuth).subscribe(isAuth => this.isAuth = isAuth);
        titleService.title$.subscribe(title => this.title = title);
    }
}
