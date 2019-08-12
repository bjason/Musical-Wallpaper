package image;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import util.ImageCollageCreator;

public class Collage extends BufferedImage {
	protected static int COLLAGE_X = 1920;
	protected static int COLLAGE_Y = 1080;
	public int COVER_X = 0;
	public int COVER_Y = 0;
	protected ImageCollageCreator creator;

	public Collage(ImageCollageCreator creator) {
		super(COLLAGE_X, COLLAGE_Y, BufferedImage.TYPE_INT_RGB);
		this.creator = creator;
	}

	protected Collage(int cOLLAGE_X2, int cOLLAGE_Y2, int typeIntRgb) {
		super(cOLLAGE_X2, cOLLAGE_Y2, typeIntRgb);
	}

	public void setCoverSize(int x, int y) {
		COVER_X = x;
		COVER_Y = y;
	}

	public Collage drawImages(String[] inputImages) throws IOException {
		Graphics g = getGraphics();

		int x = 0;
		int y = 0;
		for (String image : inputImages) {
			Cover cover = doResize(new Cover(image));

			g.drawImage(cover.getImage(), x, y, null);
			x += cover.IMAGE_X;
			if (x >= getWidth()) {
				x = 0;
				y += cover.IMAGE_Y;
			}
		}
		return this;
	}

	public void createAndSaveCollage(File outputFile) throws IOException {
		outputFile.mkdirs(); // if the output directory doesn't exist, create it
		// write the collage to the file
		ImageIO.write(this, "jpg", outputFile);
	}

	protected void setProgress(int pro) {
		creator.publicSetProgress(pro);
	}

	protected void resizeAndDraw(Cover cover, Graphics g, int y, int x, int scale) throws IOException {

		int size_x = scale * cover.IMAGE_X;
		int size_y = scale * cover.IMAGE_Y;

		g.drawImage(cover.resizeTo(size_x, size_y).getImage(), x, y, null);
	}

	protected Cover doResize(Cover cover) {
		if (COVER_X != 0 && COVER_Y != 0 && COVER_X != cover.IMAGE_X && COVER_Y != cover.IMAGE_Y) {
			return cover.resizeTo(COVER_X, COVER_Y);
		} else
			return cover;
	}

	public static String getFileName(String order, String title, String artistName, String suffix){
		title = title.replaceAll("[\\\\/:*?\"<>|]", "_");
		artistName = artistName.replaceAll("[\\\\/:*?\"<>|]", "_");
		return order + ". " + title + Cover.ARTIST_SEPARATOR + artistName + suffix;
	}

	public static String getFileName(HashMap<String, String> trackInfo, String suffix){
		String title = trackInfo.get("Title").replaceAll("[\\\\/:*?\"<>|]", "_");
		String artistName = trackInfo.get("Artist").replaceAll("[\\\\/:*?\"<>|]", "_");
		String order = trackInfo.get("order");
		return order + ". " + title + Cover.ARTIST_SEPARATOR + artistName + suffix;
	}
}
