/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License Version 2.0 as published by the
 * Free Software Foundation with classpath exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2.0 for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (please see the COPYING file); if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package org.eclipse.sw360.vmcomponents.common;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * includes not only the {@see RequestSummary} but rather the effected elements
 * @author stefan.jaeger@evosoft.com
 */
public class VMResult<T> {
    public RequestSummary requestSummary;
    public List<T> elements = new ArrayList<>();

    public VMResult(RequestSummary requestSummary){
        this.requestSummary = requestSummary;
    }

    public VMResult(RequestSummary requestSummary, T element){
        this(requestSummary, Arrays.asList(element));
    }

    public VMResult(RequestSummary requestSummary, List<T> elements){
        this.requestSummary = requestSummary;
        this.elements = elements;
    }

    public VMResult consolidateResult(VMResult additional){
        return consolidateResult(this, additional);
    }

    public static VMResult consolidateResult(VMResult consolidated, VMResult additional){
        if (consolidated == null) return additional;
        if (additional == null) return consolidated;
        // consolidate items
        if (consolidated.elements == null){
            consolidated.elements = new ArrayList<>();
        }
        if (additional.elements != null){
            consolidated.elements.addAll(additional.elements);
        }
        // check for null requestSummary
        if (consolidated.requestSummary == null){
            consolidated.requestSummary = additional.requestSummary;
        } else if (additional.requestSummary == null){
            // nothing to do
        } else {
            // consolidate status
            if (consolidated.requestSummary.getRequestStatus() == RequestStatus.SUCCESS) {
                consolidated.requestSummary.setRequestStatus(additional.requestSummary.getRequestStatus());
            }
            // count elements
            consolidated.requestSummary.totalElements += additional.requestSummary.getTotalElements();
            consolidated.requestSummary.totalAffectedElements += additional.requestSummary.getTotalAffectedElements();
            // append messages
            if (consolidated.requestSummary.getMessage() == null){
                consolidated.requestSummary.setMessage(additional.requestSummary.getMessage());
            } else if (additional.requestSummary.getMessage() == null){
                // nothing to do
            } else {
                consolidated.requestSummary.message += "\n" + additional.requestSummary.getMessage();
            }
        }
        return consolidated;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("VMResult{");
        sb.append("requestSummary=").append(requestSummary);
        sb.append(", elements=").append(elements);
        sb.append('}');
        return sb.toString();
    }
}
