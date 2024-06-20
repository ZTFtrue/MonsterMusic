/***************************************************************************
 *   Copyright (C) 2011 by Paul Lutus                                      *
 *   lutusp@arachnoid.com                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/
package com.ztftrue.music.effects;


import org.apache.commons.math3.util.FastMath;

/**
 * @author lutusp
 */
// http://en.wikipedia.org/wiki/Digital_biquad_filter
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
    private double a0, a1, a2, b0, b1, b2;
    private double x1, x2, y, y1, y2;
    public double gain_abs;
    private int type;
    BIND_TYPE bindType = BIND_TYPE.Q;

    public enum BIND_TYPE {
        Q,
        BW,
        S
    }

    double center_freq, sample_rate, Q, gainDB, bw;

    public BiQuadraticFilter() {
    }

    public BiQuadraticFilter(int type, double center_freq, double sample_rate, double Q, double gainDB) {
        configure(type, center_freq, sample_rate, Q, gainDB);
    }

    // constructor without gain setting
    public BiQuadraticFilter(int type, double center_freq, double sample_rate, double Q) {
        configure(type, center_freq, sample_rate, Q, 0);
    }

    public void reset() {
        x1 = x2 = y1 = y2 = 0;
    }

    public double frequency() {
        return center_freq;
    }


    public void configure(int type, double center_freq, double sample_rate, double Q) {
        configure(type, center_freq, sample_rate, Q, 0);
    }

    public void configure(int type, double center_freq, double sample_rate, double gainDB, double Q) {
        reset();
        Q = (Q == 0) ? 1e-9 : Q;
        this.type = type;
        this.sample_rate = sample_rate;
        this.Q = Q;
        this.gainDB = gainDB;
        reconfigure(center_freq);
    }

    public void configure(int type, double center_freq, double sample_rate, double gainDB,BIND_TYPE bindType, double bw) {
        reset();
        this.type = type;
        this.bindType = BIND_TYPE.BW;
        this.bw = bw;
        this.sample_rate = sample_rate;
        this.gainDB = gainDB;
        this.Q = bw / 60.0;
        reconfigure(center_freq);
    }

    // allow parameter change while running
    public void reconfigure(double cf) {

        center_freq = cf;
        // only used for peaking and shelving filters
        gain_abs = FastMath.pow(10, gainDB / 40);
        double omega = 2 * FastMath.PI * cf / sample_rate;
        double sn = FastMath.sin(omega);
        double cs = FastMath.cos(omega);
        double alpha = 0.0;
        if (bindType == (BIND_TYPE.BW)) {
            double bwq = bw / 60.0;
            double LN2 = 0.69314718055994530942;
            alpha = sn * FastMath.sinh(LN2 / 2.0 * bwq * omega / FastMath.sin(omega));
        } else if (bindType == BIND_TYPE.Q) {
            alpha = sn / (2 * Q);
        } else if (bindType == BIND_TYPE.S) {
            alpha = sn * FastMath.sqrt((gain_abs + 1.0 / gain_abs) * (1 / Q - 1) + 2) / 2.0;
        }
        double beta = FastMath.sqrt(gain_abs + gain_abs);
        switch (type) {
            case Gain -> {
                b0 = gain_abs;
                a0 = 1.0;
                a1 = a2 = b1 = b2 = 0.0;
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
                a0 = 1.0 + alpha;
                a1 = -2.0 * cs;
                a2 = 1.0 - alpha;
            }
            case HIGHPASS -> {
                b0 = b2 = (1 + cs) / 2;
                b1 = -(1.0 + cs);
                a0 = 1.0 + alpha;
                a1 = -2.0 * cs;
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
                b0 = gain_abs * ((gain_abs + 1) - (gain_abs - 1) * cs + beta * sn);
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
    public double[] constants() {
        return new double[]{a1, a2, b0, b1, b2};
    }

    // perform one filtering step
    public double filter(double x) {
        y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = x;
        y2 = y1;
        y1 = y;
        return y;
    }
}
