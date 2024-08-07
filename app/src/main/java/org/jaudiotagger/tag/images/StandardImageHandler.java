package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ImageWriter;

import org.jaudiotagger.tag.id3.valuepair.ImageFormats;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import coil.decode.BitmapFactoryDecoder;

/**
 * Image Handling used when running on standard JVM
 */
public class StandardImageHandler implements ImageHandler {
    private static StandardImageHandler instance;

    public static StandardImageHandler getInstanceOf() {
        if (instance == null) {
            instance = new StandardImageHandler();
        }
        return instance;
    }

    private StandardImageHandler() {

    }

    /**
     * Resize the image until the total size require to store the image is less than maxsize
     *
     * @param artwork
     * @param maxSize
     * @throws IOException
     */
    public void reduceQuality(Artwork artwork, int maxSize) throws IOException {
        while (artwork.getBinaryData().length > maxSize) {
            Bitmap srcImage = (Bitmap) artwork.getImage();
            int w = srcImage.getWidth();
            int newSize = w / 2;
            makeSmaller(artwork, newSize);
        }
    }

    /**
     * Resize image using Java 2D
     *
     * @param artwork
     * @param size
     * @throws java.io.IOException
     */
    public void makeSmaller(Artwork artwork, int size) throws IOException {
        Bitmap srcImage = (Bitmap) artwork.getImage();

        int w = srcImage.getWidth();
        int h = srcImage.getHeight();

        // Determine the scaling required to get desired result.
        float scaleW = (float) size / (float) w;
        float scaleH = (float) size / (float) h;

        //Create an image buffer in which to paint on, create as an opaque Rgb type image, it doesnt matter what type
        //the original image is we want to convert to the best type for displaying on screen regardless
//        Bitmap bi = new Bitmap(size, size, Bitmap.TYPE_INT_RGB);
        Bitmap bi = Bitmap.createScaledBitmap(srcImage, (int) scaleW, (int) scaleH, true);
//        // Set the scale.
//        AffineTransform tx = new AffineTransform();
//        tx.scale(scaleW, scaleH);
//
//        // Paint image.
//        Graphics2D g2d = bi.createGraphics();
//        g2d.drawImage(srcImage, tx, null);
//        g2d.dispose();


        if (artwork.getMimeType() != null && isMimeTypeWritable(artwork.getMimeType())) {
            artwork.setBinaryData(writeImage(bi, artwork.getMimeType()));
        } else {
            artwork.setBinaryData(writeImageAsPng(bi));
        }
    }

    public boolean isMimeTypeWritable(String mimeType) {
        return getCompressFormat(mimeType) != null;
    }

    // Returns the corresponding Bitmap.CompressFormat for a given MIME type
    private Bitmap.CompressFormat getCompressFormat(String mimeType) {
        switch (mimeType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return Bitmap.CompressFormat.JPEG;
            case "image/png":
                return Bitmap.CompressFormat.PNG;
            case "image/webp":
                return Bitmap.CompressFormat.WEBP;
            default:
                return null; // Unsupported MIME type
        }
    }

    /**
     * Write buffered image as required format
     *
     * @param bi
     * @param mimeType
     * @return
     * @throws IOException
     */
    public byte[] writeImage(Bitmap bi, String mimeType) throws IOException {
        Bitmap.CompressFormat format;
        switch (mimeType) {
            case "image/jpeg":
            case "image/jpg":
                format = Bitmap.CompressFormat.JPEG;
                break;
            case "image/png":
                format = Bitmap.CompressFormat.PNG;
                break;
//            case "image/webp":
//                format = Bitmap.CompressFormat.WEBP;
//                break;
            default:
                throw new IOException("Cannot write to this mimetype");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bi.compress(format, 100, baos);
        return baos.toByteArray();

    }

    /**
     * @param bi
     * @return
     * @throws IOException
     */
    public byte[] writeImageAsPng(Bitmap bi) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bi.compress(Bitmap.CompressFormat.PNG, 100, baos);
//        ImageIO.write(bi, ImageFormats.MIME_TYPE_PNG, baos);
        return baos.toByteArray();
    }

    /**
     * Show read formats
     * <p>
     * On Windows supports png/jpeg/bmp/gif
     */
    public void showReadFormats() {
//        String[] formats = ImageIO.getReaderMIMETypes();
//        for (String f : formats) {
//            System.out.println("r" + f);
//        }
    }

    /**
     * Show write formats
     * <p>
     * On Windows supports png/jpeg/bmp
     */
    public void showWriteFormats() {
//        String[] formats = ImageIO.getWriterMIMETypes();
//        for (String f : formats) {
//            System.out.println(f);
//        }
    }
}
