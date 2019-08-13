/*
 * Copyright Siemens AG, 2017, 2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('modules/sw360Wizard', [ 'jquery', 'modules/button' ], function($, button) {

    /**
     * This module provides a wizardination of some given html, specialized on doing backend calls after each
     * step to receive data for the content of the next step. And here is what you have to do:
     *
     * 1. Write some markup describing the steps of your wizard
     * <div id="myWizard" data-step-id="0" data-foo="bar">
     *     <div class="wizardHeader">
     *         <!-- define your list of step headers -->
     *         <ul>
     *             <li class="active">1. Step Name<br /><small>This can be a short description</small></li>
     *             <li>2. Another Step Name<br /><small>Another description</small></li>
     *         </ul>
     *     </div>
     *     <div class="wizardBody">
     *         <!-- define your list of steps -->
     *         <div class="step active" data-step-id="1">
     *             Default content of step 1 before response is received
     *         </div>
     *         <div class="step" data-step-id="2">
     *             Default content of step 2 before response is received
     *         </div>
     *     </div>
     * </div>
     *
     * 2. You can then grab the wizard framework via require and configure it inside like that:
     * require([ 'modules/sw360Wizard', 'jquery' ], function(wizard, $) {
     *     wizard({
     *         wizardRoot: $('#myWizard'),
     *         postUrl: your-post-url-goes-here,
     *         postParamsPrefix: your-post-params-prefix-goes-here,
     *         steps: [
     *             {
     *                 renderHook: function($stepElement, data) {
     *                     $stepElement.html(data);
     *                 },
     *                 submitHook: function($stepElement) {
     *                     $stepElement.data('myAdditionalData', 'foo');
     *                 },
     *                 submitErrorHook: function($stepElement, textStatus, error) {
     *                     alert('An error happened while communicating with the server: ' + textStatus + error);
     *                 }
     *             },
     *             {
     *                 renderHook: function($stepElement, data) {
     *                     $stepElement.html(data);
     *                 },
     *                 submitHook: function($stepElement) {
     *                     $stepElement.data('myAdditionalData', 'bar');
     *                 },
     *                 submitErrorHook: function($stepElement, textStatus, error) {
     *                     alert('An error happened while communicating with the server: ' + textStatus + error);
     *                 }
     *             }
     *         ],
     *         finishCb: function(data) {
     *             window.location.href = data.redirectUrl;
     *         }
     *     });
     * });
     *
     * You might have guessed already that all intermediate requests are POST requests to the same URL and
     * they contain all the data that is available as data-attribute on the step nodes or that has been
     * added via $.data() to the step element. And each parameter key will be prefixed as configured which is
     * especially useful in portlet environments as parameters will be only passed to the correct portlet if
     * they are prefixed accordingly.
     *
     * The initial call is not part of the configuration as it happens on convention: you can add
     * data-attributes on the wizard root node or via $.data() to this element and it will be included in the
     * initial call that will try to receive data for step 1.
     *
     * When the data for a step is returned from the server, the renderHook will be called with the $element
     * that this data was queried for and the data itself. You can then generate your html and set it as the
     * content of the given $element.
     *
     * When a user thinks he is done with your current step and clicks the next-button, you get the
     * chance to handle any input from your step-html, validate it and add it as data-attribute or $.data()
     * so that it will be included in the upcoming backend call to retrieve the data for the next step.
     * If you return false from the submitHook, no request will be send and you are responsible for
     * displaying user feedback before returning.
     * If the request fails for technical reasons, you can handle that case in the submitErrorHook of the
     * still current step.
     * But if the request succeeded and you just do not receive the expected data from the backend, you have
     * to advise the user what he needs to do in the next renderHook as the step will be switched no matter
     * what you are trying to return from your renderHook.
     *
     * When a user has clicked through all steps, the next button caption is changed into finish and the
     * data received from this backend call will be passed into the special finishCb where you might want to
     * redirect your user onto another page.
     */
    var sw360Wizard = function(config) {
        var $wizardRoot = $(config.wizardRoot),
            firstStep = $('.wizardBody div.step:first', $wizardRoot),
            lastStep = $('.wizardBody div.step:last', $wizardRoot);

        $wizardRoot.append('' +
            '<div class="wizardFooter btn-group content-right">' +
            '    <button type="button" class="wizardBack btn btn-secondary" disabled>Back</button>' +
            '    <button type="button" class="wizardNext btn btn-primary">Next</button>' +
            '</div>'
        );

        function changeHeaderState(lastActiveIndex, activeIndex) {
            $('.wizardHeader li', $wizardRoot).each(function(index, element) {
                if (index === lastActiveIndex) {
                    $(element).removeClass('active');
                }
                if (index === activeIndex) {
                    $(element).addClass('active');
                }
            });
        }

        function determineAndSetFooterState() {
            if (firstStep.hasClass('active')) {
                $('.wizardBack', $wizardRoot).attr('disabled', true);
            } else {
                $('.wizardBack', $wizardRoot).attr('disabled', false);
            }

            if (lastStep.hasClass('active')) {
                $('.wizardNext', $wizardRoot).text('Finish')
            } else {
                $('.wizardNext', $wizardRoot).text('Next');
            }
        }

        function reworkPostData(data) {
            var reworkedPostData = {};
            $.each(data, function(key, value) {
                if (typeof value === 'object') {
                    value = JSON.stringify(value);
                }
                reworkedPostData[config.postParamsPrefix + key] = value;
            });

            return reworkedPostData;
        }

        function fillContentOfStep(activeElement, activeIndex, nextElement, nextIndex) {
            var proceed = true,
                proceedTmp;

            if (nextElement[0] === firstStep[0]) {
                // noop, just send the initial data from the root element
            } else {
                proceedTmp = config.steps[activeIndex].submitHook(activeElement);
                if (typeof proceedTmp === 'boolean') {
                    proceed = proceedTmp;
                }
            }

            if (proceed) {
                button.wait($('.wizardNext', $wizardRoot));
                $.ajax({
                    method: 'POST',
                    url: config.postUrl,
                    data: reworkPostData(activeElement.data()),
                    cache: false
                }).done(function(data, textStatus, xhr) {
                    try {
                        var dataJson = JSON.parse(data);
                        if (activeElement[0] === lastStep[0]) {
                            config.finishCb(dataJson);
                        } else {
                            config.steps[nextIndex].renderHook(nextElement, dataJson);
                        }
                    } catch(error) {
                        config.steps[activeIndex].submitErrorHook(activeElement, '', error);
                    }
                }).fail(function(xhr, textStatus, error){
                    config.steps[activeIndex].submitErrorHook(activeElement, textStatus, error);
                    return false;
                }).always(function() {
                    button.finish($('.wizardNext', $wizardRoot));
                });
            } else {
                return false;
            }
        }

        $(document).ready(function() {
            fillContentOfStep($wizardRoot, null, firstStep, 0);

            $('.wizardNext', $wizardRoot).on('click', function() {
                $('.wizardBody .step', $wizardRoot).each(function(index, element) {
                    var $elem = $(element),
                        proceed = true,
                        proceedTmp;

                    if ($elem.hasClass('active')) {
                        proceedTmp = fillContentOfStep($elem, index, $elem.next(), index+1);
                        if (typeof proceedTmp === 'boolean') {
                            proceed = proceedTmp;
                        }

                        if (proceed && $elem[0] !== lastStep[0]) {
                            $elem.removeClass('active');
                            $elem.next().addClass('active');

                            changeHeaderState(index, index+1);
                        }

                        // break each()
                        return false;
                    }
                });

                determineAndSetFooterState();
            });

            $('.wizardBack', $wizardRoot).on('click', function() {
                $('.wizardBody .step', $wizardRoot).each(function(index, element) {
                    var $elem = $(element);
                    if ($elem.hasClass('active') && $elem[0] !== firstStep[0]) {
                        $elem.removeClass('active');
                        $elem.prev().addClass('active');

                        changeHeaderState(index, index-1);

                        // break each()
                        return false;
                    }
                });

                determineAndSetFooterState();
            });
        });

        return {
                // public API
        };
    };

    return sw360Wizard;

});
