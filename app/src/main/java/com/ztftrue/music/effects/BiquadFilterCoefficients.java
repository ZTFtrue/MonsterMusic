package com.ztftrue.music.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

public class BiquadFilterCoefficients {
    private static final Logger logger = Logger.getLogger(BiquadFilterCoefficients.class.getName());

    private double[] b;
    private double[] a;

    public BiquadFilterCoefficients() {
        this(new double[]{1.0, 0.0, 0.0}, new double[]{1.0, 0.0, 0.0});
    }

    public BiquadFilterCoefficients(double[] coeffsB, double[] coeffsA) {
        if (coeffsA.length != 3 || coeffsB.length != 3) {
            throw new IllegalArgumentException("Coefficient vectors must each have 3 elements.");
        }
        this.b = coeffsB;
        this.a = coeffsA;
    }

    public boolean isStable() {
        // Implementation of stability check (not provided in original code)
        return true;
    }

    public void normalize() {
        double normalize = a[0];
        for (int i = 0; i < b.length; i++) {
            b[i] = b[i] / normalize;
        }
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i] / normalize;
        }
    }

    public double[] getB() {
        return b;
    }

    public double[] getA() {
        return a;
    }

//    public Complex evalTransferFunction(Complex z) {
//        Complex numerator = b.get(0)*(z.im)+(b.get(1))*(z.im)+(b.get(2));
//        Complex denominator = a.get(0).multiply(z).add(a.get(1)).multiply(z).add(a.get(2));
//        return numerator.divide(denominator);
//    }

    public Pair<Complex, Complex> getPoles() {
        // Implementation of pole calculation (not provided in original code)
        return new Pair<>(new Complex(0, 0), new Complex(0, 0));
    }

    public Pair<Double, Double> findPeakFrequencyRadiansPerSample() {
        // Implementation of peak frequency finding (not provided in original code)
        return new Pair<>(0.0, 0.0);
    }

    public double gainMagnitudeAtFrequency(double frequencyRadiansPerSample) {
        Complex z = Complex.polar(1.0, frequencyRadiansPerSample);
        return 0; //evalTransferFunction(z).abs();
    }

    public double gainMagnitudeAtFrequency(double frequency, double sampleRate) {
        return gainMagnitudeAtFrequency(2 * Math.PI * frequency / sampleRate);
    }

    public double phaseResponseAtFrequency(double frequencyRadiansPerSample) {
        Complex z = Complex.polar(1.0, frequencyRadiansPerSample);
        return 0; //evalTransferFunction(z).arg();
    }

    public double phaseResponseAtFrequency(double frequency, double sampleRate) {
        return phaseResponseAtFrequency(2 * Math.PI * frequency / sampleRate);
    }

    public void asLadderFilterCoefficients(List<Double> k, List<Double> v) {
//        MakeLadderCoefficientsFromTransferFunction(b, a, k, v);
    }

    public double estimateDecayTime(double decayDb) {
        // Implementation of decay time estimation (not provided in original code)
        return 0.0;
    }

    public void setGainAtFrequency(double gain, double frequencyRadiansPerSample) {
        double oldGain = gainMagnitudeAtFrequency(frequencyRadiansPerSample);
        adjustGain(gain / oldGain);
    }

    public void setPeakGain(double newPeakGain) {
        setGainAtFrequency(newPeakGain, findPeakFrequencyRadiansPerSample().getFirst());
    }

    public void adjustGain(double gainMultiplier) {
        for (int i = 0; i < b.length; i++) {
            b[i] = b[i] * gainMultiplier;
        }
    }

    public String toString() {
        return "BiquadFilterCoefficients{" + "b=" + b + ", a=" + a + '}';
    }

    public static void makeLadderCoefficientsFromTransferFunction(
            List<Double> coeffsB, List<Double> coeffsA, List<Double> coeffsK, List<Double> coeffsV) {
        // Implementation of ladder coefficients calculation (not provided in original code)
    }

    public static Pair<Double, Double> findPeakByBisection(
            Function<Double, Double> fun, double minimum, double maximum) {
        // Implementation of peak finding by bisection (not provided in original code)
        return new Pair<>(0.0, 0.0);
    }

    class BiquadFilterCascadeCoefficients {
        private List<BiquadFilterCoefficients> coeffs;

        public BiquadFilterCascadeCoefficients() {
            this.coeffs = Collections.singletonList(new BiquadFilterCoefficients());
        }

        public BiquadFilterCascadeCoefficients(List<BiquadFilterCoefficients> coeffsVector) {
            if (coeffsVector.isEmpty()) {
                this.coeffs = Collections.singletonList(new BiquadFilterCoefficients());
            } else {
                this.coeffs = new ArrayList<>(coeffsVector);
            }
        }

        public BiquadFilterCascadeCoefficients(BiquadFilterCoefficients biquadCoeffs) {
            this.coeffs = Collections.singletonList(biquadCoeffs);
        }

        public boolean isStable() {
            // Implementation of stability check (not provided in original code)
            return true;
        }

        public void appendBiquad(BiquadFilterCoefficients biquadCoeffs) {
            coeffs.add(biquadCoeffs);
            simplify();
        }

        public void appendDenominator(double[] aCoeffs) {
            if (aCoeffs.length != 3 || aCoeffs[0] == 0.0) {
                throw new IllegalArgumentException("Invalid denominator coefficients.");
            }
            appendBiquad(new BiquadFilterCoefficients(new double[]{1.0, 0.0, 0.0}, aCoeffs));
        }

        public void appendNumerator(double[] bCoeffs) {
            if (bCoeffs.length != 3) {
                throw new IllegalArgumentException("Invalid numerator coefficients.");
            }
            appendBiquad(new BiquadFilterCoefficients(bCoeffs, new double[]{1.0, 0.0, 0.0}));
        }

        public void asPolynomialRatio(List<Double> b, List<Double> a) {
            // Implementation of polynomial ratio conversion (not provided in original code)
        }

        public void asLadderFilterCoefficients(List<Double> k, List<Double> v) {
            List<Double> b = new ArrayList<>();
            List<Double> a = new ArrayList<>();
            asPolynomialRatio(b, a);
            BiquadFilterCoefficients.makeLadderCoefficientsFromTransferFunction(b, a, k, v);
        }

        public void simplify() {
            // Implementation of simplification (not provided in original code)
        }

        public Complex evalTransferFunction(Complex z) {
            Complex result = new Complex(1, 0);
            for (BiquadFilterCoefficients coeff : coeffs) {
//                result = result.multiply(coeff.evalTransferFunction(z));
            }
            return result;
        }

        public Pair<Double, Double> findPeakFrequencyRadiansPerSample() {
            // Implementation of peak frequency finding (not provided in original code)
            return new Pair<>(0.0, 0.0);
        }

        public double gainMagnitudeAtFrequency(double frequencyRadiansPerSample) {
            Complex z = Complex.polar(1.0, frequencyRadiansPerSample);
            return evalTransferFunction(z).abs();
        }

        public double gainMagnitudeAtFrequency(double frequency, double sampleRate) {
            return gainMagnitudeAtFrequency(2 * Math.PI * frequency / sampleRate);
        }

        public double phaseResponseAtFrequency(double frequencyRadiansPerSample) {
            Complex z = Complex.polar(1.0, frequencyRadiansPerSample);
            return evalTransferFunction(z).arg();
        }

        public double phaseResponseAtFrequency(double frequency, double sampleRate) {
            return phaseResponseAtFrequency(2 * Math.PI * frequency / sampleRate);
        }

        public void setGainAtFrequency(double gain, double frequencyRadiansPerSample) {
            double oldGain = gainMagnitudeAtFrequency(frequencyRadiansPerSample);
            adjustGain(gain / oldGain);
        }

        public void setPeakGain(double newPeakGain) {
            setGainAtFrequency(newPeakGain, findPeakFrequencyRadiansPerSample().getFirst());
        }

        public void adjustGain(double gainMultiplier) {
            coeffs.get(0).adjustGain(gainMultiplier);
        }

        public BiquadFilterCoefficients get(int index) {
            return coeffs.get(index);
        }

        public void set(int index, BiquadFilterCoefficients biquadFilterCoefficients) {
            coeffs.set(index, biquadFilterCoefficients);
        }

        public int size() {
            return coeffs.size();
        }

        public String toString() {
            return "BiquadFilterCascadeCoefficients{" + "coeffs=" + coeffs + '}';
        }
    }

    static class Complex {
        private final double re;
        private final double im;

        public Complex(double re, double im) {
            this.re = re;
            this.im = im;
        }

        public double abs() {
            return Math.hypot(re, im);
        }

        public double arg() {
            return Math.atan2(im, re);
        }

        public Complex add(Complex b) {
            return new Complex(this.re + b.re, this.im + b.im);
        }

        public Complex multiply(Complex b) {
            return new Complex(this.re * b.re - this.im * b.im, this.re * b.im + this.im * b.re);
        }

        public Complex multiply(double scalar) {
            return new Complex(this.re * scalar, this.im * scalar);
        }

        public Complex divide(Complex b) {
            double denominator = b.re * b.re + b.im * b.im;
            return new Complex((this.re * b.re + this.im * b.im) / denominator,
                    (this.im * b.re - this.re * b.im) / denominator);
        }

        public static Complex polar(double r, double theta) {
            return new Complex(r * Math.cos(theta), r * Math.sin(theta));
        }

        public String toString() {
            return String.format("Complex{re=%f, im=%f}", re, im);
        }
    }

    static class Pair<F, S> {
        private final F first;
        private final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }

        public String toString() {
            return String.format("Pair{first=%s, second=%s}", first, second);
        }
    }
}
