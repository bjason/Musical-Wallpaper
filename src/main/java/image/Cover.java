package image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import util.ImageCollageCreator;

@SuppressWarnings("unused")
public class Cover {
    private BufferedImage image;
    private String name;
    private String artistName;
    public int IMAGE_X;
    public int IMAGE_Y;
    public final static int DETAIL_X = 2000;
    public final static String ARTIST_SEPARATOR = " - ";

    public Cover(BufferedImage image, String name) {
        this.image = image;
        this.name = name;

        setSize();
    }

    public Cover(BufferedImage image) {
        this.image = image;

        setSize();
    }

    public Cover(String name) throws IOException {
        String dir;
        this.name = name;

        dir = ImageCollageCreator.sourceDir + File.separator + name + ".jpg";
        this.image = getImage(dir);

        setSize();
    }

    private void setSize() {
        IMAGE_X = image.getWidth();
        IMAGE_Y = image.getHeight();
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    // split name
    public String getArtistName() {
        String str = name.split(ARTIST_SEPARATOR)[1];
        artistName = str.substring(0, str.lastIndexOf('.'));
        return artistName;
    }

    public BufferedImage getImage(String dir) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new File(dir));
        return bufferedImage;
    }

    final private static int DRAWSTRING_TITLE = 0;
    final private static int DRAWSTRING_ARTIST = 1;
    final private static int x_title = 10, x_artist = 30, y_title = 150, y_artist = 240;
    final private static int TITLE_SIZE = 70, ARTIST_SIZE = 40;

    private Graphics2D background;

    public BufferedImage createDetailSection() {
        BufferedImage section = new BufferedImage(DETAIL_X, IMAGE_Y, BufferedImage.TYPE_INT_RGB);
        background = section.createGraphics();
        String[] str = getRankAndTrackName();
        //
        // background.setBackground(Color.WHITE);

        // //TODO background.setColor(getMainColor());
        background.setColor(Color.WHITE);
        background.fillRect(0, 0, DETAIL_X, IMAGE_Y);
        background.dispose();

        // draw text on the image
        // draw rank and name
        background = section.createGraphics();
        background.setColor(Color.GRAY);
        background.setFont(new Font("Constantia", Font.BOLD, 150));
        background.drawString(str[0], 10, 100);

        DrawString(str[1], DRAWSTRING_TITLE);

        DrawString(getArtistName(), DRAWSTRING_ARTIST);

        // artist name
        background.setColor(Color.BLACK);
        background.setFont(new Font("Constantia", Font.ITALIC, 40));
        background.drawString(getArtistName(), 30, 220);

        background.dispose();
        return section;
    }

    public BufferedImage createDetailSection(HashMap<String, String> trackInfo) {
        BufferedImage section = new BufferedImage(DETAIL_X, IMAGE_Y, BufferedImage.TYPE_INT_RGB);
        background = section.createGraphics();
        //
        // background.setBackground(Color.WHITE);

        // //TODO background.setColor(getMainColor());
        background.setColor(Color.WHITE);
        background.fillRect(0, 0, DETAIL_X, IMAGE_Y);
        background.dispose();

        // draw text on the image
        // draw rank and name
        background = section.createGraphics();
        background.setColor(Color.GRAY);
        background.setFont(new Font("Constantia", Font.BOLD, 150));
        background.drawString(trackInfo.get("order"), 10, 100);

        DrawString(trackInfo.get("Title"), DRAWSTRING_TITLE);

        DrawString(trackInfo.get("Artist"), DRAWSTRING_ARTIST);

        background.dispose();
        return section;
    }

    private void DrawString(String str, int id) {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        final Font en_font;
        final Font cn_font;
        int size;

        final int x, y;
        if (id == DRAWSTRING_TITLE) // 0 for title
        {
            x = x_title;
            y = y_title;
            size = TITLE_SIZE;
            en_font = new Font("Constantia", Font.BOLD, size);
            cn_font = new Font("FZYaSongS-M-GB", Font.BOLD, size);
        } else { // 1 for artist
            x = x_artist;
            y = y_artist;
            size = ARTIST_SIZE;
            en_font = new Font("Constantia", Font.ITALIC, size);
            cn_font = new Font("FZYaSongS-M-GB", Font.ITALIC, size);
        }


        background.setColor(Color.BLACK);
        int cn_pos = findChineseChar(str, 0);
        int en_pos = findEnglishChar(str, 0);
        int width = 0;

        if (cn_pos == -1) {
            background.setFont(en_font);
            background.drawString(str, x, y);

            return;
        }

        while (cn_pos != -1 || en_pos != -1) {
            if (en_pos == -1) en_pos = str.length();
            if (cn_pos == -1) cn_pos = str.length();
            // there are chinese char
            // draw english char first
            if (en_pos < cn_pos) {
                if (en_pos == -1) en_pos = 0;
                String en_str = str.substring(en_pos, cn_pos);
                background.setFont(en_font);
                background.drawString(en_str, x + width, y);
                width += (int) en_font.getStringBounds(en_str, frc).getWidth();

                cn_pos = findChineseChar(str, en_pos + 1);
                en_pos = findEnglishChar(str, cn_pos);
            } else {
                // draw chinese char
                if (cn_pos == -1) cn_pos = 0;
                String cn_str = str.substring(cn_pos, en_pos);
                background.setFont(cn_font);
                background.drawString(cn_str, x + width, y);
                width += (int) cn_font.getStringBounds(cn_str, frc).getWidth();

                en_pos = findEnglishChar(str, cn_pos + 1);
                cn_pos = findChineseChar(str, en_pos);
            }
        }
    }

    public String[] getRankAndTrackName() {
        String str = name.split(ARTIST_SEPARATOR)[0];
        String rankAndTrackName[] = new String[2];

        rankAndTrackName[0] = str.substring(0, str.indexOf('.')); // rank

        int i = str.indexOf('.');
        if (i > 0) {
            rankAndTrackName[1] = str.substring(i + 1); // track name
        }
        return rankAndTrackName;
    }

    // detect a string contains Chinese characters or not
    // https://stackoverflow.com/questions/26357938/detect-chinese-character-in-java
    public static boolean containsHanScript(String s) {
        for (int i = 0; i < s.length(); ) {
            int codepoint = s.codePointAt(i);
            i += Character.charCount(codepoint);
            if (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    //    https://stackoverflow.com/questions/37430869/how-to-find-the-first-chinese-character-in-a-java-string
    public static int findChineseChar(String s, int startPos) {
        if (startPos < 0 || startPos >= s.length()) return -1;
        for (int i = startPos; i < s.length(); ) {
            int index = i;
            int codepoint = s.codePointAt(i);
            i += Character.charCount(codepoint);
            if (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
                return index;
            }
        }
        return -1;
    }

    public static int findEnglishChar(String s, int startPos) {
        if (startPos < 0 || startPos >= s.length()) return -1;
        for (int i = startPos; i < s.length(); ) {
            int index = i;
            int codepoint = s.codePointAt(i);
            i += Character.charCount(codepoint);
            if (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.LATIN) {
                return index;
            }
        }
        return -1;
    }

    static String getNonEnglishCharacter(String s) {
        return s.replaceAll("\\P{IsHan}+", "");
    }

    static String getEnglishCharacter(String s) {
        return s.replaceAll("^\\P{IsHan}+", "");
    }

    // private Color getMainColor() {
    // Map m = new HashMap();
    // for (int i = 0; i < IMAGE_X; i++) {
    // for (int j = 0; j < IMAGE_Y; j++) {
    // int rgb = image.getRGB(i, j);
    // int[] rgbArr = getRGBArr(rgb);
    // // Filter out grays....
    // if (!isGray(rgbArr)) {
    // Integer counter = (Integer) m.get(rgb);
    // if (counter == null)
    // counter = 0;
    // counter++;
    // m.put(rgb, counter);
    // }
    // }
    // }
    // String colourHex = getMostCommonColour(m);
    // System.out.println(colourHex);
    // return null;
    // }

    // public static String getMostCommonColour(Map map) {
    // LinkedList list = new LinkedList(map.entrySet());
    // Collections.sort(list, new Comparator() {
    // public int compare(Object o1, Object o2) {
    // return ((Comparable) ((Map.Entry)
    // (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
    // }
    // });
    // Map.Entry me = (Map.Entry) list.get(list.size() - 1);
    // int[] rgb = getRGBArr((Integer) me.getKey());
    // return Integer.toHexString(rgb[0]) + " " + Integer.toHexString(rgb[1]) +
    // " " + Integer.toHexString(rgb[2]);
    // }

    public static int[] getRGBArr(int pixel) {
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return new int[]{red, green, blue};

    }

    public static boolean isGray(int[] rgbArr) {
        int rgDiff = rgbArr[0] - rgbArr[1];
        int rbDiff = rgbArr[0] - rgbArr[2];
        // Filter out black, white and grays...... (tolerance within 10 pixels)
        int tolerance = 10;
        if (rgDiff > tolerance || rgDiff < -tolerance)
            if (rbDiff > tolerance || rbDiff < -tolerance) {
                return false;
            }
        return true;
    }

    // use it as
    // BufferedImage img=new AlbumCover().scaleImage(50,50,"c:/test.jpg");
    public Cover resizeTo(int WIDTH, int HEIGHT) {
        BufferedImage result = null;

        try {
            result = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = (Graphics2D) result.createGraphics();
            g2d.addRenderingHints(
                    new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
            g2d.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return new Cover(result, name);
    }
}
