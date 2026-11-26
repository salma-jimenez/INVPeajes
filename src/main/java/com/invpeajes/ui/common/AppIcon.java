package com.invpeajes.ui.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Ícono de RutaPeaje para la barra de título y la barra de tareas de Windows:
 * cuadro azul redondeado con dos montañas blancas y una carretera.
 * Se dibuja con código (no necesita archivo de imagen) en varios tamaños,
 * y Windows escoge el que mejor le quede.
 *
 * Uso en cada ventana: {@code setIconImages(AppIcon.images());}
 *
 * @author Salomejimenez
 */
public final class AppIcon {

    private static final Color BLUE_TOP = new Color(0x3B82F6);
    private static final Color BLUE_BOTTOM = new Color(0x1D4ED8);
    private static final int[] SIZES = {16, 20, 24, 32, 40, 48, 64, 128, 256};

    private static List<Image> images;

    private AppIcon() {
    }

    /**
     * Ícono en varios tamaños, listo para {@code JFrame.setIconImages}.
     * @return Imágenes del ícono (se crean una sola vez).
     */
    public static synchronized List<Image> images() {
        if (images == null) {
            List<Image> list = new ArrayList<>();
            for (int size : SIZES) {
                list.add(paint(size));
            }
            images = list;
        }
        return images;
    }

    /**
     * Dibuja el ícono en un tamaño.
     * @param size Lado en píxeles.
     * @return Imagen cuadrada con fondo transparente.
     */
    public static BufferedImage paint(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 100.0, size / 100.0);   // se dibuja en una cuadrícula de 100 x 100

        // Fondo: cuadro azul redondeado
        g.setPaint(new GradientPaint(0, 0, BLUE_TOP, 0, 100, BLUE_BOTTOM));
        g.fill(new RoundRectangle2D.Double(0, 0, 100, 100, 26, 26));

        // Montañas blancas: una grande a la derecha y una pequeña a la izquierda
        Path2D mountains = new Path2D.Double();
        mountains.moveTo(14, 80);
        mountains.lineTo(36, 46);
        mountains.lineTo(46, 60);
        mountains.lineTo(62, 22);
        mountains.lineTo(88, 80);
        mountains.closePath();
        g.setColor(Color.WHITE);
        g.fill(mountains);

        // Carretera azul que sube por la montaña grande
        Path2D road = new Path2D.Double();
        road.moveTo(54, 80);
        road.curveTo(56, 66, 68, 62, 64, 50);
        road.curveTo(61, 42, 63, 36, 62, 30);
        g.setColor(BLUE_BOTTOM);
        g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(road);

        g.dispose();
        return img;
    }
}
