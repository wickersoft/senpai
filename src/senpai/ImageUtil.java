/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package senpai;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 *
 * @author Dennis
 */
public class ImageUtil {

    public static void fillFitNewSourceImage(Graphics sourceImgGraphics, BufferedImage newSourceImg, int srcImageWidth, int srcImageHeight) {
        double width = newSourceImg.getWidth();
        double height = newSourceImg.getHeight();
        double verticalScaleFactor = srcImageHeight / height;
        double horizontalScaleFactor = srcImageWidth / width;
        double scaleFactor = Math.max(verticalScaleFactor, horizontalScaleFactor);
        int newWidth = (int) (width * scaleFactor);
        int newHeight = (int) (height * scaleFactor);
        int yOffset = Math.min((srcImageHeight - newHeight) / 2, 0);
        int xOffset = Math.min((srcImageWidth - newWidth) / 2, 0);
        Image scaledImg = newSourceImg.getScaledInstance(newWidth,
                newHeight, Image.SCALE_SMOOTH);
        sourceImgGraphics.drawImage(scaledImg, xOffset, yOffset, null);
    }
}
