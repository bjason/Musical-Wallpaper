package image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import util.ImageCollageCreator;

@SuppressWarnings("unused")
public class ChartCollage extends Collage {

	public ChartCollage(int x, int y, ImageCollageCreator creator) {
		super(x, y, BufferedImage.TYPE_INT_RGB);
		COLLAGE_X = x;
		COLLAGE_Y = y;
		this.creator = creator;
	}

	public Collage drawImages(String[] inputImages) throws IOException {
		Graphics g = getGraphics();

		int x = 0;
		int y = 0;
		int i = 0;
		for (String image : inputImages) {
			Cover cover = doResize(new Cover(image));
			System.out.println(cover.IMAGE_X);

			g.drawImage(cover.getImage(), x, y, null);
			x += cover.IMAGE_X;

			// draw detail section bar
			g.drawImage(cover.createDetailSection(), x, y, null);

			x = 0;
			y += cover.IMAGE_Y;
			setProgress(50 + i * (50 / inputImages.length));
		}
		return this;
	}
}
