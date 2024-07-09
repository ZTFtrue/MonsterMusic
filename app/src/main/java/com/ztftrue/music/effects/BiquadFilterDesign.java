//package com.ztftrue.music.effects;
//
//public class BiquadFilterDesign {
//
//    private static void checkArguments(double sampleRateHz, double cornerFrequencyHz, double qualityFactor) {
//        if (cornerFrequencyHz >= sampleRateHz / 2) {
//            throw new IllegalArgumentException("Corner frequency must be less than half the sample rate.");
//        }
//        if (cornerFrequencyHz <= 0.0) {
//            throw new IllegalArgumentException("Corner frequency must be greater than 0.");
//        }
//        if (qualityFactor <= 0.0) {
//            throw new IllegalArgumentException("Quality factor must be greater than 0.");
//        }
//    }
//
//    private static void checkArgumentsBandEdges(double sampleRateHz, double lowerBandEdgeHz, double upperBandEdgeHz) {
//        if (sampleRateHz / 2 <= upperBandEdgeHz) {
//            throw new IllegalArgumentException("Upper band edge must be less than half the sample rate.");
//        }
//        if (upperBandEdgeHz <= lowerBandEdgeHz) {
//            throw new IllegalArgumentException("Upper band edge must be greater than lower band edge.");
//        }
//        if (lowerBandEdgeHz <= 0.0) {
//            throw new IllegalArgumentException("Lower band edge must be greater than 0.");
//        }
//    }
//
//    public static BiquadFilterCoefficients lowpassBiquadFilterCoefficients(
//            double sampleRateHz, double cornerFrequencyHz, double qualityFactor) {
//        checkArguments(sampleRateHz, cornerFrequencyHz, qualityFactor);
//        double omegaN = 2.0 * Math.PI * cornerFrequencyHz;
//        return BilinearTransform.transform(
//                new double[]{0.0, 0.0, omegaN * omegaN},
//                new double[]{1.0, omegaN / qualityFactor, omegaN * omegaN},
//                sampleRateHz, cornerFrequencyHz);
//    }
//
//    public static BiquadFilterCoefficients highpassBiquadFilterCoefficients(
//            double sampleRateHz, double cornerFrequencyHz, double qualityFactor) {
//        checkArguments(sampleRateHz, cornerFrequencyHz, qualityFactor);
//        double omegaN = 2.0 * Math.PI * cornerFrequencyHz;
//        return BilinearTransform.transform(
//                new double[]{1.0, 0.0, 0.0},
//                new double[]{1.0, omegaN / qualityFactor, omegaN * omegaN},
//                sampleRateHz, cornerFrequencyHz);
//    }
//
//    private static BiquadFilterCoefficients analogBandpassBiquadFilterCoefficients(
//            double centerFrequencyHz, double qualityFactor) {
//        double omegaN = 2.0 * Math.PI * centerFrequencyHz;
//        return new BiquadFilterCoefficients(
//                new double[]{0.0, omegaN / qualityFactor, 0.0},
//                new double[]{1.0, omegaN / qualityFactor, omegaN * omegaN});
//    }
//
//    public static BiquadFilterCoefficients bandpassBiquadFilterCoefficients(
//            double sampleRateHz, double centerFrequencyHz, double qualityFactor) {
//        checkArguments(sampleRateHz, centerFrequencyHz, qualityFactor);
//        BiquadFilterCoefficients bandpass = analogBandpassBiquadFilterCoefficients(centerFrequencyHz, qualityFactor);
//        return BilinearTransform.transform(bandpass.getB(), bandpass.getA(), sampleRateHz, centerFrequencyHz);
//    }
//
//    public static BiquadFilterCoefficients bandstopBiquadFilterCoefficients(
//            double sampleRateHz, double centerFrequencyHz, double qualityFactor) {
//        checkArguments(sampleRateHz, centerFrequencyHz, qualityFactor);
//        double omegaN = 2.0 * Math.PI * centerFrequencyHz;
//        return BilinearTransform.transform(
//                new double[]{1.0, 0.0, omegaN * omegaN},
//                new double[]{1.0, omegaN / qualityFactor, omegaN * omegaN},
//                sampleRateHz, centerFrequencyHz);
//    }
//
//    public static BiquadFilterCoefficients rangedBandpassBiquadFilterCoefficients(
//            double sampleRateHz, double lowerPassbandEdgeHz, double upperPassbandEdgeHz) {
//        checkArgumentsBandEdges(sampleRateHz, lowerPassbandEdgeHz, upperPassbandEdgeHz);
//        double omega1 = BilinearTransform.prewarp(lowerPassbandEdgeHz, sampleRateHz);
//        double omega2 = BilinearTransform.prewarp(upperPassbandEdgeHz, sampleRateHz);
//        return BilinearTransform.transform(
//                new double[]{0.0, omega2 - omega1, 0.0},
//                new double[]{1.0, omega2 - omega1, omega2 * omega1},
//                sampleRateHz, 0.0);
//    }
//
//    public static BiquadFilterCoefficients rangedBandstopBiquadFilterCoefficients(
//            double sampleRateHz, double lowerStopbandEdgeHz, double upperStopbandEdgeHz) {
//        checkArgumentsBandEdges(sampleRateHz, lowerStopbandEdgeHz, upperStopbandEdgeHz);
//        double omega1 = BilinearTransform.prewarp(lowerStopbandEdgeHz, sampleRateHz);
//        double omega2 = BilinearTransform.prewarp(upperStopbandEdgeHz, sampleRateHz);
//        return BilinearTransform.transform(
//                new double[]{1.0, 0.0, omega2 * omega1},
//                new double[]{1.0, omega2 - omega1, omega2 * omega1},
//                sampleRateHz, 0.0);
//    }
//
//    public static BiquadFilterCoefficients lowShelfBiquadFilterCoefficients(
//            float sampleRateHz, float cornerFrequencyHz, float Q, float gain) {
//        checkArguments(sampleRateHz, cornerFrequencyHz, Q);
//        if (gain <= 0) {
//            throw new IllegalArgumentException("Gain must be greater than 0.");
//        }
//        double sqrtk = Math.sqrt(gain);
//        double omega = 2 * Math.PI * cornerFrequencyHz / sampleRateHz;
//        double beta = Math.sin(omega) * Math.sqrt(sqrtk) / Q;
//
//        double sqrtkMinusOneCosOmega = (sqrtk - 1) * Math.cos(omega);
//        double sqrtkPlusOneCosOmega = (sqrtk + 1) * Math.cos(omega);
//
//        return new BiquadFilterCoefficients(
//                new double[]{
//                        sqrtk * ((sqrtk + 1) - sqrtkMinusOneCosOmega + beta),
//                        sqrtk * 2.0 * ((sqrtk - 1) - sqrtkPlusOneCosOmega),
//                        sqrtk * ((sqrtk + 1) - sqrtkMinusOneCosOmega - beta)
//                },
//                new double[]{
//                        (sqrtk + 1) + sqrtkMinusOneCosOmega + beta,
//                        -2.0 * ((sqrtk - 1) + sqrtkPlusOneCosOmega),
//                        (sqrtk + 1) + sqrtkMinusOneCosOmega - beta
//                });
//    }
//
//    public static BiquadFilterCoefficients highShelfBiquadFilterCoefficients(
//            float sampleRateHz, float cornerFrequencyHz, float Q, float gain) {
//        checkArguments(sampleRateHz, cornerFrequencyHz, Q);
//        if (gain <= 0) {
//            throw new IllegalArgumentException("Gain must be greater than 0.");
//        }
//        double sqrtk = Math.sqrt(gain);
//        double omega = 2 * Math.PI * cornerFrequencyHz / sampleRateHz;
//        double beta = Math.sin(omega) * Math.sqrt(sqrtk) / Q;
//
//        double sqrtkMinusOneCosOmega = (sqrtk - 1) * Math.cos(omega);
//        double sqrtkPlusOneCosOmega = (sqrtk + 1) * Math.cos(omega);
//
//        return new BiquadFilterCoefficients(
//                new double[]{
//                        sqrtk * ((sqrtk + 1) + sqrtkMinusOneCosOmega + beta),
//                        sqrtk * -2.0 * ((sqrtk - 1) + sqrtkPlusOneCosOmega),
//                        sqrtk * ((sqrtk + 1) + sqrtkMinusOneCosOmega - beta)
//                },
//                new double[]{
//                        (sqrtk + 1) - sqrtkMinusOneCosOmega + beta,
//                        2.0 * ((sqrtk - 1) - sqrtkPlusOneCosOmega),
//                        (sqrtk + 1) - sqrtkMinusOneCosOmega - beta
//                });
//    }
//
//    public static BiquadFilterCoefficients parametricPeakBiquadFilterCoefficients(
//            float sampleRateHz, float centerFrequencyHz, float Q, float gain) {
//        checkArguments(sampleRateHz, centerFrequencyHz, Q);
//        if (gain < 0) {
//            throw new IllegalArgumentException("Gain must be non-negative.");
//        }
//        BiquadFilterCoefficients resonator = analogBandpassBiquadFilterCoefficients(centerFrequencyHz, Q);
//        double[] b = resonator.getB();
//        double[] a = resonator.getA();
//
//        if (gain < 1) {
//            for (int i = 0; i < b.length; i++) {
//                b[i] *= gain;
//            }
//        } else {
//            for (int i = 0; i < a.length; i++) {
//                a[i] /= gain;
//            }
//        }
//
//        return BilinearTransform.transform(b, a, sampleRateHz, centerFrequencyHz);
//    }
//
//
//    static  class BilinearTransform{
//
//
//    }
//
//}
