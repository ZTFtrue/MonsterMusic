package org.jaudiotagger.tag.id3;

/**
 * This interface indicates that the tag supports ID3
 */
public interface Id3SupportingTag {
    AbstractID3v2Tag getID3Tag();

    void setID3Tag(AbstractID3v2Tag t);
}
