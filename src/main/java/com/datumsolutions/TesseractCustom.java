package com.datumsolutions;

import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;

import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.ITessAPI.TessBaseAPI;
import net.sourceforge.tess4j.ITessAPI.TessResultRenderer;
import net.sourceforge.tess4j.util.ImageIOHelper;
import com.datumsolutions.util.PdfUtilities;
import net.sourceforge.tess4j.util.Utils;

public class TesseractCustom extends net.sourceforge.tess4j.Tesseract {
    private static TesseractCustom instance;
    private String language = "eng";
    private String datapath = "./";
    private RenderedFormat renderedFormat;
    private int psm;
    private int ocrEngineMode;
    private final Properties prop;
    private final List<String> configList;
    private TessAPI api;
    private TessBaseAPI handle;
    private static final Logger logger = Logger.getLogger(TesseractCustom.class.getName());

    public TesseractCustom() {
        super();
        this.renderedFormat = RenderedFormat.PDF;
        this.psm = -1;
        this.ocrEngineMode = 3;
        this.prop = new Properties();
        this.configList = new ArrayList();
    }

    protected TessAPI getAPI() {
        return this.api;
    }

    protected TessBaseAPI getHandle() {
        return this.handle;
    }

    /** @deprecated */
    @Deprecated
    public static synchronized TesseractCustom getInstance() {
        if(instance == null) {
            instance = new TesseractCustom();
        }
        return instance;
    }

    public void setDatapath(String var1) {
        this.datapath = var1;
    }

    public void setLanguage(String var1) {
        this.language = var1;
    }

    public void setOcrEngineMode(int var1) {
        this.ocrEngineMode = var1;
    }

    public void setPageSegMode(int var1) {
        this.psm = var1;
    }

    public void setHocr(boolean var1) {
        this.renderedFormat = var1?RenderedFormat.HOCR:RenderedFormat.TEXT;
        this.prop.setProperty("tessedit_create_hocr", var1?"1":"0");
    }

    public void setTessVariable(String var1, String var2) {
        this.prop.setProperty(var1, var2);
    }

    public void setConfigs(List<String> var1) {
        this.configList.clear();
        if(var1 != null) {
            this.configList.addAll(var1);
        }

    }

    public String doOCR(File var1) throws TesseractException {
        return this.doOCR((File)var1, (Rectangle)null);
    }

    public String doOCR(File var1, Rectangle var2) throws TesseractException {
        try {
            return this.doOCR(ImageIOHelper.getIIOImageList(var1), var1.getPath(), var2);
        } catch (Exception var4) {
            logger.log(Level.SEVERE, var4.getMessage(), var4);
            throw new TesseractException(var4);
        }
    }

    public String doOCR(BufferedImage var1) throws TesseractException {
        return this.doOCR((BufferedImage)var1, (Rectangle)null);
    }

    public String doOCR(BufferedImage var1, Rectangle var2) throws TesseractException {
        try {
            return this.doOCR(ImageIOHelper.getIIOImageList(var1), var2);
        } catch (Exception var4) {
            logger.log(Level.SEVERE, var4.getMessage(), var4);
            throw new TesseractException(var4);
        }
    }

    public String doOCR(List<IIOImage> var1, Rectangle var2) throws TesseractException {
        return this.doOCR(var1, (String)null, var2);
    }

    public String doOCR(List<IIOImage> var1, String var2, Rectangle var3) throws TesseractException {
        this.init();
        this.setTessVariables();

        try {
            StringBuilder var4 = new StringBuilder();
            int var5 = 0;
            Iterator var6 = var1.iterator();

            while(var6.hasNext()) {
                IIOImage var7 = (IIOImage)var6.next();
                ++var5;

                try {
                    this.setImage(var7.getRenderedImage(), var3);
                    var4.append(this.getOCRText(var2, var5));
                } catch (IOException var12) {
                    logger.log(Level.SEVERE, var12.getMessage(), var12);
                }
            }

            if(this.renderedFormat == RenderedFormat.HOCR) {
                var4.insert(0, "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n<html>\n<head>\n<title></title>\n<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n<meta name=\'ocr-system\' content=\'tesseract\'/>\n</head>\n<body>\n").append("</body>\n</html>\n");
            }

            String var14 = var4.toString();
            return var14;
        } finally {
            this.dispose();
        }
    }

    public String doOCR(int var1, int var2, ByteBuffer var3, Rectangle var4, int var5) throws TesseractException {
        return this.doOCR(var1, var2, var3, (String)null, var4, var5);
    }

    public String doOCR(int var1, int var2, ByteBuffer var3, String var4, Rectangle var5, int var6) throws TesseractException {
        this.init();
        this.setTessVariables();

        String var7;
        try {
            this.setImage(var1, var2, var3, var5, var6);
            var7 = this.getOCRText(var4, 1);
        } catch (Exception var11) {
            logger.log(Level.SEVERE, var11.getMessage(), var11);
            throw new TesseractException(var11);
        } finally {
            this.dispose();
        }

        return var7;
    }

    protected void init() {
        this.api = TessAPI.INSTANCE;
        this.handle = this.api.TessBaseAPICreate();
        StringArray var1 = new StringArray((String[])this.configList.toArray(new String[0]));
        PointerByReference var2 = new PointerByReference();
        var2.setPointer(var1);
        this.api.TessBaseAPIInit1(this.handle, this.datapath, this.language, this.ocrEngineMode, var2, this.configList.size());
        if(this.psm > -1) {
            this.api.TessBaseAPISetPageSegMode(this.handle, this.psm);
        }

    }

    protected void setTessVariables() {
        Enumeration var1 = this.prop.propertyNames();

        while(var1.hasMoreElements()) {
            String var2 = (String)var1.nextElement();
            this.api.TessBaseAPISetVariable(this.handle, var2, this.prop.getProperty(var2));
        }

    }

    protected void setImage(RenderedImage var1, Rectangle var2) throws IOException {
        this.setImage(var1.getWidth(), var1.getHeight(), ImageIOHelper.getImageByteBuffer(var1), var2, var1.getColorModel().getPixelSize());
    }

    protected void setImage(int var1, int var2, ByteBuffer var3, Rectangle var4, int var5) {
        int var6 = var5 / 8;
        int var7 = (int)Math.ceil((double)(var1 * var5) / 8.0D);
        this.api.TessBaseAPISetImage(this.handle, var3, var1, var2, var6, var7);
        if(var4 != null && !var4.isEmpty()) {
            this.api.TessBaseAPISetRectangle(this.handle, var4.x, var4.y, var4.width, var4.height);
        }

    }

    protected String getOCRText(String var1, int var2) {
        if(var1 != null && !var1.isEmpty()) {
            this.api.TessBaseAPISetInputName(this.handle, var1);
        }

        Pointer var3 = this.renderedFormat == RenderedFormat.HOCR?this.api.TessBaseAPIGetHOCRText(this.handle, var2 - 1):this.api.TessBaseAPIGetUTF8Text(this.handle);
        String var4 = var3.getString(0L);
        this.api.TessDeleteText(var3);
        return var4;
    }

    private TessResultRenderer createRenderers(List<RenderedFormat> var1) {
        TessResultRenderer var2 = null;
        Iterator var3 = var1.iterator();

        while(var3.hasNext()) {
            RenderedFormat var4 = (RenderedFormat)var3.next();

            String var5 = this.api.TessBaseAPIGetDatapath(this.handle);
            if(var2 == null) {
                var2 = this.api.TessPDFRendererCreate(var5);
            } else {
                this.api.TessResultRendererInsert(var2, this.api.TessPDFRendererCreate(var5));
            }
        }

        return var2;
    }

    public void createDocuments(String var1, String var2, List<RenderedFormat> var3) throws TesseractException {
        this.createDocuments(new String[]{var1}, new String[]{var2}, var3);
    }

    public void createDocuments(String[] var1, String[] var2, List<RenderedFormat> var3) throws TesseractException {
        if(var1.length != var2.length) {
            throw new RuntimeException("The two arrays must match in length.");
        } else {
            this.init();
            this.setTessVariables();

            try {
                for(int var4 = 0; var4 < var1.length; ++var4) {
                    File var5 = null;

                    try {
                        String var6 = var1[var4];
                        if(var6.toLowerCase().endsWith(".pdf")) {
                            var5 = PdfUtilities.convertPdf2Tiff(new File(var6));
                            var6 = var5.getPath();
                        }

                        TessResultRenderer var7 = this.createRenderers(var3);
                        this.createDocuments(var6, var2[var4], var7);
                    } catch (Exception var16) {
                        logger.log(Level.SEVERE, var16.getMessage(), var16);
                    } finally {
                        if(var5 != null && var5.exists()) {
                            var5.delete();
                        }

                    }
                }
            } finally {
                this.dispose();
            }

        }
    }

    private void createDocuments(String var1, String var2, TessResultRenderer var3) throws TesseractException {
        this.api.TessBaseAPISetInputName(this.handle, var1);
        this.api.TessResultRendererBeginDocument(var3, var1);
        int var4 = this.api.TessBaseAPIProcessPages1(this.handle, var1, (String)null, 0, var3);
        this.api.TessResultRendererEndDocument(var3);
        if(var4 == 0) {
            throw new TesseractException("Error during processing page.");
        } else {
            this.writeToFiles(var2, var3);
        }
    }

    private void writeToFiles(String var1, TessResultRenderer var2) throws TesseractException {
        Map var3 = this.getRendererOutput(var2);
        Iterator var4 = var3.entrySet().iterator();

        while(var4.hasNext()) {
            Entry var5 = (Entry)var4.next();
            String var6 = (String)var5.getKey();
            byte[] var7 = (byte[])var5.getValue();

            try {
                File var8 = new File(var1 + "." + var6);
                Utils.writeFile(var7, var8);
            } catch (IOException var9) {
                logger.log(Level.SEVERE, var9.getMessage(), var9);
            }
        }

    }

    private Map<String, byte[]> getRendererOutput(TessResultRenderer var1) throws TesseractException {
        HashMap var2;
        for(var2 = new HashMap(); var1 != null; var1 = this.api.TessResultRendererNext(var1)) {
            String var3 = this.api.TessResultRendererExtention(var1).getString(0L);
            PointerByReference var4 = new PointerByReference();
            IntByReference var5 = new IntByReference();
            int var6 = this.api.TessResultRendererGetOutput(var1, var4, var5);
            if(var6 == 1) {
                int var7 = var5.getValue();
                byte[] var8 = var4.getValue().getByteArray(0L, var7);
                var2.put(var3, var8);
            }
        }

        return var2;
    }

    protected void dispose() {
        this.api.TessBaseAPIDelete(this.handle);
    }
}
