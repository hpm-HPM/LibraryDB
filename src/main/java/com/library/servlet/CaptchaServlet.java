package com.library.servlet;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * 生成 4 位图形验证码，答案保存在 Session 中。
 */
@WebServlet("/api/captcha")
public class CaptchaServlet extends HttpServlet {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        HttpSession session = req.getSession();
        session.setAttribute("captcha", code.toString());

        resp.setContentType("image/png");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 252));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < 8; i++) {
                g.setColor(new Color(120 + random.nextInt(100), 140 + random.nextInt(90), 180 + random.nextInt(70)));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            g.setFont(new Font("Arial", Font.BOLD, 27));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(30 + random.nextInt(100), 60 + random.nextInt(100), 120 + random.nextInt(110)));
                int x = 12 + i * 26 + random.nextInt(4);
                int y = 29 + random.nextInt(6);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
            }

            for (int i = 0; i < 50; i++) {
                g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", resp.getOutputStream());
    }
}