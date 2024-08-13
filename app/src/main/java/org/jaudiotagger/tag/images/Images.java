package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 * BufferedImage methods
 *
 * Not compatible with Android, delete from your source tree.
 */
public class Images
{
    public static Bitmap getImage(Artwork artwork) throws IOException
    {
        return (Bitmap)artwork.getImage();
    }
}
