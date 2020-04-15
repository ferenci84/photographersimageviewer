package com.dynamicrpogrammingsolutions.photographersimageviewer;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;

public class RotateImage {

    public static Image rotateImage(Image img,final int quadrants) {
        final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(img,null);
        return SwingFXUtils.toFXImage(rotateImage(bufferedImage,quadrants),null);
    }

    public static BufferedImage rotateImage(final BufferedImage image, int quadrants)
    {
        AffineTransform tx = new AffineTransform();

        tx.translate(image.getHeight() / 2,image.getWidth() / 2);
        tx.rotate(quadrants* (Math.PI / 2));
        // first - center image at the origin so rotate works OK
        tx.translate(-image.getWidth() / 2,-image.getHeight() / 2);

        AffineTransformOp transformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        return transformOp.filter(image, null);
    }

}
