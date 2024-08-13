package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 * Image Handler
 */
public interface ImageHandler
{
    public void reduceQuality(Artwork artwork, int maxSize) throws IOException;
    public void makeSmaller(Artwork artwork,int size) throws IOException;
    public boolean isMimeTypeWritable(String mimeType);
    public byte[] writeImage(Bitmap bi, String mimeType) throws IOException;
    public byte[] writeImageAsPng(Bitmap bi) throws IOException;
    public void showReadFormats();
    public void showWriteFormats();
}
