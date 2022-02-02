## SW360 client

#### How to run the app locally?

1. Install [nodejs](https://nodejs.org/en/)
2. In a terminal, enter `npm install -g @angular/cli`. It installs the most recent Angular version via npm globally.
3. In a terminal, open the **app directory** which is the root directory containing `src, e2e` etc.
4. Enter `npm install`. This installs the apps dependencies.
5. Enter `ng serve -o`. A browser tab should open, if not follow terminal instructions.

Using `ng-serve` the app locally runs with live reload against file changes.

#### Deploy

To deploy a static version enter `ng build --prod` in the **app directory**. This generates a `dist` folder containing the static app.

If anything fails, leave me an issue :)
