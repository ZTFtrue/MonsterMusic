package com.ztftrue.music.effects;

/**
 * from <a href="https://github.com/JorenSix/TarsosDSP/blob/master/core/src/main/java/be/tarsos/dsp/effects/DelayEffect.java">TarsosDSP DelayEffect</a>
 */
public class DelayEffect {
    private final float sampleRate;
    private float[] echoBuffer;//in seconds
    private int position;
    private float decay;
    private boolean withFeedBack = false;
    private boolean withFeedBackDeal = false;
    private float newEchoLength;

    /**
     * @param echoLength in seconds
     * @param sampleRate the sample rate in Hz.
     * @param decay      The decay of the echo, a value between 0 and 1. 1 meaning no decay, 0 means immediate decay (not echo effect).
     */
    public DelayEffect(float echoLength, float decay, float sampleRate) {
        this.sampleRate = sampleRate;
        setDecay(decay);
        setEchoLength(echoLength);
        applyNewEchoLength();
    }

    /**
     * @param newEchoLength A new echo buffer length in seconds.
     */
    public void setEchoLength(float newEchoLength) {
        this.newEchoLength = newEchoLength;
    }

    private void applyNewEchoLength() {
        if (newEchoLength != -1) {
            float[] newEchoBuffer = new float[(int) (sampleRate * newEchoLength)];
            if (echoBuffer != null) {
                for (int i = 0; i < newEchoBuffer.length; i++) {
                    if (position >= echoBuffer.length) {
                        position = 0;
                    }
                    newEchoBuffer[i] = echoBuffer[position];
                    position++;
                }
            }
            this.echoBuffer = newEchoBuffer;
            newEchoLength = -1;
        }
        withFeedBackDeal = withFeedBack;
    }

    /**
     * A decay, should be a value between zero and one.
     *
     * @param newDecay the new decay (preferably between zero and one).
     */
    public void setDecay(float newDecay) {
        this.decay = newDecay;
    }

    public float process(float[] floatBuffer) {
        float max = 0;
        for (int i = 0; i < floatBuffer.length; i++) {
            if (position >= echoBuffer.length) {
                position = 0;
            }
            float f = floatBuffer[i];
            //output is the input added with the decayed echo
            floatBuffer[i] = floatBuffer[i] + echoBuffer[position] * decay;
            //store the sample in the buffer;
//            if (floatBuffer[i] > 1.0f) {
//                floatBuffer[i] = 1.0f;
//            } else if (floatBuffer[i] < -1.0f) {
//                floatBuffer[i] = -1.0f;
//            }
            max = Math.max(max, Math.abs(floatBuffer[i]));
            if (withFeedBackDeal) {
                echoBuffer[position] = floatBuffer[i];// multiple times, defined delay times
            } else {
                echoBuffer[position] = f;// Just once
            }
            position++;
        }
        applyNewEchoLength();
        return max;
    }

    public boolean isWithFeedBack() {
        return withFeedBack;
    }

    public void setWithFeedBack(boolean withFeedBack) {
        this.withFeedBack = withFeedBack;
    }
}
