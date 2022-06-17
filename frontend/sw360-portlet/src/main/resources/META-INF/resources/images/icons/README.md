# Icons

SVG files should be used wherever possible since they can be colored via CSS.
This also allows disabling icons by just setting an opacity.

All icons are packed together inside `icons.svg`. This way, they can be included
as follows, e.g. for the `fossology` icon.
```
<svg>
    <title>FOSSology</title>
    <use href="<%=request.getContextPath()%>/images/icons.svg#fossology"/>
</svg>
```
**Note**: The `title` attribute does not work on `svg`-tags therefore we use the `title`-tag here.

You may also use a custom tag when inside an JSP-file:
```
<sw360:icon title="FOSSology" icon="fossology" className="<custom class names>" />
```


## Add a new icon

To add a new icon, you have to create it via your preferred tool first. Save it in format `svg`.
If you do not know any tool, you may try [Inkscape](http://www.inkscape.org).

Afterwards you have to repack `icons.svg`. The easiest way would be to use the following site:
https://icomoon.io/app/#/select

1.  Click on `Import icons` at the top.
1.  Select all icons to pack. These should be all files ending with `.svg` except `icons.svg` itself as well as template files ending with `_template.svg`.
1.  Now select all imported icons by clicking at them.
1.  Click on `Generate SVG & More` on the bottom left.
1.  Use the `cog`-icon to configure the export. No option have to be checked.
1.  Close the dialog and click on `Download` on the bottom left.
1.  In the downloaded archive, extract the file `symbol-defs.svg` and copy its content to `icons.svg`.
1.  Remove the `icon-` prefix in the `id`-attribute of the `symbol`-tags inside `icons.svg`.
1.  Remove all `title` tags
