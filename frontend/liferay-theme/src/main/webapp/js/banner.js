/*
 * Copyright Siemens AG, 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

function closeBanner() {
	sessionStorage.setItem('header', true);
	document.getElementById('updateMessage').style.display = 'none';
}

function pageLoad() {
	if (sessionStorage.getItem('header') == 'true') {
		document.getElementById('updateMessage').style.display = 'none';
	}
}
window.addEventListener("DOMContentLoaded", pageLoad );
