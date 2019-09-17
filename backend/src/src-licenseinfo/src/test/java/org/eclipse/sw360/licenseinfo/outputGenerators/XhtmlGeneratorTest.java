/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * With modifications by Siemens AG, 2018-2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.outputGenerators;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.projects.Project;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.io.SAXReader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.StringReader;
import java.util.*;
import java.util.function.BinaryOperator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class XhtmlGeneratorTest {
    static Collection<LicenseInfoParsingResult> lipresults;
    static Collection<LicenseInfoParsingResult> lipresults2;
    static Collection<LicenseInfoParsingResult> lipresults3;
    static Collection<LicenseInfoParsingResult> lipresultsEmpty;

    static String xmlString;
    static String xmlString2;
    static String xmlString3;
    static String xmlStringEmpty;

    static Document document;
    static Document document2;
    static Document document3;
    static Document documentEmpty;

    static XhtmlGenerator xhtmlGenerator;

    static String cr = "cr";
    static String cr1 = "cr1";
    static String cr2 = "cr2";
    static String CR1 = "CR1";
    static String CR2 = "CR2";
    static String l1 = "l1";
    static String l2 = "l2";
    static String L1 = "L1";
    static String L2 = "L2";
    static String t1 = "first row\nsecond row";
    static String T1 = "T1";
    static String T2 = "T2";
    static String releaseName = "myrelease";
    static String vendorName = "vendor";
    static String version1 = "1";
    static String version2 = "2";


    @BeforeClass
    public static void setUp() throws Exception {
        //first LicenseInfoParsingResult
        LicenseInfo li = new LicenseInfo();
        Set<String> copyrights = new HashSet<String>(Arrays.asList(cr1, cr2));
        li.setCopyrights(copyrights);

        LicenseNameWithText lnt1 = new LicenseNameWithText().setLicenseName(l1).setLicenseText(t1);
        LicenseNameWithText lnt2 = new LicenseNameWithText().setLicenseName(l2).setLicenseText(t1);
        Set<LicenseNameWithText> licenseNameWithTexts = new HashSet<>(Arrays.asList(lnt1, lnt2));

        li.setLicenseNamesWithTexts(licenseNameWithTexts);

        LicenseInfoParsingResult lipresult = generateLIPResult(li, releaseName, version1, vendorName);
        lipresults = Collections.singletonList(lipresult);

        //second LicenseInfoParsingResult
        LicenseInfo li2 = new LicenseInfo();
        Set<String> copyrights2 = new HashSet<String>(Arrays.asList(CR1, CR2));
        li2.setCopyrights(copyrights2);

        LicenseNameWithText lnt11 = new LicenseNameWithText().setLicenseName(L1).setLicenseText(T1);
        LicenseNameWithText lnt12 = new LicenseNameWithText().setLicenseName(L2).setLicenseText(T2);
        Set<LicenseNameWithText> licenseNameWithTexts2 = new HashSet<>(Arrays.asList(lnt11, lnt12));

        li2.setLicenseNamesWithTexts(licenseNameWithTexts2);

        LicenseInfoParsingResult lipresult2 = generateLIPResult(li2, releaseName, version2, vendorName);
        lipresults2 = Arrays.asList(lipresult,lipresult2);

        //LicenseInfoParsingResult with a single copyright and license
        LicenseInfo li3 = new LicenseInfo();
        Set<String> copyrights3 = new HashSet<String>(Arrays.asList(cr));
        li3.setCopyrights(copyrights3);

        LicenseNameWithText lnt31 = new LicenseNameWithText().setLicenseName(l1).setLicenseText(t1);
        Set<LicenseNameWithText> licenseNameWithTexts3 = new HashSet<>(Arrays.asList(lnt31));

        li3.setLicenseNamesWithTexts(licenseNameWithTexts3);

        LicenseInfoParsingResult lipresult3 = generateLIPResult(li3, releaseName, version1, vendorName);
        lipresults3 = Collections.singletonList(lipresult3);

        //LicenseInfoParsingResult with a no copyright and license
        LicenseInfo liEmpty = new LicenseInfo();
        Set<String> copyrightsEmpty = new HashSet();
        liEmpty.setCopyrights(copyrightsEmpty);

        Set<LicenseNameWithText> licenseNameWithTextsEmpty= new HashSet();

        liEmpty.setLicenseNamesWithTexts(licenseNameWithTextsEmpty);

        LicenseInfoParsingResult lipresultEmpty = generateLIPResult(liEmpty, releaseName, version1, vendorName);
        lipresultsEmpty = Collections.singletonList(lipresultEmpty);
        Collection<ObligationParsingResult> obligationResults = new HashSet();

        xhtmlGenerator = new XhtmlGenerator(OutputFormatVariant.DISCLOSURE, "License Disclosure as XHTML");

        Project p = new Project();
        p.setName("myproject");
        p.setVersion("1.0");
        p.setLicenseInfoHeaderText("Lorem");
        p.setObligationsText("Ipsum");
        Map<String, String> externalIds = Collections.emptyMap();

        xmlString = xhtmlGenerator.generateOutputFile(lipresults, p, obligationResults, null, externalIds);
        xmlString2 = xhtmlGenerator.generateOutputFile(lipresults2, p, obligationResults, null, externalIds);
        xmlString3 = xhtmlGenerator.generateOutputFile(lipresults3, p, obligationResults, null, externalIds);
        xmlStringEmpty = xhtmlGenerator.generateOutputFile(lipresultsEmpty, p, obligationResults, null, externalIds);

        generateDocumentsFromXml();
    }

    private static void generateDocumentsFromXml() throws Exception{

        EntityResolver resolver = new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) {
                if ( publicId.equals( "-//W3C//DTD XHTML 1.0 Strict//EN" ) ) {
                    InputStream in = getClass().getResourceAsStream(
                            "/xhtml1-strict.dtd"
                    );
                    return new InputSource( in );
                }
                if(publicId.equals("-//W3C//ENTITIES Latin 1 for XHTML//EN")){
                    InputStream in = getClass().getResourceAsStream(
                            "/xhtml-lat1.ent"
                    );
                    return new InputSource( in );
                }
                if(publicId.equals("-//W3C//ENTITIES Symbols for XHTML//EN")){
                    InputStream in = getClass().getResourceAsStream(
                            "/xhtml-symbol.ent"
                    );
                    return new InputSource( in );
                }
                if(publicId.equals("-//W3C//ENTITIES Special for XHTML//EN")){
                    InputStream in = getClass().getResourceAsStream(
                            "/xhtml-special.ent"
                    );
                    return new InputSource( in );
                }
                return null;
            }
        };

        SAXReader reader = new SAXReader();
        reader.setEntityResolver(resolver);
        document = reader.read(new InputSource(new StringReader(xmlString)));
        document2 = reader.read(new InputSource(new StringReader(xmlString2)));
        document3 = reader.read(new InputSource(new StringReader(xmlString3)));
        documentEmpty = reader.read(new InputSource(new StringReader(xmlStringEmpty)));
    }

    private static LicenseInfoParsingResult generateLIPResult(LicenseInfo info, String releaseName, String version, String vendor){
        return new LicenseInfoParsingResult()
                .setStatus(LicenseInfoRequestStatus.SUCCESS)
                .setLicenseInfo(info)
                .setName(releaseName)
                .setVendor(vendor)
                .setVersion(version);
    }

    @Test
    public void testGenerateOutputFile_EmptyCopyrightAndLicense() throws Exception {
        String copyrights = findCopyrights(documentEmpty, releaseNameString(vendorName, releaseName, version1));
        String licenses = findLicenses(documentEmpty,releaseNameString(vendorName, releaseName, version1));
        assertThat(copyrights, is(""));
        assertThat(licenses, is(""));
    }

    private String releaseNameString(String vName, String rName, String version) {
        return vName + "_" + rName + "_" + version;
    }

    @Test
    public void testGenerateOutputFile_parseSingleCopyright() throws Exception {
        String copyrights = findCopyrights(document3, releaseNameString(vendorName, releaseName, version1));
        assertThat(copyrights, is("\n"+ cr));
    }

    @Test
    public void testGenerateOutputFile_parseCopyrights() throws Exception {
        String copyrights = findCopyrights(document, releaseNameString(vendorName, releaseName, version1));
        assertThat(copyrights, containsString(cr1));
        assertThat(copyrights, containsString(cr2));
    }

    @Test
    public void testGenerateOutputFile_parseCopyrightsFromTwoReleases() throws Exception {
        String copyrights = findCopyrights(document2, releaseNameString(vendorName, releaseName, version1));
        assertThat(copyrights, containsString(cr1));
        assertThat(copyrights.contains(cr2), is(true));
        copyrights = findCopyrights(document2, releaseNameString(vendorName, releaseName, version2));
        assertThat(copyrights, containsString(CR1));
        assertThat(copyrights, containsString(CR2));
    }

    @Test
    public void testGenerateOutputFile_parseSingleLicense() throws Exception {
        String licenses = findLicenses(document3,releaseNameString(vendorName, releaseName, version1));
        assertThat(licenses, is(t1 + "\n"));
    }

    @Test
    public void testGenerateOutputFile_parseLicenses() throws Exception {
        String licenses = findLicenses(document, releaseNameString(vendorName, releaseName, version1));
        assertThat(licenses, containsString(t1));
    }

    @Test
    public void testGenerateOutputFile_parseLicensesFromTwoReleases() throws Exception {
        String licenses = findLicenses(document2, releaseNameString(vendorName, releaseName, version1));
        assertThat(licenses.contains(t1), is(true));
        licenses = findLicenses(document2, releaseNameString(vendorName, releaseName, version2));
        assertThat(licenses, containsString(T1));
        assertThat(licenses, containsString(T2));
    }

    private String findCopyrights(Document document, String releaseNameString) {
        List list = document.selectNodes("//*[local-name()='li'][@id='" + releaseNameString + "']/*[@class='copyrights']/text()");
        StringBuffer result = new StringBuffer();

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Text text = (Text) iter.next();
            if(! "".equals(text.getStringValue().trim())) {
                result.append(text.getStringValue());
            }
        }
        return result.toString();
    }

    private String findLicenses(Document document, String releaseNameString) {
        List list = document.selectNodes("//*[local-name()='li'][@id='"+ releaseNameString + "']/*[local-name()='ul'][@class='licenseEntries']");
        StringBuffer result = new StringBuffer();

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Element element = (Element) iter.next();
            for (Node liObject : element.content()) {
                if (liObject.getNodeType() == Node.ELEMENT_NODE) {
                    Element liElement = (Element) liObject;
                    String licenseEntryId = liElement.attribute("id").getValue();
                    String licenseTextId = licenseEntryId.replace("licenseEntry", "licenseText");
                    List licenseTexts = document
                            .selectNodes("//*[local-name()='pre'][@id='" + licenseTextId + "']/text()");
                    Object licenseText = licenseTexts.stream().map(l -> ((Text) l).getStringValue()).reduce("",
                            (BinaryOperator<String>) (l1, l2) -> (String) (l1 + l2));
                    result.append(((String) licenseText).trim());
                    result.append("\n");
                }
            }
        }

        return result.toString();
    }
}
