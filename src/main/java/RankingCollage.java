import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

@SuppressWarnings("unused")
public class RankingCollage extends Collage {
	static int COLLAGE_X = 1300;

	public RankingCollage(int y, ImageCollageCreator creator) {
		super(creator);
		COLLAGE_Y = y;
	}

	protected void drawImages(String[] inputImages) throws IOException {
		collage = new BufferedImage(COLLAGE_X, COLLAGE_Y, BufferedImage.TYPE_INT_RGB);
		Graphics g = collage.getGraphics();

		int x = 0;
		int y = 0;
		int i = 0;
		for (String image : inputImages) {
			AlbumCover cover = new AlbumCover(image);
			BufferedImage bufferedImage = cover.getImage();

			g.drawImage(bufferedImage, x, y, null);
			x += IMAGE_X;

			// draw detail section bar
			g.drawImage(cover.createDetailSection(), x, y, null);

			x = 0;
			y += IMAGE_Y;
			setProgress(50 + i * (50 / inputImages.length));
		}
	}
}
