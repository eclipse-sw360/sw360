/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.common;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;

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
            consolidated.requestSummary.setTotalElements(nullToZero(consolidated.requestSummary.getTotalElements())
                    + nullToZero(additional.requestSummary.getTotalElements()));
            consolidated.requestSummary.setTotalAffectedElements(
                    nullToZero(consolidated.requestSummary.getTotalAffectedElements())
                            + nullToZero(additional.requestSummary.getTotalAffectedElements()));
            // append messages
            if (consolidated.requestSummary.getMessage() == null){
                consolidated.requestSummary.setMessage(additional.requestSummary.getMessage());
            } else if (additional.requestSummary.getMessage() == null){
                // nothing to do
            } else {
                consolidated.requestSummary.setMessage(consolidated.requestSummary.getMessage()
                        + "\n" + additional.requestSummary.getMessage());
            }
        }
        return consolidated;
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
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
