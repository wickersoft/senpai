/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package senpai;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import javax.imageio.ImageIO;

/**
 *
 * @author Dennis
 */
public class AspectRatioStats {

    static final HashSet<String> knownNames = new HashSet<>();
    static final ArrayList<Double> ratioLogs = new ArrayList<>(20000);
    static double sum = 0;

    public static void main(String[] args) {
        File randomDirectory = new File("C:/Dennis/Bilder/---/random/");
        File cnnLearnDirectory = new File("D:/CNN Training");
        File cache = new File("C:/users/dennis/desktop/cache.txt");
        try {
            FileInputStream fis = new FileInputStream(cache);
            byte[] cacheTextBuf = new byte[(int) cache.length()];
            fis.read(cacheTextBuf);
            fis.close();
            String s = new String(cacheTextBuf);
            String[] lines = s.split("\r\n");
            for (String line : lines) {
                String[] columns = line.split(": ");
                knownNames.add(columns[0]);
                double d = Double.parseDouble(columns[1]);
                ratioLogs.add(d);
                sum += d;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        System.out.println("Loaded " + knownNames.size() + " files from cache");
        
        //parseSubdirectory(randomDirectory);
        parseSubdirectory(cnnLearnDirectory);

        double mean = sum / (double) ratioLogs.size();
        double squareErrorSum = 0;
        for (double d : ratioLogs) {
            squareErrorSum += Math.pow(mean - d, 2);
        }
        squareErrorSum /= (ratioLogs.size() - 1);
        double stdDev = Math.sqrt(squareErrorSum);

        int bins = 24;
        double span = 3;

        int[] histogram = new int[bins];

        for (double d : ratioLogs) {
            int bin = (int) Math.floor((d - mean) / stdDev * bins / span / 2);
            if (bin > -(bins / 2) && bin < (bins / 2)) {
                histogram[bin + (bins / 2)]++;
            }
        }

        System.out.println("-----------------------");
        System.out.println("Samples: " + ratioLogs.size());
        System.out.println("Mean: " + mean);
        System.out.println("StdDev: " + stdDev);
        for (int i = 0; i < (bins / 2); i++) {
            System.out.println(((i - (bins / 2.0)) * span / bins * 2.0) + "σ: " + histogram[i]);
        }
        for (int i = (bins / 2); i < bins; i++) {
            System.out.println(((i - (bins / 2.0) + 1) * span / bins * 2.0) + "σ: " + histogram[i]);
        }

    }

    static void parseSubdirectory(File directory) {
        File[] contents = directory.listFiles();
        for (File subFile : contents) {
            if (subFile.isDirectory()) {
                parseSubdirectory(subFile);
            } else {
                String name = subFile.getName();
                if (!knownNames.contains(name) && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"))) {
                    try {
                        BufferedImage bi = ImageIO.read(subFile);
                        double logRatio = Math.log((double) bi.getWidth() / (double) bi.getHeight()) * 1.4426950408889634;
                        ratioLogs.add(logRatio);
                        sum += logRatio;
                        System.out.println(name + ": " + logRatio);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

}
