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
        image: "url('/sw360-portlet/webjars/jquery-ui/themes/base/images/ui-icons_777777_256x240.png')",
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

var AutocompleteBy = {
    byName: "name",
    byOwner: "owner",
    byUrl: "url"
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
    function codeScoop() {
        var _this = this;

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

        this.api = {
            componentUrl: "",
            autocompleteUrl: "",
            compositeUrl: "",
            releaseUrl: "",
            purlUrl: ""
        };

        this._proxy_api = function (method, url, request, callback) {
            var xhr = new XMLHttpRequest();
            xhr.open(method, url, true);
            xhr.setRequestHeader("Content-type", "application/json");
            xhr.setRequestHeader("Accept", "application/json");
            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    var dto = JSON.parse(this.responseText);
                    callback(dto);
                }
            };
            xhr.send(request);
        };

        this._match_component = function (url, componentSearch, callback) {
            _this._proxy_api("POST", url, JSON.stringify(componentSearch), callback);
        };

        this._fetch_composite = function (compositeUrl, requestData, callback) {
            _this._proxy_api("POST", compositeUrl, JSON.stringify(requestData), callback);
        };

        this._fetch_releases_by_component_id = function (url, requestData, callback) {
            _this._proxy_api("POST", url, JSON.stringify(requestData), callback);
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

        this._autocomplete_repo = function (autocompleteBy, searchValue, limit, callback) {
            var autocompleteRequest = {
                search: searchValue.trim(),
                by: autocompleteBy,
                limit: limit
            };
            _this._proxy_api("POST", _this.api.autocompleteUrl, JSON.stringify(autocompleteRequest), function (data) {
                callback(data.repositories);
            });
        };

        this._listen_autocomplete = function () {

            var autoFillForm = function (data) {
                _this.formElements.name.value = data.name;
                _this.formElements.description.innerHTML = data.origin.title;
                if (data.origin.homepageUrl) {
                    _this.formElements.homepage.value = data.origin.homepageUrl;
                } else {
                    _this.formElements.homepage.value = data.origin.url;
                }

                var categories = _this._fetch_categories(data.origin.categories);
                _this.formElements.categories.value = categories.join(",");

                document.getElementById("add-external-id").click();
                var inputs = document.getElementsByTagName("input");
                inputs = Array.from(inputs);
                var filtered = inputs.filter(function(inp) {
                    return inp.name.startsWith("_components_WAR_sw360portlet_externalIdKeyexternalIdsTableRow") && inp.value === ''
                })[0];
                filtered.value = "purl.id";

                filtered = inputs.filter(function(inp) {
                    return inp.name.startsWith("_components_WAR_sw360portlet_externalIdValueexternalIdsTableRow") && inp.value === ''
                })[0];
                filtered.value = data.purl;
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
                    var data = JSON.parse(e.target.getAttribute("data"));

                    if (data.by === AutocompleteBy.byName) {
                        _this.formElements.name.value = data.name;
                    } else if (data.by === AutocompleteBy.byUrl) {
                        _this.formElements.homepage.value = e.target.innerHTML;
                    }

                    clean();
                    autoFillForm(data);
                }
            };

            var nameChanged = function () {
                if (_this.formElements.name.value.length < 2) {
                    return;
                }

                _this._autocomplete_repo(AutocompleteBy.byName, _this.formElements.name.value, 5, function (repoList) {
                    clean();
                    if (repoList.length > 0) {
                        for (var i = 0; i < repoList.length; i++) {
                            var repo = repoList[i];
                            var div = document.createElement("div");
                            var p = document.createElement("p");
                            p.innerHTML = repo.owner + "/" + repo.name;
                            repo.by = AutocompleteBy.byName;
                            p.setAttribute("data", JSON.stringify(repo));
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

                _this._autocomplete_repo(AutocompleteBy.byUrl, _this.formElements.homepage.value, 5, function (repoList) {
                    clean();
                    if (repoList.length > 0) {
                        for (var i = 0; i < repoList.length; i++) {
                            var repo = repoList[i];
                            var div = document.createElement("div");
                            var p = document.createElement("p");
                            p.innerHTML = repo.origin.url;
                            repo.by = AutocompleteBy.byUrl;
                            p.setAttribute("data", JSON.stringify(repo));

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

            var origin = externalComponent.origin;
            if (!origin) {
                return result;
            }

            if (sw360Component.description !== origin.title.replace(/</g, "&lt;").replace(/>/g, "&gt;")) {
                result["description"] = origin.title;
            }


            if (origin["homepageUrl"] && (sw360Component.homepage !== origin["homepageUrl"])) {
                result["homepage"] = origin["homepageUrl"];
            } else if (!origin["homepageUrl"] && origin.url && (sw360Component.homepage !== origin.url)) {
                result["homepage"] = origin.url;
            }

            var categories = _this._fetch_categories(origin["categories"]);

            if (!_this._match_string_array(sw360Component.categories, categories)) {
                result["categories"] = categories;
            }

            if (!_this._match_string_array(sw360Component.languages, origin["languages"])) {
                result["languages"] = origin["languages"];
            }

            if (sw360Component.licenses.indexOf(origin["license"]) < 0) {
                sw360Component.licenses.push(origin["license"]);
                result["licenses"] = sw360Component.licenses;
            }

            var purlPresent = false;
            sw360Component.externalIds.forEach(function (id) {
                if (id.value === externalComponent.purl) {
                    purlPresent = true;
                }
            });
            if (!purlPresent) {
                result["purl"] = externalComponent.purl;
            }
            return result;
        };

        this.activateAutoFill = function (componentUrl, autocompleteUrl) {
            _this.api.componentUrl = componentUrl;
            _this.api.autocompleteUrl = autocompleteUrl;
            _this._install_form_element();
            _this._install_autocomplete_box();
            _this._listen_autocomplete();
        };

        var urlParams = "&sEcho=1&iColumns=5&sColumns=%2C%2C%2C%2C&iDisplayStart=0" +
            "&iDisplayLength=25&mDataProp_0=vndrs&bSortable_0=true&mDataProp_1=name" +
            "&bSortable_1=true&mDataProp_2=lics&bSortable_2=true&mDataProp_3=cType" +
            "&bSortable_3=true&mDataProp_4=id&bSortable_4=true&iSortCol_0=1" +
            "&sSortDir_0=asc&iSortingCols=1&_=";

        this.activateIndexes = function (tableID, dataUrl, compositeUrl) {
            _this.api.compositeUrl = compositeUrl + urlParams + new Date().getTime();
            dataUrl = dataUrl + urlParams + new Date().getTime();

            var xhr = new XMLHttpRequest();
            xhr.open("GET", dataUrl, true);
            xhr.setRequestHeader("Accept", "application/json");
            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    var sw360ComponentsResponse = JSON.parse(this.responseText);
                    var componentSearchData = _this._composite_data_from_sw360_components(sw360ComponentsResponse.aaData);
                    _this._fetch_composite(_this.api.compositeUrl, componentSearchData, function (responseData) {
                        for (var i = 0; i < responseData.length; i++) {
                            var data = responseData[i];
                            _this.indexData[data.uuid] = data;
                        }
                    });
                }
            };
            xhr.send(null);
        };

        this._composite_data_from_sw360_components = function (sw360Components) {
            var componentSearchData = [];
            for (var i = 0; i < sw360Components.length; i++) {
                var swComponent = sw360Components[i];
                var ownerQuery = swComponent["vndrs"].length === 0 ? "" : swComponent["vndrs"][0];
                var searchQuery = swComponent.name;
                if (searchQuery.indexOf("/") > 0) {
                    var searchQueryData = searchQuery.split("/");
                    searchQuery = searchQueryData[1];
                    if (!ownerQuery) {
                        ownerQuery = searchQueryData[0];
                    }
                }
                componentSearchData.push({
                    uuid: swComponent.DT_RowId,
                    ownerQuery: ownerQuery,
                    searchQuery: searchQuery
                });
            }
            return componentSearchData;
        };

        this._get_composite_data_item = function (id, field, secondField) {
            var elementData = _this.indexData[id];

            if (!elementData) {
                return "";
            }

            var origin = elementData["origin"];

            if (origin && field === "rate") {
                elementData = origin[field];
                return elementData ? elementData : "";
            }
            if (origin && field === "logo") {
                return '<img src="' + origin["logo"] + '" style="display:block;width:30px;height:30px;border-radius:3px"/>';
            }
            if (origin && field === "index") {
                elementData = origin["index"];
                if (elementData) {
                    elementData = elementData[secondField];
                    return elementData ? elementData : "";
                }
            }

            return "";
        };

        this._update_indexes = function () {
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

        _this.create_table = function () {
            var table = document.createElement("div");
            table.style.display = "flex";
            table.style.flexDirection = "column";
            table.style.width = "90%";
            table.style.paddingRight = "5%";
            table.style.height = codeScoopCss.dynamicSizeXS;
            table.style.overflowY = "scroll";

            return table;
        };

        this.activateReleaseAutocomplete = function (componentUrl,
                                                     releaseUrl,
                                                     purlUrl) {
            _this.api.componentUrl = componentUrl;
            _this.api.releaseUrl = releaseUrl;
            _this.api.purlUrl = purlUrl;

            var callTo = _this.api.releaseUrl;

            var vendor = document.getElementById("VENDOR_IDDisplay").value;
            var name = document.querySelectorAll("[name='_components_WAR_sw360portlet_NAME']")[0].value;

            var query = {
                uuid: "",
                searchQuery: name,
                ownerQuery: vendor,
                limit: 5,
                filter: {
                    minScore: 100
                }
            };

            if (sw360Purl && sw360Purl !== "") {
                callTo = _this.api.purlUrl;
                query = [sw360Purl];
            } else {
                console.log("kick off matching releases without purl");
                return;
            }

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
                    var component = JSON.parse(e.target.getAttribute("data"));

                    _this._fetch_releases_by_component_id(_this.api.releaseUrl, {searchQuery: component.id}, function (releasesList) {
                        var languageInput = document.getElementById("programminglanguages");
                        var languages = component.origin.languages;
                        for (var k = 0; k < languages.length; k++) {
                            if (k !== 0) {
                                languageInput.value = languageInput.value + ", ";
                            }
                            languageInput.value = languageInput.value + languages[k];
                        }

                        var table = _this.create_table();

                        for (var i = 0; i < releasesList.length; i++) {
                            var release = releasesList[i];

                            var row = document.createElement("div");
                            row.style.margin = codeScoopCss.sizeXS + " 0";
                            row.style.borderBottom = codeScoopCss.border + codeScoopCss.colorGray;
                            row.innerHTML = "<strong>" + release.version + "</strong>";
                            table.appendChild(row);

                            var arrow = document.createElement("div");
                            arrow.style.display = "flex";
                            arrow.style.flexDirection = "row";
                            arrow.style.justifyContent = "space-between";
                            arrow.style.margin = codeScoopCss.sizeXS + " 0";
                            arrow.style.minHeight = codeScoopCss.sizeL;

                            var date = release.dateUTC;
                            var dateArray = date.split("/");
                            date = dateArray[2] + "-" + dateArray[1] + "-" + dateArray[0];
                            var div = document.createElement("div");
                            div.innerHTML = "Version: " + release.version + ", Date: " + date;
                            arrow.appendChild(div);

                            var selectButton = document.createElement("button");
                            selectButton.innerHTML = "select";
                            selectButton.style.cursor = "pointer";
                            selectButton.setAttribute("class", "addButton");
                            var license = release.license;
                            if (!license) {
                                license = component.origin.license;
                            }
                            selectButton.setAttribute("data", JSON.stringify({
                                version: release.version,
                                date: date,
                                downloadUrl: release.downloadUrl,
                                license: license
                            }));
                            arrow.appendChild(selectButton);

                            table.appendChild(arrow);
                        }
                        _this.modal.refresh(table, "Please select release");
                        table.addEventListener("click", function (e) {
                            if (e.target["nodeName"] === "BUTTON") {
                                _this.modal.close();
                                var data = JSON.parse(e.target.getAttribute("data"));
                                document.getElementById("comp_version").value = data.version;
                                document.getElementById("releaseDate").value = data.date;
                                document.getElementById("downloadUrl").value = data.downloadUrl;
                                document.getElementById("MAIN_LICENSE_IDSDisplay").value = data.license;
                                document.getElementById("MAIN_LICENSE_IDS").value = data.license;
                            }
                        });
                    });
                }
            };

            releaseButton.onclick = function () {
                if (!_this.modal.isOpened()) {
                    _this.modal.open();
                    _this._match_component(callTo, query, function (componentList) {
                        if (componentList.length > 0) {
                            var table = _this.create_table();

                            for (var i = 0; i < componentList.length; i++) {
                                var component = componentList[i];

                                var row = document.createElement("div");
                                row.style.display = "flex";
                                row.style.flexDirection = "row";
                                row.style.justifyContent = "space-between";
                                row.style.padding = codeScoopCss.sizeXS;
                                row.style.minHeight = codeScoopCss.sizeL;
                                row.style.borderBottom = codeScoopCss.border + codeScoopCss.colorGray;

                                var div = document.createElement("div");
                                var p = document.createElement("p");
                                p.innerHTML = (component.origin ? component.origin.owner : "? ") + "/" + component.name + " " + component.purl;
                                div.appendChild(p);
                                row.appendChild(div);

                                var selectButton = document.createElement("button");
                                selectButton.innerHTML = "select";
                                selectButton.style.cursor = "pointer";
                                selectButton.setAttribute("class", "addButton");
                                selectButton.setAttribute("data", JSON.stringify(component));
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
                                                addReleaseUrl,
                                                componentUrl,
                                                releasesUrl,
                                                purlUrl) {
            _this.api.componentUrl = componentUrl;
            _this.api.releasesUrl = releasesUrl;
            _this.api.purlUrl = purlUrl;

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

            var sw360Component = _this._read_sw360_component_from_page();

            var callTo = _this.api.componentUrl;
            var query = {
                uuid: "",
                searchQuery: sw360Component.name,
                ownerQuery: sw360Component.vendor,
                limit: 1,
                filter: {
                    languages: sw360Component.languages,
                    licenses: sw360Component.licenses
                }
            };

            var purlPresent = "";
            sw360Component.externalIds.forEach(function (id) {
                if (id.value.startsWith("pkg://")) {
                    purlPresent = id.value;
                }
            });
            if (purlPresent !== "") {
                callTo = _this.api.purlUrl;
                query = [purlPresent];
            } else {
                console.log("kick off matching releases without purl");
                return;
            }

            _this._match_component(callTo, query, function (componentList) {
                if (componentList.length > 0) {
                    _this._fetch_releases_by_component_id(_this.api.releasesUrl, {searchQuery: componentList[0].id}, function (externalReleases) {
                        var absentReleases = [];

                        for (var i = 0; i < externalReleases.length; i++) {
                            var version = externalReleases[i].version;
                            if (existReleases[version] === undefined) {
                                absentReleases.push(version);
                            }
                        }

                        if (absentReleases.length > 0) {
                            _this._init_missed_release_button(absentReleases.length, addReleaseUrl);
                        }
                    })
                }
            });
        };

        this._read_sw360_component_from_page = function () {
            var sw360Component = {
                description: "",
                homepage: "",
                categories: [],
                languages: [],
                licenses: [],
                vendor: "",
                name: "",
                externalIds: []
            };

            try {
                var vendor = document
                    .getElementById("releaseAggregateTable")
                    .getElementsByTagName("tbody")[0]
                    .getElementsByTagName("tr")[0]
                    .getElementsByTagName("td")[1].innerHTML;
                if (vendor.indexOf(",") > 0) {
                    vendor = vendor.split(",")[0];
                }

                var name = document
                    .getElementById("componentOverview")
                    .getElementsByTagName("tbody")[0]
                    .getElementsByTagName("tr")[0]
                    .getElementsByTagName("td")[1].innerHTML;
                if (name.indexOf("/") > 0) {
                    var data = name.split("/");
                    name = data[1];
                    if (!vendor) {
                        vendor = data[0];
                    }
                }
                sw360Component.name = name ? name : "";
                sw360Component.vendor = vendor ? vendor : "";
            } catch (e) {
                console.debug("name/vendor not readable");
            }

            try {
                sw360Component.description = document
                    .getElementById("up_Summary")
                    .getElementsByTagName("p")[0]
                    .innerHTML;
            } catch (e) {
                console.debug("description not readable");
            }

            var tableData = document
                .getElementById("componentOverview")
                .getElementsByTagName("tbody")[0]
                .getElementsByTagName("tr");

            try {
                sw360Component.homepage = tableData[5]
                    .getElementsByTagName("td")[1]
                    .getElementsByTagName("a")[0]
                    .innerHTML;
            } catch (e) {
                console.debug("homepage not readable");
            }

            try {
                sw360Component.categories = _this._clean_string_array(
                    tableData[3]
                        .getElementsByTagName("td")[1]
                        .innerHTML
                        .split(","));
            } catch (e) {
                console.debug("categories not readable");
            }

            tableData = document
                .getElementById("releaseAggregateTable")
                .getElementsByTagName("tbody")[0]
                .getElementsByTagName("tr");

            try {
                sw360Component.languages = _this._clean_string_array(
                    tableData[1]
                        .getElementsByTagName("td")[1]
                        .innerHTML
                        .split(","));
            } catch (e) {
                console.debug("languages not readable");
            }

            try {
                var licenses = tableData[4]
                    .getElementsByTagName("td")[1]
                    .getElementsByTagName("a");
                for (var i = 0; i < licenses.length; i++) {
                    sw360Component.licenses.push(licenses[i].innerHTML);
                }
            } catch (e) {
                console.debug("licenses not readable");
            }

            try {
                var spans = document.querySelectorAll('.mapDisplayRootItem span');
                spans = Array.from(spans);
                var p = 0;
                do {
                    var spanName = spans[p];
                    var spanValue = spans[p + 1];
                    var external = {
                        name: spanName.innerHTML.trim(),
                        value: spanValue.innerHTML.trim()
                    };
                    sw360Component.externalIds.push(external);
                    p = p + 2;
                } while (p < spans.length);
            } catch (e) {
                console.debug("purl not available");
            }

            return sw360Component;
        };

        this.activateMerge = function (componentUrl) {
            _this.api.componentUrl = componentUrl + urlParams + new Date().getTime();

            var sw360ComponentFromPage = _this._read_sw360_component_from_page();
            var externalIds = sw360ComponentFromPage.externalIds;

            var updateComponentDiff = function (e) {
                var diff = JSON.parse(e.target.getAttribute("data"));
                var form = document.getElementById("component_edit_form");
                _this.modal.setLoader();
                var keys = Object.keys(diff);
                for (var i = 0; i < keys.length; i++) {
                    if (keys[i] === "purl") {
                        externalIds.push({
                            name:"purl.id",
                            value: diff["purl"]
                        });
                    } else {
                        var input = document.createElement("input");
                        input.name = edit_form_fields[keys[i]];
                        input.value = diff[keys[i]];
                        form.appendChild(input);
                    }
                }

                if (externalIds.length > 0) {
                    externalIds.forEach(function (id) {
                        var timestamp =  Date.now() + (Math.floor(Math.random() * Math.floor(1000)));

                        var input = document.createElement("input");
                        input.name = edit_form_fields.externalIdKey + timestamp;
                        input.value = id.name;
                        form.appendChild(input);

                        var input2 = document.createElement("input");
                        input2.name = edit_form_fields.externalIdValue + timestamp;
                        input2.value = id.value;
                        form.appendChild(input2);
                    });
                }

                form.submit();
            };

            var showDiffForMerge = function (externalComponent) {
                var diff = _this._check_component_diff(sw360ComponentFromPage, externalComponent);
                var keys = Object.keys(diff);
                if (keys.length > 0) {
                    var table = _this.create_table();

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
                    var data = e.target.getAttribute("data");
                    showDiffForMerge(JSON.parse(data));
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

                    var limit = 3;
                    if (!sw360ComponentFromPage.vendor) {
                        limit += 2;
                    }
                    if (!sw360ComponentFromPage.name) {
                        limit += 2;
                    }
                    if (!sw360ComponentFromPage.languages || sw360ComponentFromPage.languages.length === 0) {
                        limit += 2;
                    }
                    if (!sw360ComponentFromPage.licenses || sw360ComponentFromPage.licenses.length === 0) {
                        limit += 2;
                    }

                    var query = {
                        uuid: "",
                        searchQuery: sw360ComponentFromPage.name,
                        ownerQuery: sw360ComponentFromPage.vendor,
                        limit: limit,
                        filter: {
                            languages: sw360ComponentFromPage.languages,
                            licenses: sw360ComponentFromPage.licenses
                        }
                    };

                    _this._match_component(_this.api.componentUrl, query, function (componentList) {
                        if (componentList.length > 0) {
                            var table = _this.create_table();

                            for (var i = 0; i < componentList.length; i++) {
                                var component = componentList[i];

                                var row = document.createElement("div");
                                row.style.display = "flex";
                                row.style.flexDirection = "row";
                                row.style.justifyContent = "space-between";
                                row.style.padding = codeScoopCss.sizeXS;
                                row.style.minHeight = codeScoopCss.sizeL;
                                row.style.borderBottom = codeScoopCss.border + codeScoopCss.colorGray;

                                var div = document.createElement("div");
                                var p = document.createElement("p");
                                p.innerHTML = component["owner"] + "/" + component["name"] + " " + component["purl"];
                                div.appendChild(p);
                                row.appendChild(div);

                                var selectButton = document.createElement("button");
                                selectButton.innerHTML = "select";
                                selectButton.style.cursor = "pointer";
                                selectButton.setAttribute("class", "addButton");
                                selectButton.setAttribute("data", JSON.stringify(component));
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
