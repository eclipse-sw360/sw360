[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

# SW360: Naming a Component
---------------------------

**The name is the most important criteria to identify software components. Unfortunately there is no common naming scheme available.**

## Usage and Handling of Components

- If you create a component entry, most likely you will go ahead with a release entry, otherwise, the component stays an empty shell
- Uploading source packages / actual software as attachment makes sense at the release, not at the component
- If you have created a component and release entry, you can go ahead and assign a vendor to a release.

This very clear approach enables a number of issues, please keep the following goals in mind:

- Duplicate entries need to be removed
- Separating vendor from components names and release tags brings clarity to component naming
- Interaction with other systems is a must today. As such we need to support external ids such as the CPE standard which also implement this 3-parts separation
- Having the clear modelling of data enables better search and filtering abilities of the component catalogue.

## Checklist

- Does the component already exist on SW360 (think about possible different names)?
- What is the name of the component homepage?
- What is the name of the community? Please note that repositories like Maven, GitHub, CodePlex, - CodeGuru are not vendors in our understanding!
- How is component called on repositories like Maven, NuGet, etc.?
- Take care: use the name and not the id!
- Search SW360 for the component repository id.
- Search SW360 for all possible name variations.
- Ask your local software clearing expert for help.

## Naming a Component - Special Cases

### .Net Component from GitHub

![draft_30](SW360_NamingaComponentimage/draft_30.png) In some case it is difficult to determine the real name of a component, like for example *Microsoft Entity Framework for .Net Core (or Entity Framework Core or Aspnet EntityFrameworkCore or ASP.NET EntityFrameworkCore)*. In these cases it might be the best way to use that package name as specified on Nuget, in this case **Microsoft.EntityFrameworkCore**.

### Java Components

The name of a Java component should be how it is called by the Java community. Typically this is the name as it can be found on the project homepage or on the source code repository page.

Examples:

- 'Spring Framework' (from project home page [<span style="color:red">&#8599;</span> https://spring.io/projects](https://spring.io/projects) or also from source code repository [<span style="color:red">&#8599;</span> https://github.com/spring-projects/spring-framework](https://github.com/spring-projects/spring-framework))
- 'Spring Data Redis' (from project home page [<span style="color:red">&#8599;</span> https://spring.io/projects/spring-data](https://spring.io/projects/spring-data) or also from source code repository [<span style="color:red">&#8599;</span> https://github.com/spring-projects/spring-data-redis](https://github.com/spring-projects/spring-data-redis))
- 'Thymeleaf' (from project home page [<span style="color:red">&#8599;</span> https://www.thymeleaf.org/](https://www.thymeleaf.org/); source code repository [<span style="color:red">&#8599;</span> https://github.com/thymeleaf/thymeleaf](https://github.com/thymeleaf/thymeleaf))
- 'Thymeleaf Spring 5 Integration' (from project home page [<span style="color:red">&#8599;</span> https://www.thymeleaf.org/download.html](https://www.thymeleaf.org/download.html) or source code repository page [<span style="color:red">&#8599;</span> https://github.com/thymeleaf/thymeleaf-spring](https://github.com/thymeleaf/thymeleaf-spring) → [<span style="color:red">&#8599;</span> thymeleaf-spring5](https://github.com/thymeleaf/thymeleaf-spring/tree/3.0-master/thymeleaf-spring5)
- 'Commons Codec' (from project home page [<span style="color:red">&#8599;</span> https://commons.apache.org/proper/commons-codec/](https://commons.apache.org/proper/commons-codec/) or source code repository page [<span style="color:red">&#8599;</span> https://github.com/apache/commons-codec](https://github.com/apache/commons-codec)) [or better 'Apache Commons Codec'? But 'Apache' is already the vendor']

Do not use jar names or Gradle/Maven artifactIds, like 'spring-framework'. Main reason is that from such a name one cannot see if this component is a whole component (here the Spring Framework) or only the Java archive spring-framework-<version>.jar (which is only a subset of the Spring Framework)!

Hierarchical Java components:

Java components often consist of multiple subcomponents (typically jars) where the sources are stored in a hierarchical structure in the source code repoistory. E.g. for 'Spring Framework' there is one repository [<span style="color:red">&#8599;</span> https://github.com/spring-projects/spring-framework](https://github.com/spring-projects/spring-framework) with several sub folders for individual jars. In general for such cases there should be only one (main) component in SW360 covering all the subcomponents.

In some exceptional cases one wants to do the clearing only for one subcomponent or a subset of a hierarchical components. In such a case one can either add the name of the sub component to the component name to mark the subset (like 'Thymeleaf Spring 5 Integration' above, showing that only the Spring 5 related is covered, and not Spring 3 or 4) or one could use the name of the top level component (like 'Thymeleaf Spring Integration') and have seprate releases for the subset ('3.0.9.RELEASE Spring 5').

Identifying a (new or existing) SW360 component for a java archive:

Java developers typically have to start with a Java archive which they want to add to a product, or with the related Gradle/Maven coordinates (groupId/artifactId/version). Possible ways to identify the related component (name) are: examine the related pom.xml or the MANIFEST.MF file of the jar. There one can often find more information like the community homepage or source code repository URL from which then again to determine the component (name).

*Unfortunately SW360 does not provide any support here (besides searching for the artifactId and thus hopefully find the related component). It would be a good idea to store also the Gradle/Maven coordinates of Java binaries with the SW360 components and make them searchable (note: multiple artifactIds per component need to be supported!) and/or to also upload and store the binaries of a registerd SW360 component (or at least the file hashes) and provide additional functionality to identify an unknown binary by uploading the same to SW360.*

## Component Scope

We base software clearing for open source components on the scan of the source code. If there is only one common source code for a group of components, then it does not make sense to have a lot of distinct (sub)component that all point to a common source.

### Example

There is a Java component called Logback ([<span style="color:red">&#8599;</span> https://logback.qos.ch/](https://logback.qos.ch/)). There is only one singe source (and binary) archive available from the original authors. This archive contains three Java libraries: logback-core.jar, logback-access.jar and logback-classic.jar. In **SW360 there should be only one component Logback!** It is confusing to have also "Logback core", "logback-core", "logback core", "logback classic" and "logback-classic".

## Naming a Component – <span style="color:red">Bad Examples</span>

### Json.Net

There is a component that is available on NuGet by the name 'Json.NET' and the id 'Newtonsoft.Json'. On the component homepage [<span style="color:red">&#8599;</span> http://www.newtonsoft.com/json](http://www.newtonsoft.com/json) it is called 'Json.NET'.

Just some examples of naming and how it could be improved:

- 14 x Vendor = 'Open Source Software', Name = 'Json.NET' => **wrong**!
- 1 x Vendor = 'Newtonsoft', Name = 'Json.NET (COTS)' => **wrong**!
- 2 x Vendor = 'NuGet Gallery', Name = Json.NET' => **wrong**!
- 1 x Vendor = 'CodePlex', Name = Json.NET' => **wrong**!
- 4 x Vendor = 'Open Source Software', Name = 'Newtonsoft Json.NET' => **wrong**!

The proper identification (Vendor = 'Newtonsoft', Name = 'Json.NET') has to be used!

### Oracle JavaBeans Activation Framework

Just some examples of naming and how it could be improved:

- 3 x Vendor = 'Open Source Software', Name = 'Activation' => **wrong**!
- 3 x Vendor = 'Open Source Software', Name = 'Oracle JavaBeans Activation Framework'

### Oracle Java Mail

Just some examples of naming and how it could be improved:

- 3 x Vendor = 'Open Source Software', Name = 'Mail' => **wrong**!
- 5 x Vendor = 'Open Source Software', Name = 'Oracle JavaMail API' => **wrong**!
- 4 x Vendor = 'Oracle', Name = 'Oracle JavaMail API'

### Moment.js

Just some examples of naming and how it could be improved:

- 7 x Vendor = 'GitHub', Name = 'moment' => **wrong**!
- 2 x Vendor = 'Open Source Software', Name = 'moment' => **wrong**!
- 2 x Vendor = 'Open Source Software', Name = 'Moment JS' => **wrong**!
- 3 x Vendor = 'Open Source Software', Name = 'MomentJS' => **wrong**!
- 3 x Vendor = 'Open Source Software', Name = 'Moment.js'

Just look on the community homepage: there is the name in bold letters:
Moment.js – consider this name.
