import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Collage {
	protected int COLLAGE_X = 1920;
	protected int COLLAGE_Y = 1080;
	protected int IMAGE_X;
	protected int IMAGE_Y;
	protected BufferedImage collage;
	protected String[] thisCollageImages;
	protected ImageCollageCreator creator;

	public Collage(int x, int y, ImageCollageCreator creator) {
		setImageSize(x, y);
		this.creator = creator;
	}

	public Collage(ImageCollageCreator creator) {
		this.creator = creator;
	}

	public void setImageSize(int x, int y) {
		this.IMAGE_X = x;
		this.IMAGE_Y = y;
	}

	protected void drawImages(String[] inputImages) throws IOException {
		collage = new BufferedImage(COLLAGE_X, COLLAGE_Y, BufferedImage.TYPE_INT_RGB);
		Graphics g = collage.getGraphics();

		int x = 0;
		int y = 0;
		for (String image : inputImages) {
			BufferedImage bufferedImage = new AlbumCover(image).getImage();
			g.drawImage(bufferedImage, x, y, null);
			x += IMAGE_X;
			if (x >= collage.getWidth()) {
				x = 0;
				y += IMAGE_Y;
			}
		}
	}

	protected void createAndSaveCollage(BufferedImage image, File outputFile) throws IOException {
		outputFile.mkdirs(); // if the output directory doesn't exist, create it
		// write the collage to the file
		ImageIO.write(image, "jpg", outputFile);
	}

	protected void createAndSaveCollage(File outputFile) throws IOException {
		outputFile.mkdirs(); // if the output directory doesn't exist, create it
		// write the collage to the file
		ImageIO.write(collage, "jpg", outputFile);
		System.out.println("here");
	}

	protected void setProgress(int pro) {
		creator.publicSetProgress(pro);
	}
}
