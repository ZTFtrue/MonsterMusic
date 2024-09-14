package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 * Image Handler
 */
public interface ImageHandler
{
    void reduceQuality(Artwork artwork, int maxSize) throws IOException;
    void makeSmaller(Artwork artwork, int size) throws IOException;
    boolean isMimeTypeWritable(String mimeType);
    byte[] writeImage(Bitmap bi, String mimeType) throws IOException;
    byte[] writeImageAsPng(Bitmap bi) throws IOException;
    void showReadFormats();
    void showWriteFormats();
}
