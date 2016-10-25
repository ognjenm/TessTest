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
		String inputPdf = "/Users/ognjenm/code/open_source/testPdf/files/scan.pdf";
		String outputPdf = "./out"; // no extension needed

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

			ProcessPdfDocument processPdfDocument = new ProcessPdfDocument(properties);
			processPdfDocument.doOCR(inputPdf, outputPdf, true);

		} catch (TesseractException e) {
			e.printStackTrace();
		}
	}



}
