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
	private int numTracks = 0;

	private final String ALLOWED_EXTENSION = ".jpg";
	static final String sourceDir = System.getProperty("user.home") + File.separator + "Pictures" + File.separator
			+ "Spotify Playlist Visualizor" + File.separator + "Album art";
	static final String outputDir = System.getProperty("user.home") + File.separator + "Pictures" + File.separator
			+ "Spotify Playlist Visualizor" + File.separator + "Collages";

	private int IMAGE_X;
	private int IMAGE_Y;
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
		int baseSize = 0;

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
			baseSize = (size - 6 * ZuneCollage.INTERVAL) / 4;
			break;
		}
		IMAGE_X = size;
		IMAGE_Y = size;

		// first delete any previously saved collages
		delPrevCollgs();

		ArrayList<String> allImages = getImageFilenames(sourceDir);

		int count = 0;
		numTracks = allImages.size();
		Collage collage = null;
		String[] thisCollageImages = null;
		File outputFile = null;

		if (isOrderMode == true) {
			// make the playlist a countdown
			Collections.reverse(allImages);

			thisCollageImages = getCollageImagesNames(allImages, numTracks);
			collage = new RankingCollage(numTracks * size, this);
			collage.setImageSize(IMAGE_X, IMAGE_Y);

			outputFile = getOutputFilename(outputDir);

		} else if (isZuneMode == true) {

			Collections.shuffle(allImages);

			thisCollageImages = getCollageImagesNames(allImages, numTracks);
			collage = new ZuneCollage(baseSize, baseSize, this);

			outputFile = getOutputFilename(outputDir);
		} else {
			ArrayList<String> unusedImagesNames = new ArrayList<>(allImages);
			Collections.shuffle(allImages); // randomize order of images in
											// collages
			int imagesPerCollage = getImagesPerCollage();
			thisCollageImages = new String[imagesPerCollage];

			// roughly calculate progress for the loading bar
			int approxRequiredIterations = unusedImagesNames.size() / imagesPerCollage;

			while (unusedImagesNames.size() >= imagesPerCollage) {
				count++;
				// grab the next imagesPerCollage images from the shuffled
				// unused images

				for (int i = 0; i < imagesPerCollage; i++) {
					thisCollageImages[i] = unusedImagesNames.get(0);
					unusedImagesNames.remove(0); // as we use each image,
													// remove
													// it
					// from the unused list
				}

				// generate a unique filename for each, just "collage x.jpg"
				outputFile = getOutputFilename(outputDir, count);

				collage = new Collage(IMAGE_X, IMAGE_Y, this);

				collage.drawImages(thisCollageImages);
				collage.createAndSaveCollage(outputFile);

				setProgress((int) (((double) count / approxRequiredIterations) * 100));
			}

			// if are some leftover images (ie unusedImages.size() %
			// imagesPerCollage != 0)
			// then use of the already used images to fill this collage
			if (unusedImagesNames.size() > 0) {
				collage = new Collage(IMAGE_X, IMAGE_Y, this);

				count++;
				// start by using all remaining unused images
				ArrayList<String> lastCollageImages = new ArrayList<>(unusedImagesNames);
				// then top up with any other images
				while (lastCollageImages.size() < imagesPerCollage) {
					int imagesToAdd = imagesPerCollage - lastCollageImages.size();
					if (imagesToAdd > allImages.size()) {
						imagesToAdd = allImages.size();
					}
					lastCollageImages.addAll(allImages.subList(0, imagesToAdd));
					Collections.shuffle(allImages);
				}

				outputFile = getOutputFilename(outputDir, count);
				thisCollageImages = lastCollageImages.toArray(new String[0]);
			}
		}

		collage.drawImages(thisCollageImages);
		collage.createAndSaveCollage(outputFile);
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
			// only the image names
		}
	}

	private void delPrevCollgs() {
		new File(outputDir).mkdirs(); // create the folders if they don't exist
		for (File file : new File(outputDir).listFiles()) {
			file.delete();
		}
	}

	public void publicSetProgress(int prog) {
		setProgress(prog);
	}

	private int getImagesPerCollage() {
		// calculate how many images can be fit in horizontally and vertically
		// round up so that there is no empty space at the edge of the wallpaper
		int x = (int) Math.ceil((double) COLLAGE_X / IMAGE_X);
		int y = (int) Math.ceil((double) COLLAGE_Y / IMAGE_Y);
		return x * y;
	}
}