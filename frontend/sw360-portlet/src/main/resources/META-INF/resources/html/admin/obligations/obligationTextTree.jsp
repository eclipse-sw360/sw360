<%--
  ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp"%>
<%@ page import="org.eclipse.sw360.portal.users.UserCacheHolder" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.Obligation" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode" %>

<style type="text/css">
.tree-container {
    padding-left: 0;
}

.tree-container ul,
.tree-container li {
    list-style-type: circle;
    margin: 0.5rem;
}

.tree-container .btn-link {
    color: blue;
}

#obligationText {
    padding-left: 0;
    list-style-type: none !important;
}

#out {
    font-size: 100%;
}

#tree input {
    width: 15rem;
}

#tree .obObject {
    width: 20rem;
}

.elementType, .other,
.obLangElement, .obAction, .obObject {
    display: inline-block;
}

#obligationTitle {
    color: black;
    font-weight: bold;
}

#out {
    font-size: 1rem;
    line-height: 3rem;
}
</style>
<tr>
    <td colspan="3">
        <div class="form-group">
            <label for="obligsText"><liferay-ui:message key="text"/></label>
            <div class="invalid-feedback" id="empty-text">
                <liferay-ui:message key="please.enter.a.text" />
            </div>
            <div id="obligationTree">
                <main class="container tree-container">
                    <div class="wrapper">
                        <div class="main-ctn">
                            <div id="tree">
                                <ul id="obligationText">
                                    <li class="tree-node" id="root">
                                        <input id="obligationTitle" type="text" name="<portlet:namespace/><%=ObligationNode._Fields.NODE_TEXT%>" class="elementType form-control" placeholder="<liferay-ui:message key="title" />">
                                        <span class="controls">
                                            &raquo;
                                            <a class="btn-link" href="#" data-func="add-child"
                                                >+<liferay-ui:message key="child" /></a
                                            >
                                        </span>
                                    </li>
                                </ul>
                            </div>

                            <hr />

                            <div class="right">
                                <b class="app-subtitle"><liferay-ui:message key="preview" /></b>
                                <div class="output-tree-ctn" style="padding-left: 2.5rem">
                                    <pre id="out"></pre>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>

                <div style="display: none;" class="hidden" id="template">
                    <ul>
                        <li class="tree-node">
                            <input type="text" name="<portlet:namespace/><%=ObligationNode._Fields.NODE_TYPE%>" class="elementType form-control" list="typeList" placeholder="<liferay-ui:message key="type" />">
                            <datalist id="typeList" class="typeListData">
                                <option value="<Obligation>">
                            </datalist>

                            <%-- Obligation element --%>
                            <input type="text" name="<portlet:namespace/><%=ObligationElement._Fields.LANG_ELEMENT%>" class="obLangElement form-control" list="obLangElement" element-type="<Obligation>" placeholder="<liferay-ui:message key="language.element" />">
                            <datalist id="obLangElement" class="obLangElementData">
                            </datalist>

                            <input type="text" name="<portlet:namespace/><%=ObligationElement._Fields.ACTION%>" class="obAction form-control" list="obAction" element-type="<Obligation>" placeholder="<liferay-ui:message key="action" />">
                            <datalist id="obAction" class="obActionData">
                            </datalist>

                            <input type="text" name="<portlet:namespace/><%=ObligationElement._Fields.OBJECT%>" class="obObject form-control" list="obObject" element-type="<Obligation>" placeholder="<liferay-ui:message key="object" />">
                            <datalist id="obObject" class="obObjectData">
                            </datalist>

                            <%-- Other Type --%>
                            <input type="text" name="<portlet:namespace/><%=ObligationNode._Fields.NODE_TEXT%>" class="other form-control" list="otherText" element-type="Other" placeholder="<liferay-ui:message key="text" />">
                            <datalist id="otherText" class="otherTextData">
                            </datalist>

                            <%-- Action with element --%>
                            <span class="controls">
                                &raquo;
                                <a class="btn-link" href="#"Attribute data-func="add-sibling">+<liferay-ui:message key="sibling" /></a> |
                                <a class="btn-link" href="#" data-func="add-child">+<liferay-ui:message key="child" /></a> |
                                <a class="btn-link" href="#" data-func="delete"><liferay-ui:message key="delete" /></a> |
                                <a class="btn-link" href="#" data-func="import" id="importObligationElementtButton"><liferay-ui:message key="import" /></a>
                            </span>
                        </li>
                    </ul>
                </div>
            </div>
            <%@ include file="/html/admin/obligations/includes/searchObligationElements.jsp" %>
            <%@ include file="/html/utils/includes/requirejs.jspf" %>
        </div>
    </td>
</tr>
<script>
require(['jquery', 'modules/dialog', 'bridges/datatables', 'utils/keyboard'], function($, dialog, datatables, keyboard) {
    var $dataTable,
    $dialog;

    let obligationObj = jQuery.parseJSON(JSON.stringify(${ obligationJson }));
    let obligationNodeListObj = jQuery.parseJSON(JSON.stringify(${ obligationNodeListJson }));
    let obligationElementListObj = jQuery.parseJSON(JSON.stringify(${ obligationElementListJson }));
    let obligationTextObj = jQuery.parseJSON(JSON.stringify(${ obligationTextJson }));

    $(document).ready(function () {
        const indent = "    ",                  // Use 4 spaces for indentation of previewing
            ul_template = $("#template > ul");

        const action = {
            "add-sibling": function (obj) {
                var newNode = ul_template.clone();
                var newId = Date.now();
                newNode.find('#otherText').attr('id', newId);
                newNode.find("[list='otherText']").attr('list', newId);
                obj.parent().after(newNode);
                $('.elementType').on('change keyup', changeElementType);
                showElementControls(newNode.find(".elementType"));
            },
            "add-child": function (obj) {
                var newNode = ul_template.clone();
                var newId = Date.now();
                newNode.find('#otherText').attr('id', newId);
                newNode.find("[list='otherText']").attr('list', newId);
                obj.append(newNode);
                $('.elementType').on('change keyup', changeElementType);
                showElementControls(newNode.find(".elementType"));
            },
            delete: function (obj) {
                obj.parent().remove();
            },
            "import": function (obj) {
                showObligationElementDialog(obj);
            },
        };

        $(document).on("click", "li.tree-node .controls > a", function () {
            action[this.getAttribute("data-func")]($(this).closest("li"));
            updatePreview();
            return false;
        });

        //Disable the first textbox because the title will be set automatically
        $("#root .elementType").first().prop('disabled', true);

        $('#todoTitle').on('change keyup', (event) => {
            $("#root .elementType").first().val(document.getElementById('todoTitle').value)
            updatePreview()
        });

        const typeSuggestions = getTypeSuggestions();

        const obligationSuggestions = getObligationSuggestions();

        function getJSONObjectKeys(object) {
            var keys = [];

            for (var key in object) {
                keys.push(key);
            }

            return keys;
        }

        function getTypeSuggestions() {
            var suggestions = {};

            for (let i = 0; i < obligationNodeListObj.length; i++) {
                let nodeType = obligationNodeListObj[i].nodeType;
                if (nodeType != "" && nodeType != "ROOT" && !suggestions.hasOwnProperty(nodeType)) {
                    suggestions[nodeType] = new Set();
                }
                if (suggestions.hasOwnProperty(nodeType)) {
                    suggestions[nodeType].add(obligationNodeListObj[i].nodeText);
                }
            }

            delete suggestions.Obligation;
            suggestions['<Obligation>'] = new Set();

            return suggestions;
        }

        function getObligationSuggestions() {
            var suggestions = {};
            suggestions['LE'] = new Set();
            suggestions['Action'] = new Set();
            suggestions['Object'] = new Set();

            for (let i = 0; i < obligationElementListObj.length; i++) {
                suggestions['LE'].add(obligationElementListObj[i].langElement);
                suggestions['Action'].add(obligationElementListObj[i].action);
                suggestions['Object'].add(obligationElementListObj[i].object);
            }

            suggestions['LE'].add("YOU MUST")
            suggestions['LE'].add("YOU MUST NOT")

            if (suggestions['Action'].size === 0) {
                suggestions['Action'].add("Provide")
                suggestions['Action'].add("Modify")
            }

            if (suggestions['Object'].size === 0) {
                suggestions['Object'].add("Copyright notices")
                suggestions['Object'].add("License text")
            }

            return suggestions;
        }

        function addSuggestion(selector, suggestions) {
            $.each($(selector), function(i, element) {
                $(element).empty();
                $.each(suggestions.sort(), function(j, suggestion) {
                    $(element).html($(element).html() + "<option value=\"" + suggestion + "\">");
                });
            });
        }

        addSuggestion(".typeListData", getJSONObjectKeys(typeSuggestions));
        addSuggestion(".obLangElementData", Array.from(obligationSuggestions['LE']));
        addSuggestion(".obActionData", Array.from(obligationSuggestions['Action']));
        addSuggestion(".obObjectData", Array.from(obligationSuggestions['Object']));


        $(".elementType").each(function() {
            showElementControls($(this));
        });

        function changeElementType() {
            showElementControls($(this));

            const type = $(this).val();

            if (typeSuggestions.hasOwnProperty(type)) {
                addSuggestion($(this).siblings('.otherTextData'), Array.from(typeSuggestions[type]));
            }
        }

        function showElementControls(typeControl) {
            var type = typeControl.val();
            var siblings = typeControl.siblings();
            siblings.hide();

            if (type == '<Obligation>') {
                typeControl.css('font-style', 'italic');

                $.each(siblings, function(key, sibling) {
                    if ($(sibling).attr('element-type') == type) {
                        $(sibling).show();
                    }
                });
            } else {
                typeControl.css('font-style', 'normal');

                $.each(siblings, function(key, sibling) {
                    if ($(sibling).attr('element-type') == undefined) {
                        $(sibling).hide();
                        return;
                    }
                    if ($(sibling).attr('element-type') != '<Obligation>') {
                        $(sibling).show();
                    }
                });
            }

            typeControl.siblings().each(function(key, element) {
                if($(element).is('ul')) {
                    $(element).css('display', '');
                }
            });
        }

        function getNodeText(node, pad) {
            let padding = pad || "",
                out = "",
                items = node.children("li");

            items.each(function (index) {
                if ($(this).attr('id') != "root") {
                    if ($(this).children(".elementType").val() == "<Obligation>") {
                        out += padding +
                                ($(this).children(".obLangElement").val() == null ? "" : $(this).children(".obLangElement").val()) + " " +
                                ($(this).children(".obAction").val() == null ? "" : $(this).children(".obAction").val()) + " " +
                                ($(this).children(".obObject").val() == null ? "" : $(this).children(".obObject").val()) + "\n";
                    } else {
                        out += padding +
                                $(this).children(".elementType").val() + " " +
                                ($(this).children(".other").val() == null ? "" : $(this).children(".other").val()) + "\n";
                    }
                }

                const childNodes = $(this).children("ul");

                if (childNodes.length) {
                    out += getNodeText(childNodes, padding + indent);
                }
            });

            return out;
        }

        function updatePreview() {
            $("#out").text($('#todoTitle').val() + '\n' + getNodeText($("#obligationText")));
        }

        $(document).on("keyup", "#tree input", updatePreview);

        $(document)
            .on("mouseover", "li, #tree", function (e) {
                $(this).children(".controls").show();
                e.stopPropagation();
            })
            .on("mouseout", "li, #tree", function (e) {
                $(this).children(".controls").hide();
                e.stopPropagation();
            });

        let obligationText = obligationObj.text;

        // buildTreeNodeFromText(obligationText);
        buildNode(obligationTextObj, $('#root'));

        let oblTitle = obligationObj.title;

        $('#todoTitle').val(oblTitle);

        $('#root').find('input').first().val(oblTitle);

        updatePreview();

        // Processing for Import Obligation dialog

        keyboard.bindkeyPressToClick('searchobligationelement', 'searchbuttonobligation');

        $('[data-dismiss=modal]').on('click', function (e) {
            var $t = $(this),
            target = $t[0].href || $t.data("target") || $t.parents('.modal') || [];

        $(target)
           .find("input,textarea,select")
               .val('')
               .end()
           .find("input[type=checkbox], input[type=radio]")
               .prop("checked", "")
               .end();
        })

        // if search func work can call func showObligationElements() after click #searchbuttonobligation

        $('#searchbuttonobligation').on('click', function() {
            showObligationElements();
        });

        function showObligationElements() {
            obligationElementContentFromAjax('<%=PortalConstants.OBLIGATION_ELEMENT_SEARCH%>', $('#searchobligationelement').val(), function(data) {
                    if ($dataTable) {
                        $dataTable.destroy();
                    }
                    $('#obligationElementSearchResultstable tbody').html(data);
                    makeObligatiobElementsDataTable();
            });
        }

        $('#obligationElementSearchResultstable').on('change', 'input', function() {
            $dialog.enablePrimaryButtons($('#obligationElementSearchResultstable input:checked').length > 0);
        });

        function obligationElementContentFromAjax(what, where, callback) {
            $dialog.$.find('.spinner').show();
            $dialog.$.find('#obligationElementSearchResultstable').hide();
            $dialog.$.find('#searchbuttonobligation').prop('disabled', true);
            $dialog.enablePrimaryButtons(false);

            jQuery.ajax({
                type: 'POST',
                url: '<%=viewObligationELementURL%>',
                data: {
                    '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                    '<portlet:namespace/><%=PortalConstants.WHERE%>': where
                },
                success: function (data) {
                    callback(data);

                    $dialog.$.find('.spinner').hide();
                    $dialog.$.find('#obligationElementSearchResultstable').show();
                    $dialog.$.find('#searchbuttonobligation').prop('disabled', false);
                },
                error: function() {
                    $dialog.alert('Can not import Obligation ELement');
                }
            });
        }

        function makeObligatiobElementsDataTable() {
            $dataTable = datatables.create('#obligationElementSearchResultstable', {
                destroy: true,
                paging: false,
                info: false,
                language: {
                    emptyTable: "<liferay-ui:message key="no.obligation.element.found" />",
                    processing: "<liferay-ui:message key="processing" />",
                    loadingRecords: "<liferay-ui:message key="loading" />",
                    emptyTable: "<liferay-ui:message key="please.perform.a.new.search" />"
                },
                select: 'single'
            }, undefined, [0]);
            datatables.enableCheckboxForSelection($dataTable, 0);
        }

        function showObligationElementDialog(obj) {
            if($dataTable) {
                $dataTable.destroy();
                $dataTable = undefined;
            }

            $dialog = dialog.open('#searchObligationElementsDialog', {
            }, function(submit, callback) {
                var obligationElement = [];
                if($("input[type='radio'].form-check-input").is(':checked')) {
                    var selected_value =  $("input[type='radio'].form-check-input:checked").val();
                    obligationElement.push($("input[type='radio'].form-check-input:checked").attr("lang"));
                    obligationElement.push($("input[type='radio'].form-check-input:checked").attr("action"));
                    obligationElement.push($("input[type='radio'].form-check-input:checked").attr("object"));

                    obj.first().children(".elementType").val("<Obligation>")
                    obj.first().children(".other").attr("style","display:none;")
                    obj.first().children(".obLangElement").attr("style","").val(obligationElement[0])
                    obj.first().children(".obAction").attr("style","").val(obligationElement[1])
                    obj.first().children(".obObject").attr("style","").val(obligationElement[2])
                }
                updatePreview();
                callback(true);
            }, function() {
                this.$.find('.spinner').hide();
                this.$.find('#obligationElementSearchResultstable').hide();
                this.$.find('#searchobligationelement').val('');
                this.enablePrimaryButtons(false);
            });
        }
    });
});
</script>