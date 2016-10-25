package com.datumsolutions;

import com.datumsolutions.util.FileUtils;
import com.datumsolutions.util.PdfUtilities;
import com.googlecode.jhocr.converter.HocrToPdf;
import com.googlecode.jhocr.util.enums.PDFF;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.pdmodel.text.PDTextState;

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
        tessaractInstance.setLanguage(properties.getProperty("lang","eng"));
        tessaractInstance.setDatapath(properties.getProperty("tessdata.path","#"));
        // check for tessdata dir path from configuration
        // else use bundled tessdata
        if("#".equals(properties.getProperty("tessdata.path")))
        {
            tessaractInstance.setDatapath(properties.getProperty("tessdata.path"));
        }
    }

    /**
     * @param inputPdfPath
     * @param outputPdfPath
     * @param returnOriginalImages
     * @return
     * @throws TesseractException
     */
    public Boolean doOCR(String inputPdfPath, String outputPdfPath, Boolean returnOriginalImages) throws TesseractException {

        //TODO: check if PDF has embanded fonts. If so do not OCR or do makePdfWithUpscaledImages

        PDDocument document = null;
        Boolean isScanned = true;
        try {
            document = PDDocument.load(new File(inputPdfPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List pages = document.getDocumentCatalog().getAllPages();
        Iterator iter = pages.iterator();
        while (iter.hasNext()) {
            PDPage page = (PDPage) iter.next();
            PDResources resources = page.getResources();
            Map pageFonts = resources.getFonts();
            if (pageFonts != null) {
                System.out.println(pageFonts.get(0));
                isScanned = true;
                break;
            }
        }

        //doc.getGraphicsState().getTextState().getRenderingMode() == PDTextState.RENDERING_MODE_NEITHER_FILL_NOR_STROKE_TEXT

        if(returnOriginalImages && !isScanned)
        {
            this.makePdfWithOriginalImages(inputPdfPath, outputPdfPath);
        }
        else {
            this.makePdfWithUpscaledImages(inputPdfPath, outputPdfPath);
        }
        return true;
    }


    private void makePdfWithOriginalImages(String inputPdf, String outputPdf) throws TesseractException {

        String tempdirProperty = "java.io.tmpdir";
        String tempDirPath = System.getProperty(tempdirProperty);
        System.out.println("OS current temporary directory is " + tempDirPath);
        File tempDir = FileUtils.createTempDir();
       // tempDir = new File("/Users/ognjenm/code/open_source/testPdf/WORKINGDIR");

        //HOCR
        tessaractInstance.setHocr(true);
        File inputFile = new File(inputPdf);

        //Export Images from PDF into pngs 300ppi resolution
        File[] files = PdfUtilities.convertPdf2Png(inputFile,tempDir);

        List<String> orig = null;
        try {
            orig = PdfUtilities.getImagesFromPdf(inputPdf, tempDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(orig.size()!=files.length)
        {
            //TODO: Falback to makePdfWithUpscaledImages ???
            throw new TesseractException("Different number of pages detected");
        }

        int counter = 0;
        for (File file : files) {
            String hocrResult = tessaractInstance.doOCR(file);
            //replace working images with original

            // I think that I don't need this????
            hocrResult = hocrResult.replace(file.getAbsolutePath(), orig.get(counter));

            try {
                FileOutputStream os;
                String pdfFile = file.getAbsolutePath().replaceFirst("[.][^.]+$", "") +".pdf";
                os = new FileOutputStream(pdfFile);
                InputStream stream = new ByteArrayInputStream(hocrResult.getBytes("UTF-8"));

                HocrToPdf hocrToPdf = new HocrToPdf(os);
                hocrToPdf.addHocrDocument(stream, new FileInputStream(new File(orig.get(counter))));
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
        //FileUtils.deleteDirectory(tempDir);
    }

    private void makePdfWithUpscaledImages(String inputPdf, String outputPdf) throws TesseractException {
        // DIRECTLY GENERATE PDF
        List<ITesseract.RenderedFormat> list = new ArrayList<ITesseract.RenderedFormat>();
        list.add(ITesseract.RenderedFormat.PDF);
        tessaractInstance.createDocuments(inputPdf, outputPdf, list);
    }

}
