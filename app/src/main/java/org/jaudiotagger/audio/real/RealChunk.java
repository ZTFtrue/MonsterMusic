package org.jaudiotagger.audio.real;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.Utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public record RealChunk(String id, int size, byte[] bytes) {

    private static final String RMF = ".RMF";
    private static final String PROP = "PROP";
    private static final String MDPR = "MDPR";
    private static final String CONT = "CONT";
    private static final String DATA = "DATA";
    private static final String INDX = "INDX";

    public static RealChunk readChunk(RandomAccessFile raf)
            throws CannotReadException, IOException {
        final String id = Utils.readString(raf, 4);
        final int size = (int) Utils.readUint32(raf);
        if (size < 8) {
            throw new CannotReadException(
                    "Corrupt file: RealAudio chunk length at position "
                            + (raf.getFilePointer() - 4)
                            + " cannot be less than 8");
        }
        if (size > (raf.length() - raf.getFilePointer() + 8)) {
            throw new CannotReadException(
                    "Corrupt file: RealAudio chunk length of " + size
                            + " at position " + (raf.getFilePointer() - 4)
                            + " extends beyond the end of the file");
        }
        final byte[] bytes = new byte[size - 8];
        raf.readFully(bytes);
        return new RealChunk(id, size, bytes);
    }

    public DataInputStream getDataInputStream() {
        return new DataInputStream(new ByteArrayInputStream(bytes()));
    }

    public boolean isCONT() {
        return CONT.equals(id);
    }

    public boolean isPROP() {
        return PROP.equals(id);
    }


    @Override
    public String toString() {
        return id + "\t" + size;
    }
}
