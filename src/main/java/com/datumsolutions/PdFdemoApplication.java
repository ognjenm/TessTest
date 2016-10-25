package com.datumsolutions;

import com.datumsolutions.util.FileUtils;
import com.datumsolutions.util.PdfUtilities;
import com.googlecode.jhocr.converter.HocrToPdf;
import com.googlecode.jhocr.util.JHOCRUtil;
import com.googlecode.jhocr.util.enums.PDFF;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageIOHelper;


import java.io.*;
import java.util.*;

public class PdFdemoApplication {

	static String appBasePath;

	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();
		String inputPdf = args[0];
		String outputPdf = args[1];

		try {

			File jarPath=new File(PdFdemoApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			appBasePath=jarPath.getParentFile().getParent().replace("%20", " ").replace("\\", "/");
			String propertiesPath=jarPath.getParentFile().getAbsolutePath();

			System.out.println(" propertiesPath-"+propertiesPath);

			try
			{
				System.out.println("TESSDATA PATH: "+properties.getProperty("tessdata.path"));
				properties.load(new FileInputStream(propertiesPath+"/application.properties"));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}

			String tempdirProperty = "java.io.tmpdir";
			String tempDirPath = System.getProperty(tempdirProperty);
			System.out.println("OS current temporary directory is " + tempDirPath);
			File tempDir = FileUtils.createTempDir();
			//USING WORKING DIR FOR DEBUG
			//COMMENT THIS IN PRODUCTION
			// Below is screnshot of settings in Intelij
			// https://www.dropbox.com/s/1ttb8lwn2qfbz0m/Screenshot%202016-10-24%2017.02.34.png?dl=1
			//tempDir = new File("/Users/ognjenm/code/open_source/testPdf/WORKINGDIR/png");

			TesseractCustom tessaractInstance = new TesseractCustom();
			tessaractInstance.setLanguage(properties.getProperty("lang","eng"));
			tessaractInstance.setDatapath(properties.getProperty("tessdata.path","#"));
			// check for tessdata dir path from configuration
			// else use bundled tessdata
			if("#".equals(properties.getProperty("tessdata.path")))
			{
				tessaractInstance.setDatapath(properties.getProperty("tessdata.path"));
			}


			//makePdfWithUpscaledImages(tessaractInstance, inputPdf, outputPdf);

			makePdfWithOriginalImages(tessaractInstance, inputPdf, outputPdf, tempDir);


		} catch (TesseractException e) {
			e.printStackTrace();
		}
	}


	private static void makePdfWithOriginalImages(TesseractCustom tessaractInstance, String inputPdf, String outputPdf, File tempDir) throws TesseractException {
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

	private static void makePdfWithUpscaledImages(TesseractCustom tessaractInstance, String inputPdf, String outputPdf) throws TesseractException {
		// DIRECTLY GENERATE PDF
		List<ITesseract.RenderedFormat> list = new ArrayList<ITesseract.RenderedFormat>();
		list.add(ITesseract.RenderedFormat.PDF);
		tessaractInstance.createDocuments(inputPdf, outputPdf, list);

	}


}
