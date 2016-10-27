package com.datumsolutions;

import com.datumsolutions.hocr.HocrSingle;
import com.datumsolutions.util.FileUtils;
import com.datumsolutions.util.PdfUtilities;
import com.googlecode.jhocr.converter.HocrToPdf;
import com.googlecode.jhocr.util.enums.PDFF;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import org.apache.pdfbox.multipdf.Overlay;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;

import java.io.*;
import java.util.*;

/**
 * Created by ognjenm on 25/10/16.
 */
public class ProcessPdfDocument {

    private TesseractCustom tessaractInstance;


    public ProcessPdfDocument(Properties properties)
    {
        tessaractInstance = new TesseractCustom();
        tessaractInstance.setLanguage("eng");
        //tessaractInstance.setDatapath(properties.getProperty("tessdata.path","#"));
        // check for tessdata dir path from configuration
        // else use bundled tessdata
//        if("#".equals(properties.getProperty("tessdata.path")))
//        {
//            tessaractInstance.setDatapath(properties.getProperty("tessdata.path"));
//        }
        tessaractInstance.setDatapath(LoadLibs.extractTessResources("tessdata").getAbsolutePath());

    }

    /**
     * @param inputPdfPath
     * @param outputPdfPath
     * @param returnOriginalImages
     * @return
     * @throws TesseractException
     */
    public Boolean doOCR(String inputPdfPath, String outputPdfPath, Boolean returnOriginalImages) throws TesseractException {

        makePdfWithOriginalPdf(inputPdfPath, outputPdfPath);
//        if(returnOriginalImages)
//        {
//            this.makePdfWithOriginalImages(inputPdfPath, outputPdfPath);
//        }
//        else {
//            this.makePdfWithUpscaledImages(inputPdfPath, outputPdfPath);
//        }
        return true;
    }


    private void makePdfWithOriginalImages(String inputPdf, String outputPdf) throws TesseractException {

        String tempdirProperty = "java.io.tmpdir";
        String tempDirPath = System.getProperty(tempdirProperty);
        System.out.println("OS current temporary directory is " + tempDirPath);
        File tempDir = FileUtils.createTempDir();
        //tempDir = new File("/Users/ognjenm/code/open_source/testPdf/WORKINGDIR");

        //HOCR
        tessaractInstance.setHocr(true);
        File inputFile = new File(inputPdf);

        //Export Images from PDF into pngs 300ppi resolution
        File[] files = PdfUtilities.convertPdf2Png(inputFile,tempDir);


//        Arrays.stream(files).parallel().forEach((file) -> {
//                    Tesseract1 tesseract = new Tesseract1();
//                    try {
//                        tesseract.doOCR(file); // Ignore Result
//                    } catch (TesseractException e) {
//
//                    }
//                }
//        );


        List<String> orig = null;
            int counter = 0;
            for (File file : files) {
                String hocrResult = tessaractInstance.doOCR(file);
                //replace working images with original
                // I think that I don't need this????
                //hocrResult = hocrResult.replace(file.getAbsolutePath(), orig.get(counter));

                try {
                    FileOutputStream os;
                    String pdfFile = file.getAbsolutePath().replaceFirst("[.][^.]+$", "") +".pdf";
                    os = new FileOutputStream(pdfFile);
                    InputStream stream = new ByteArrayInputStream(hocrResult.getBytes("UTF-8"));

                    HocrToPdf hocrToPdf = new HocrToPdf(os);
                    hocrToPdf.addHocrDocument(stream, new FileInputStream(file));
                    hocrToPdf.setPdfFormat(PDFF.PDF_A_1B);
                    hocrToPdf.convert();
                    os.close();
                    stream.close();
                    counter++;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // find resulting pdf files and order them
            File[] workingFiles = tempDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().matches("workingimage\\d{3}\\.pdf$");
                }
            });

            Arrays.sort(workingFiles, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareTo(f2.getName());
                }
            });

            PdfUtilities.mergePdf(workingFiles, new File(outputPdf + ".pdf"));
            FileUtils.deleteDirectory(tempDir);

    }

    private void makePdfWithUpscaledImages(String inputPdf, String outputPdf) throws TesseractException {
        // DIRECTLY GENERATE PDF
        List<ITesseract.RenderedFormat> list = new ArrayList<ITesseract.RenderedFormat>();
        list.add(ITesseract.RenderedFormat.PDF);
        tessaractInstance.createDocuments(inputPdf, outputPdf, list);
    }

    private void makePdfWithOriginalPdf(String inputPdf, String outputPdf) throws TesseractException {

        String tempdirProperty = "java.io.tmpdir";
        String tempDirPath = System.getProperty(tempdirProperty);
        System.out.println("OS current temporary directory is " + tempDirPath);
        File tempDir = FileUtils.createTempDir();
        //tempDir = new File("/Users/ognjenm/code/open_source/testPdf/WORKINGDIR");
        tessaractInstance.setHocr(true);
        File inputFile = new File(inputPdf);
        File[] files = PdfUtilities.convertPdf2Png(inputFile,tempDir);

        for (File file : files) {
            String hocrResult = tessaractInstance.doOCR(file);
            try {
                FileOutputStream os;
                String pdfFile = file.getAbsolutePath().replaceFirst("[.][^.]+$", "") +"_textonly.pdf";
                HocrSingle hocrSingle = new HocrSingle();
                hocrSingle.process(hocrResult, pdfFile, file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // find resulting pdf files and order them
        File[] workingFiles = tempDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().matches("workingimage\\d{3}\\_textonly.pdf$");
            }
        });

        Arrays.sort(workingFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        PdfUtilities.mergePdf(workingFiles, new File(outputPdf + "_empty.pdf"));

        PDDocument doc = null;
        Overlay overlay = new Overlay();

        try {
            doc = PDDocument.load(new File(inputPdf));
            //for all the pages, you can add overlay guide, indicating watermark the original pages with the watermark document.
            HashMap<Integer, String> overlayGuide = new HashMap<Integer, String>();
            int i = 0;
            for (File pdfSinglePage: workingFiles) {
                overlayGuide.put(i+1, pdfSinglePage.getAbsolutePath());
                i++;
            }
            overlay.setInputPDF(doc);
            overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
            PDDocument result = overlay.overlay(overlayGuide);
            result.save(outputPdf + ".pdf");
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                overlay.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileUtils.deleteDirectory(tempDir);

    }

}
