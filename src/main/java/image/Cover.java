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
import java.util.*;

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
    final private static int DRAWSTRING_YEAR = 2;
    final private static int DRAWSTRING_LABEL = 3;
    final private static int[] drawing_x = new int[]{10, 20, 20, 20};
    final private static int[] drawing_y = new int[]{180, 270, 230, 295};
    final private static int[] SIZE = new int[]{70, 40, 40, 15};
    final public static String EN_FONT = "Constantia";
    final public static String NUM_FONT = "Constantia";
    final public static String CN_FONT = "FZYaSongS-M-GB";
    private Color mainColor;

    private Graphics2D background;

    public BufferedImage createDetailSection() {
        BufferedImage section = new BufferedImage(DETAIL_X, IMAGE_Y, BufferedImage.TYPE_INT_RGB);
        background = section.createGraphics();
        String[] str = getRankAndTrackName();
        //
        // background.setBackground(Color.WHITE);

        background.setColor(Color.WHITE);
        background.fillRect(0, 0, DETAIL_X, IMAGE_Y);
        background.dispose();

        // draw text on the image
        // draw rank and name
        background = section.createGraphics();
        background.setColor(Color.GRAY);
        background.setFont(new Font(EN_FONT, Font.BOLD, 180));
        background.drawString(str[0], 10, 140);

        DrawString(str[1], DRAWSTRING_TITLE);

        DrawString(getArtistName(), DRAWSTRING_ARTIST);

        // artist name
        background.setColor(Color.BLACK);
        background.setFont(new Font(EN_FONT, Font.ITALIC, 40));
        background.drawString(getArtistName(), 30, 220);

        background.dispose();
        return section;
    }

    public BufferedImage createDetailSection(HashMap<String, String> trackInfo) {
        BufferedImage section = new BufferedImage(DETAIL_X, IMAGE_Y, BufferedImage.TYPE_INT_RGB);
        background = section.createGraphics();
        //
        // background.setBackground(Color.WHITE);
        mainColor = getMainColor();

        background.setColor(Color.WHITE);
        background.fillRect(0, 0, DETAIL_X, IMAGE_Y);
        background.dispose();

        // draw text on the image
        // draw rank and name
        background = section.createGraphics();
        background.setColor(Color.lightGray);
        background.setFont(new Font(NUM_FONT, Font.BOLD, 180));
        background.drawString(trackInfo.get("order"), 10, 120);

        DrawString(trackInfo.get("Title"), DRAWSTRING_TITLE);

        DrawString(trackInfo.get("Artist"), DRAWSTRING_ARTIST);

        if (trackInfo.containsKey("ReleaseDate")) {
            DrawString(trackInfo.get("ReleaseDate"), DRAWSTRING_YEAR);
        }

//        if (trackInfo.containsKey("Label")) {
//            DrawString(trackInfo.get("Label"), DRAWSTRING_LABEL);
//        }

        background.dispose();
        return section;
    }

    private void DrawString(String str, int id) {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        final Font en_font;
        final Font cn_font;
        int size;

        final int x, y;
        x = drawing_x[id];
        y = drawing_y[id];
        size = SIZE[id];

//        if (str.contains("An Adventure in The Dark Clouds Market")) {
//            str = "大石碎胸口";
//        }

        switch (id) {
            case DRAWSTRING_TITLE:
                en_font = new Font(EN_FONT, Font.BOLD, size);
                cn_font = new Font(CN_FONT, Font.BOLD, size);
                background.setColor(getDarkerColor(mainColor));
                break;
            case DRAWSTRING_ARTIST:
                en_font = new Font(EN_FONT, Font.ITALIC, size);
                cn_font = new Font(CN_FONT, Font.ITALIC, size);
                background.setColor(Color.BLACK);
                break;
            case DRAWSTRING_LABEL:
                en_font = new Font(EN_FONT, Font.PLAIN, size);
                cn_font = new Font(CN_FONT, Font.PLAIN, size);
                background.setColor(Color.GRAY);
                str = "(c)" + str;
                break;
            default:
                en_font = new Font(EN_FONT, Font.ITALIC, size);
                cn_font = new Font(CN_FONT, Font.ITALIC, size);
                background.setColor(Color.GRAY);

                String[] strs = str.split("-");
                str = "(" + strs[0] + ")";
        }

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
                String en_str = str.substring(en_pos, cn_pos);
                background.setFont(en_font);
                background.drawString(en_str, x + width, y);
                width += (int) en_font.getStringBounds(en_str, frc).getWidth();

                cn_pos = findChineseChar(str, en_pos + 1);
                en_pos = findEnglishChar(str, cn_pos);
            } else {
                // draw chinese char
                String cn_str = str.substring(cn_pos, en_pos);
                background.setFont(cn_font);
                background.drawString(cn_str, x + width, y);
                width += (int) cn_font.getStringBounds(cn_str, frc).getWidth();

                en_pos = findEnglishChar(str, cn_pos + 1);
                cn_pos = findChineseChar(str, en_pos);
            }
        }
    }

    // https://stackoverflow.com/questions/3116260/given-a-background-color-how-to-get-a-foreground-color-that-makes-it-readable-o
    private float CalculateLuminance(Color c) {
        return (float) (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue());
    }

    private ArrayList<Integer> HexToRBG(String colorStr) {
        ArrayList<Integer> rbg = new ArrayList<Integer>();
        rbg.add(Integer.valueOf(colorStr.substring(1, 3), 16));
        rbg.add(Integer.valueOf(colorStr.substring(3, 5), 16));
        rbg.add(Integer.valueOf(colorStr.substring(5, 7), 16));
        return rbg;
    }

    public Color getDarkerColor(Color c) {
        float luminance = this.CalculateLuminance(c);
        while (luminance > 180) {
            c = c.darker();
            luminance = this.CalculateLuminance(c);
        }
        return c;
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

    private Color getMainColor() {
        Map m = new HashMap();
        for (int i = 0; i < IMAGE_X; i++) {
            for (int j = 0; j < IMAGE_Y; j++) {
                int rgb = image.getRGB(i, j);
                int[] rgbArr = getRGBArr(rgb);
                // Filter out grays....
                if (!isGray(rgbArr)) {
                    Integer counter = (Integer) m.get(rgb);
                    if (counter == null)
                        counter = 0;
                    counter++;
                    m.put(rgb, counter);
                }
            }
        }
        Color color = getMostCommonColour(m);
//        Color color = new Color();
        return color;
    }

    public static Color getMostCommonColour(Map map) {
        try {
            LinkedList list = new LinkedList(map.entrySet());
            Collections.sort(list, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Comparable) ((Map.Entry)
                            (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
                }
            });
            Map.Entry me = (Map.Entry) list.get(list.size() - 1);
            int[] rgb = getRGBArr((Integer) me.getKey());
            return new Color(rgb[0], rgb[1], rgb[2]);
//        return Integer.toHexString(rgb[0]) + " " + Integer.toHexString(rgb[1]) +
//                " " + Integer.toHexString(rgb[2]);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return Color.BLACK;
        }
    }

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
