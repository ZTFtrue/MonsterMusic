package com.ztftrue.music.effects;


import org.apache.commons.math3.util.FastMath;

@SuppressWarnings("unused")
final public class BiQuadraticFilter {

    final static int LOWPASS = 0;
    final static int HIGHPASS = 1;
    final static int BANDPASS = 2;
    final static int PEAK = 3;
    final static int NOTCH = 4;
    final static int LOWSHELF = 5;
    final static int HIGHSHELF = 6;
    final static int Gain = 7;
    BIND_TYPE bindType = BIND_TYPE.Q;
    float center_freq, sample_rate, Q, gainDB, bw;
    private float a0, a1, a2, b0, b1, b2;
    private float x1, x2, y, y1, y2;
    private float gain_abs;
    private int type;

    public BiQuadraticFilter() {
    }

    public BiQuadraticFilter(int type, float center_freq, float sample_rate, float Q, float gainDB) {
        configure(type, center_freq, sample_rate, Q, gainDB);
    }

    // constructor without gain setting
    public BiQuadraticFilter(int type, float center_freq, float sample_rate, float Q) {
        configure(type, center_freq, sample_rate, Q, 0);
    }

    public void reset() {
        x1 = x2 = y1 = y2 = 0;
    }

    public double frequency() {
        return center_freq;
    }

    public void configure(int type, float center_freq, float sample_rate, float Q, float gainDB) {
        reset();
        Q = (Q == 0f) ? (float) 1e-9 : Q;
        this.type = type;
        this.sample_rate = sample_rate;
        this.Q = Q;
        this.gainDB = gainDB;
        reconfigure(center_freq);
    }

    public void configure(int type, float center_freq, float sample_rate, float Q) {
        configure(type, center_freq, sample_rate, Q, 0);
    }

    // allow parameter change while running
    public void reconfigure(float cf) {
        center_freq = cf;
        // only used for peaking and shelving filters
        gain_abs = (float) FastMath.pow(10, gainDB / 40);
        double omega = 2 * FastMath.PI * cf / sample_rate;
        float sn = (float) FastMath.sin(omega);
        float cs = (float) FastMath.cos(omega);
        float alpha = 0.0f;
        if (bindType == (BIND_TYPE.BW)) {
            float bwq = bw / 60.0f;
            double LN2 = 0.69314718055994530942;
            alpha = (float) (sn * FastMath.sinh(LN2 / 2.0 * bwq * omega / FastMath.sin(omega)));
        } else if (bindType == BIND_TYPE.Q) {
            alpha = sn / (2 * Q);
        } else if (bindType == BIND_TYPE.S) {
            alpha = (float) (sn * FastMath.sqrt((gain_abs + 1.0 / gain_abs) * (1 / Q - 1) + 2) / 2.0);
        }
        float beta = (float) FastMath.sqrt(gain_abs + gain_abs);
        switch (type) {
            case Gain -> {
                b0 = gain_abs;
                a0 = 1.0f;
                a1 = a2 = b1 = b2 = 0.0f;
            }
            case BANDPASS -> {
                b0 = alpha;
                b1 = 0;
                b2 = -alpha;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
            }
            case LOWPASS -> {
                b0 = (1 - cs) / 2;
                b1 = 1 - cs;
                b2 = (1 - cs) / 2;
                a0 = 1.0f + alpha;
                a1 = -2.0f * cs;
                a2 = 1.0f - alpha;
            }
            case HIGHPASS -> {
                b0 = b2 = (1 + cs) / 2;
                b1 = -(1.0f + cs);
                a0 = 1.0f + alpha;
                a1 = -2.0f * cs;
                a2 = 1 - alpha;
            }
            case NOTCH -> {
                b0 = 1;
                b1 = -2 * cs;
                b2 = 1;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
            }
            case PEAK -> {
                b0 = 1 + (alpha * gain_abs);
                b1 = -2 * cs;
                b2 = 1 - (alpha * gain_abs);
                a0 = 1 + (alpha / gain_abs);
                a1 = -2 * cs;
                a2 = 1 - (alpha / gain_abs);
            }
            case LOWSHELF -> {
                b0 = gain_abs * ((gain_abs + 1f) - (gain_abs - 1f) * cs + beta * sn);
                b1 = 2 * gain_abs * ((gain_abs - 1) - (gain_abs + 1) * cs);
                b2 = gain_abs * ((gain_abs + 1) - (gain_abs - 1) * cs - beta * sn);
                a0 = (gain_abs + 1) + (gain_abs - 1) * cs + beta * sn;
                a1 = -2 * ((gain_abs - 1) + (gain_abs + 1) * cs);
                a2 = (gain_abs + 1) + (gain_abs - 1) * cs - beta * sn;
            }
            case HIGHSHELF -> {
                b0 = gain_abs * ((gain_abs + 1) + (gain_abs - 1) * cs + beta * sn);
                b1 = -2 * gain_abs * ((gain_abs - 1) + (gain_abs + 1) * cs);
                b2 = gain_abs * ((gain_abs + 1) + (gain_abs - 1) * cs - beta * sn);
                a0 = (gain_abs + 1) - (gain_abs - 1) * cs + beta * sn;
                a1 = 2 * ((gain_abs - 1) - (gain_abs + 1) * cs);
                a2 = (gain_abs + 1) - (gain_abs - 1) * cs - beta * sn;
            }
        }
        // prescale flter constants
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
    }

    // provide a static amplitude result for testing
    public double result(double f) {
        double phi = FastMath.pow((FastMath.sin(2.0 * FastMath.PI * f / (2.0 * sample_rate))), 2.0);
        double r = (FastMath.pow(b0 + b1 + b2, 2.0) - 4.0 * (b0 * b1 + 4.0 * b0 * b2 + b1 * b2) * phi + 16.0 * b0 * b2 * phi * phi) / (FastMath.pow(1.0 + a1 + a2, 2.0) - 4.0 * (a1 + 4.0 * a2 + a1 * a2) * phi + 16.0 * a2 * phi * phi);
        if (r < 0) {
            r = 0;
        }
        return FastMath.sqrt(r);
    }

    // provide a static decibel result for testing
    public double log_result(double f) {
        double r;
        try {
            r = 20 * FastMath.log10(result(f));
        } catch (Exception e) {
            r = -100;
        }
        if (Double.isInfinite(r) || Double.isNaN(r)) {
            r = -100;
        }
        return r;
    }

    // return the constant set for this filter
    public float[] constants() {
        return new float[]{a1, a2, b0, b1, b2};
    }

    // perform one filtering step
    public float filter(float x) {
        y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = x;
        y2 = y1;
        y1 = y;
        return (y);
    }

    enum BIND_TYPE {
        Q,
        BW,
        S
    }
}