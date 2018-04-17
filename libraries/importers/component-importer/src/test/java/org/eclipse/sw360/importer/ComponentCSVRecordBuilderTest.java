/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.importer;

import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.junit.Test;

import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author johannes.najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ComponentCSVRecordBuilderTest {

    @Test
    public void testFillComponent() throws Exception {
        final Component component = new Component();
        component.setName("Name").setBlog("BLog").setCategories(ImmutableSet.of("cat2", "cat3"))
                .setComponentType(ComponentType.COTS).setCreatedBy("theCreator")
                .setCreatedOn("the4thDay").setDescription("desc").setHomepage("homePage")
                .setBlog("blog also").setMailinglist("Mighty Mails")
                .setSoftwarePlatforms(ImmutableSet.of("Firefox", "Python"));

        final ComponentCSVRecordBuilder componentCSVRecordBuilder = new ComponentCSVRecordBuilder().fill(component);
        final ComponentCSVRecord build = componentCSVRecordBuilder.build();

        assertThat(build.getComponent(), is(component));


    }

    @Test
    public void testFillRelease() throws Exception {

        Release release = new Release();

        release.setName("Name").setCreatedBy("theCreator").setCreatedOn("the5thDay")
                .setVersion("6").setCpeid("cpe2.3://///***").setReleaseDate("theDayOfTheRelease")
                .setDownloadurl("http://www.siemens.com").setMainlineState(MainlineState.MAINLINE)
                .setClearingState(ClearingState.NEW_CLEARING)
                .setContributors(ImmutableSet.of("me", "myself", "and", "I", "are", "singing"))
                .setModerators(ImmutableSet.of("and", "dancing")).setSubscribers(ImmutableSet.of("to", "a"))
                .setLanguages(ImmutableSet.of("silent", "tune")).setOperatingSystems(ImmutableSet.of("which", "is", "licensed"))
                .setMainLicenseIds(ImmutableSet.of("under", "GPL"));


        final ComponentCSVRecordBuilder componentCSVRecordBuilder = new ComponentCSVRecordBuilder().fill(release);
        final ComponentCSVRecord build = componentCSVRecordBuilder.build();
        assertThat(build.getRelease(null, null, null), is(release));

    }

    @Test
    public void testFillVendor() throws Exception {
        Vendor vendor = new Vendor();
        vendor.setFullname("VendorName").setShortname("Ven").setUrl("http://www.siemens.com");
        final ComponentCSVRecordBuilder componentCSVRecordBuilder = new ComponentCSVRecordBuilder().fill(vendor);
        final ComponentCSVRecord build = componentCSVRecordBuilder.build();
        assertThat(build.getVendor(), is(vendor));
    }

    @Test
    public void testFillClearingInfo() throws Exception {
        final ClearingInformation clearingInformation = new ClearingInformation();


        clearingInformation.setExternalSupplierID("C4S")
                .setAdditionalRequestInfo("NG").setEvaluated("eval").setProcStart("proc")
                .setRequestID("req").setScanned("e").setClearingStandard("CL").setComment("wittyh comment")
                .setExternalUrl("share me").setBinariesOriginalFromCommunity(true).setBinariesSelfMade(false)
                .setComponentLicenseInformation(true).setSourceCodeDelivery(true)
                .setSourceCodeOriginalFromCommunity(false).setSourceCodeToolMade(false)
                .setSourceCodeSelfMade(false).setScreenshotOfWebSite(true)
                .setFinalizedLicenseScanReport(false).setLicenseScanReportResult(true)
                .setLegalEvaluation(true).setLicenseAgreement(false)
                .setComponentClearingReport(false).setCountOfSecurityVn(2323);

        final ComponentCSVRecordBuilder componentCSVRecordBuilder = new ComponentCSVRecordBuilder().fill(clearingInformation);

        final ComponentCSVRecord build = componentCSVRecordBuilder.build();
        assertThat(build.getClearingInformation(), is(clearingInformation));
    }

    @Test
    public void testFillEccInfo() throws Exception {
        final EccInformation eccInformation = newDefaultEccInformation();


        eccInformation
                .setEccStatus(ECCStatus.APPROVED)
                .setAL("AL")
                .setECCN("ECCN")
                .setMaterialIndexNumber("MIN")
                .setEccComment("Comment")
                .setAssessorContactPerson("JN")
                .setAssessorDepartment("T")
                .setAssessmentDate("date");

        final ComponentCSVRecordBuilder componentCSVRecordBuilder = new ComponentCSVRecordBuilder().fill(eccInformation);

        final ComponentCSVRecord build = componentCSVRecordBuilder.build();
        assertThat(build.getEccInformation(), is(eccInformation));
    }

    @Test
    public void testFillRepository() throws Exception {
        final Repository repository = new Repository();
        repository.setRepositorytype(RepositoryType.ALIENBRAIN).setUrl("http://www.siemens.com");
        final ComponentCSVRecordBuilder componentCSVRecordBuilder = ComponentCSVRecord.builder().fill(repository);
        final ComponentCSVRecord build = componentCSVRecordBuilder.build();
        assertThat(build.getRepository(), is(repository));
    }
}