/**
 * @author : Paul Taylor
 * @author : Eric Farng
 * <p>
 * Version @version:$Id$
 * <p>
 * MusicTag Copyright (C)2003,2004
 * <p>
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 *
 */
package org.jaudiotagger.tag.datatype;

import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.InvalidDataTypeException;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Represents a field/data type that can be held within a frames body, these map loosely onto
 * Section 4. ID3v2 frame overview at http://www.id3.org/id3v2.4.0-structure.txt
 */
public abstract class AbstractDataType {
    protected static final String TYPE_ELEMENT = "element";

    //Logger
    public static Logger logger = Logger.getLogger("org.jaudiotagger.tag.datatype");

    /**
     * Holds the data
     */
    protected Object value = null;

    /**
     * Holds the key such as "Text" or "PictureType", the naming of keys are fairly arbitary but are intended
     * to make it easier to for the developer, the keys themseleves are not written to the tag.
     */
    protected String identifier = "";

    /**
     * Holds the calling body, allows an datatype to query other objects in the
     * body such as the Text Encoding of the frame
     */
    protected AbstractTagFrameBody frameBody = null;

    /**
     * Holds the size of the data in file when read/written
     */
    protected int size;

    /**
     * Construct an abstract datatype identified by identifier and linked to a framebody without setting
     * an initial value.
     *
     * @param identifier to allow retrieval of this datatype by name from framebody
     * @param frameBody  that the dataype is associated with
     */
    protected AbstractDataType(String identifier, AbstractTagFrameBody frameBody) {
        this.identifier = identifier;
        this.frameBody = frameBody;
    }

    /**
     * Construct an abstract datatype identified by identifier and linked to a framebody initilised with a value
     *
     * @param identifier to allow retrieval of this datatype by name from framebody
     * @param frameBody  that the dataype is associated with
     * @param value      of this DataType
     */
    protected AbstractDataType(String identifier, AbstractTagFrameBody frameBody, Object value) {
        this.identifier = identifier;
        this.frameBody = frameBody;
        setValue(value);
    }

    /**
     * This is used by subclasses, to clone the data within the copyObject
     *
     * TODO:It seems to be missing some of the more complex value types.
     * @param copyObject
     */
    public AbstractDataType(AbstractDataType copyObject) {
        // no copy constructor in super class
        this.identifier = copyObject.identifier;
        if (copyObject.value == null) {
            this.value = null;
        } else if (copyObject.value instanceof String) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof Boolean) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof Byte) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof Character) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof Double) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof Float) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof Integer) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof Long) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof Short) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof MultipleTextEncodedStringNullTerminated.Values) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof PairedTextEncodedStringNullTerminated.ValuePairs) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof PartOfSet.PartOfSetValue) {
            this.value = copyObject.value;
        } else if (copyObject.value instanceof boolean[]) {
            this.value = ((boolean[]) copyObject.value).clone();
        } else if (copyObject.value instanceof byte[]) {
            this.value = ((byte[]) copyObject.value).clone();
        } else if (copyObject.value instanceof char[]) {
            this.value = ((char[]) copyObject.value).clone();
        } else if (copyObject.value instanceof double[]) {
            this.value = ((double[]) copyObject.value).clone();
        } else if (copyObject.value instanceof float[]) {
            this.value = ((float[]) copyObject.value).clone();
        } else if (copyObject.value instanceof int[]) {
            this.value = ((int[]) copyObject.value).clone();
        } else if (copyObject.value instanceof long[]) {
            this.value = ((long[]) copyObject.value).clone();
        } else if (copyObject.value instanceof short[]) {
            this.value = ((short[]) copyObject.value).clone();
        } else if (copyObject.value instanceof Object[]) {
            this.value = ((Object[]) copyObject.value).clone();
        } else if (copyObject.value instanceof ArrayList) {
            this.value = ((ArrayList) copyObject.value).clone();
        } else if (copyObject.value instanceof LinkedList) {
            this.value = ((LinkedList) copyObject.value).clone();
        } else {
            throw new UnsupportedOperationException("Unable to create copy of class " + copyObject.getClass());
        }
    }

    /**
     * Get the framebody associated with this datatype
     *
     * @return the framebody that this datatype is associated with
     */
    public AbstractTagFrameBody getBody() {
        return frameBody;
    }

    /**
     * Set the framebody that this datatype is associated with
     *
     * @param frameBody
     */
    public void setBody(AbstractTagFrameBody frameBody) {
        this.frameBody = frameBody;
    }

    /**
     * Return the key as declared by the frame bodies datatype list
     *
     * @return the key used to reference this datatype from a framebody
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Get value held by this Object
     *
     * @return value held by this Object
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the value held by this datatype, this is used typically used when the
     * user wants to modify the value in an existing frame.
     *
     * @param value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Simplified wrapper for reading bytes from file into Object.
     * Used for reading Strings, this class should be overridden
     * for non String Objects
     *
     * @param arr
     * @throws org.jaudiotagger.tag.InvalidDataTypeException
     */
    final public void readByteArray(byte[] arr) throws InvalidDataTypeException {
        readByteArray(arr, 0);
    }

    /**
     * This defines the size in bytes of the datatype being
     * held when read/written to file.
     *
     * @return the size in bytes of the datatype
     */
    abstract public int getSize();

    /**
     * @param obj
     * @return whether this and obj are deemed equivalent
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof AbstractDataType object)) {
            return false;
        }
        if (!this.identifier.equals(object.identifier)) {
            return false;
        }
        if ((this.value == null) && (object.value == null)) {
            return true;
        } else if ((this.value == null) || (object.value == null)) {
            return false;
        }
        // boolean[]
        if (this.value instanceof boolean[] && object.value instanceof boolean[]) {
            return Arrays.equals((boolean[]) this.value, (boolean[]) object.value);
            // byte[]
        } else if (this.value instanceof byte[] && object.value instanceof byte[]) {
            return Arrays.equals((byte[]) this.value, (byte[]) object.value);
            // char[]
        } else if (this.value instanceof char[] && object.value instanceof char[]) {
            return Arrays.equals((char[]) this.value, (char[]) object.value);
            // double[]
        } else if (this.value instanceof double[] && object.value instanceof double[]) {
            return Arrays.equals((double[]) this.value, (double[]) object.value);
            // float[]
        } else if (this.value instanceof float[] && object.value instanceof float[]) {
            return Arrays.equals((float[]) this.value, (float[]) object.value);
            // int[]
        } else if (this.value instanceof int[] && object.value instanceof int[]) {
            return Arrays.equals((int[]) this.value, (int[]) object.value);
            // long[]
        } else if (this.value instanceof long[] && object.value instanceof long[]) {
            return Arrays.equals((long[]) this.value, (long[]) object.value);
            // Object[]
        } else if (this.value instanceof Object[] && object.value instanceof Object[]) {
            return Arrays.equals((Object[]) this.value, (Object[]) object.value);
            // short[]
        } else if (this.value instanceof short[] && object.value instanceof short[]) {
            return Arrays.equals((short[]) this.value, (short[]) object.value);
        } else return this.value.equals(object.value);
    }

    /**
     * This is the starting point for reading bytes from the file into the ID3 datatype
     * starting at offset.
     * This class must be overridden
     *
     * @param arr
     * @param offset
     * @throws org.jaudiotagger.tag.InvalidDataTypeException
     */
    public abstract void readByteArray(byte[] arr, int offset) throws InvalidDataTypeException;


    /**
     * Starting point write ID3 Datatype back to array of bytes.
     * This class must be overridden.
     *
     * @return the array of bytes representing this datatype that should be written to file
     */
    public abstract byte[] writeByteArray();

    /**
     * Return String Representation of Datatype     *
     */
    public void createStructure() {
        MP3File.getStructureFormatter().addElement(identifier, getValue().toString());
    }

}
