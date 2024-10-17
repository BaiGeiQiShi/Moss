package edu.gatech.cc.domgad;

import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.BinomialDistribution;

public class RandomGenerator
{
    public static Random rand = new Random();
    
    public static int getUniformInt(int l, int h) {
	return l + (int) ((h-l+1) * Math.random());
    }

    public static double getUniformDouble(double l, double h) {
	return l + (h - l) * rand.nextDouble();
    }

    public static double getNorm(double mean, double sdev) {
	return new NormalDistribution(mean, sdev).sample();
    }

    public static int getBinomial(int n, double p) {
	return new BinomialDistribution(n, p).sample();
    }
}
