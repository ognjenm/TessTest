package com.datumsolutions;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PdFdemoApplication {

	static String appBasePath;

	public static void main(String[] args) throws IOException {
		Properties prop = new Properties();
		String inputPdf = args[0];
		String outputPdf = args[1];

		try {

			File jarPath=new File(PdFdemoApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			appBasePath=jarPath.getParentFile().getParent().replace("%20", " ").replace("\\", "/");
			String propertiesPath=jarPath.getParentFile().getAbsolutePath();

			System.out.println(" propertiesPath-"+propertiesPath);

			try
			{
				prop.load(new FileInputStream(propertiesPath+"\\application.properties"));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}

			TesseractCustom tessaractInstance = TesseractCustom.getInstance();
			tessaractInstance.setLanguage(prop.getProperty("lang","eng"));
			tessaractInstance.setDatapath(prop.getProperty("tessdata.path","/usr/local/Cellar/tesseract/3.04.01_2/share/tessdata"));

			//tessaractInstance.setDatapath("/usr/local/Cellar/tesseract/3.04.01_2/share/tessdata");
			List<ITesseract.RenderedFormat> list = new ArrayList<ITesseract.RenderedFormat>();
			list.add(ITesseract.RenderedFormat.PDF);
			tessaractInstance.createDocuments(inputPdf,outputPdf, list);

		} catch (TesseractException e) {
			e.printStackTrace();
		}
	}
}
