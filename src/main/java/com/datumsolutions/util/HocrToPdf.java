package com.datumsolutions.util;
/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2007
 * @author Florian Hackenberger <florian@hackenberger.at>
 */

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.id.jericho.lib.html.Source;
import au.id.jericho.lib.html.StartTag;


import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.CMYKColor;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

/** A quickhack for converting from hOCR to PDF
 * @author fhackenberger
 */
public class HocrToPdf {

    /**
     * @param args
     */
    public static void GeneratePDFfrom(CharSequence hocrLocation, String destPdf) {
        try {
//            if(args.length < 1 || args[0] == "--help" || args[0] == "-h") {
//                System.out.print(
//                        "Usage: java com.acoveo.hocrtopdf.HocrToPdf INPUTURL.html OUTPUTURL.pdf\n" +
//                                "\n" +
//                                "Converts hOCR files into PDF\n" +
//                                "\n" +
//                                "Example: java com.acoveo.hocrtopdf.HocrToPdf file:///home/username/hocr.html ./output.pdf\n");
//                if(args.length < 1)
//                    System.exit(-1);
//                else
//                    System.exit(0);
//            }
            URL inputHOCRFile = null;
            FileOutputStream outputPDFStream = null;
//            try {
//                inputHOCRFile = new URL(hocrLocation);
//            } catch (MalformedURLException e) {
//                System.out.println("The first parameter has to be a valid URL");
//                System.exit(-1);
//            }
            try {
                outputPDFStream = new FileOutputStream(destPdf);
            } catch (FileNotFoundException e) {
                System.out.println("The second parameter has to be a valid URL");
                System.exit(-1);
            }

            // The resolution of a PDF file (using iText) is 72pt per inch
            float pointsPerInch = 72.0f;

            // Using the jericho library to parse the HTML file
           // Source source=new Source(inputHOCRFile);
            Source source=new Source(hocrLocation);


            // Find the tag of class ocr_page in order to load the scanned image
            StartTag pageTag = source.findNextStartTag(0, "class", "ocr_page", false);
            Pattern imagePattern = Pattern.compile("image\\s+([^;]+)");
            Matcher imageMatcher = imagePattern.matcher(pageTag.getElement().getAttributeValue("title"));
            if(!imageMatcher.find()) {
                System.out.println("Could not find a tag of class \"ocr_page\", aborting.");
                System.exit(-1);
            }
            // Load the image
            Image pageImage = null;
            try {
                URL url = new URL("file://" + imageMatcher.group(1).replace("\"", ""));
                //URL url = new URL("file:///Users/ognjenm/code/open_source/testPdf/files/30310242.png");


                pageImage = Image.getInstance(url);
            } catch (MalformedURLException e) {
                System.out.println("Could not load the scanned image from: " + "file://" + imageMatcher.group(1) + ", aborting.");
                System.exit(-1);
            }
            float dotsPerPointX = pageImage.getDpiX() / pointsPerInch;
            float dotsPerPointY = pageImage.getDpiY() / pointsPerInch;
            float pageImagePixelHeight = pageImage.getHeight();
            Document pdfDocument = new Document(new Rectangle(pageImage.getWidth() / dotsPerPointX, pageImage.getHeight() / dotsPerPointY));
            PdfWriter pdfWriter = PdfWriter.getInstance(pdfDocument, outputPDFStream);
            pdfDocument.open();
            // first define a standard font for our text
            Font defaultFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, CMYKColor.BLACK);

            // Put the text behind the picture (reverse for debugging)
            PdfContentByte cb = pdfWriter.getDirectContentUnder();
            //PdfContentByte cb = pdfWriter.getDirectContent();

            pageImage.scaleToFit(pageImage.getWidth() / dotsPerPointX, pageImage.getHeight() / dotsPerPointY);
            pageImage.setAbsolutePosition(0, 0);
            // Put the image in front of the text (reverse for debugging)
            pdfWriter.getDirectContent().addImage(pageImage);
            //pdfWriter.getDirectContentUnder().addImage(pageImage);

            // In order to place text behind the recognised text snippets we are interested in the bbox property
            Pattern bboxPattern = Pattern.compile("bbox(\\s+\\d+){4}");
            // This pattern separates the coordinates of the bbox property
            Pattern bboxCoordinatePattern = Pattern.compile("(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
            // Only tags of the ocr_line class are interesting
            StartTag ocrLineTag = source.findNextStartTag(0, "class", "ocr_line", false);
            while(ocrLineTag != null) {
                au.id.jericho.lib.html.Element lineElement = ocrLineTag.getElement();
                Matcher bboxMatcher = bboxPattern.matcher(lineElement.getAttributeValue("title"));
                if(bboxMatcher.find()) {
                    // We found a tag of the ocr_line class containing a bbox property
                    Matcher bboxCoordinateMatcher = bboxCoordinatePattern.matcher(bboxMatcher.group());
                    bboxCoordinateMatcher.find();
                    int[] coordinates = {Integer.parseInt((bboxCoordinateMatcher.group(1))),
                            Integer.parseInt((bboxCoordinateMatcher.group(2))),
                            Integer.parseInt((bboxCoordinateMatcher.group(3))),
                            Integer.parseInt((bboxCoordinateMatcher.group(4)))};
                    String line = lineElement.getContent().extractText();
                    float bboxWidthPt = (coordinates[2] - coordinates[0]) / dotsPerPointX;
                    float bboxHeightPt = (coordinates[3] - coordinates[1]) / dotsPerPointY;

                    // Put the text into the PDF
                    cb.beginText();
                    // Comment the next line to debug the PDF output (visible Text)
                    cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_INVISIBLE);
                    // TODO: Scale the text width to fit the OCR bbox
                    cb.setFontAndSize(defaultFont.getBaseFont(), Math.round(bboxHeightPt));
                    cb.moveText((float)(coordinates[0] / dotsPerPointX), (float)((pageImagePixelHeight - coordinates[3]) / dotsPerPointY));
                    cb.showText(line);
                    cb.endText();
                }
                ocrLineTag = source.findNextStartTag(ocrLineTag.getEnd(), "class", "ocr_line", false);
            }
            pdfDocument.close();
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}