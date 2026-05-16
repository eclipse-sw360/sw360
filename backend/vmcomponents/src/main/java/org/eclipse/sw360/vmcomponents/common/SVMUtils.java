/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.common;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.db.SvmHttpClientFactory;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.apache.thrift.TBase;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author stefan.jaeger@evosoft.com
 */
public class SVMUtils {

    private static final Logger log = LogManager.getLogger(SVMUtils.class);

    private SVMUtils() {}

    public static String prepareJSONRequestAndGetResponse(String url) throws IOException {
        return prepareJSONRequestAndGetResponse(url, null);
    }

    /**
     * Performs an HTTP GET request against the given URL and returns the response body as a String.
     * If {@code modifiedAfter} provided, use it with {@code modified_after} parameter.
     *
     * @param url the URL to call
     * @param modifiedAfter timestamp to fetch updates after
     * @return the response body
     * @throws IOException if the request fails or returns a non-200 status code
     */
    public static String prepareJSONRequestAndGetResponse(String url, String modifiedAfter) throws IOException {
        String finalUrl = url;
        if (CommonUtils.isNotNullEmptyOrWhitespace(modifiedAfter)) {
            String separator = url.contains("?") ? "&" : "?";
            finalUrl = url + separator + "modified_after=" + modifiedAfter;
        }
        log.debug("Call URL: {}", finalUrl);
        CloseableHttpClient httpClient = SvmHttpClientFactory.getTrustedClient();
        HttpGet httpGet = new HttpGet(finalUrl);
        httpGet.setHeader("Accept", "application/json");
        return httpClient.execute(httpGet, response -> {
            int code = response.getCode();
            if (code != 200) {
                String errorMessage = "Failed : HTTP error code : " + code;
                log.error(errorMessage);
                throw new IOException(errorMessage);
            }
            String body = new String(response.getEntity().getContent().readAllBytes());
            log.debug("Response from Server .... \n{}{}",
                    body.substring(0, Math.min(500, body.length())),
                    body.length() > 500 ? "..." : "");
            return body;
        });
    }

    /**
     * Compute the {@code modified_after} timestamp for a delta SVM call based
     * on last run timestamp and the offset. Calculated as
     * {@code lastEndDate - offsetDays}.
     * <p>
     * SW360 stores {@code VMProcessReporting.endDate} in server's local
     * timezone. All shift arithmetic is performed in that local zone. The
     * resulting timestamp is then converted to <strong>UTC</strong> for the SVM
     * request, since SVM expects {@code modified_after} in UTC
     * ({@code yyyy-MM-dd'T'HH:mm:ss}).
     *
     * @param lastEndDate end-date string of the last successful sync (local time)
     * @param offsetDays  number of days to subtract before sending to SVM (overlap window)
     * @return UTC timestamp string suitable for SVM's {@code modified_after} parameter.
     *         The function can return {@code null} if lastEndDate is null or
     *         there was error in calculating the new date. This should be
     *         handled and used for force complete sync.
     */
    @Nullable
    public static String calculateModifiedAfter(String lastEndDate, int offsetDays) {
        try {
            if (CommonUtils.isNullEmptyOrWhitespace(lastEndDate)) {
                return null;
            }
            // 1) Parse and shift in server-local time.
            SimpleDateFormat sw360Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date lastDate = sw360Format.parse(lastEndDate);

            ZoneId localZone = ZoneId.systemDefault();
            LocalDateTime localLast = LocalDateTime.ofInstant(lastDate.toInstant(), localZone);
            LocalDateTime localShifted = localLast.minusDays(offsetDays);

            // 2) Anchor in local zone, then convert to UTC for SVM output.
            ZonedDateTime utc = localShifted.atZone(localZone).withZoneSameInstant(ZoneOffset.UTC);

            DateTimeFormatter svmFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return utc.format(svmFormat);
        } catch (Exception e) {
            log.error("Error calculating modified_after parameter: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create new {@link RequestSummary} object with given values.
     * @return New RequestSummary object.
     */
    @Contract("_, _, _, _ -> new")
    public static RequestSummary newRequestSummary(RequestStatus status, int totalElements, int totalAffectedElements, String message) {
        RequestSummary summary = new RequestSummary();
        summary.setRequestStatus(status);
        summary.setTotalElements(totalElements);
        summary.setTotalAffectedElements(totalAffectedElements);
        summary.setMessage(message);
        return summary;
    }

    /**
     * For a given SVM component, get ID.
     * @param t Component to get ID for.
     * @return The ID of the component in SVM.
     * @param <T> Type of SVM Object. Can be one of: {@link VMComponent},
     *           {@link VMAction}, {@link VMPriority} or {@link Vulnerability}.
     */
    public static <T extends TBase> String getVmid(T t) {
        if (VMComponent.class.isAssignableFrom(t.getClass()))
            return ((VMComponent) t).getVmid();
        else if (VMAction.class.isAssignableFrom(t.getClass()))
            return ((VMAction) t).getVmid();
        else if (VMPriority.class.isAssignableFrom(t.getClass()))
            return ((VMPriority) t).getVmid();
        else if (Vulnerability.class.isAssignableFrom(t.getClass()))
            return ((Vulnerability) t).getExternalId();
        else
            throw new IllegalArgumentException("unknown type " + t.getClass().getSimpleName());
    }

    /**
     * For a given SVM component, get CouchDB ID.
     * @param t Component to get ID for.
     * @return The ID of the component in CouchDB.
     * @param <T> Type of SVM Object. Can be one of: {@link VMComponent},
     *           {@link VMAction}, {@link VMPriority} or {@link Vulnerability}.
     */
    public static <T extends TBase> String getId(T t) {
        if (VMComponent.class.isAssignableFrom(t.getClass()))
            return ((VMComponent) t).getId();
        else if (VMAction.class.isAssignableFrom(t.getClass()))
            return ((VMAction) t).getId();
        else if (VMPriority.class.isAssignableFrom(t.getClass()))
            return ((VMPriority) t).getId();
        else if (Vulnerability.class.isAssignableFrom(t.getClass()))
            return ((Vulnerability) t).getId();
        else
            throw new IllegalArgumentException("unknown type " + t.getClass().getSimpleName());
    }
}
