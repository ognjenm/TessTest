package com.datumsolutions.util;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;


import com.aspose.pdf.Document;
import com.aspose.pdf.Page;


import org.apache.log4j.Logger;

import javax.imageio.ImageIO;

public class PDFUtils {
	static final Logger logger = Logger.getLogger(PDFUtils.class);
	private static PDFUtils instance = null;

	private static final String NEWLINE = "\n";
	private static final String SEPARATOR = ": ";

	/**
	 * Static constructor to load all the licenses only once to save computing
	 * time
	 * 
	 * @throws Exception
	 */
	protected PDFUtils() throws Exception {
		Properties prop = new Properties();
		// Load all the Aspose Licenses
		try {
			prop.load(new InputStreamReader(PDFUtils.class.getClassLoader()
					.getResourceAsStream(RRTConstant.PROPERTIES), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}

		com.aspose.pdf.License licensePDF = new com.aspose.pdf.License();

		try {
			licensePDF.setLicense(PDFUtils.class.getClassLoader()
					.getResourceAsStream(
							prop.getProperty(RRTConstant.LICENSE_PATH)));
		} catch (Exception e) {

		}


	}

	public static synchronized PDFUtils getInstance() throws Exception {
		if (instance == null) {
			instance = new PDFUtils();
		}

		return instance;
	}

	/**
	 * Convert an image to a PDF file
	 * 
	 * @param input
	 *            Byte array of the image
	 * @param fileName
	 *            Name of the file or the image
	 * @return Outputstream of the PDF file
	 * @throws Exception
	 */
	public ByteArrayOutputStream imageToPDF(byte[] input, String fileName)
			throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		com.aspose.pdf.Document document = new com.aspose.pdf.Document();

		Page page = document.getPages().add();
		page.getPageInfo().getMargin().setBottom(0);
		page.getPageInfo().getMargin().setLeft(0);
		page.getPageInfo().getMargin().setRight(0);
		page.getPageInfo().getMargin().setTop(0);

		com.aspose.pdf.Image image1 = new com.aspose.pdf.Image();
		page.getParagraphs().add(image1);

		image1.setImageStream(new ByteArrayInputStream(input));

		try {
			document.save(os);
		} catch (Exception e) {
			throw e;
		}

		return os;
	}


	public void convertfromOHCR() {
		final String myDir = "/Users/ognjenm/code/open_source/testPdf/files/";
		Document doc = new Document(myDir + "scan3.pdf");
		// Create callBack - logic recognize text for pdf images. Use outer OCR supports HOCR standard(http://en.wikipedia.org/wiki/HOCR).
		// We have used free google tesseract OCR(http://en.wikipedia.org/wiki/Tesseract_%28software%29)

		// End callBack

		doc.convert(new Document.CallBackGetHocr() {
			@Override
			public String invoke(java.awt.image.BufferedImage img) {
				File outputfile = new File(myDir + "test.jpg");
				try {
					ImageIO.write(img, "jpg", outputfile);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				try {
					java.lang.Process process = Runtime.getRuntime().exec("tesseract" + " " + myDir + "test.jpg" + " " + myDir + "out hocr");
					System.out.println("tesseract" + " " + myDir + "test.jpg" + " " + myDir + "out hocr");
					process.waitFor();

				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// reading out.html to string
				File file = new File(myDir + "out.html");
				StringBuilder fileContents = new StringBuilder((int) file.length());
				Scanner scanner = null;
				try {
					scanner = new Scanner(file);
					String lineSeparator = System.getProperty("line.separator");
					while (scanner.hasNextLine()) {
						fileContents.append(scanner.nextLine() + lineSeparator);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} finally {
					if (scanner != null)
						scanner.close();
				}
				// deleting temp files
				File fileOut = new File(myDir + "out.html");
				if (fileOut.exists()) {
					fileOut.delete();
				}
				File fileTest = new File(myDir + "test.jpg");
				if (fileTest.exists()) {
					fileTest.delete();
				}
				return fileContents.toString();
			}
		});
		doc.save(myDir + "output971.pdf");
	}



}
