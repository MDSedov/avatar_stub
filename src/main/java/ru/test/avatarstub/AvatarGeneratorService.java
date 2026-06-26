package ru.test.avatarstub;

import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Random;

@Service
public class AvatarGeneratorService {

    private static final int PROGRESS_STEP = 1000;

    private final Path avatarDir;
    private final int width;
    private final int height;
    private final int targetSizeBytes;

    public AvatarGeneratorService(AvatarProperties properties) {
        this.avatarDir = Paths.get(properties.getDir());
        this.width = properties.getWidth();
        this.height = properties.getHeight();
        this.targetSizeBytes = properties.getTargetSizeKb() * 1024;
    }

    public void generatePool(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }

        try {
            Files.createDirectories(avatarDir);

            long startedAt = System.currentTimeMillis();
            int generated = 0;
            int skipped = 0;

            for (int i = 1; i <= count; i++) {
                Path file = avatarDir.resolve(String.format("avatar_%06d.jpg", i));

                if (Files.exists(file)) {
                    skipped++;
                    continue;
                }

                BufferedImage image = generateAvatar(i);
                byte[] jpeg = toJpegNearTargetSize(image);

                Files.write(file, jpeg, StandardOpenOption.CREATE_NEW);
                generated++;

                if (i % PROGRESS_STEP == 0 || i == count) {
                    long elapsedMs = System.currentTimeMillis() - startedAt;
                    System.out.println("Avatars processed: " + i + "/" + count
                            + ", generated: " + generated
                            + ", skipped: " + skipped
                            + ", elapsedSec: " + elapsedMs / 1000);
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Cannot generate avatars into " + avatarDir.toAbsolutePath(), e);
        }
    }

    private BufferedImage generateAvatar(int seed) {
        Random random = new Random(seed);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Color bg1 = new Color(50 + random.nextInt(120), 50 + random.nextInt(120), 50 + random.nextInt(120));
        Color bg2 = new Color(80 + random.nextInt(120), 80 + random.nextInt(120), 80 + random.nextInt(120));

        for (int y = 0; y < height; y++) {
            float ratio = y / (float) Math.max(1, height - 1);
            int r = mix(bg1.getRed(), bg2.getRed(), ratio);
            int gr = mix(bg1.getGreen(), bg2.getGreen(), ratio);
            int b = mix(bg1.getBlue(), bg2.getBlue(), ratio);
            g.setColor(new Color(r, gr, b));
            g.drawLine(0, y, width, y);
        }

        // Controlled noise makes JPEG size realistic and avoids tiny files.
        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width; x += 2) {
                int rgb = image.getRGB(x, y);
                int delta = random.nextInt(50) - 25;
                int r = clamp(((rgb >> 16) & 0xFF) + delta);
                int gr = clamp(((rgb >> 8) & 0xFF) + delta);
                int b = clamp((rgb & 0xFF) + delta);
                image.setRGB(x, y, new Color(r, gr, b).getRGB());
            }
        }

        // Face
        Color skin = new Color(215 + random.nextInt(25), 165 + random.nextInt(35), 130 + random.nextInt(30));
        g.setColor(skin);
        g.fillOval(145, 95, 260, 260);

        // Hair
        g.setColor(new Color(20 + random.nextInt(80), 15 + random.nextInt(60), 10 + random.nextInt(50)));
        g.fillArc(140, 70, 270, 190, 0, 180);

        // Ears
        g.setColor(skin.darker());
        g.fillOval(125, 205, 45, 75);
        g.fillOval(380, 205, 45, 75);

        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval(215, 210, 45, 30);
        g.fillOval(295, 210, 45, 30);

        Color eyeColor = new Color(random.nextInt(80), random.nextInt(110), random.nextInt(120));
        g.setColor(eyeColor);
        g.fillOval(229, 216, 16, 16);
        g.fillOval(309, 216, 16, 16);

        g.setColor(Color.BLACK);
        g.fillOval(234, 220, 7, 7);
        g.fillOval(314, 220, 7, 7);

        // Brows
        g.setStroke(new BasicStroke(5));
        g.drawLine(205, 195, 265, 188);
        g.drawLine(290, 188, 350, 195);

        // Nose
        g.setColor(new Color(155, 95, 85));
        g.setStroke(new BasicStroke(3));
        g.drawLine(275, 240, 260, 292);
        g.drawLine(260, 292, 287, 292);

        // Mouth
        g.setStroke(new BasicStroke(5));
        g.setColor(new Color(115, 35, 45));
        g.drawArc(235, 295, 90, 42, 195, 150);

        // Body
        g.setColor(new Color(random.nextInt(180), random.nextInt(180), random.nextInt(180)));
        g.fillRoundRect(150, 375, 250, 190, 60, 60);

        // Number for debug uniqueness
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(new Color(20, 20, 20));
        g.drawString("#" + seed, 18, 520);

        g.dispose();
        return image;
    }

    private byte[] toJpegNearTargetSize(BufferedImage image) throws IOException {
        byte[] best = null;
        int bestDiff = Integer.MAX_VALUE;

        float low = 0.35f;
        float high = 0.95f;

        for (int i = 0; i < 8; i++) {
            float quality = (low + high) / 2.0f;
            byte[] current = toJpeg(image, quality);
            int diff = Math.abs(current.length - targetSizeBytes);

            if (diff < bestDiff) {
                best = current;
                bestDiff = diff;
            }

            if (current.length > targetSizeBytes) {
                high = quality;
            } else {
                low = quality;
            }
        }

        return best;
    }

    private byte[] toJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");

        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG writers found");
        }

        ImageWriter writer = writers.next();
        JPEGImageWriteParam param = new JPEGImageWriteParam(null);
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(targetSizeBytes);
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            return baos.toByteArray();

        } finally {
            writer.dispose();
        }
    }

    private int mix(int a, int b, float ratio) {
        return Math.round(a + (b - a) * ratio);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
