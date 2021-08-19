

    // @UsedAsLiferayAction
    // public void deleteSpdxDocument(ActionRequest request, ActionResponse response) throws PortletException, IOException {
    //     RequestStatus requestStatus = SpdxPortletUtils.deleteSpdxDocument(request, log);

    //     String userEmail = UserCacheHolder.getUserFromRequest(request).getEmail();
    //     String id = request.getParameter(PortalConstants.SPDX_DOCUMENT_ID);
    //     setSessionMessage(request, requestStatus, "SPDXDocument", "delete");

    //     response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
    //     response.setRenderParameter(RELEASE_ID, request.getParameter(RELEASE_ID));
    // }


    // @UsedAsLiferayAction
    // public void updateSpdxDocument(ActionRequest request, ActionResponse response) throws PortletException, IOException {
    //     String id = request.getParameter(RELEASE_ID);
    //     final User user = UserCacheHolder.getUserFromRequest(request);

    //     if (id != null) {
    //         try {
    //             ComponentService.Iface releaseClient = thriftClients.makeComponentClient();
    //             Release release = releaseClient.getReleaseById(id, user);

    //             SPDXDocument spdxDocunent;
    //             String spdxDocunentId = request.getParameter(SPDX_DOCUMENT_ID);
    //             if (spdxDocunentId != null) {
    //                 SPDXDocumentService.Iface client = thriftClients.makeSPDXClient();
    //                 spdxDocunent = client.getSPDXDocumentForEdit(spdxDocunentId, user);
    //                 SpdxPortletUtils.updateSpdxDocumentFromRequest(request, release);
    //                 String ModerationRequestCommentMsg = request.getParameter(MODERATION_REQUEST_COMMENT);
    //                 user.setCommentMadeDuringModerationRequest(ModerationRequestCommentMsg);

    //                 RequestStatus requestStatus = client.updateSPDXDocument(spdxDocunent, user);
    //                 setSessionMessage(request, requestStatus, "SPDXDocument", "update", printName(spdxDocunent));

    //                 cleanUploadHistory(user.getEmail(), releaseId);

    //                 // successful update of release means we want to send a redirect to the detail
    //                 // view to make sure that no POST gets executed twice by some browser reload or
    //                 // back button click (POST-redirect-GET pattern)
    //                 String portletId = (String) request.getAttribute(WebKeys.PORTLET_ID);
    //                 ThemeDisplay tD = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
    //                 long plid = tD.getPlid();

    //                 LiferayPortletURL redirectUrl = PortletURLFactoryUtil.create(request, portletId, plid,
    //                         PortletRequest.RENDER_PHASE);
    //                 redirectUrl.setParameter(PAGENAME, PAGENAME_RELEASE_DETAIL);
    //                 redirectUrl.setParameter(COMPONENT_ID, id);
    //                 redirectUrl.setParameter(RELEASE_ID, releaseId);

    //                 request.setAttribute(WebKeys.REDIRECT, redirectUrl.toString());
    //                 sendRedirect(request, response);
                
    //             } else {
    //                 release = new Release();
    //                 release.setComponentId(component.getId());
    //                 release.setClearingState(ClearingState.NEW_CLEARING);
    //                 ComponentPortletUtils.updateReleaseFromRequest(request, release);

    //                 String cyclicLinkedReleasePath = client.getCyclicLinkedReleasePath(release, user);
    //                 if (!isNullEmptyOrWhitespace(cyclicLinkedReleasePath)) {
    //                     FossologyAwarePortlet.addCustomErrorMessage(CYCLIC_LINKED_RELEASE + cyclicLinkedReleasePath,
    //                             PAGENAME_EDIT_RELEASE, request, response);
    //                     prepareRequestForReleaseEditAfterDuplicateError(request, release);
    //                     response.setRenderParameter(COMPONENT_ID, request.getParameter(COMPONENT_ID));
    //                     return;
    //                 }

    //                 AddDocumentRequestSummary summary = client.addRelease(release, user);

    //                 AddDocumentRequestStatus status = summary.getRequestStatus();
    //                 switch(status){
    //                     case SUCCESS:
    //                         response.setRenderParameter(RELEASE_ID, summary.getId());
    //                         String successMsg = "Release " + printName(release) + " added successfully";
    //                         SessionMessages.add(request, "request_processed", successMsg);
    //                         response.setRenderParameter(PAGENAME, PAGENAME_EDIT_RELEASE);
    //                         break;
    //                     case DUPLICATE:
    //                         setSW360SessionError(request, ErrorMessages.RELEASE_DUPLICATE);
    //                         response.setRenderParameter(PAGENAME, PAGENAME_EDIT_RELEASE);
    //                         prepareRequestForReleaseEditAfterDuplicateError(request, release);
    //                         break;
    //                     case NAMINGERROR:
    //                         setSW360SessionError(request, ErrorMessages.RELEASE_NAME_VERSION_ERROR);
    //                         response.setRenderParameter(PAGENAME, PAGENAME_EDIT_RELEASE);
    //                         prepareRequestForReleaseEditAfterDuplicateError(request, release);
    //                         break;
    //                     default:
    //                         setSW360SessionError(request, ErrorMessages.RELEASE_NOT_ADDED);
    //                         response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
    //                 }

    //                 response.setRenderParameter(COMPONENT_ID, request.getParameter(COMPONENT_ID));
    //             }
    //         } catch (TException e) {
    //             log.error("Error fetching release from backend!", e);
    //         }
    //     }
    // }


    // private void prepareReleaseDetailView(RenderRequest request, RenderResponse response) throws PortletException {
    //     String id = request.getParameter(COMPONENT_ID);
    //     String releaseId = request.getParameter(RELEASE_ID);
    //     final User user = UserCacheHolder.getUserFromRequest(request);

    //     if (isNullOrEmpty(id) && isNullOrEmpty(releaseId)) {
    //         throw new PortletException("Component or Release ID not set!");
    //     }

    //     try {
    //         ComponentService.Iface client = thriftClients.makeComponentClient();
    //         FossologyService.Iface fossologyClient = thriftClients.makeFossologyClient();
    //         Component component;
    //         Release release = null;

    //         if (!isNullOrEmpty(releaseId)) {
    //             release = client.getReleaseById(releaseId, user);

    //             ExternalToolProcessStep processStep = SW360Utils.getExternalToolProcessStepOfFirstProcessForTool(
    //                     release, ExternalTool.FOSSOLOGY, FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD);
    //             ConfigContainer fossologyConfig = fossologyClient.getFossologyConfig();
    //             Map<String, Set<String>> configKeyToValues = fossologyConfig.getConfigKeyToValues();
    //             String fossologyJobsViewLink = null;
    //             if (!configKeyToValues.isEmpty()) {
    //                 fossologyJobsViewLink = createFossologyJobViewLink(processStep, configKeyToValues,
    //                         fossologyJobsViewLink);
    //             }

    //             PortletUtils.setCustomFieldsDisplay(request, user, release);

    //             request.setAttribute(FOSSOLOGY_JOB_VIEW_LINK, fossologyJobsViewLink);
    //             request.setAttribute(RELEASE_ID, releaseId);
    //             request.setAttribute(RELEASE, release);
    //             request.setAttribute(DOCUMENT_ID, releaseId);
    //             request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_RELEASE);
    //             setAttachmentsInRequest(request, release);
    //             setSpdxAttachmentsInRequest(request, release);

    //             setUsingDocs(request, releaseId, user, client);
    //             putDirectlyLinkedReleaseRelationsInRequest(request, release);
    //             request.setAttribute(IS_USER_ALLOWED_TO_MERGE, PermissionUtils.isUserAtLeast(USER_ROLE_ALLOWED_TO_MERGE_OR_SPLIT_COMPONENT, user));

    //             if (isNullOrEmpty(id)) {
    //                 id = release.getComponentId();
    //             }
    //             Set<UserGroup> allSecRoles = !CommonUtils.isNullOrEmptyMap(user.getSecondaryDepartmentsAndRoles())
    //                     ? user.getSecondaryDepartmentsAndRoles().entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet())
    //                     : new HashSet<UserGroup>();
    //             boolean isVulEditable = PermissionUtils.isUserAtLeast(UserGroup.SECURITY_ADMIN, user)
    //                     || PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(UserGroup.SECURITY_ADMIN, allSecRoles);
    //             putVulnerabilitiesInRequestRelease(request, releaseId, user, isVulEditable);
    //             request.setAttribute(VULNERABILITY_VERIFICATION_EDITABLE, isVulEditable);
    //         }

    //         component = client.getComponentById(id, user);
    //         request.setAttribute(COMPONENT, component);

    //         addComponentBreadcrumb(request, response, component);
    //         if (release != null) {
    //             addReleaseBreadcrumb(request, response, release);
    //         }

    //     } catch (TException e) {
    //         log.error("Error fetching release from backend!", e);
    //         setSW360SessionError(request, ErrorMessages.ERROR_GETTING_RELEASE);
    //     }

    // }
















    // private void prepareReleaseEdit(RenderRequest request, RenderResponse response) throws PortletException {
    //     ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());

    //     String id = request.getParameter(COMPONENT_ID);
    //     String releaseId = request.getParameter(RELEASE_ID);
    //     final User user = UserCacheHolder.getUserFromRequest(request);
    //     request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_RELEASE);
    //     request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user));

    //     if (isNullOrEmpty(id) && isNullOrEmpty(releaseId)) {
    //         throw new PortletException("Component or Release ID not set!");
    //     }

    //     try {
    //         ComponentService.Iface client = thriftClients.makeComponentClient();
    //         Component component;
    //         Release release;

    //         if (!isNullOrEmpty(releaseId)) {
    //             release = client.getReleaseByIdForEdit(releaseId, user);
    //             request.setAttribute(RELEASE, release);
    //             request.setAttribute(DOCUMENT_ID, releaseId);
    //             setAttachmentsInRequest(request, release);

    //             putDirectlyLinkedReleaseRelationsInRequest(request, release);
    //             Map<RequestedAction, Boolean> permissions = release.getPermissions();
    //             DocumentState documentState = release.getDocumentState();
    //             setUsingDocs(request, releaseId, user, client);
    //             addEditDocumentMessage(request, permissions, documentState);

    //             if (isNullOrEmpty(id)) {
    //                 id = release.getComponentId();
    //             }
    //             component = client.getComponentById(id, user);

    //         } else {
    //             component = client.getComponentById(id, user);
    //             release = (Release) request.getAttribute(RELEASE);
    //             if(release == null) {
    //                 release = new Release();
    //                 release.setComponentId(id);
    //                 release.setClearingState(ClearingState.NEW_CLEARING);
    //                 release.setVendorId(component.getDefaultVendorId());
    //                 release.setVendor(component.getDefaultVendor());
    //                 request.setAttribute(RELEASE, release);
    //                 putDirectlyLinkedReleaseRelationsInRequest(request, release);
    //                 setAttachmentsInRequest(request, release);
    //                 setUsingDocs(request, null, user, client);
    //                 SessionMessages.add(request, "request_processed", LanguageUtil.get(resourceBundle,"new.license"));
    //             }
    //         }

    //         PortletUtils.setCustomFieldsEdit(request, user, release);
    //         addComponentBreadcrumb(request, response, component);
    //         if (!isNullOrEmpty(release.getId())) { //Otherwise the link is meaningless
    //             addReleaseBreadcrumb(request, response, release);
    //         }

    //         Map<String, String> externalIds = component.getExternalIds();
    //         if (externalIds != null && externalIds.containsKey("purl.id")) {
    //             request.setAttribute(COMPONENT_PURL, externalIds.get("purl.id"));
    //         } else {
    //             request.setAttribute(COMPONENT_PURL, "");
    //         }

    //         Set<UserGroup> allSecRoles = !CommonUtils.isNullOrEmptyMap(user.getSecondaryDepartmentsAndRoles())
    //                 ? user.getSecondaryDepartmentsAndRoles().entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet())
    //                 : new HashSet<UserGroup>();

    //         request.setAttribute(COMPONENT, component);
    //         request.setAttribute(IS_USER_AT_LEAST_ECC_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.ECC_ADMIN, user)
    //                 || PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(UserGroup.ECC_ADMIN, allSecRoles) ? "Yes" : "No");

    //     } catch (TException e) {
    //         log.error("Error fetching release from backend!", e);
    //         setSW360SessionError(request, ErrorMessages.ERROR_GETTING_RELEASE);
    //     }
    // }
