
## SW360 Multi-Language System

### Introduction

SW360 multi-language can support multiple languages. In this way, SW360 can internationalize. With each localization, user can select the appropriate language and contribute more local translations.

### Language Extension 

By the default SW360 language is English, language extensions are added and based on English.
With each language, it will have a special language extension that is language for local.

Language extension contains a pair key and value:
* Key: used to get value of key and replace text to key in source code.
* Value: the local language translation.

### How to create new language

#### Add new language extension

Add new language properties file in the folder 
`${SW360_REPOSITORY}/frontend/sw360-portlet/src/main/resources/content`

Example:

```     
    resources
        |__content
            |__Language.properties
            |__Language_vi.properties
            |__Language_ja.properties
            |__...

```

Create any translations you want in additional language properties files ( base on Language.properties ), append the locale’s ID to the file name (`Language_xx.properties`).

Example:

* Language.properties

```
    welcome.to.sw360=Welcome to SW360!
```

* Language_vi.properties

```
    welcome.to.sw360=Chào mừng bạn đến với SW360!
```

### How to deploy

Refer to the document:
https://github.com/eclipse/sw360/wiki/Deploy-Natively

### Setting page languages

After deploy SW360 success:

1. Open the panel on the left side by clicking the button at the top left.
2. Go to SW360 -> Site Builder -> Pages > Public Pages.
    * Press one by one portlet.
    * Click icon "Show actions".
    * Click "Configure".
3. On the left of field "Name" select your language.
4. Enter the translation base on default language.
5. Click "Save".
6. Repeat step 1.-5. with Private Pages.

**Note:** 

* Some languages are not available so need to be added:
    - Go to Control Panel > Configuration > Instance Setting.
    - Click "Localization".
    - Select available language and move to current language.
    - Click "Save".
* Some portlets contain sub-portlet, example "Projects".
 
### Setting user language

1. Go to Account Settings.
2. Select language.
3. Click "Save".

### Export LAR files:

If you want share LAR files, export to the folder 
`${SW360_REPOSITORY}/frontend/configuration/`

Example:

```
    configuration
        |__Private_Pages.lar
        |__Public_Pages.lar
        |__Private_Pages_BDP_Import.lar
        |__Private_Pages_WS_Import.lar
        |__Private_Pages_vi.lar
        |__Public_Pages_vi.lar
        |__Private_Pages_BDP_Import_vi.lar
        |__Private_Pages_WS_Import_vi.lar
        |__...
```

Export LAR file:

1. Go to SW360 > Publishing > Export
2. Click icon "+".
3. Select "Pages Options" and "Pages to Export".
4. Click "Export".
5. Repeat step 1.-4. with Public Pages.

**Note:** 

* Pages Options and Page Export are selected follow:
    - Private_Pages
    - Public_Pages
    - Private_Pages_BDP_Import
    - Private_Pages_WS_Import

Now, new language is add to SW360.


