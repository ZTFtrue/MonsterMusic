package org.jaudiotagger.audio;

/**
 * Representation of AudioHeader
 *
 * <p>Contains info about the Audio Header
 */
public interface AudioHeader
{
    /**
     * @return the audio file type
     */
    String getEncodingType();

    /**
     * @return the ByteRate of the Audio, this is the total average amount of bytes of data sampled per second
     */
    Integer getByteRate();



    /**
     * @return the BitRate of the Audio, this is the amount of kilobits of data sampled per second
     */
    String getBitRate();

    /**
     * @return bitRate as a number, this is the amount of kilobits of data sampled per second
     */
    long getBitRateAsNumber();


    /**
     *
     * @return length of the audio data in bytes, exactly what this means depends on the audio format
     *
     * TODO currently only used by Wav/Aiff/Flac/Mp4
     */
    Long getAudioDataLength();


    /**
     *
     * @return the location in the file where the audio samples start
     *
     * TODO currently only used by Wav/Aiff/Flac/Mp4
     */
    Long getAudioDataStartPosition();


    /**
     *
     * @return the location in the file where the audio samples end
     *
     * TODO currently only used by Wav/Aiff/Flac/Mp4
     */
    Long getAudioDataEndPosition();


    /**
     * @return the Sampling rate, the number of samples taken per second
     */
    String getSampleRate();

    /**
     * @return he Sampling rate, the number of samples taken per second
     */
    int getSampleRateAsNumber();

    /**
     * @return the format
     */
    String getFormat();

    /**
     * @return the number of channels (i.e 1 = Mono, 2 = Stereo)
     */
    String getChannels();

    /**
     * @return if the sampling bitRate is variable or constant
     */
    boolean isVariableBitRate();

    /**
     * @return track length in seconds
     */
    int getTrackLength();

    /**
     *
     * @return track length as float
     */
    double getPreciseTrackLength();

    /**
     * @return the number of bits in each sample
     */
    int getBitsPerSample();

    /**
     *
     * @return if the audio codec is lossless or lossy
     */
    boolean isLossless();

    /**
     *
     * @return the total number of samples, this can usually be used in conjunction with the
     * sample rate to determine the track duration
     */
    Long getNoOfSamples();
}
