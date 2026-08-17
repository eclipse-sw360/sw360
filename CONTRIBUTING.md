# Contributions are Welcome!

Contributions are welcome, we are happy for all contributions.
You could contribute in many forms, such as with bug reports,
documentations or presentations. In case you would like to make
contributions to this repository, you would need to meet the
contribution guidelines which help to maintain a good level
of quality for contributions.

## Eclipse Development Process

This Eclipse Foundation open project is governed by the Eclipse Foundation
Development Process and operates under the terms of the Eclipse IP Policy.

* https://eclipse.org/projects/dev_process
* https://www.eclipse.org/org/documents/Eclipse_IP_Policy.pdf

## Eclipse Contributor Agreement

In order to be able to contribute to Eclipse Foundation projects you must
electronically sign the Eclipse Contributor Agreement (ECA).

* https://www.eclipse.org/legal/ECA.php

The ECA provides the Eclipse Foundation with a permanent record that you agree
that each of your contributions will comply with the commitments documented in
the [Developer Certificate of Origin](https://www.eclipse.org/legal/DCO.php)
(DCO). Having an ECA on file associated with the email address matching the
"Author" field of your contribution's Git commits fulfils the DCO's requirement
that you sign-off on your contributions.

For more information, please see the Eclipse Committer Handbook:
https://www.eclipse.org/projects/handbook/#resources-commit

## Terms of Use

This repository is subject to the Terms of Use of the Eclipse Foundation

* https://www.eclipse.org/legal/termsofuse.php

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
* make are that all files have a proper license header (see below)
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

In our project documentation and wiki we have more information:

* Definition of Done and code style considerations: https://github.com/eclipse-sw360/sw360/wiki/Dev-DoD-and-Style
* Commit message format: https://github.com/eclipse-sw360/sw360/wiki/Dev-Semantic-Commits
* Project Documentation: https://eclipse.dev/sw360/docs/

## License Header

Please make sure any file you newly create contains a proper license header like this:

```
/*
 * Copyright (c) {date} {owner}[ and others]. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
```
You should, of course, adapt this header to use the specific mechanism for comments pertaining to the type of file you create, e.g. using something like

```
#
# Copyright (c) {date} {owner}[ and others]. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v. 2.0 which is available at
# https://www.eclipse.org/legal/epl-2.0.
#
# SPDX-License-Identifier: EPL-2.0
#
```

for configuration files.

## Contact and More Links

For contacting the projects, please consider the following channels:

* The eclipse-maintained mailing list: `sw360-dev@eclipse.org`
* https://accounts.eclipse.org/mailing-list/sw360-dev
* The slack channel and more coordinates can be found here: https://eclipse.dev/sw360/
* Issue Tracker: https://github.com/eclipse-sw360/sw360/issues

Then, you find more links useful about contributing and writing code:

* Eclipse Foundation git contribution guidelines: https://wiki.eclipse.org/Development_Resources/Contributing_via_Git
