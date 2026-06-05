/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search;

import java.util.List;

import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.search.SearchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchHandler handler;

    public SearchController(SearchHandler handler){
        this.handler = handler;
    }

    @GetMapping
    List<SearchResult> searchFiltered(
        @RequestParam(name = "text", required = true) String text, 
        @RequestHeader("X-User-Email") String userEmail,
        @RequestParam(name = "typeMask", required = false) List<String> typeMask) {
        try {
            if(typeMask == null) 
                return handler.search(text, null);
            else 
                return handler.searchFiltered(text, null,typeMask);
        } catch (Exception ex) { 
            throw new SW360Exception("Search execution failed for text: " + text, ex);
        }
    }
    
}
