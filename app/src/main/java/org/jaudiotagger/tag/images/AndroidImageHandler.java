package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 Image Handling to to use when running on Android

 TODO need to provide Android compatible implementations
 */
public class AndroidImageHandler implements ImageHandler
{
    private static AndroidImageHandler instance;

    public static AndroidImageHandler getInstanceOf()
    {
        if(instance==null)
        {
            instance = new AndroidImageHandler();
        }
        return instance;
    }

    private AndroidImageHandler()
    {

    }

    /**
     * Resize the image until the total size require to store the image is less than maxsize
     * @param artwork
     * @param maxSize
     * @throws IOException
     */
    public void reduceQuality(Artwork artwork, int maxSize) throws IOException
    {
        throw new UnsupportedOperationException();
    }
     /**
     * Resize image using Java 2D
      * @param artwork
      * @param size
      * @throws java.io.IOException
      */
    public void makeSmaller(Artwork artwork,int size) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isMimeTypeWritable(String mimeType)
    {
        throw new UnsupportedOperationException();
    }

    /**
     *  Write buffered image as required format
     *
     * @param bi
     * @param mimeType
     * @return
     * @throws IOException
     */
    public byte[] writeImage(Bitmap bi, String mimeType) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param bi
     * @return
     * @throws IOException
     */
    public byte[] writeImageAsPng(Bitmap bi) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Show read formats
     *
     * On Windows supports png/jpeg/bmp/gif
     */
    public void showReadFormats()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Show write formats
     *
     * On Windows supports png/jpeg/bmp
     */
    public void showWriteFormats()
    {
        throw new UnsupportedOperationException();
    }
}
