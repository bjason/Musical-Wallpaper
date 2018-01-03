import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/// Picks x random images from directory y and stitches them into a single image, saved to file z until all images are used
public class ImageCollageCreator extends SwingWorker<Void, Void> {
	private final int COLLAGE_X = 1920; // TODO calculate these properly, based
										// on screen res
	private final int COLLAGE_Y = 1080;
	private int ORDER_X = 1300;
	private int ORDER_Y = 0;

	private final String ALLOWED_EXTENSION = ".jpg";
	static final String sourceDir = System.getProperty("user.home") + File.separator + "Pictures"
			+ File.separator + "Musical Wallpaper" + File.separator + "Album art";
	static final String outputDir = System.getProperty("user.home") + File.separator + "Pictures"
			+ File.separator + "Musical Wallpaper" + File.separator + "Collages";

	private int IMAGE_X;
	private int IMAGE_Y;
	private static boolean isOrderMode = false;

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
		if (imageSizeCode < 3) {
			size = AlbumArtGrabber.SPOTIFY_IMAGE_SIZES[imageSizeCode];
		}
		if (imageSizeCode == 3) {
			isOrderMode = true;
		}
		IMAGE_X = size;
		IMAGE_Y = size;

		// first delete any previously saved collages
		delPrevCollgs();

		ArrayList<String> allImages = getImageFilenames(sourceDir);

		int count = 0;

		if (isOrderMode == true) {
			int numTracks = AlbumArtGrabber.numTracks;
			String[] thisCollageImages = new String[numTracks];
			ORDER_Y = numTracks * 300;

			Collections.reverse(allImages);
			//make the playlist a countdown

			for (count = 0; count < numTracks; count++) {
				thisCollageImages[count] = allImages.get(0);
				allImages.remove(0);

				setProgress(50 + 50 * (count / numTracks));
			}

			File outputFile = getOutputFilename(outputDir);
			BufferedImage image = drawOrder(thisCollageImages);
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
					unusedImagesNames.remove(0); // as we use each image, remove
													// it
					// from the unused list
				}

				// generate a unique filename for each, just "collage x.jpg"
				File outputFile = getOutputFilename(outputDir, count);
				BufferedImage image = drawImages(thisCollageImages);
				createAndSaveCollage(image, outputFile);

				setProgress(50 + (int) (((double) count / approxRequiredIterations) * 50));
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

	private BufferedImage drawOrder(String[] inputImages) throws IOException {
		BufferedImage result = new BufferedImage(ORDER_X, ORDER_Y, BufferedImage.TYPE_INT_RGB);
		Graphics g = result.getGraphics();

		int x = 0;
		int y = 0;
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
		}

		return result;
	}

	//delete all the images in the 'collages 'directory 
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
