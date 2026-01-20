# Contributions are Welcome!

Contributions are welcome, we are happy for all contributions.
You could contribute in many forms, such as with bug reports,
documentations or presentations. In case you would like to make
contributions to this repository, you would need to meet the
contribution guidelines which help to maintain a good level
of quality for contributions.

## Eclipse Contributor Agreement

Before we can accept your contribution, you must
electronically sign the Eclipse Contributor Agreement (ECA).

* http://www.eclipse.org/legal/ECA.php

Commits that are provided by non-committers must have a Signed-off-by field in
the footer indicating that the author is aware of the terms by which the
contribution has been provided to the project. The non-committer must
additionally have an Eclipse Foundation account and must have a signed Eclipse
Contributor Agreement (ECA) on file.

For more information, please see the Eclipse Committer Handbook:
https://www.eclipse.org/projects/handbook/#resources-commit

## Basic D-o-D

Please see below the link for our definition of done, but in a nutshell:

* do not break any test
* add a test if it makes sense
* all new files have license and copyright information (see below)
* in existing files, with relevant contribution, you have added your copyright information
* commit style is OK (see below: semantic commits)

## Preparing your contribution

We work with the pull requests of Github.com in order to

* provide transparency for what is merged
* provide a description of the contribution
* allow discussions
* use the review feature
* provide the results of the CI run
* provide the results of the Eclipse IP check
* ... and presumably it has more advantages.

As a consequence, your very welcomed code contributions could be provided as pull requests.  We use the feature branch workflow (cf. https://guides.github.com/introduction/flow/), you could consider the following approach:

* fork our repository in your space
* create a new branch for your contribution
* apply your contribution to the new branch
* make sure that all files have a proper license header (see below)
* make sure you include tests for testable stuff
* very important: all existing tests need to pass after your changes
* commit your changes into that branch
  * use the signed commit (option `-s`)
  * use the conventional change log style for the commit message, for example. Example:`feat(rest): add endpoint for getting the authors of sw360`
* Squash multiple commits to a useful and self-container unit
  * idea is that others can cherry pick easily your functionality
* Push your changed branch to your upstream fork
* Create a pull request at our project from your fork.

## After Submitting: Pull Request is Open

After you have opened your pull requests, please have a look directly after some hours if the CI and the Eclipse IP check was successful. if not, pls. consider applying changes to "make the echeks green" before others are looking at your PR

From time to time, you could consider check again your PR, if the developers would have questions or if a discussion in your pull request in going on.

## Merging your Contribution

If everything is all right, the contribution can be merged by one of the committers. We have the following guidelines for merging (accepting) pull requests:

* generally, the proposed contribution shall be useful
* the continuous integration ran successfully
* the Eclipse IP check is OK
* code review is good
* testing was successful
* Definition of done is met


**To Committers**: If the merge commit is made, please consider to add the test and review information:

```
review-by:email@domain.com
```
and
```
tested-by:email@domain.com
```


## Further Resources

In our project wiki we have some more information

* Definition of Done and code style considerations: https://github.com/eclipse/sw360/wiki/Dev-DoD-and-Style
* Commit message format: https://github.com/eclipse/sw360/wiki/Dev-Semantic-Commits


## License Header

Please make sure any file you newly create contains a proper license header like this:

````
/*
 * Copyright Your Orgnisation, 202X. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
````
You should, of course, adapt this header to use the specific mechanism for comments pertaining to the type of file you create, e.g. using something like

````
#
# Copyright Your Orgnisation, 202X. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
````

for configuration files.

## Contact and More Links

For contacting the projects, please consider the following channels:

* The eclipse-maintained mailing list: `sw360-dev@eclipse.org`
* The slack channel and more coordinates can be found here: https://github.com/eclipse/sw360/wiki#getting-started

Then, you find more links useful about contributing and writing code:

* Eclipse Foundation git contribution guidelines: https://wiki.eclipse.org/Development_Resources/Contributing_via_Git
