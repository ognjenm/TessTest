package com.datumsolutions;

import net.sourceforge.tess4j.TesseractException;
import java.io.*;
import java.util.*;

public class PdFdemoApplication {

	static String appBasePath;

	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();
		String inputPdf = "/Users/ognjenm/code/open_source/testPdf/files/scan3.pdf";
		String outputPdf = "./out"; // no extension needed

		try {

			File jarPath=new File(PdFdemoApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			appBasePath=jarPath.getParentFile().getParent().replace("%20", " ").replace("\\", "/");
			String propertiesPath=jarPath.getParentFile().getAbsolutePath();

			System.out.println(" propertiesPath-"+propertiesPath);

			try
			{
				properties.load(new FileInputStream(propertiesPath+"/application.properties"));
				System.out.println("TESSDATA PATH: "+properties.getProperty("tessdata.path"));

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
