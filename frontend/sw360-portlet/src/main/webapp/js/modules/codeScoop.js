/*
 * Copyright Siemens AG, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define("modules/codeScoop", [], function() {
    function codeScoop(apiUrl, apiToken) {

        var _this = this;

        if (!apiUrl || !apiToken) {
            throw "CodeScoop API init error: please set codescoop.url & codescoop.token in ~/frontend/sw360-portlet/src/main/resources/sw360.properties";
        }

        if (apiUrl.lastIndexOf("/") !== apiUrl.length - 1) {
            apiUrl = apiUrl + "/";
        }

        this.apiUrl = apiUrl;

        this.apiToken = apiToken;

        this.formElements = {
            form: undefined,
            name: undefined,
            categories: undefined,
            homepage: undefined,
            blog: undefined,
            wiki: undefined,
            mailing: undefined,
            description: undefined,
            componentList: undefined
        };

        this.indexTable = undefined;

        this.indexData = undefined;

        this.timeout = undefined;

        this.indexDone = false;

        this.interval = 200;

        this.installFormElement = function () {
            _this.formElements.form = document.getElementsByTagName("form")[0];
            var inputs = _this.formElements.form.getElementsByTagName("input");
            for (var i = 0; i < inputs.length; i++) {
                if (inputs[i].name.indexOf("NAME") > 0) {
                    _this.formElements.name = inputs[i];
                }
                if (inputs[i].name.indexOf("CATEGORIES") > 0) {
                    _this.formElements.categories = inputs[i];
                }
                if (inputs[i].name.indexOf("HOMEPAGE") > 0) {
                    _this.formElements.homepage = inputs[i];
                }
                if (inputs[i].name.indexOf("BLOG") > 0) {
                    _this.formElements.blog = inputs[i];
                }
                if (inputs[i].name.indexOf("WIKI") > 0) {
                    _this.formElements.wiki = inputs[i];
                }
                if (inputs[i].name.indexOf("MAILINGLIST") > 0) {
                    _this.formElements.mailing = inputs[i];
                }
                if (inputs[i].name.indexOf("MAILINGLIST") > 0) {
                    _this.formElements.mailing = inputs[i];
                }
            }
            var textAreas = _this.formElements.form.getElementsByTagName("textarea");
            for (var i = 0; i < textAreas.length; i++) {
                if (textAreas[i].name.indexOf("DESCRIPTION") > 0) {
                    _this.formElements.description = textAreas[i];
                }
            }
        };

        this.installAutocompleteBox = function () {
            _this.formElements.componentList = document.createElement("div");
            _this.formElements.componentList.id = "codescoop-autocomplete";
            _this.formElements.componentList.style.display = "none";
            _this.formElements.componentList.style.height = "auto";
            _this.formElements.componentList.style.minHeight = "20px";
            _this.formElements.componentList.style.color = "#555";
            _this.formElements.componentList.style.backgroundColor = "#FFF";
            _this.formElements.componentList.style.fontSize = "14px";
            _this.formElements.componentList.style.fontWeight = "100";
            _this.formElements.componentList.style.padding = "4px 6px";
            _this.formElements.componentList.style.border = "1px solid #DDD";
            _this.formElements.componentList.style.position = "absolute";
            _this.formElements.componentList.style.zIndex = "99999999";

            var rect = _this.formElements.name.getBoundingClientRect(),
                scrollLeft = window.pageXOffset || document.documentElement.scrollLeft,
                scrollTop = window.pageYOffset || document.documentElement.scrollTop;

            _this.formElements.componentList.style.left = parseInt(rect.left + scrollLeft).toString() + "px";
            _this.formElements.componentList.style.top = parseInt(rect.top + scrollTop + _this.formElements.name.offsetHeight).toString() + "px";
            _this.formElements.componentList.style.width = parseInt(_this.formElements.name.offsetWidth - 13).toString() + "px";

            document.body.appendChild(_this.formElements.componentList);
        };

        this.listenAutocomplete = function () {

            var autoFillForm = function(id) {
                var ownerName = id.split("/");
                var xhr = new XMLHttpRequest();
                xhr.open("GET", _this.apiUrl + "repository/" + ownerName[0] + "/" + ownerName[1] + "/", true);
                xhr.setRequestHeader("X-Api-Key", _this.apiToken);
                xhr.setRequestHeader("X-User-Login", "test");
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 4 && xhr.status === 200) {
                        var repo = JSON.parse(this.responseText);
                        _this.formElements.description.innerHTML = repo.title;
                        _this.formElements.homepage.value = repo.url;
                        _this.formElements.blog.value = repo.url;
                        _this.formElements.wiki.value = repo.url;
                        _this.formElements.mailing.value = repo.url;
                        _this.formElements.categories.value = repo.categories.join(',');
                    }
                };
                xhr.send(null);
            };

            var clean = function() {
                _this.formElements.componentList.removeEventListener("click", select);
                _this.formElements.componentList.innerHTML = "";
                _this.formElements.componentList.style.display = "none";
            };

            var select = function(e) {
                if (e.target.nodeName === "P") {
                    var id = e.target.innerHTML;
                    _this.formElements.name.value = id;
                    clean();
                    autoFillForm(id);
                }
            };

            _this.formElements.name.addEventListener("keyup", function () {
                if (_this.formElements.name.value.length < 2) {
                    return;
                }
                var xhr = new XMLHttpRequest();
                xhr.open("GET", _this.apiUrl + "autocomplete?data=repo&limit=5&search=" + _this.formElements.name.value, true);
                xhr.setRequestHeader("X-Api-Key", _this.apiToken);
                xhr.setRequestHeader("X-User-Login", "test");
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 4 && xhr.status === 200) {
                        clean();
                        var repoList = JSON.parse(this.responseText);

                        if (repoList.length > 0) {
                            for (var i = 0; i < repoList.length; i++) {
                                var repoID = repoList[i].owner + "/" + repoList[i].name;
                                var div = document.createElement("div");
                                var p = document.createElement("p");
                                p.innerHTML = repoID;

                                div.style.cursor = "pointer";
                                div.style.margin = "0 0 3px 0";

                                div.appendChild(p);
                                _this.formElements.componentList.appendChild(div);
                            }

                            _this.formElements.componentList.style.display = "block";
                            _this.formElements.componentList.addEventListener("click", select);
                        }
                    }
                };
                xhr.send(null);
            });
        };

        this.activateAutoFill = function() {
            _this.installFormElement();
            _this.installAutocompleteBox();
            _this.listenAutocomplete();
        };

        this.activateIndexes = function(tableID, tableData) {
            var requestData = [];
            _this.indexTable = document.getElementById(tableID);

            for (var i = 0; i < tableData.length; i ++) {
                var swComponent = tableData[i];
                requestData.push({
                    id: swComponent.DT_RowId,
                    vendor: swComponent.vndrs,
                    name: swComponent.name
                });
            }

            var xhr = new XMLHttpRequest();
            xhr.open("POST", this.apiUrl + "integration/siemens/composite" , true);
            xhr.setRequestHeader("X-Api-Key", this.apiToken);
            xhr.setRequestHeader("X-User-Login", "test");
            xhr.setRequestHeader("Content-type", "application/json");
            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    var responseData = JSON.parse(this.responseText);
                    _this.indexData = {};
                    for (var i = 0; i < responseData.length; i ++) {
                        var data = responseData[i];
                        _this.indexData[data.id] = data;
                    }
                }
            };
            xhr.send(JSON.stringify(requestData));
        };

        this.updateIndexes = function () {
            if (_this.indexDone) {
                return;
            }

            var updateAction = function () {
                if (Object.keys(_this.indexData).length === 0) {
                    if (_this.timeout) {
                        clearTimeout(_this.timeout);
                    }
                    _this.timeout = setTimeout(updateAction,  _this.interval);
                }
                var trs = _this.indexTable.getElementsByTagName("tbody")[0].getElementsByTagName("tr");
                for (var i = 0; i < trs.length; i++) {
                    var tr = trs[i];
                    var composite = _this.indexData[tr.getAttribute("id")].compositeIndex;
                    if (composite) {
                        var box = document.createElement("div");
                        box.style.float = "right";
                        box.style.width = "85px";

                        var pI = document.createElement("div");
                        pI.style.color = "rgb(1, 219, 187)";
                        pI.innerText = "interest: " + composite.interestPercent + "%";
                        box.appendChild(pI);

                        var pA = document.createElement("div");
                        pA.style.color = "rgb(237, 81, 56)";
                        pA.innerText = "activity: " + composite.activityPercent + "%";
                        box.appendChild(pA);

                        var pH = document.createElement("div");
                        pH.style.color = "rgb(7, 180, 0)";
                        pH.innerText = "health: " + composite.healthPercent + "%";
                        box.appendChild(pH);

                        tr.getElementsByTagName("td")[1].appendChild(box);
                    }
                }
                _this.indexDone = true;
            };

            if (_this.timeout) {
                clearTimeout(_this.timeout);
            }
            _this.timeout = setTimeout(updateAction, _this.interval);
        }
    }

    return codeScoop;
});