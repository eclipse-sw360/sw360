import { Injectable } from '@angular/core';
import { ResourceAction } from '../../../core/resources/models/resources';

@Injectable()
export class ComponentService {

    constructor() { }

    componentSpecififShowcaseAction() {
        return () => console.log("This is how you specify a component secific action!");
    }

    getActions(): ResourceAction[] {
        return [
            {
                title: 'Component specific action (mock)',
                action: this.componentSpecififShowcaseAction(),
                matIconString: 'grade',
                noneSelected: true,
                oneSelected: true,
                manySelected: true
            },
        ];
    }
}
