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
import { Injectable } from "@angular/core";
import { BehaviorSubject ,  Observable } from "rxjs";

@Injectable()
export class TitleService {

    titleSubject: BehaviorSubject<string> = new BehaviorSubject<string>("Welcome to SW360!");
    title$: Observable<string> = this.titleSubject.asObservable();
    
    lastId: string;
    registerSetAsync(id: string, placeholder?: string) {
      this.lastId = id;
      if (placeholder)
        this.titleSubject.next(placeholder);
    }
  
    setAsync(id: string, title: string) {
      if (this.lastId === id)
        this.titleSubject.next(title);
    }

    setSync(id: string, title: string) {
      this.lastId = id;
      this.titleSubject.next(title);
    }
  
}
