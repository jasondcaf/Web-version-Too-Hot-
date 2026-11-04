import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

// Two-door closet interior. You are inside the closet looking at the doors.
// Left door fills left half, right door fills right half.
// Center crack at x=400 shows mom's room peeking through.

public class GenerateCloset2 {
    public static void main(String[] args) throws Exception {
        int W = 800, H = 455;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        Random rng = new Random(42);

        int crackX = 340; // center gap start (wider reveal)
        int crackW = 120; // gap width — wide enough to see the room

        // ── Load mom's room image ────────────────────────────────────────────
        BufferedImage momsRoom = null;
        try {
            momsRoom = ImageIO.read(new File("assets/rooms/moms_room.jpg"));
        } catch (Exception ex) { /* fall back to warm light */ }

        // ── 1. Background fill — nearly black ───────────────────────────────
        g.setColor(new Color(0x10, 0x07, 0x04));
        g.fillRect(0, 0, W, H);

        // ── 2. Wood grain — LEFT door (x=0..crackX) ─────────────────────────
        for (int x = 0; x < crackX; x++) {
            int base = 0x1e + rng.nextInt(8);
            int r = Math.min(255, base + rng.nextInt(6));
            int grn = Math.max(0, base / 5 + rng.nextInt(3));
            int b = Math.max(0, base / 8);
            if (rng.nextInt(18) == 0) { r += 18; grn += 4; }
            g.setColor(new Color(Math.min(255,r), Math.min(255,grn), Math.min(255,b)));
            g.drawLine(x, 0, x, H);
        }

        // ── 3. Wood grain — RIGHT door (x=crackX+crackW..W) ─────────────────
        for (int x = crackX + crackW; x < W; x++) {
            int base = 0x1e + rng.nextInt(8);
            int r = Math.min(255, base + rng.nextInt(6));
            int grn = Math.max(0, base / 5 + rng.nextInt(3));
            int b = Math.max(0, base / 8);
            if (rng.nextInt(18) == 0) { r += 18; grn += 4; }
            g.setColor(new Color(Math.min(255,r), Math.min(255,grn), Math.min(255,b)));
            g.drawLine(x, 0, x, H);
        }

        // ── 4. Door panels — LEFT door ───────────────────────────────────────
        int lpx = 40;
        g.setColor(new Color(0x10, 0x06, 0x03, 180));
        g.fillRoundRect(lpx, 28, crackX - lpx - 18, 178, 8, 8);
        g.setColor(new Color(0x35, 0x18, 0x0c, 80));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(lpx, 28, crackX - lpx - 18, 178, 8, 8);

        g.setColor(new Color(0x10, 0x06, 0x03, 180));
        g.fillRoundRect(lpx, 224, crackX - lpx - 18, 200, 8, 8);
        g.setColor(new Color(0x35, 0x18, 0x0c, 80));
        g.drawRoundRect(lpx, 224, crackX - lpx - 18, 200, 8, 8);
        g.setStroke(new BasicStroke(1f));

        // ── 5. Door panels — RIGHT door ──────────────────────────────────────
        int rpx = crackX + crackW + 18;
        int rpw = W - rpx - 40;
        g.setColor(new Color(0x10, 0x06, 0x03, 180));
        g.fillRoundRect(rpx, 28, rpw, 178, 8, 8);
        g.setColor(new Color(0x35, 0x18, 0x0c, 80));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(rpx, 28, rpw, 178, 8, 8);

        g.setColor(new Color(0x10, 0x06, 0x03, 180));
        g.fillRoundRect(rpx, 224, rpw, 200, 8, 8);
        g.setColor(new Color(0x35, 0x18, 0x0c, 80));
        g.drawRoundRect(rpx, 224, rpw, 200, 8, 8);
        g.setStroke(new BasicStroke(1f));

        // ── 6. Center seam — dark frame strip between the two doors ──────────
        g.setColor(new Color(0x06, 0x03, 0x02));
        g.fillRect(crackX - 8, 0, 8, H);  // left door inner edge
        g.fillRect(crackX + crackW, 0, 8, H); // right door inner edge

        // ── 7. Mom's room visible through center crack ────────────────────────
        if (momsRoom != null) {
            // Draw a center strip of the mom's room image scaled to full height
            // Source: center portion of momsRoom
            int srcX = momsRoom.getWidth()  / 2 - crackW / 2;
            int srcY = 0;
            g.drawImage(momsRoom, crackX, 0, crackX + crackW, H,
                        srcX, srcY, srcX + crackW, momsRoom.getHeight(), null);
            // Bright edge lines where door meets room light
            g.setColor(new Color(0xff, 0xf8, 0xf0, 220));
            g.fillRect(crackX, 0, 2, H);
            g.fillRect(crackX + crackW - 2, 0, 2, H);
        } else {
            // Fallback warm light
            g.setColor(new Color(0xff, 0xf8, 0xf0));
            g.fillRect(crackX, 0, crackW, H);
        }
        // Soft glow bleeding onto LEFT door edge
        for (int i = 1; i <= 40; i++) {
            int alpha = (int)(180 * Math.exp(-i / 10.0));
            g.setColor(new Color(0xff, 0xe8, 0xb0, Math.min(255, alpha)));
            g.drawLine(crackX - i, 0, crackX - i, H);
        }
        // Soft glow bleeding onto RIGHT door edge
        for (int i = 1; i <= 40; i++) {
            int alpha = (int)(180 * Math.exp(-i / 10.0));
            g.setColor(new Color(0xff, 0xe8, 0xb0, Math.min(255, alpha)));
            g.drawLine(crackX + crackW + i, 0, crackX + crackW + i, H);
        }
        // Light spill on floor from opening
        for (int i = 0; i <= 60; i++) {
            int alpha = (int)(150 * Math.exp(-i / 20.0));
            g.setColor(new Color(0xff, 0xd8, 0x80, Math.min(255, alpha)));
            g.drawLine(crackX - i, H - 70 + i/2, crackX - i, H);
            g.drawLine(crackX + crackW + i, H - 70 + i/2, crackX + crackW + i, H);
        }

        // ── 8. Door knobs ────────────────────────────────────────────────────
        // Left door knob — right side, mid-height
        int lkx = crackX - 28, lky = 215;
        for (int r = 14; r >= 0; r--) {
            float frac = 1f - r / 14f;
            int alpha = (int)(frac * frac * 70);
            g.setColor(new Color(255, 240, 200, alpha));
            g.fillOval(lkx - r, lky - r, r * 2, r * 2);
        }
        g.setColor(new Color(0xd8, 0xb8, 0x60));
        g.fillOval(lkx - 5, lky - 5, 11, 11);
        g.setColor(new Color(0xff, 0xee, 0xaa));
        g.fillOval(lkx - 2, lky - 3, 5, 5);
        g.setColor(new Color(0xff, 0xff, 0xff));
        g.fillOval(lkx, lky - 2, 2, 2);

        // Right door knob — left side, mid-height
        int rkx = crackX + crackW + 28, rky = 215;
        for (int r = 14; r >= 0; r--) {
            float frac = 1f - r / 14f;
            int alpha = (int)(frac * frac * 70);
            g.setColor(new Color(255, 240, 200, alpha));
            g.fillOval(rkx - r, rky - r, r * 2, r * 2);
        }
        g.setColor(new Color(0xd8, 0xb8, 0x60));
        g.fillOval(rkx - 5, rky - 5, 11, 11);
        g.setColor(new Color(0xff, 0xee, 0xaa));
        g.fillOval(rkx - 2, rky - 3, 5, 5);
        g.setColor(new Color(0xff, 0xff, 0xff));
        g.fillOval(rkx, rky - 2, 2, 2);

        // ── 9. Far left edge (door meets frame/wall) ─────────────────────────
        g.setColor(new Color(0x06, 0x02, 0x01));
        g.fillRect(0, 0, 18, H);
        for (int i = 0; i < 18; i++) {
            g.setColor(new Color(0x06, 0x02, 0x01, 255 - i * 12));
            g.drawLine(18 + i, 0, 18 + i, H);
        }
        // Far right edge
        g.setColor(new Color(0x06, 0x02, 0x01));
        g.fillRect(W - 18, 0, 18, H);
        for (int i = 0; i < 18; i++) {
            g.setColor(new Color(0x06, 0x02, 0x01, 255 - i * 12));
            g.drawLine(W - 18 - i, 0, W - 18 - i, H);
        }

        // ── 10. Vignette overlay ──────────────────────────────────────────────
        // Top
        for (int y = 0; y < 80; y++) {
            int alpha = (int)(150 * (1.0 - y / 80.0));
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(0, y, W, y);
        }
        // Bottom
        for (int y = H; y > H - 80; y--) {
            int alpha = (int)(140 * (1.0 - (H - y) / 80.0));
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(0, y, W, y);
        }
        // Left corner
        for (int x = 0; x < 180; x++) {
            int alpha = (int)(130 * (1.0 - x / 180.0));
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(x, 0, x, H);
        }
        // Right corner
        for (int x = W; x > W - 180; x--) {
            int alpha = (int)(130 * (1.0 - (W - x) / 180.0));
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(x, 0, x, H);
        }

        // ── 11. Final depth pass ──────────────────────────────────────────────
        g.setColor(new Color(0, 0, 0, 35));
        g.fillRect(0, 0, crackX - 30, H);
        g.fillRect(crackX + crackW + 30, 0, W - crackX - crackW - 30, H);

        g.dispose();
        String out = args.length > 0 ? args[0] : "assets/preview/closet_my_design.png";
        ImageIO.write(img, "PNG", new File(out));
        System.out.println("Saved: " + out);
    }
}
