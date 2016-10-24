/**
 * Copyright @ 2009 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.datumsolutions.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.tess4j.util.LoadLibs;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.ghost4j.Ghostscript;
import org.ghost4j.GhostscriptException;
import org.slf4j.LoggerFactory;

public class PdfUtilities {

    public static final String GS_INSTALL = "\nPlease download, install GPL Ghostscript from http://www.ghostscript.com\nand/or set the appropriate environment variable.";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("PdfUtilities");

    /**
     * Converts PDF to TIFF format.
     *
     * @param inputPdfFile input file
     * @return a multi-page TIFF image
     * @throws IOException
     */
    public static File convertPdf2Tiff(File inputPdfFile) throws IOException {
        File[] pngFiles = null;

        try {
            pngFiles = convertPdf2Png(inputPdfFile);
            File tiffFile = File.createTempFile("multipage", ".tif");
            // put PNG images into a single multi-page TIFF image for return
            ImageIOHelper.mergeTiff(pngFiles, tiffFile);
            return tiffFile;
        } catch (UnsatisfiedLinkError ule) {
            throw new RuntimeException(getMessage(ule.getMessage()));
        } catch (NoClassDefFoundError ncdfe) {
            throw new RuntimeException(getMessage(ncdfe.getMessage()));
        } finally {
            if (pngFiles != null) {
                // delete temporary PNG images
                for (File tempFile : pngFiles) {
                    tempFile.delete();
                }
            }
        }
    }

    /**
     * Converts PDF to PNG format.
     *
     * @param inputPdfFile input file
     * @return an array of PNG images
     */
    public static File[] convertPdf2Png(File inputPdfFile) {
        File imageDir = inputPdfFile.getParentFile();

        if (imageDir == null) {
            String userDir = System.getProperty("user.dir");
            imageDir = new File(userDir);
        }

        //get Ghostscript instance
        Ghostscript gs = Ghostscript.getInstance();

        //prepare Ghostscript interpreter parameters
        //refer to Ghostscript documentation for parameter usage
        List<String> gsArgs = new ArrayList<String>();
        gsArgs.add("-gs");
        gsArgs.add("-dNOPAUSE");
        gsArgs.add("-dQUIET");
        gsArgs.add("-dBATCH");
        gsArgs.add("-dSAFER");
        gsArgs.add("-sDEVICE=pnggray");
        // Changes Ognjen
        gsArgs.add("-dINTERPOLATE");
        gsArgs.add("-dNumRenderingThreads=4");
        // decreased from 300ppi
        gsArgs.add("-r200");
        // End of changes
        gsArgs.add("-dGraphicsAlphaBits=4");
        gsArgs.add("-dTextAlphaBits=4");
        gsArgs.add("-sOutputFile=" + imageDir.getPath() + "/workingimage%03d.png");
        gsArgs.add(inputPdfFile.getPath());

        //gs -dSAFER -sDEVICE=png16m -dINTERPOLATE -dNumRenderingThreads=8
       // -dFirstPage=1 -dLastPage=1 -r300 -o ./output\_image.png -c 30000000
       // setvmthreshold -f my\_pdf.pdf
        //execute and exit interpreter
        try {
            synchronized (gs) {
                gs.initialize(gsArgs.toArray(new String[0]));
                gs.exit();
            }
        } catch (GhostscriptException e) {
            logger.error(e.getCause() != null ? e.getCause().toString() : e.getMessage(), e);
        } finally {
            //delete interpreter instance (safer)
            try {
                Ghostscript.deleteInstance();
            } catch (GhostscriptException e) {
                //nothing
            }
        }

        // find working files
        File[] workingFiles = imageDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().matches("workingimage\\d{3}\\.png$");
            }
        });

        Arrays.sort(workingFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        return workingFiles;
    }


    public static File[] convertPdf2Png(File inputPdfFile, File imageDir) {

        if (imageDir != null) {
            String pdfDir = imageDir.getAbsolutePath();
            imageDir = new File(pdfDir);
        }

        //get Ghostscript instance
        Ghostscript gs = Ghostscript.getInstance();

        //prepare Ghostscript interpreter parameters
        //refer to Ghostscript documentation for parameter usage
        List<String> gsArgs = new ArrayList<String>();
        gsArgs.add("-gs");
        gsArgs.add("-dNOPAUSE");
        gsArgs.add("-dQUIET");
        gsArgs.add("-dBATCH");
        gsArgs.add("-dSAFER");
        gsArgs.add("-sDEVICE=pnggray");
        // Changes Ognjen
        gsArgs.add("-dINTERPOLATE");
        gsArgs.add("-dNumRenderingThreads=4");
        // decreased from 300ppi
        gsArgs.add("-r300");
        // End of changes
        gsArgs.add("-dGraphicsAlphaBits=4");
        gsArgs.add("-dTextAlphaBits=4");
        gsArgs.add("-sOutputFile=" + imageDir.getPath() + "/workingimage%03d.png");
        gsArgs.add(inputPdfFile.getPath());

        //gs -dSAFER -sDEVICE=png16m -dINTERPOLATE -dNumRenderingThreads=8
        // -dFirstPage=1 -dLastPage=1 -r300 -o ./output\_image.png -c 30000000
        // setvmthreshold -f my\_pdf.pdf
        //execute and exit interpreter
        try {
            synchronized (gs) {
                gs.initialize(gsArgs.toArray(new String[0]));
                gs.exit();
            }
        } catch (GhostscriptException e) {
            logger.error(e.getCause() != null ? e.getCause().toString() : e.getMessage(), e);
        } finally {
            //delete interpreter instance (safer)
            try {
                Ghostscript.deleteInstance();
            } catch (GhostscriptException e) {
                //nothing
            }
        }

        // find working files
        File[] workingFiles = imageDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().matches("workingimage\\d{3}\\.png$");
            }
        });

        Arrays.sort(workingFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        return workingFiles;
    }
    /**
     * Splits PDF.
     *
     * @deprecated As of Release 3.0.
     *
     * @param inputPdfFile input file
     * @param outputPdfFile output file
     * @param firstPage begin page
     * @param lastPage end page
     */
    public static void splitPdf(String inputPdfFile, String outputPdfFile, String firstPage, String lastPage) {
        if (firstPage.trim().isEmpty()) {
            firstPage = "0";
        }
        if (lastPage.trim().isEmpty()) {
            lastPage = "0";
        }

        splitPdf(new File(inputPdfFile), new File(outputPdfFile), Integer.parseInt(firstPage), Integer.parseInt(lastPage));
    }

    /**
     * Splits PDF.
     *
     * @param inputPdfFile input file
     * @param outputPdfFile output file
     * @param firstPage begin page
     * @param lastPage end page
     */
    public static void splitPdf(File inputPdfFile, File outputPdfFile, int firstPage, int lastPage) {
        //get Ghostscript instance
        Ghostscript gs = Ghostscript.getInstance();

        //prepare Ghostscript interpreter parameters
        //refer to Ghostscript documentation for parameter usage
        //gs -sDEVICE=pdfwrite -dNOPAUSE -dQUIET -dBATCH -dFirstPage=m -dLastPage=n -sOutputFile=out.pdf in.pdf
        List<String> gsArgs = new ArrayList<String>();
        gsArgs.add("-gs");
        gsArgs.add("-dNOPAUSE");
        gsArgs.add("-dQUIET");
        gsArgs.add("-dBATCH");
        gsArgs.add("-sDEVICE=pdfwrite");
        if (firstPage > 0) {
            gsArgs.add("-dFirstPage=" + firstPage);
        }
        if (lastPage > 0) {
            gsArgs.add("-dLastPage=" + lastPage);
        }
        gsArgs.add("-sOutputFile=" + outputPdfFile.getPath());
        gsArgs.add(inputPdfFile.getPath());

        //execute and exit interpreter
        try {
            synchronized (gs) {
                gs.initialize(gsArgs.toArray(new String[0]));
                gs.exit();
            }
        } catch (GhostscriptException e) {
            logger.error(e.getCause() != null ? e.getCause().toString() : e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        } catch (UnsatisfiedLinkError ule) {
            throw new RuntimeException(getMessage(ule.getMessage()));
        } catch (NoClassDefFoundError ncdfe) {
            throw new RuntimeException(getMessage(ncdfe.getMessage()));
        } finally {
            //delete interpreter instance (safer)
            try {
                Ghostscript.deleteInstance();
            } catch (GhostscriptException e) {
                //nothing
            }
        }
    }

    private static final String PS_FILE = "lib/pdfpagecount.ps";
    private static final String pdfPageCountFilePath;

    static {
        File postscriptFile = LoadLibs.extractTessResources(PS_FILE);
        if (postscriptFile != null && postscriptFile.exists()) {
            pdfPageCountFilePath = postscriptFile.getPath();
        } else {
            pdfPageCountFilePath = PS_FILE;
        }
    }

    /**
     * Gets PDF Page Count.
     *
     * @deprecated As of Release 3.0.
     *
     * @param inputPdfFile input file
     * @return number of pages
     */
    public static int getPdfPageCount(String inputPdfFile) {
        return getPdfPageCount(new File(inputPdfFile));
    }

    /**
     * Gets PDF Page Count.
     *
     * @param inputPdfFile input file
     * @return number of pages
     */
    public static int getPdfPageCount(File inputPdfFile) {
        //get Ghostscript instance
        Ghostscript gs = Ghostscript.getInstance();

        //prepare Ghostscript interpreter parameters
        //refer to Ghostscript documentation for parameter usage
        //gs -q -sPDFname=test.pdf pdfpagecount.ps
        List<String> gsArgs = new ArrayList<String>();
        gsArgs.add("-gs");
        gsArgs.add("-dNOPAUSE");
        gsArgs.add("-dQUIET");
        gsArgs.add("-dBATCH");
        gsArgs.add("-sPDFname=" + inputPdfFile.getPath());
        gsArgs.add(pdfPageCountFilePath);

        int pageCount = 0;
        ByteArrayOutputStream os;

        //execute and exit interpreter
        try {
            synchronized (gs) {
                //output
                os = new ByteArrayOutputStream();
                gs.setStdOut(os);
                gs.initialize(gsArgs.toArray(new String[0]));
                pageCount = Integer.parseInt(os.toString().replace("%%Pages: ", ""));
                os.close();
            }
        } catch (GhostscriptException e) {
            logger.error(e.getCause() != null ? e.getCause().toString() : e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getCause() != null ? e.getCause().toString() : e.getMessage(), e);
        } finally {
            //delete interpreter instance (safer)
            try {
                Ghostscript.deleteInstance();
            } catch (GhostscriptException e) {
                //nothing
            }
        }

        return pageCount;
    }

    /**
     * Merges PDF files.
     *
     * @param inputPdfFiles array of input files
     * @param outputPdfFile output file
     */
    public static void mergePdf(File[] inputPdfFiles, File outputPdfFile) {
        //get Ghostscript instance
        Ghostscript gs = Ghostscript.getInstance();

        //prepare Ghostscript interpreter parameters
        //refer to Ghostscript documentation for parameter usage
        //gs -sDEVICE=pdfwrite -dNOPAUSE -dQUIET -dBATCH -sOutputFile=out.pdf in1.pdf in2.pdf in3.pdf
        List<String> gsArgs = new ArrayList<String>();
        gsArgs.add("-gs");
        gsArgs.add("-dNOPAUSE");
        gsArgs.add("-dQUIET");
        gsArgs.add("-dBATCH");
        gsArgs.add("-sDEVICE=pdfwrite");
        gsArgs.add("-sOutputFile=" + outputPdfFile.getPath());

        for (File inputPdfFile : inputPdfFiles) {
            gsArgs.add(inputPdfFile.getPath());
        }

        //execute and exit interpreter
        try {
            synchronized (gs) {
                gs.initialize(gsArgs.toArray(new String[0]));
                gs.exit();
            }
        } catch (GhostscriptException e) {
            logger.error(e.getCause() != null ? e.getCause().toString() : e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        } catch (UnsatisfiedLinkError ule) {
            throw new RuntimeException(getMessage(ule.getMessage()));
        } catch (NoClassDefFoundError ncdfe) {
            throw new RuntimeException(getMessage(ncdfe.getMessage()));
        } finally {
            //delete interpreter instance (safer)
            try {
                Ghostscript.deleteInstance();
            } catch (GhostscriptException e) {
                //nothing
            }
        }
    }

    static String getMessage(String message) {
        if (message.contains("library 'gs") || message.contains("ghost4j")) {
            return message + GS_INSTALL;
        }
        return message;
    }


    public static List<String> getImagesFromPdf(String inFile, File tempDir) throws IOException {
        PDDocument document = null;
        List<String> files = new ArrayList<String>();

        try
        {
            document = PDDocument.load(inFile);
            List pages = document.getDocumentCatalog().getAllPages();
            Iterator iter = pages.iterator();
            while (iter.hasNext()) {
                PDPage page = (PDPage) iter.next();
                PDResources resources = page.getResources();
                Map pageImages = resources.getImages();
                if (pageImages != null) {
                    Iterator imageIter = pageImages.keySet().iterator();
                    int counter = 1;
                    while (imageIter.hasNext()) {
                        String key = (String) imageIter.next();
                        PDXObjectImage image = (PDXObjectImage) pageImages.get(key);
                        String name = tempDir.getAbsolutePath() + "/" + "originalimage" + counter  ;
                        System.out.println( "Writing image:" + name + image.getSuffix() );
                        image.write2file(name);
                        files.add(name + "." +image.getSuffix());
                        counter ++;
                        //image.write2OutputStream(/* some output stream */);
                    }
                }
            }
        }
        finally {
            if( document != null ) document.close();
        }

        return files;
    }
}