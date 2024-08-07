package org.jaudiotagger.tag.asf;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jaudiotagger.audio.asf.data.MetadataDescriptor;
import org.jaudiotagger.tag.TagField;


import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * An <code>AbstractAsfTagImageField</code> is an abstract class for representing tag
 * fields containing image data.<br>
 * 
 * @author Christian Laireiter
 */
abstract class AbstractAsfTagImageField extends AsfTagField
{

    /**
     * Creates a image tag field.
     * 
     * @param field
     *            the ASF field that should be represented.
     */
    public AbstractAsfTagImageField(final AsfFieldKey field) {
        super(field);
    }

    /**
     * Creates an instance.
     * 
     * @param source
     *            The descriptor which should be represented as a
     *            {@link TagField}.
     */
    public AbstractAsfTagImageField(final MetadataDescriptor source) {
        super(source);
    }

    /**
     * Creates a tag field.
     * 
     * @param fieldKey
     *            The field identifier to use.
     */
    public AbstractAsfTagImageField(final String fieldKey) {
        super(fieldKey);
    }

    /**
     * This method returns an image instance from the
     * {@linkplain #getRawImageData() image content}.
     * 
     * @return the image instance
     * @throws IOException
     */
    public Bitmap getImage() throws IOException {
        return BitmapFactory.decodeStream(new ByteArrayInputStream(getRawImageData()));
    }

    /**
     * Returns the size of the {@linkplain #getRawImageData() image data}.<br>
     * 
     * @return image data size in bytes.
     */
    public abstract int getImageDataSize();

    /**
     * Returns the raw data of the represented image.<br>
     * 
     * @return raw image data
     */
    public abstract byte[] getRawImageData();

}
