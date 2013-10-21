import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;

public class BlackHole implements PConstants {
	private PVector pos;
	private float mass;
	private OrientedApplet parent;
	private float radius;
	private PImage img;

	public BlackHole(PVector pos, float mass, OrientedApplet parent) {
		this.pos = pos.get();
		this.mass = mass;
		this.parent = parent;

		// Create the image that will be used to draw the black hole
		radius = 10;
		img = this.parent.createImage((int) (4 * radius), (int) (4 * radius),
				ARGB);

		img.loadPixels();
		for (int y = 0; y < img.height; y++) {
			for (int x = 0; x < img.width; x++) {
				float relDist = PApplet.sqrt(PApplet.sq(x - img.width / 2f)
						+ PApplet.sq(y - img.height / 2f))
						/ radius;
				float grey = 0;
				float alpha = PApplet.max(0,
						PApplet.min(255, 255 * (1.3f - relDist)));
				img.pixels[x + y * img.width] = this.parent.color(grey, alpha);
			}
		}
		img.updatePixels();
	}

	public void draw() {
		parent.pushMatrix();
		parent.pushStyle();
		parent.imageMode(CENTER);
		parent.translate(pos.x, pos.y, pos.z);
		parent.scale(parent.getZoom());
		parent.image(img, 0, 0);
		parent.popStyle();
		parent.popMatrix();
	}

	public PVector getPos() {
		return pos.get();
	}

	public float getMass() {
		return mass;
	}
}
