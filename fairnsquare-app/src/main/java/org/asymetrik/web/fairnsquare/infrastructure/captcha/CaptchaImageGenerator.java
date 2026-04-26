package org.asymetrik.web.fairnsquare.infrastructure.captcha;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;

import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaChallenge;

import javax.imageio.ImageIO;

/**
 * Generates PNG images for CAPTCHA challenges using Java AWT. Requires headless AWT mode
 * ({@code java.awt.headless=true}), configured via JVM options.
 */
@ApplicationScoped
public class CaptchaImageGenerator {

    private static final Color BG_COLOR = new Color(245, 245, 245);
    private static final Color QUESTION_COLOR = new Color(30, 30, 30);
    private static final Color BOX_BG_COLOR = new Color(255, 255, 255);
    private static final Color BOX_BORDER_COLOR = new Color(100, 120, 200);
    private static final Color BOX_TEXT_COLOR = new Color(30, 30, 30);
    private static final Color NOISE_COLOR = new Color(180, 180, 180, 120);

    private static final Font QUESTION_FONT = new Font("SansSerif", Font.BOLD, 36);
    private static final Font ANSWER_FONT = new Font("SansSerif", Font.BOLD, 24);

    /**
     * Generate a PNG image for the given challenge.
     *
     * @param challenge
     *            the challenge to render
     *
     * @return PNG bytes of the generated image
     */
    public byte[] generate(CaptchaChallenge challenge) {
        BufferedImage image = new BufferedImage(CaptchaChallenge.IMAGE_WIDTH, CaptchaChallenge.IMAGE_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            applyRenderingHints(g);
            drawBackground(g);
            drawNoise(g);
            drawQuestion(g, challenge);
            drawAnswerAreas(g, challenge);
        } finally {
            g.dispose();
        }
        return toPngBytes(image);
    }

    private void applyRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawBackground(Graphics2D g) {
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, CaptchaChallenge.IMAGE_WIDTH, CaptchaChallenge.IMAGE_HEIGHT);
    }

    private void drawNoise(Graphics2D g) {
        g.setColor(NOISE_COLOR);
        g.setStroke(new BasicStroke(1.0f));
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < 12; i++) {
            int x1 = rng.nextInt(CaptchaChallenge.IMAGE_WIDTH);
            int y1 = rng.nextInt(CaptchaChallenge.IMAGE_HEIGHT);
            int x2 = rng.nextInt(CaptchaChallenge.IMAGE_WIDTH);
            int y2 = rng.nextInt(CaptchaChallenge.IMAGE_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawQuestion(Graphics2D g, CaptchaChallenge challenge) {
        String question = "%d + %d = ?".formatted(challenge.operandA(), challenge.operandB());
        g.setFont(QUESTION_FONT);
        g.setColor(QUESTION_COLOR);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(question);
        int x = (CaptchaChallenge.IMAGE_WIDTH - textWidth) / 2;
        int y = (CaptchaChallenge.TOP_ZONE_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(question, x, y);
    }

    private void drawAnswerAreas(Graphics2D g, CaptchaChallenge challenge) {
        for (CaptchaChallenge.AnswerArea area : challenge.answerAreas()) {
            // Box background
            g.setColor(BOX_BG_COLOR);
            g.fillRoundRect(area.x(), area.y(), area.width(), area.height(), 10, 10);

            // Box border
            g.setColor(BOX_BORDER_COLOR);
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(area.x(), area.y(), area.width(), area.height(), 10, 10);

            // Answer number, centered
            g.setFont(ANSWER_FONT);
            g.setColor(BOX_TEXT_COLOR);
            FontMetrics fm = g.getFontMetrics();
            String text = String.valueOf(area.answer());
            int textX = area.x() + (area.width() - fm.stringWidth(text)) / 2;
            int textY = area.y() + (area.height() - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, textX, textY);
        }
    }

    private byte[] toPngBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode CAPTCHA image as PNG", e);
        }
    }
}
