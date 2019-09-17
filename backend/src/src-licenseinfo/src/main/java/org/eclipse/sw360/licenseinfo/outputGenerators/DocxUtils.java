/*
 * Copyright Bosch Software Innovations GmbH, 2016-2018.
 * With modifications by Siemens AG, 2018.
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

import org.apache.commons.lang.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

public class DocxUtils {

    public static final int FONT_SIZE = 10;
    public static final String ALERT_COLOR = "e95850";
    public static final String FONT_FAMILY = "Calibri";
    public static final String STYLE_HEADING = "Heading2";
    public static final String STYLE_HEADING_3 = "Heading 3";
    private static final int BUFFER_SIZE = 16;
    private static final int ANCHOR_MAX_SIZE = 40;
    private static final String BOOKMARK_PREFIX = "bookmark_";

    private static String cTAbstractNumBulletXML =
            "<w:abstractNum xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" w:abstractNumId=\"0\">"
                    + "<w:multiLevelType w:val=\"hybridMultilevel\"/>"
                    + "<w:lvl w:ilvl=\"0\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"720\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Wingdings\" w:hAnsi=\"Wingdings\" w:hint=\"default\"/></w:rPr></w:lvl>"
                    + "<w:lvl w:ilvl=\"1\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"-\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"1440\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\" w:cs=\"Courier New\" w:hint=\"default\"/></w:rPr></w:lvl>"
                    + "<w:lvl w:ilvl=\"2\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"2160\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Symbol\" w:hAnsi=\"Symbol\" w:hint=\"default\"/></w:rPr></w:lvl>"
                    + "</w:abstractNum>";

    private DocxUtils() {
        //only static members
    }

    public static void addNewLines(XWPFDocument document, int numberOfNewlines) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        addNewLines(run, numberOfNewlines);
    }

    public static void addNewLines(XWPFRun run, int numberOfNewlines) {
        for (int count = 0; count < numberOfNewlines; count++) {
            run.addCarriageReturn();
            run.addBreak(BreakType.TEXT_WRAPPING);
        }
    }

    public static void addPageBreak(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.addBreak(BreakType.TEXT_WRAPPING);
        run.addBreak(BreakType.PAGE);
    }

    public static void setText(XWPFRun run, String text) {
        String[] split = text.split("\n");
        run.setText(split[0]);
        for (int i = 1; i < split.length; i++) {
            run.addBreak();
            run.setText(split[i]);
        }
    }

    public static void addFormattedText(XWPFRun run, String text, String fontFamily, int fontSize, boolean bold, String rrggbbColor) {
        run.setFontSize(fontSize);
        run.setFontFamily(fontFamily);
        run.setBold(bold);
        if (rrggbbColor != null) {
            run.setColor(rrggbbColor);
        }
        setText(run, text);
    }

    public static void addFormattedText(XWPFRun run, String text, int fontSize, boolean bold, String rrggbbColor) {
        addFormattedText(run, text, FONT_FAMILY, fontSize, bold, rrggbbColor);
    }

    public static void addFormattedText(XWPFRun run, String text, int fontSize, boolean bold) {
        addFormattedText(run, text, FONT_FAMILY, fontSize, bold, null);
    }

    public static void replaceText(XWPFDocument document, String placeHolder, String replaceText) {
        for (XWPFHeader header : document.getHeaderList()) {
            replaceAllBodyElements(header.getBodyElements(), placeHolder, replaceText);
        }

        replaceAllBodyElements(document.getBodyElements(), placeHolder, replaceText);

        for (XWPFTable table : document.getTables()) {
            for(XWPFTableRow row : table.getRows()) {
                for(XWPFTableCell cell : row.getTableCells()) {
                    replaceAllBodyElements(cell.getBodyElements(), placeHolder, replaceText);
                }
            }
        }
    }

    private static void replaceAllBodyElements(List<IBodyElement> bodyElements, String placeHolder, String replaceText) {
        for (IBodyElement bodyElement : bodyElements) {
            if (bodyElement.getElementType().compareTo(BodyElementType.PARAGRAPH) == 0)
                replaceParagraph((XWPFParagraph) bodyElement, placeHolder, replaceText);
        }
    }

    private static void replaceParagraph(XWPFParagraph paragraph, String placeHolder, String replaceText) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(run.getTextPosition());
            if (text != null && text.contains(placeHolder)) {
                text = text.replace(placeHolder, replaceText);
                String[] split = text.split("\n");
                run.setText(split[0], 0);
                for (int i = 1; i < split.length; i++) {
                    run.addBreak();
                    run.setText(split[i]);
                }
            }
        }
    }

    public static void addBulletList(XWPFDocument document, List<String> bulletListItems, boolean bulletListItemsAsLink) throws XmlException {
        CTNumbering cTNumbering = CTNumbering.Factory.parse(cTAbstractNumBulletXML);
        CTAbstractNum cTAbstractNum = cTNumbering.getAbstractNumArray(0);
        XWPFAbstractNum abstractNum = new XWPFAbstractNum(cTAbstractNum);
        XWPFNumbering numbering = document.createNumbering();
        BigInteger abstractNumID = numbering.addAbstractNum(abstractNum);
        BigInteger numID = numbering.addNum(abstractNumID);

        for (int i = 0; i < bulletListItems.size(); i++) {
            String bulletItem = bulletListItems.get(i);
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setNumID(numID);
            if (bulletListItemsAsLink) {
                addBookmarkHyperLink(paragraph, bulletItem, bulletItem);
            } else {
                setText(paragraph.createRun(), bulletItem);
            }
            if (i < bulletListItems.size() - 1) {
                paragraph.setSpacingAfter(0);
            }
        }
    }

    public static void addBookmarkHyperLink(XWPFParagraph paragraph, String hyperlinkAnchor, String hyperlinkText) {
        String bookmarkHyperLink = generateValidBookmarkName(hyperlinkAnchor);
        addHyperLink(paragraph, bookmarkHyperLink, hyperlinkText);
    }

    public static void addHyperLink(XWPFParagraph paragraph, String hyperlinkAnchor, String hyperlinkText) {
        CTHyperlink cLink = paragraph.getCTP().addNewHyperlink();
        cLink.setAnchor(hyperlinkAnchor);
        CTText ctText = CTText.Factory.newInstance();
        ctText.setStringValue(hyperlinkText);
        CTR ctr = CTR.Factory.newInstance();
        ctr.setTArray(new CTText[]{ctText});

        // format the hyperlink (underline + color)
        CTRPr rpr = ctr.addNewRPr();
        CTColor colour = CTColor.Factory.newInstance();
        colour.setVal("0000FF");
        rpr.setColor(colour);
        CTRPr rpr1 = ctr.addNewRPr();
        rpr1.addNewU().setVal(STUnderline.SINGLE);

        cLink.setRArray(new CTR[]{ctr});
    }

    public static void addBookmark(XWPFParagraph paragraph, String bookmarkAnchor, String bookmarkText) {
        CTBookmark bookmark = paragraph.getCTP().addNewBookmarkStart();
        String bookmarkName = generateValidBookmarkName(bookmarkAnchor);
        bookmark.setName(bookmarkName);
        final BigInteger bookmarkId = generateRandomId();
        bookmark.setId(bookmarkId);
        addFormattedText(paragraph.createRun(), bookmarkText, FONT_SIZE + 2, true);
        paragraph.getCTP().addNewBookmarkEnd().setId(bookmarkId);
    }

    public static String generateValidBookmarkName(String text) {
        String anchor = BOOKMARK_PREFIX + text.replaceAll("\\s+", "");
        return anchor.substring(0, Math.min(ANCHOR_MAX_SIZE, anchor.length()));
    }

    private static BigInteger generateRandomId() {
        final UUID uuid = UUID.randomUUID();
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[BUFFER_SIZE]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return new BigInteger(byteBuffer.array()).abs();
    }

    public static void removeParagraph(XWPFDocument document, String paragraphText) {
        XWPFParagraph paragraphToDelete = document.getParagraphs().stream()
                .filter(p -> StringUtils.equalsIgnoreCase(paragraphText, p.getParagraphText()))
                .findFirst().orElse(null);
        if (paragraphToDelete != null) {
            document.removeBodyElement(document.getPosOfParagraph(paragraphToDelete));
        }
    }
}
