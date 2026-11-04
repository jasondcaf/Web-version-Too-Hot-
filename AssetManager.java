package game;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches room background images from the assets/rooms folder.
 * Falls back gracefully if an asset is missing.
 */
public class AssetManager {

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    // Cache a scaled-down version ready for drawing at target size
    private static final Map<String, BufferedImage> SCALED = new HashMap<>();

    public static BufferedImage getRoom(String name) {
        if (CACHE.containsKey(name)) return CACHE.get(name);
        String[] exts  = {".jpg", ".png", ".jpeg"};
        String[] bases = {"assets/rooms/", "../assets/rooms/"};
        for (String base : bases) {
            for (String ext : exts) {
                try {
                    File f = new File(base + name + ext);
                    if (f.exists()) {
                        BufferedImage img = ImageIO.read(f);
                        CACHE.put(name, img);
                        return img;
                    }
                } catch (Exception ignored) {}
            }
        }
        CACHE.put(name, null);   // remember miss so we don't retry every frame
        return null;
    }

    /**
     * Returns a version of the room background already scaled to (w × h),
     * cached after first call.  Drawing a pre-scaled image is faster than
     * calling drawImage with scaling every frame.
     */
    public static BufferedImage getRoomScaled(String name, int w, int h) {
        String key = name + "@" + w + "x" + h;
        if (SCALED.containsKey(key)) return SCALED.get(key);

        BufferedImage src = getRoom(name);
        if (src == null) { SCALED.put(key, null); return null; }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        SCALED.put(key, out);
        return out;
    }
}
