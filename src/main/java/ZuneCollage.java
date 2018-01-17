import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

public class ZuneCollage extends Collage {
	public static int INTERVAL = 2;

	public ZuneCollage(int x, int y, ImageCollageCreator creator) {
		super(x, y, creator);
	}

	public ZuneCollage(ImageCollageCreator creator) {
		super(creator);
	}

	private BufferedImage addGradient(BufferedImage image) {
		BufferedImage combinedImage = new BufferedImage(COLLAGE_X, COLLAGE_Y, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = combinedImage.createGraphics();

		// generate a gradient and set the transparency
		int orientation = (int) (Math.random() * 30 + 30);
		BufferedImage gradient = createGradientMask();

		// draw this gradient over the origin image
		g.drawImage(image, 0, 0, null);

		float opacity = 0.5f;
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
		g.drawImage(gradient, 0, 0, null);

		g.dispose();

		return combinedImage;
	}

	public BufferedImage createGradientMask() {
		// algorithm derived from Romain Guy's blog
		int width = COLLAGE_X;
		int height = COLLAGE_Y;
		BufferedImage gradient = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = gradient.createGraphics();

		int transparency = (int) (0.8 * 255);

		GradientPaint paint = new GradientPaint(0.0f, 0.0f, new Color(66, 27, 82, transparency),
				// orientation == SwingConstants.HORIZONTAL ? width : 0.0f,
				// orientation == SwingConstants.VERTICAL ? height : 0.0f,
				width, height, new Color(1, 66, 34, transparency));

		g.setPaint(paint);
		g.fill(new Rectangle2D.Double(0, 0, width, height));

		g.dispose();
		gradient.flush();

		return gradient;
	}

	protected void drawImages(String[] inputImages) throws IOException {
		collage = new BufferedImage(COLLAGE_X, COLLAGE_Y, BufferedImage.TYPE_INT_RGB);
		Graphics g = collage.getGraphics();

		// the biggest size of the album cover is 300, and the scale is 4:3:1
		// generate a layout array that indicates the layout of the wallpaper
		// one grid equals the minimum size of the album cover which is 300/4,
		// and the medium size pictures take up 9 grids, the biggest take up 16
		int colomns = (int) Math.floor(COLLAGE_X / IMAGE_X) + 1;
		int rows = (int) Math.floor(COLLAGE_Y / IMAGE_X) + 1;
		// int total = rows * colomns;

		int[][] layout = new int[rows][colomns];
		// generate a table to pre distribute the layout of the wallpapaer
		boolean[] usedAsLarge = new boolean[inputImages.length];

		// initialize the array
		for (int row[] : layout)
			Arrays.fill(row, -1); // -1 for available
		Arrays.fill(usedAsLarge, false);

		// generate random layout
		for (int x = 0; x < rows; x++) {
			for (int y = 0; y < colomns; y++) {
				int randomNum = (int) (Math.random() * 20);
				int order = (int) (Math.random() * inputImages.length);

				if (randomNum > 18 && notBeenAssigned(layout, x, y, rows, colomns, 4)) {

					int tmporder = updateOrder(inputImages, usedAsLarge, order);
					if (tmporder == -1) {
						Arrays.fill(usedAsLarge, false);
					} else {
						order = tmporder;
					}
					usedAsLarge[order] = true;
					for (int k = 0; k < 4; k++) {
						for (int m = 0; m < 4; m++) {
							layout[Math.min(x + k, rows - 1)][Math.min(y + m, colomns - 1)] = order;
						}
					}

					scaleAndDraw(inputImages[order], g, x, y, 4);

				} else if (randomNum > 16 && notBeenAssigned(layout, x, y, rows, colomns, 3)) {
					// to a medium size cover
					// identify the layout
					int tmporder = updateOrder(inputImages, usedAsLarge, order);
					if (tmporder == -1) {
						Arrays.fill(usedAsLarge, false);
					} else {
						order = tmporder;
					}
					usedAsLarge[order] = true;
					for (int k = 0; k < 3; k++) {
						for (int m = 0; m < 3; m++) {
							layout[Math.min(x + k, rows - 1)][Math.min(y + m, colomns - 1)] = order;
						}
					}

					scaleAndDraw(inputImages[order], g, x, y, 3);

				} else {// distribute the grid to a minimum size cover
					if (layout[x][y] == -1) {

						while (hasSameCover(colomns, rows, layout, x, y, order)) {
							order = (order + 1) % inputImages.length;
						}
						layout[x][y] = order;

						scaleAndDraw(inputImages[order], g, x, y, 1);
					}
				}

				if (y >= colomns) {
					y = 0;
					x += 1;
				}

				setProgress((int) (100 * (y + x * colomns) / (rows * colomns)));
			}
		}

	}

	// pick one picture to fill the large area, if there is zero, return -1
	private int updateOrder(String[] inputImages, boolean[] usedAsLarge, int order) {
		int tmporder = order;
		for (int i = 0; i < usedAsLarge.length; i++) {
			if (usedAsLarge[tmporder]) {
				tmporder = (tmporder + 1) % inputImages.length;
			}
		}

		if (order == tmporder) {
			tmporder = -1;
		}
		return tmporder;
	}

	private void scaleAndDraw(String image, Graphics g, int y, int x, int scale) throws IOException {
		int size = scale * IMAGE_X + (scale - 1) * 2 * INTERVAL;
		int pixel_x = x * (IMAGE_X + 2 * INTERVAL) - 20;
		int pixel_y = y * (IMAGE_X + 2 * INTERVAL) - 20;
		AlbumCover cover = new AlbumCover(image);
		g.drawImage(cover.scaleImage(size, size), pixel_x, pixel_y, null);
	}

	private boolean notBeenAssigned(int[][] layout, int x, int y, int rows, int colomns, int finite) {
		boolean usable = true;
		for (int k = 0; k < finite; k++) {
			for (int m = 0; m < finite; m++) {
				if (layout[Math.min(x + k, rows - 1)][Math.min(y + m, colomns - 1)] != -1) {
					usable = false;
					break;
				}
			}
		}
		return usable;
	}

	private boolean hasSameCover(int colomns, int rows, int[][] layout, int x, int y, int order) {
		boolean result = false;
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				int scanx = Math.max(Math.min(x + i, rows - 1), 0);
				int scany = Math.max(Math.min(y + j, colomns - 1), 0);

				if (layout[scanx][scany] == order) {
					result = true;
					break;
				}
			}
		}
		return result;
	}
}
