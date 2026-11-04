import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public class GenerateCloset {
    public static void main(String[] args) throws Exception {
        int W = 800, H = 455;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Random rng = new Random(42);

        // ══════════════════════════════════════════════════════════════════════
        // CLOSET INTERIOR — close-up of door filling entire frame, hair-thin
        // crack of warm light on the right edge. Dark, claustrophobic, moody.
        // ══════════════════════════════════════════════════════════════════════

        // ── 1. Entire background: nearly-black dark wood fills everything ─────
        g.setColor(new Color(0x18, 0x09, 0x06));
        g.fillRect(0, 0, W, H);

        // ── 2. Wood grain texture across the whole door surface ───────────────
        //    Simulate grain with many thin semi-transparent vertical lines
        //    varying in brightness. The door fills x=0..760, y=0..H.
        for (int x = 0; x < 760; x++) {
            int base = 0x1e + rng.nextInt(8);   // varies 30–37
            int r = Math.min(255, base + rng.nextInt(6));
            int grn = Math.max(0, base / 5 + rng.nextInt(3));
            int b = Math.max(0, base / 8);
            // Occasional slightly lighter grain streak
            if (rng.nextInt(18) == 0) { r += 18; grn += 4; }
            g.setColor(new Color(Math.min(255,r), Math.min(255,grn), Math.min(255,b)));
            g.drawLine(x, 0, x, H);
        }

        // ── 3. Large door panel recesses — barely darker rectangles ──────────
        //    Upper panel
        int panelX = 120, panelW = 520;
        g.setColor(new Color(0x10, 0x06, 0x03, 180));
        g.fillRoundRect(panelX, 30, panelW, 175, 8, 8);
        // Subtle raised-edge highlight (lighter strip on top/left of panel)
        g.setColor(new Color(0x35, 0x18, 0x0c, 80));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(panelX, 30, panelW, 175, 8, 8);
        //    Lower panel
        g.setColor(new Color(0x10, 0x06, 0x03, 180));
        g.fillRoundRect(panelX, 225, panelW, 215, 8, 8);
        g.setColor(new Color(0x35, 0x18, 0x0c, 80));
        g.drawRoundRect(panelX, 225, panelW, 215, 8, 8);

        // ── 4. Left edge of frame: thin very dark strip (where door meets jamb) ─
        g.setColor(new Color(0x08, 0x03, 0x02));
        g.fillRect(0, 0, 22, H);
        // Slight shadow fade inward
        for (int i = 0; i < 18; i++) {
            g.setColor(new Color(0x08, 0x03, 0x02, 255 - i * 12));
            g.drawLine(22 + i, 0, 22 + i, H);
        }

        // ── 5. Dark vignette overlay: corners are almost pure black ──────────
        //    Left third darkens toward edge
        for (int x = 0; x < 260; x++) {
            int alpha = (int)(160 * (1.0 - x / 260.0));
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(x, 0, x, H);
        }
        //    Right side darkens toward the light crack approach zone
        for (int x = 760; x >= 620; x--) {
            int alpha = (int)(120 * (1.0 - (760 - x) / 140.0));
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(x, 0, x, H);
        }
        //    Top/bottom vignette
        for (int y = 0; y < 80; y++) {
            int alpha = (int)(140 * (1.0 - y / 80.0));
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(0, y, W, y);
        }
        for (int y = H; y > H - 80; y--) {
            int alpha = (int)(140 * (1.0 - (H - y) / 80.0));
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(0, y, W, y);
        }

        // ── 6. Door knob — small glowing white/cream dot ─────────────────────
        //    Positioned slightly right of center, at mid-height
        int knobX = 490, knobY = 215;
        // Soft glow halo
        for (int r = 14; r >= 0; r--) {
            float frac = 1f - r / 14f;
            int alpha = (int)(frac * frac * 80);
            g.setColor(new Color(255, 240, 200, alpha));
            g.fillOval(knobX - r, knobY - r, r * 2, r * 2);
        }
        g.setColor(new Color(0xf0, 0xe8, 0xd0));
        g.fillOval(knobX - 4, knobY - 4, 9, 9);
        g.setColor(new Color(0xff, 0xff, 0xff));
        g.fillOval(knobX - 1, knobY - 2, 3, 3);

        // ── 7. Light crack — right edge of door, very thin warm sliver ───────
        int crackX = 762;
        // Extremely bright core (2px)
        g.setColor(new Color(0xff, 0xf8, 0xf0));
        g.fillRect(crackX, 0, 2, H);
        // First glow band (warm yellow, 4px)
        g.setColor(new Color(0xff, 0xe8, 0xb0, 210));
        g.fillRect(crackX - 2, 0, 2, H);
        g.setColor(new Color(0xff, 0xd8, 0x80, 160));
        g.fillRect(crackX - 5, 0, 3, H);
        g.setColor(new Color(0xff, 0xc0, 0x50, 100));
        g.fillRect(crackX - 9, 0, 4, H);
        g.setColor(new Color(0xe0, 0x98, 0x28, 60));
        g.fillRect(crackX - 15, 0, 6, H);
        g.setColor(new Color(0xb0, 0x68, 0x18, 30));
        g.fillRect(crackX - 24, 0, 9, H);
        // The crack continues brighter to the right edge of canvas
        g.setColor(new Color(0xff, 0xf8, 0xf0));
        g.fillRect(crackX + 2, 0, W - crackX - 2, H);
        // Soften right side
        for (int x = crackX + 4; x < W; x++) {
            int alpha = Math.min(255, (int)(255 * (1.0 - (x - crackX - 4) / 34.0)));
            g.setColor(new Color(0xff, 0xf0, 0xd0, alpha));
            g.drawLine(x, 0, x, H);
        }

        // ── 8. Final darkening pass to make everything richer/deeper ─────────
        g.setColor(new Color(0, 0, 0, 45));
        g.fillRect(0, 0, crackX - 28, H);

        g.dispose();

        String outPath = args.length > 0 ? args[0] : "assets/preview/closet_preview.png";
        ImageIO.write(img, "PNG", new File(outPath));
        System.out.println("Saved: " + outPath);
    }
}
