## SW360 Multi-Language System

### Introduction

SW360 multi-language supports multiple languages. In this way, SW360 can be internationalized. With each localization, users can select the appropriate language and contribute additional local translations.

### Language Extension

By default, the SW360 language is English. Language extensions are added and are based on English. Each language has a specific language extension for the locale.

A language extension contains a key-value pair:
* Key: Used to retrieve the value and replace the corresponding text in the source code.
* Value: The local language translation.

### How to Create a New Language

#### Add New Language Extension

Add a new language properties file in the folder:
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


Create any translations you want in additional language properties files (based on `Language.properties`) and append the locale ID to the file name (`Language_xx.properties`).

Example:

* Language.properties

```
   
* Language_vi.properties

```

* Language_vi.properties

```
    welcome.to.sw360=Chào mừng bạn đến với SW360!
```

### How to Deploy

Refer to the document:
https://github.com/eclipse/sw360/wiki/Deploy-Natively

### Setting Page Languages

After deploying SW360 successfully:

1. Open the panel on the left side by clicking the button at the top left.
2. Go to SW360 -> Site Builder -> Pages > Public Pages.
   * Click each portlet one by one.
   * Click the "Show Actions" icon.
   * Click "Configure".
3. On the left side of the "Name" field, select your language.
4. Enter the translation based on the default language.
5. Click "Save".
6. Repeat steps 1–5 for Private Pages.

**Note:**

* Some languages may not be available and need to be added:
  - Go to Control Panel > Configuration > Instance Settings.
  - Click "Localization".
  - Select the available language and move it to the current languages list.
  - Click "Save".
* Some portlets contain sub-portlets, for example, "Projects".

### Setting User Language

1. Go to Account Settings.
2. Select a language.
3. Click "Save".

### Export LAR Files

If you want to share LAR files, export them to the folder:
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

To export LAR files:

1. Go to SW360 > Publishing > Export.
2. Click the "+" icon.
3. Select "Page Options" and "Pages to Export".
4. Click "Export".
5. Repeat steps 1–4 for Public Pages.

**Note:**

* Page Options and Pages to Export should be selected as follows:
  - Private_Pages
  - Public_Pages
  - Private_Pages_BDP_Import
  - Private_Pages_WS_Import

Now, the new language is added to SW360.



