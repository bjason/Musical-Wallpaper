import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

@SuppressWarnings("unused")
public class AlbumCover {
	private BufferedImage image;
	private String name;
	private String artistName;
	final static int IMAGE_X = 300;
	final static int IMAGE_Y = 300;
	final static int DETAIL_X = 1000;
	final static String ARTIST_SEPARATOR = " byArtist ";

	public AlbumCover(BufferedImage image, String name) {
		this.image = image;
		this.name = name;
	}

	public AlbumCover(String name) throws IOException {
		String dir;
		this.name = name;

		dir = ImageCollageCreator.sourceDir + File.separator + name;
		this.image = getImage(dir);
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

	public BufferedImage createDetailSection() {
		BufferedImage section = new BufferedImage(DETAIL_X, IMAGE_Y, BufferedImage.TYPE_INT_RGB);
		Graphics2D background = section.createGraphics();
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
		String[] str = getRankandTrackName();
		background.drawString(str[0], 10, 100);

		background.setColor(Color.BLACK);
		background.setFont(new Font("Constantia", Font.BOLD, 70));
		background.drawString(str[1], 10, 170);

		// artist name
		background.setColor(Color.BLACK);
		background.setFont(new Font("Constantia", Font.ITALIC, 40));
		background.drawString(getArtistName(), 30, 220);

		background.dispose();
		return section;
	}

	public String[] getRankandTrackName() {
		String str = name.split(ARTIST_SEPARATOR)[0];
		String rankAndTrackName[] = new String[2];

		rankAndTrackName[0] = str.substring(0, str.indexOf('.')); // rank

		int i = str.indexOf('.');
		if (i > 0) {
			rankAndTrackName[1] = str.substring(i + 1); // track name
		}
		return rankAndTrackName;
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
		return new int[] { red, green, blue };

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
	public BufferedImage scaleImage(int WIDTH, int HEIGHT) {
		BufferedImage bi = null;
		try {
			bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = (Graphics2D) bi.createGraphics();
			g2d.addRenderingHints(
					new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
			g2d.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return bi;
	}
}
