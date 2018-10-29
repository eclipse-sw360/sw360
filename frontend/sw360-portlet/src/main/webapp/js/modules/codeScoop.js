/**
 * Copyright (c) 2017-present, Codescoop Oy.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

var codeScoopCss = {
    positionHalfSize: "50%",
    positionTransform: "translateX(-50%) translateY(-50%)",
    border: "1px solid ",
    modalBackground: {
        image: "url('/sw360-portlet/webjars/jquery-ui/1.12.1/images/ui-icons_777777_256x240.png')",
        position: "-95px -128px"
    },
    colorWhite: "#ffffff",
    colorBlack: "#333333",
    colorLight: "#e9e9e9",
    colorGray: "#c5c5c5",
    modalPadding: "0.2em 0.2em 2em 0.2em",
    modalSize: "45%",
    modalMinSize: "150px",
    modalContentSize: "3em",
    sizeXXS: "0.2em",
    sizeXS: "0.5em",
    sizeS: "1.5em",
    sizeM: "15px",
    sizeL: "20px",
    fontWeight: "100",
    autocompletePadding: "4px 6px",
    dynamicSizeXS: "calc(100% - 0.5em)",
    dynamicSizeS: "calc(100% - 1em)",
    dynamicSizeM: "calc(100% - 20px)",
    dynamicSizeFull: "100%"
};

function Modal() {

    var _this = this;

    this.opened = false;
    this.modalBox = undefined;
    this.modalTitle = undefined;
    this.modalContent = undefined;
    this.modalOverlay = undefined;

    this.init = function () {
        var modalBox = document.createElement("div");
        modalBox.style.display = "none";
        modalBox.style.position = "fixed";
        modalBox.style.zIndex = "999999";
        modalBox.style.left = codeScoopCss.positionHalfSize;
        modalBox.style.top = codeScoopCss.positionHalfSize;
        modalBox.style.transform = codeScoopCss.positionTransform;
        modalBox.style.border = codeScoopCss.border + codeScoopCss.colorGray;
        modalBox.style.backgroundColor = codeScoopCss.colorWhite;
        modalBox.style.color = codeScoopCss.colorBlack;
        modalBox.style.padding = codeScoopCss.modalPadding;
        modalBox.style.width = codeScoopCss.modalSize;
        modalBox.style.minWidth = codeScoopCss.modalMinSize;
        modalBox.style.height = codeScoopCss.modalSize;
        modalBox.style.minHeight = codeScoopCss.modalMinSize;
        _this.modalBox = modalBox;

        var titleBox = document.createElement("div");
        titleBox.style.width = codeScoopCss.dynamicSizeS;
        titleBox.style.height = codeScoopCss.sizeS;
        titleBox.style.padding = codeScoopCss.sizeXS;
        titleBox.style.background = codeScoopCss.colorLight;
        titleBox.style.borderRadius = codeScoopCss.sizeXXS;
        modalBox.appendChild(titleBox);

        var modalTitle = document.createElement("div");
        modalTitle.style.maxWidth = codeScoopCss.dynamicSizeM;
        modalTitle.style.height = "auto";
        modalTitle.style.cssFloat = "left";
        modalTitle.style.fontWeight = "bold";
        modalTitle.style.color = codeScoopCss.colorBlack;
        titleBox.appendChild(modalTitle);
        _this.modalTitle = modalTitle;

        var modalClose = document.createElement("button");
        modalClose.style.width = codeScoopCss.sizeL;
        modalClose.style.height = codeScoopCss.sizeL;
        modalClose.style.cssFloat = "right";
        modalClose.style.backgroundImage = codeScoopCss.modalBackground.image;
        modalClose.style.backgroundPosition = codeScoopCss.modalBackground.position;
        modalClose.style.border = codeScoopCss.border + codeScoopCss.colorGray;
        modalClose.style.borderRadius = codeScoopCss.sizeXXS;
        titleBox.appendChild(modalClose);
        modalClose.onclick = _this.close;
        _this.modalClose = modalClose;

        var modalContent = document.createElement("div");
        modalContent.style.width = codeScoopCss.dynamicSizeFull;
        modalContent.style.height = codeScoopCss.dynamicSizeXS;
        modalContent.style.overflowY = "hidden";
        modalContent.style.display = "flex";
        modalContent.style.justifyContent = "center";
        modalBox.appendChild(modalContent);
        _this.modalContent = modalContent;

        document.body.appendChild(modalBox);
    };

    this.open = function (content, title) {
        if (content) {
            _this.modalTitle.innerHTML = "";
            _this.modalContent.innerHTML = "";
            _this.modalContent.appendChild(content);
            if (title) {
                _this.modalTitle.innerHTML = title;
            }
        } else {
            _this.modalTitle.innerHTML = "Fetching data...";
            _this.setLoader();
        }

        _this.modalBox.style.display = "block";
        _this.modalOverlay = document.createElement("div");
        _this.modalOverlay.style.zIndex = "100";
        _this.modalOverlay.setAttribute("class", "ui-widget-overlay ui-front");
        document.body.appendChild(_this.modalOverlay);
        _this.opened = true;
    };

    this.setLoader = function () {
        _this.modalTitle.innerHTML = "Fetching data...";

        _this.modalContent.innerHTML = "";
        var loader = document.createElement("img");
        loader.style.display = "flex";
        loader.style.width = codeScoopCss.modalContentSize;
        loader.style.height = codeScoopCss.modalContentSize;
        loader.style.margin = codeScoopCss.modalContentSize + " 0";
        loader.setAttribute("src", "/sw360-portlet/images/loader.gif");
        _this.modalContent.appendChild(loader);
    };

    this.refresh = function (content, title) {
        _this.modalTitle.innerHTML = "";
        _this.modalContent.innerHTML = "";
        _this.modalContent.appendChild(content);
        if (title) {
            _this.modalTitle.innerHTML = title;
        }
    };

    this.close = function () {
        _this.modalTitle.innerHTML = "";
        _this.modalBox.style.display = "none";
        _this.modalContent.innerHTML = "";
        _this.modalOverlay.remove();
        _this.opened = false;
    };

    this.isOpened = function () {
        return _this.opened;
    };

    _this.init();
}

define("modules/codeScoop", [], function () {
    function codeScoop(apiUrl, apiToken) {
        if (!apiUrl) {
            console.error("Codescoop plugin error: invalid codescoop.url property");
            return null;
        }
        if (!apiToken) {
            console.error("Codescoop plugin error: invalid codescoop.token property");
            return null;
        }

        var _this = this;

        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        this.formElements = {
            form: undefined,
            name: undefined,
            categories: undefined,
            homepage: undefined,
            description: undefined,
            componentList: undefined,
            componentListUrl: undefined
        };
        this.indexData = {};
        this.timeout = undefined;
        this.indexDone = false;
        this.interval = 500;
        this.modal = new Modal();

        this._api = function (method, path, request, callback) {
            var xhr = new XMLHttpRequest();
            xhr.open(method, _this.apiUrl + path, true);
            xhr.setRequestHeader("X-Api-Key", _this.apiToken);
            xhr.setRequestHeader("X-User-Login", "sw360");
            xhr.setRequestHeader("Content-type", "application/json");
            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    var dto = JSON.parse(this.responseText);
                    callback(dto);
                }
            };
            xhr.send(request);
        };

        this._fetch_repo = function (owner, name, callback) {
            _this._api("GET", "/integration/siemens/repository/" + owner + "/" + name + "/", null, callback);
        };

        this._fetch_composite = function (requestData, callback) {
            _this._api("POST", "integration/siemens/composite", JSON.stringify(requestData), callback);
        };

        this._fetch_releases_by_repo_id = function (id, callback) {
            _this._api("GET", "integration/siemens/releases?gitHubRepoId=" + id, null, callback);
        };

        this._fetch_releases_by_repo_owner_name = function (requestData, callback) {
            _this._api("POST", "integration/siemens/releases", JSON.stringify(requestData), callback);
        };

        this._install_form_element = function () {
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
            }
            var textAreas = _this.formElements.form.getElementsByTagName("textarea");
            for (var j = 0; j < textAreas.length; j++) {
                if (textAreas[j].name.indexOf("DESCRIPTION") > 0) {
                    _this.formElements.description = textAreas[j];
                }
            }
        };

        this._build_autocomplete_box = function (element) {
            var list = document.createElement("div");
            list.id = "codescoop-autocomplete";
            list.style.display = "none";
            list.style.height = "auto";
            list.style.minHeight = codeScoopCss.sizeL;
            list.style.color = codeScoopCss.colorBlack;
            list.style.backgroundColor = codeScoopCss.colorWhite;
            list.style.fontSize = codeScoopCss.sizeM;
            list.style.fontWeight = codeScoopCss.fontWeight;
            list.style.padding = codeScoopCss.autocompletePadding;
            list.style.border = codeScoopCss.border + codeScoopCss.colorLight;
            list.style.position = "absolute";
            list.style.zIndex = "99999999";

            var rect = element.getBoundingClientRect(),
                scrollLeft = window.pageXOffset || document.documentElement.scrollLeft,
                scrollTop = window.pageYOffset || document.documentElement.scrollTop;

            list.style.left = parseInt((rect.left + scrollLeft).toString(), 10) + "px";
            list.style.top = parseInt((rect.top + scrollTop + element.offsetHeight).toString(), 10) + "px";
            list.style.width = parseInt((element.offsetWidth - 13).toString(), 10) + "px";

            return list;
        };

        this._install_autocomplete_box = function () {
            _this.formElements.componentList = _this._build_autocomplete_box(_this.formElements.name);
            document.body.appendChild(_this.formElements.componentList);
            _this.formElements.componentListUrl = _this._build_autocomplete_box(_this.formElements.homepage);
            document.body.appendChild(_this.formElements.componentListUrl);
        };

        this._autocomplete_repo = function (searchType, searchValue, limit, callback) {
            var xhr = new XMLHttpRequest();
            xhr.open("GET", _this.apiUrl + "autocomplete?data=" + searchType + "&limit=" + limit + "&search=" + searchValue, true);
            xhr.setRequestHeader("X-Api-Key", _this.apiToken);
            xhr.setRequestHeader("X-User-Login", "test");
            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    var repoDtoList = JSON.parse(this.responseText);
                    callback(repoDtoList);
                }
            };
            xhr.send(null);
        };

        this._listen_autocomplete = function () {

            var autoFillForm = function (id) {
                var ownerName = id.split("/");
                _this._fetch_repo(ownerName[0], ownerName[1], function (repo) {
                    _this.formElements.name.value = id;
                    _this.formElements.description.innerHTML = repo.title;
                    if (repo.homepageUrl) {
                        _this.formElements.homepage.value = repo.homepageUrl;
                    } else {
                        _this.formElements.homepage.value = repo.url;
                    }

                    repo.categories = _this._fetch_categories(repo["hierarchicalCategories"]);
                    _this.formElements.categories.value = repo.categories.join(",");
                })
            };

            var clean = function () {
                _this.formElements.componentList.removeEventListener("click", select);
                _this.formElements.componentList.innerHTML = "";
                _this.formElements.componentList.style.display = "none";

                _this.formElements.componentListUrl.removeEventListener("click", select);
                _this.formElements.componentListUrl.innerHTML = "";
                _this.formElements.componentListUrl.style.display = "none";
            };

            var select = function (e) {
                if (e.target.nodeName === "P") {
                    var data = e.target.getAttribute("data").split(":");

                    if (data[0] === "name") {
                        _this.formElements.name.value = data[1];
                    } else if (data[0] === "url") {
                        _this.formElements.homepage.value = e.target.innerHTML;
                    }

                    clean();
                    autoFillForm(data[1]);
                }
            };

            var nameChanged = function () {
                if (_this.formElements.name.value.length < 2) {
                    return;
                }

                _this._autocomplete_repo("name", _this.formElements.name.value, 5, function (repoList) {
                    clean();
                    if (repoList.length > 0) {
                        for (var i = 0; i < repoList.length; i++) {
                            var repoID = repoList[i]["owner"] + "/" + repoList[i]["name"];
                            var div = document.createElement("div");
                            var p = document.createElement("p");
                            p.innerHTML = repoID;
                            p.setAttribute("data", "name:" + repoID);

                            div.style.cursor = "pointer";
                            div.style.marginTop = codeScoopCss.sizeXXS;

                            div.appendChild(p);
                            _this.formElements.componentList.appendChild(div);
                        }

                        _this.formElements.componentList.style.display = "block";
                        _this.formElements.componentList.addEventListener("click", select);
                    }
                });
            };
            _this.formElements.name.addEventListener("keyup", nameChanged);
            _this.formElements.name.onpaste = nameChanged;

            var homepageChanged = function () {
                if (_this.formElements.homepage.value.length < 2) {
                    return;
                }

                _this._autocomplete_repo("url", _this.formElements.homepage.value, 5, function (repoList) {
                    clean();
                    if (repoList.length > 0) {
                        for (var i = 0; i < repoList.length; i++) {
                            var repoID = repoList[i]["owner"] + "/" + repoList[i]["name"];
                            var div = document.createElement("div");
                            var p = document.createElement("p");
                            p.innerHTML = repoList[i].url;
                            p.setAttribute("data", "url:" + repoID);

                            div.style.cursor = "pointer";
                            div.style.marginTop = codeScoopCss.sizeXXS;

                            div.appendChild(p);
                            _this.formElements.componentListUrl.appendChild(div);
                        }

                        _this.formElements.componentListUrl.style.display = "block";
                        _this.formElements.componentListUrl.addEventListener("click", select);
                    }
                });
            };
            _this.formElements.homepage.addEventListener("keyup", homepageChanged);
            _this.formElements.homepage.onpaste = homepageChanged;
        };

        this._init_missed_release_button = function (size, url) {
            var releaseButton = document.createElement("button");
            releaseButton.innerHTML = "Populate Releases (" + size + ")";
            releaseButton.setAttribute("class", "addButton");
            releaseButton.style.cursor = "pointer";

            var target = document
                .getElementsByClassName("pageHeader")[0]
                .getElementsByClassName("pull-right")[0];
            target.insertBefore(releaseButton, target.firstChild);

            releaseButton.onclick = function () {
                window.location = url;
            };
        };

        this._clean_string_array = function (array) {
            var result = [];
            for (var i = 0; i < array.length; i++) {
                var str = array[i];
                str = str.trim();
                if (str.length > 0) {
                    result.push(str);
                }
            }
            return result;
        };

        this._match_string_array = function (array1, array2) {
            if (array1.length !== array2.length) {
                return false;
            }
            array1.sort();
            array2.sort();
            for (var i = 0; i < array1.length; i++) {
                if (array1[i] !== array2[i]) {
                    return false;
                }
            }
            return true;
        };

        this._fetch_categories = function (hierarchicalCategories) {
            var categories = [];
            for (var i = 0; i < hierarchicalCategories.length; i++) {
                categories.push(hierarchicalCategories[i]["name"])
            }
            return categories;
        };

        this._check_component_diff = function (sw360Component, externalComponent) {
            var result = {};

            if (sw360Component.description !== externalComponent.title.replace(/</g,"&lt;").replace(/>/g,"&gt;")) {
                result["description"] = externalComponent.title;
            }

            if (externalComponent.homepageUrl === "") {
                externalComponent.homepageUrl = externalComponent.url;
            }

            if (sw360Component.homepage !== externalComponent.homepageUrl) {
                result["homepage"] = externalComponent.homepageUrl;
            }

            externalComponent.categories = _this._fetch_categories(externalComponent["hierarchicalCategories"]);

            if (!_this._match_string_array(sw360Component.categories, externalComponent.categories)) {
                result["categories"] = externalComponent.categories;
            }

            if (!_this._match_string_array(sw360Component.languages, externalComponent["langs"])) {
                result["languages"] = externalComponent["langs"];
            }

            if (sw360Component.licenses.indexOf(externalComponent["license"]) < 0) {
                sw360Component.licenses.push(externalComponent["license"]);
                result["licenses"] = sw360Component.licenses;
            }
            return result;
        };

        this.activateAutoFill = function () {
            _this._install_form_element();
            _this._install_autocomplete_box();
            _this._listen_autocomplete();
        };

        this.activateIndexes = function (tableID, dataUrl) {
            dataUrl = dataUrl + "&sEcho=1&iColumns=5&sColumns=%2C%2C%2C%2C&iDisplayStart=0" +
                "&iDisplayLength=25&mDataProp_0=vndrs&bSortable_0=true&mDataProp_1=name" +
                "&bSortable_1=true&mDataProp_2=lics&bSortable_2=true&mDataProp_3=cType" +
                "&bSortable_3=true&mDataProp_4=id&bSortable_4=true&iSortCol_0=1" +
                "&sSortDir_0=asc&iSortingCols=1&_=" + new Date().getTime();
            var xhr = new XMLHttpRequest();
            xhr.open("GET", dataUrl, true);
            xhr.setRequestHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    var response = JSON.parse(this.responseText);

                    var requestData = [];

                    for (var i = 0; i < response.aaData.length; i++) {
                        var swComponent = response.aaData[i];
                        var vendor = swComponent["vndrs"].length === 0 ? null : swComponent["vndrs"][0];
                        requestData.push({
                            id: swComponent.DT_RowId,
                            vendor: vendor,
                            name: swComponent.name
                        });
                    }

                    _this._fetch_composite(requestData, function (responseData) {
                        for (var i = 0; i < responseData.length; i++) {
                            var data = responseData[i];
                            _this.indexData[data.id] = data;
                        }
                    });
                }
            };
            xhr.send(null);
        };

        this.getData = function (id, field, secondField) {
            var elementData = _this.indexData[id];
            if (elementData && field === "logo") {
                return '<img src="' + elementData["logo"] + '" style="display:block;width:30px;height:30px;border-radius:3px"/>';
            }
            if (elementData && field === "compositeIndex") {
                elementData = elementData[field];
                if (elementData) {
                    elementData = elementData[secondField];
                    return elementData ? elementData : "";
                }
                return "";
            }
            return elementData ? elementData[field] : "";
        };

        this.updateIndexes = function () {
            if (_this.indexDone) {
                return;
            }

            var _dataTable = this;

            var updateAction = function () {
                if (Object.keys(_this.indexData).length === 0) {
                    if (_this.timeout) {
                        clearTimeout(_this.timeout);
                    }
                    _this.timeout = setTimeout(updateAction, _this.interval);
                    return;
                }
                _dataTable.api().draw();
                _this.indexDone = true;
            };

            if (_this.timeout) {
                clearTimeout(_this.timeout);
            }
            _this.timeout = setTimeout(updateAction, _this.interval);
        };

        this.activateReleaseAutocomplete = function () {
            var releaseButton = document.createElement("button");
            releaseButton.innerHTML = "Populate release";
            releaseButton.setAttribute("class", "addButton");
            releaseButton.style.cursor = "pointer";

            var target = document
                .getElementsByClassName("pageHeader")[0]
                .getElementsByClassName("pull-right")[0];
            target.insertBefore(releaseButton, target.firstChild);

            var selectRepoFromList = function (e) {
                if (e.target.nodeName === "BUTTON") {
                    _this.modal.setLoader();
                    var id = e.target.getAttribute("data");
                    _this._fetch_releases_by_repo_id(id, function (dto) {
                        var languageInput = document.getElementById("programminglanguages");
                        var languages = dto["repo"]["langs"];
                        for (var k = 0; k < languages.length; k++) {
                            if (k !== 0) {
                                languageInput.value = languageInput.value + ", ";
                            }
                            languageInput.value = languageInput.value + languages[k];
                        }

                        var table = document.createElement("div");
                        table.style.width = "80%";
                        table.style.height = codeScoopCss.dynamicSizeXS;
                        table.style.overflowY = "scroll";

                        var componentReleasesKey = Object.keys(dto.releases);

                        for (var iKey = 0; iKey < componentReleasesKey.length; iKey++) {
                            var name = componentReleasesKey[iKey];
                            if (dto.releases[name][0]["releaseSource"] === "RELEASE_GITHUB") {
                                var ownerName = name.split("/");
                                if (componentReleasesKey.indexOf(ownerName[1]) > -1) {
                                    componentReleasesKey.splice(iKey, 1);
                                    componentReleasesKey.splice(componentReleasesKey.indexOf(ownerName[1]));
                                    componentReleasesKey.unshift(ownerName[1]);
                                } else {
                                    componentReleasesKey.splice(iKey, 1);
                                    componentReleasesKey.unshift(name);
                                }
                                break;
                            }
                        }

                        for (var i = 0; i < componentReleasesKey.length; i++) {
                            var row = document.createElement("div");
                            row.style.margin = codeScoopCss.sizeXS + " 0";
                            row.style.borderBottom = codeScoopCss.border + codeScoopCss.colorGray;
                            row.innerHTML = "<strong>" + componentReleasesKey[i] + "</strong>";
                            table.appendChild(row);

                            var releases = dto.releases[componentReleasesKey[i]];

                            for (var j = 0; j < releases.length; j++) {
                                var rel = releases[j];

                                var arrow = document.createElement("div");
                                arrow.style.display = "flex";
                                arrow.style.flexDirection = "row";
                                arrow.style.justifyContent = "space-between";
                                arrow.style.margin = codeScoopCss.sizeXS + " 0";
                                arrow.style.minHeight = codeScoopCss.sizeL;

                                var date = rel["dateUTC"];
                                var dateArray = date.split("/");
                                date = dateArray[2] + "-" + dateArray[1] + "-" + dateArray[0];
                                var div = document.createElement("div");
                                div.innerHTML = "Version: " + rel.version + ", Date: " + date;
                                arrow.appendChild(div);

                                var selectButton = document.createElement("button");
                                selectButton.innerHTML = "select";
                                selectButton.style.cursor = "pointer";
                                selectButton.setAttribute("class", "addButton");
                                var license = rel["license"];
                                if (!license) {
                                    license = "MIT"
                                }
                                selectButton.setAttribute("data", rel.version + "*" + date + "*" + rel["downloadUrl"] + "*" + license);
                                arrow.appendChild(selectButton);

                                table.appendChild(arrow);
                            }
                        }
                        _this.modal.refresh(table, "Please select release");
                        table.addEventListener("click", function (e) {
                            if (e.target["nodeName"] === "BUTTON") {
                                _this.modal.close();
                                var data = e.target.getAttribute("data").split("*");
                                document.getElementById("comp_version").value = data[0];
                                document.getElementById("releaseDate").value = data[1];
                                document.getElementById("downloadUrl").value = data[2];
                                document.getElementById("MAIN_LICENSE_IDSDisplay").value = data[3];
                                document.getElementById("MAIN_LICENSE_IDS").value = data[3];
                            }
                        });
                    });
                }
            };

            releaseButton.onclick = function () {
                if (!_this.modal.isOpened()) {
                    _this.modal.open();
                    var vendor = document.getElementById("VENDOR_IDDisplay").value;
                    var name = document.querySelectorAll("[name='_components_WAR_sw360portlet_NAME']")[0].value;
                    if (name.indexOf("/") > 0) {
                        var vendorName = name.split("/");
                        vendor = vendorName[0];
                        name = vendorName[1];
                    }

                    _this._autocomplete_repo("name", name, 100, function (repoList) {
                        var filteredRepoList = [];
                        if (vendor && vendor.length > 0) {
                            vendor = vendor.toLowerCase();
                            for (var iRepo = 0; iRepo < repoList.length; iRepo++) {
                                if (repoList[iRepo]["owner"].toLowerCase().indexOf(vendor) === 0) {
                                    filteredRepoList.push(repoList[iRepo]);
                                }
                            }
                        } else {
                            filteredRepoList = repoList;
                        }

                        if (filteredRepoList.length > 0) {
                            var table = document.createElement("div");
                            table.style.display = "flex";
                            table.style.flexDirection = "column";
                            table.style.width = "80%";
                            table.style.height = codeScoopCss.dynamicSizeXS;
                            table.style.overflowY = "scroll";

                            for (var i = 0; i < filteredRepoList.length; i++) {
                                var repo = filteredRepoList[i];

                                var row = document.createElement("div");
                                row.style.display = "flex";
                                row.style.flexDirection = "row";
                                row.style.justifyContent = "space-between";
                                row.style.padding = codeScoopCss.sizeXS;
                                row.style.minHeight = codeScoopCss.sizeL;
                                row.style.borderBottom = codeScoopCss.border + codeScoopCss.colorGray;

                                var div = document.createElement("div");
                                var p = document.createElement("p");
                                p.innerHTML = "Vendor: " + repo["owner"] + ", Name: " + repo["name"];
                                div.appendChild(p);
                                row.appendChild(div);

                                var selectButton = document.createElement("button");
                                selectButton.innerHTML = "select";
                                selectButton.style.cursor = "pointer";
                                selectButton.setAttribute("class", "addButton");
                                selectButton.setAttribute("data", repo.id);
                                row.appendChild(selectButton);

                                table.appendChild(row);
                            }
                            _this.modal.refresh(table, "Please choose component to match data");
                            table.addEventListener("click", selectRepoFromList);
                        } else {
                            var info = document.createElement("span");
                            info.style.display = "flex";
                            info.innerHTML = "NOT SUPPORTED";
                            _this.modal.refresh(info);
                        }
                    });
                }
            };
        };

        this.activateMissedReleases = function (releaseData,
                                                componentInfoTableId,
                                                releaseAggregateTableId,
                                                addReleaseUrl) {
            var existReleases = {};
            if (releaseData.length > 0) {
                for (var i = 0; i < releaseData.length; i++) {
                    var release = releaseData[i];
                    release = release["1"];
                    var box = document.createElement("div");
                    box.innerHTML = release;
                    release = box.getElementsByTagName("a")[0].innerHTML;
                    existReleases[release] = true;
                }
            }

            var componentName = document
                .getElementById(componentInfoTableId)
                .getElementsByTagName("tbody")[0]
                .getElementsByTagName("tr")[0]
                .getElementsByTagName("td")[1]
                .innerHTML;
            var componentVendor = document
                .getElementById(releaseAggregateTableId)
                .getElementsByTagName("tbody")[0]
                .getElementsByTagName("tr")[0]
                .getElementsByTagName("td")[1]
                .innerHTML.split(",")[0];

            var request = {
                "vardor": componentVendor,
                "name": componentName
            };

            _this._fetch_releases_by_repo_owner_name(request, function (dto) {
                var absentReleases = [];
                var componentReleasesKey = Object.keys(dto["releases"]);

                for (var i = 0; i < componentReleasesKey.length; i++) {
                    var name = componentReleasesKey[i];
                    var releasesByType = dto["releases"][name];
                    for (var j = 0; j < releasesByType.length; j++) {
                        var release = releasesByType[j];
                        if (existReleases[release.version] === undefined) {
                            absentReleases.push(release);
                        }
                    }
                }

                if (absentReleases.length > 0) {
                    _this._init_missed_release_button(absentReleases.length, addReleaseUrl);
                }
            });
        };

        this.activateMerge = function () {
            var readComponent = function () {
                var sw360Component = {
                    description: "",
                    homepage: "",
                    categories: [],
                    languages: [],
                    licenses: []
                };

                sw360Component.description = document
                    .getElementById("up_Summary")
                    .getElementsByTagName("p")[0]
                    .innerHTML;

                var tableData = document
                    .getElementById("componentOverview")
                    .getElementsByTagName("tbody")[0]
                    .getElementsByTagName("tr");

                sw360Component.homepage = tableData[5]
                    .getElementsByTagName("td")[1]
                    .getElementsByTagName("a")[0]
                    .innerHTML;
                sw360Component.categories = _this._clean_string_array(
                    tableData[3]
                        .getElementsByTagName("td")[1]
                        .innerHTML
                        .split(","));

                tableData = document
                    .getElementById("releaseAggregateTable")
                    .getElementsByTagName("tbody")[0]
                    .getElementsByTagName("tr");

                sw360Component.languages = _this._clean_string_array(
                    tableData[1]
                        .getElementsByTagName("td")[1]
                        .innerHTML
                        .split(","));

                var licenses = tableData[4]
                    .getElementsByTagName("td")[1]
                    .getElementsByTagName("a");
                for (var i = 0; i < licenses.length; i++) {
                    sw360Component.licenses.push(licenses[i].innerHTML);
                }

                return sw360Component;
            };

            var updateComponentDiff = function (e) {
                var diff = JSON.parse(e.target.getAttribute("data"));
                var form = document.getElementById("component_edit_form");
                _this.modal.setLoader();
                var keys = Object.keys(diff);
                for (var i = 0; i < keys.length; i++) {
                    var input = document.createElement("input");
                    input.name = edit_form_fields[keys[i]];
                    input.value = diff[keys[i]];
                    form.appendChild(input);
                }
                form.submit();
            };

            var showDiffForMerge = function (externalRepo) {
                var sw360Component = readComponent();

                var diff = _this._check_component_diff(sw360Component, externalRepo);
                var keys = Object.keys(diff);
                if (keys.length > 0) {
                    var table = document.createElement("div");
                    table.style.display = "flex";
                    table.style.flexDirection = "column";
                    table.style.width = "80%";
                    table.style.height = codeScoopCss.dynamicSizeXS;
                    table.style.overflowY = "scroll";

                    for (var i = 0; i < keys.length; i++) {
                        var row = document.createElement("div");
                        row.style.display = "flex";
                        row.style.flexDirection = "row";
                        row.style.justifyContent = "space-between";
                        row.style.padding = codeScoopCss.sizeXS;
                        row.style.minHeight = codeScoopCss.sizeL;
                        row.style.borderBottom = codeScoopCss.border + codeScoopCss.colorGray;
                        table.appendChild(row);

                        var name = document.createElement("div");
                        name.style.width = "35%";
                        name.innerHTML = keys[i];
                        row.appendChild(name);

                        var val = document.createElement("div");
                        val.style.width = "65%";
                        var data = diff[keys[i]];
                        if (Array.isArray(data)) {
                            data = data.join(", ");
                        }
                        val.innerHTML = data;
                        row.appendChild(val);
                    }

                    var selectButton = document.createElement("button");
                    selectButton.innerHTML = "Update component data";
                    selectButton.style.cursor = "pointer";
                    selectButton.style.marginTop = codeScoopCss.sizeM;
                    selectButton.setAttribute("class", "addButton");
                    selectButton.setAttribute("data", JSON.stringify(diff));
                    table.appendChild(selectButton);
                    selectButton.onclick = updateComponentDiff;

                    _this.modal.refresh(table, "Merging component");
                } else {
                    var info = document.createElement("p");
                    info.innerHTML = "Components identical, nothing to merge";
                    _this.modal.refresh(info, "Merging component");
                }
            };

            var selectRepoFromList = function (e) {
                if (e.target.nodeName === "BUTTON") {
                    _this.modal.setLoader();
                    var id = e.target.getAttribute("data");
                    _this._fetch_releases_by_repo_id(id, function (dto) {
                        showDiffForMerge(dto["repo"]);
                    });
                }
            };

            var mergeButton = document.createElement("button");
            mergeButton.innerHTML = "Merge from External Source";
            mergeButton.setAttribute("class", "addButton");
            mergeButton.style.cursor = "pointer";
            mergeButton.style.marginLeft = codeScoopCss.sizeXS;

            var target = document
                .getElementsByClassName("pageHeader")[0]
                .getElementsByClassName("pull-right")[0];
            target.insertBefore(mergeButton, target.firstChild);

            mergeButton.onclick = function () {
                if (!_this.modal.isOpened()) {
                    _this.modal.open();

                    var name = document
                        .getElementById("componentOverview")
                        .getElementsByTagName("tbody")[0]
                        .getElementsByTagName("tr")[0]
                        .getElementsByTagName("td")[1].innerHTML;

                    var vendor = document
                        .getElementById("releaseAggregateTable")
                        .getElementsByTagName("tbody")[0]
                        .getElementsByTagName("tr")[0]
                        .getElementsByTagName("td")[1].innerHTML;

                    if (name.indexOf("/") > 0) {
                        var vendorName = name.split("/");
                        vendor = vendorName[0];
                        name = vendorName[1];
                    } else if (vendor.indexOf(",") > 0) {
                        vendor = vendor.split(",")[0];
                    }

                    _this._autocomplete_repo("name", name, 100, function (repoList) {
                        var filteredRepoList = [];
                        if (vendor && vendor.length > 0) {
                            vendor = vendor.toLowerCase();
                            for (var iRepo = 0; iRepo < repoList.length; iRepo++) {
                                if (repoList[iRepo]["owner"].toLowerCase().indexOf(vendor) === 0) {
                                    filteredRepoList.push(repoList[iRepo]);
                                }
                            }
                        } else {
                            filteredRepoList = repoList;
                        }

                        if (filteredRepoList.length > 0) {
                            var table = document.createElement("div");
                            table.style.display = "flex";
                            table.style.flexDirection = "column";
                            table.style.width = "80%";
                            table.style.height = codeScoopCss.dynamicSizeXS;
                            table.style.overflowY = "scroll";

                            for (var i = 0; i < filteredRepoList.length; i++) {
                                var repo = filteredRepoList[i];

                                var row = document.createElement("div");
                                row.style.display = "flex";
                                row.style.flexDirection = "row";
                                row.style.justifyContent = "space-between";
                                row.style.padding = codeScoopCss.sizeXS;
                                row.style.minHeight = codeScoopCss.sizeL;
                                row.style.borderBottom = codeScoopCss.border + codeScoopCss.colorGray;

                                var div = document.createElement("div");
                                var p = document.createElement("p");
                                p.innerHTML = "Vendor: " + repo["owner"] + ", Name: " + repo["name"];
                                div.appendChild(p);
                                row.appendChild(div);

                                var selectButton = document.createElement("button");
                                selectButton.innerHTML = "select";
                                selectButton.style.cursor = "pointer";
                                selectButton.setAttribute("class", "addButton");
                                selectButton.setAttribute("data", repo.id);
                                row.appendChild(selectButton);

                                table.appendChild(row);
                            }
                            _this.modal.refresh(table, "Please choose component to match data");
                            table.addEventListener("click", selectRepoFromList);
                        } else {
                            var info = document.createElement("span");
                            info.style.display = "flex";
                            info.innerHTML = "NOT SUPPORTED";
                            _this.modal.refresh(info);
                        }
                    });
                }
            };
        };
    }

    return codeScoop;
});