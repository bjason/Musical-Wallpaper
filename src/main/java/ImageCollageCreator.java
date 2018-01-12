import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
//TODO there are still some blank grid in the picture
//TODO add gradient effect

/// Picks x random images from directory y and stitches them into a single image, saved to file z until all images are used
public class ImageCollageCreator extends SwingWorker<Void, Void> {
	private final int COLLAGE_X = 1920; // TODO calculate these properly, based
										// on screen res
	private final int COLLAGE_Y = 1080;
	private int ORDER_X = 1300;
	private int ORDER_Y = 0;
	private int numTracks = 0;

	private final String ALLOWED_EXTENSION = ".jpg";
	static final String sourceDir = System.getProperty("user.home") + File.separator + "Pictures" + File.separator
			+ "Musical Wallpaper" + File.separator + "Album art";
	static final String outputDir = System.getProperty("user.home") + File.separator + "Pictures" + File.separator
			+ "Musical Wallpaper" + File.separator + "Collages";

	private int IMAGE_X;
	private int IMAGE_Y;
	private int INTERVAL = 2;
	private int BASE_SIZE;
	private static boolean isOrderMode = false;
	private static boolean isZuneMode = false;

	protected String errorCode = null; // to avoid SwingWorkers missing
										// exception handling

	@Override
	protected Void doInBackground() throws Exception {
		try {
			createAndSaveImages();
		} catch (Exception e) {
			errorCode = "Could not generate album art collages. Error code: " + e.getMessage();
			e.printStackTrace();
		}
		return null;
	}

	private void createAndSaveImages() throws IOException {
		int imageSizeCode = Integer.parseInt(PropertiesManager.getProperty("imageSizeCode"));
		int size = 300;

		switch (imageSizeCode) {
		case 0:
		case 1:
		case 2:
			size = AlbumArtGrabber.SPOTIFY_IMAGE_SIZES[imageSizeCode];
			break;
		case 3:
			isOrderMode = true;
			break;
		case 4:
			isZuneMode = true;
			BASE_SIZE = (300 - 6 * INTERVAL) / 4;
			break;
		}
		IMAGE_X = size;
		IMAGE_Y = size;

		// first delete any previously saved collages
		delPrevCollgs();

		ArrayList<String> allImages = getImageFilenames(sourceDir);

		int count = 0;
		numTracks = allImages.size();

		if (isOrderMode == true) {
			ORDER_Y = numTracks * 300;

			// make the playlist a countdown
			Collections.reverse(allImages);

			String[] thisCollageImages = getCollageImagesNames(allImages, numTracks);

			File outputFile = getOutputFilename(outputDir);
			BufferedImage image = drawRank(thisCollageImages);
			createAndSaveCollage(image, outputFile);

		} else if (isZuneMode == true) {
			Collections.shuffle(allImages);
			String[] thisCollageImages = getCollageImagesNames(allImages, numTracks);

			File outputFile = getOutputFilename(outputDir);
			BufferedImage image = drawZune(thisCollageImages);
			// BufferedImage imageWithGradient = addGradient(image);
			createAndSaveCollage(image, outputFile);

		} else {
			ArrayList<String> unusedImagesNames = new ArrayList<>(allImages);

			Collections.shuffle(allImages); // randomize order of images in
											// collages
			int imagesPerCollage = getImagesPerCollage();

			// roughly calculate progress for the loading bar
			int approxRequiredIterations = unusedImagesNames.size() / imagesPerCollage;

			while (unusedImagesNames.size() >= imagesPerCollage) {
				count++;
				// grab the next imagesPerCollage images from the shuffled
				// unused images
				String[] thisCollageImages = new String[imagesPerCollage];
				for (int i = 0; i < imagesPerCollage; i++) {
					thisCollageImages[i] = unusedImagesNames.get(0);
					unusedImagesNames.remove(0); // as we use each image,
													// remove
													// it
					// from the unused list
				}

				// generate a unique filename for each, just "collage x.jpg"
				File outputFile = getOutputFilename(outputDir, count);
				BufferedImage image = drawImages(thisCollageImages);
				createAndSaveCollage(image, outputFile);

				setProgress((int) (((double) count / approxRequiredIterations) * 100));
			}

			// if are some leftover images (ie unusedImages.size() %
			// imagesPerCollage != 0)
			// then use of the already used images to fill this collage
			if (unusedImagesNames.size() > 0) {
				count++;
				// start by using all remaining unused images
				ArrayList<String> thisCollageImages = new ArrayList<>(unusedImagesNames);
				// then top up with any other images
				while (thisCollageImages.size() < imagesPerCollage) {
					int imagesToAdd = imagesPerCollage - thisCollageImages.size();
					if (imagesToAdd > allImages.size()) {
						imagesToAdd = allImages.size();
					}
					thisCollageImages.addAll(allImages.subList(0, imagesToAdd));
					Collections.shuffle(allImages);
				}

				File outputFile = getOutputFilename(outputDir, count);
				BufferedImage image = drawImages(thisCollageImages.toArray(new String[0]));
				createAndSaveCollage(image, outputFile);
			}
		}
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

	private String[] getCollageImagesNames(ArrayList<String> allImages, int numTracks) {
		String[] thisCollageImages = new String[numTracks];
		for (int count = 0; count < numTracks; count++) {
			thisCollageImages[count] = allImages.get(0);
			allImages.remove(0);

			setProgress(50 * (count / numTracks));
		}
		return thisCollageImages;
	}

	private BufferedImage drawZune(String[] thisCollageImages) throws IOException {
		BufferedImage result = new BufferedImage(COLLAGE_X, COLLAGE_Y, BufferedImage.TYPE_INT_RGB);
		Graphics g = result.getGraphics();

		// the biggest size of the album cover is 300, and the scale is 4:3:1
		// generate a layout array that indicates the layout of the wallpaper
		// one grid equals the minimum size of the album cover which is 300/4,
		// and the medium size pictures take up 9 grids, the biggest take up 16
		int colomns = (int) Math.floor(COLLAGE_X / BASE_SIZE) + 1;
		int rows = (int) Math.floor(COLLAGE_Y / BASE_SIZE) + 1;
		// int total = rows * colomns;

		int[][] layout = new int[rows][colomns];
		// generate a table to pre distribute the layout of the wallpapaer
		boolean[] usedAsLarge = new boolean[thisCollageImages.length];

		// initialize the array
		for (int row[] : layout)
			Arrays.fill(row, -1); // -1 for available
		for (boolean b : usedAsLarge) {
			b = false;
		}

		// generate random layout
		for (int x = 0; x < rows; x++) {
			for (int y = 0; y < colomns; y++) {
				int randomNum = (int) (Math.random() * 20);
				int order = (int) (Math.random() * thisCollageImages.length);

				if (randomNum > 18 && notBeenAssigned(layout, x, y, rows, colomns, 4)) {
					while (usedAsLarge[order]) {
						order = (order + 1) % thisCollageImages.length;
					}
					usedAsLarge[order] = true;
					for (int k = 0; k < 4; k++) {
						for (int m = 0; m < 4; m++) {
							layout[Math.min(x + k, rows - 1)][Math.min(y + m, colomns - 1)] = order;
						}
					}

					scaleAndDraw(thisCollageImages[order], g, x, y, 4);

				} else if (randomNum > 16 && notBeenAssigned(layout, x, y, rows, colomns, 3)) {
					// to a medium size cover
					// identify the layout
					while (usedAsLarge[order]) {
						order = (order + 1) % thisCollageImages.length;
					}
					usedAsLarge[order] = true;
					for (int k = 0; k < 3; k++) {
						for (int m = 0; m < 3; m++) {
							layout[Math.min(x + k, rows - 1)][Math.min(y + m, colomns - 1)] = order;
						}
					}

					scaleAndDraw(thisCollageImages[order], g, x, y, 3);

				} else {// distribute the grid to a minimum size cover
					if (layout[x][y] == -1) {
						
						while (hasSameCover(colomns, rows, layout, x, y, order)) {
							order = (order + 1) % thisCollageImages.length;
						}
						layout[x][y] = order;

						scaleAndDraw(thisCollageImages[order], g, x, y, 1);
					}
				}

				if (y >= colomns) {
					y = 0;
					x += 1;
				}

				setProgress((int) (100 * (y + x * colomns) / (rows * colomns)));
			}
		}

		return result;

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

	private void scaleAndDraw(String image, Graphics g, int y, int x, int scale) throws IOException {
		int size = scale * BASE_SIZE + (scale - 1) * 2 * INTERVAL;
		int pixel_x = x * (BASE_SIZE + 2 * INTERVAL) - 20;
		int pixel_y = y * (BASE_SIZE + 2 * INTERVAL) - 20;
		AlbumCover cover = new AlbumCover(image);
		g.drawImage(cover.scaleImage(size, size), pixel_x, pixel_y, null);
	}

	private BufferedImage drawRank(String[] inputImages) throws IOException {
		BufferedImage result = new BufferedImage(ORDER_X, ORDER_Y, BufferedImage.TYPE_INT_RGB);
		Graphics g = result.getGraphics();

		int x = 0;
		int y = 0;
		int i = 0;
		for (String image : inputImages) {
			// BufferedImage bufferedImage = ImageIO.read(new File(image));
			AlbumCover cover = new AlbumCover(image);
			BufferedImage bufferedImage = cover.getImage();

			g.drawImage(bufferedImage, x, y, null);
			x += IMAGE_X;

			// draw detail section bar
			g.drawImage(cover.createDetailSection(), x, y, null);

			x = 0;
			y += IMAGE_Y;
			setProgress(50 + i * (50 / numTracks));
		}

		return result;
	}

	// delete all the images in the 'collages 'directory
	private void delPrevCollgs() {
		new File(outputDir).mkdirs(); // create the folders if they don't exist
		for (File file : new File(outputDir).listFiles()) {
			file.delete();
		}
	}

	private int getImagesPerCollage() {
		// calculate how many images can be fit in horizontally and vertically
		// round up so that there is no empty space at the edge of the wallpaper
		int x = (int) Math.ceil((double) COLLAGE_X / IMAGE_X);
		int y = (int) Math.ceil((double) COLLAGE_Y / IMAGE_Y);
		return x * y;
	}

	private File getOutputFilename(String outputDir) {
		return new File(outputDir + File.separator + "Albums in " + AlbumArtGrabber.playlistName + ".jpg");
	}

	private File getOutputFilename(String outputDir, int count) {
		return new File(outputDir + File.separator + "collage " + count + ".jpg");
	}

	private ArrayList<String> getImageFilenames(String inputDir) {
		File[] files = new File(inputDir).listFiles();
		if (files == null) {
			errorCode = "No directary built.";
			return null;
		} else {
			ArrayList<String> images = new ArrayList<>();
			for (File image : files) {
				if (image.isFile() && image.getAbsolutePath().endsWith(ALLOWED_EXTENSION)) {
					images.add(image.getName());
				}
			}

			return images;
		}
	}

	private void createAndSaveCollage(BufferedImage image, File outputFile) throws IOException {
		outputFile.mkdirs(); // if the output directory doesn't exist, create it
		// write the collage to the file
		ImageIO.write(image, "jpg", outputFile);
	}

	private BufferedImage drawImages(String[] inputImages) throws IOException {
		BufferedImage result = new BufferedImage(COLLAGE_X, COLLAGE_Y, BufferedImage.TYPE_INT_RGB);
		Graphics g = result.getGraphics();

		int x = 0;
		int y = 0;
		for (String image : inputImages) {
			BufferedImage bufferedImage = ImageIO.read(new File(image));
			g.drawImage(bufferedImage, x, y, null);
			x += IMAGE_X;
			if (x >= result.getWidth()) {
				x = 0;
				y += IMAGE_Y;
			}
		}
		return result;
	}
}