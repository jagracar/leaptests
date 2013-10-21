import java.util.ArrayList;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;

public class Star implements PConstants {
	private PVector pos;
	private PVector vel;
	private BlackHole bh;
	private OrientedApplet parent;
	private PVector screenPos;
	private float radius;
	private PImage img;
	private PImage flaresImg;
	private float noiseSeed;
	private ArrayList<PVector> trail;
	private int trailCounter;

	public Star(PVector pos, PVector vel, BlackHole bh, OrientedApplet parent) {
		this.pos = pos.get();
		this.vel = vel.get();
		this.bh = bh;
		this.parent = parent;

		// Calculate the current position of the star in the screen
		calculateScreenPos();

		// Create the image that will be used to draw the star
		radius = 10;
		img = this.parent.createImage((int) (6 * radius), (int) (6 * radius),
				ARGB);

		img.loadPixels();
		for (int y = 0; y < img.height; y++) {
			for (int x = 0; x < img.width; x++) {
				float relDistSq = (PApplet.sq(x - img.width / 2f) + PApplet
						.sq(y - img.height / 2f)) / PApplet.sq(radius);
				float grey = PApplet.max(0, 255 * (1 - relDistSq));
				float alpha = PApplet.max(0,
						PApplet.min(255, 255 * (1.2f - relDistSq)));
				img.pixels[x + y * img.width] = this.parent.color(grey, alpha);
			}
		}
		img.updatePixels();

		// Create the flares image and the seed for the flares
		flaresImg = parent.createImage(img.width, img.height, ARGB);
		noiseSeed = parent.random(1000);

		// Create an array list to save the trail points
		trail = new ArrayList<PVector>(1000);
		trailCounter = 0;
	}

	public void update(float dt, int iterations) {
		// Update the coordinates
		PVector acc = PVector.mult(pos,
				-bh.getMass() / PApplet.pow(pos.mag(), 3));

		for (int i = 0; i < iterations; i++) {
			pos.add(PVector.mult(vel, dt));
			pos.add(PVector.mult(acc, dt * dt / 2));

			vel.add(PVector.mult(acc, dt / 2));
			acc = PVector.mult(pos, -bh.getMass() / PApplet.pow(pos.mag(), 3));
			vel.add(PVector.mult(acc, dt / 2));
		}

		// Add the point to the trail if necessary
		trailCounter++;

		if (trailCounter > 5) {
			trailCounter = 0;
			trail.add(pos.get());

			if (trail.size() > 1000) {
				trail.remove(0);
			}
		}

		// Calculate the position of the point on the screen
		calculateScreenPos();

		// Update the flares image
		updateFlaresImage();
	}

	protected void calculateScreenPos() {
		PVector bhPos = bh.getPos();

		parent.pushMatrix();
		parent.translate(bhPos.x, bhPos.y, bhPos.z);
		parent.rotateX(parent.getXAng());
		parent.rotateY(parent.getYAng());
		parent.scale(parent.getZoom());
		parent.translate(pos.x, pos.y, pos.z);

		// Save the screen position
		screenPos = new PVector(parent.modelX(0, 0, 0), parent.modelY(0, 0, 0),
				parent.modelZ(0, 0, 0));
		parent.popMatrix();
	}

	protected void updateFlaresImage() {
		// Prepare the flares image for the next iteration
		int width = flaresImg.width;
		int height = flaresImg.height;
		noiseSeed += 0.1;

		flaresImg.loadPixels();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float dist = PApplet.sqrt(PApplet.sq(x - width / 2f)
						+ PApplet.sq(y - height / 2f));

				if (dist < 0.9 * radius) {
					float relAng = (PApplet.atan2((float) (y - height / 2f),
							(float) (x - width / 2f)) + parent.noise(x))
							/ TWO_PI;
					flaresImg.pixels[x + y * width] = parent.color(255 * parent
							.noise(0.1f * (dist - noiseSeed), 3 * relAng));
				}
			}
		}

		// Make the changes in a temporal array
		int[] tempFlaresImg = new int[width * height];

		for (int y = 2; y < height - 2; y++) {
			for (int x = 2; x < width - 2; x++) {
				float greySum = 0;
				float counter = 0;

				for (int i = -2; i < 3; i++) {
					for (int j = -2; j < 3; j++) {
						greySum += parent.red(flaresImg.pixels[x + i + (y + j)
								* width]);
						counter++;
					}
				}

				float newGrey = greySum / counter;
				tempFlaresImg[x + y * width] = parent.color(newGrey, newGrey);
			}
		}

		// Replace the flares image pixels with the temporal array
		flaresImg.pixels = tempFlaresImg;
		flaresImg.updatePixels();
	}

	public void draw() {
		parent.pushMatrix();
		parent.pushStyle();
		parent.imageMode(CENTER);
		parent.translate(screenPos.x, screenPos.y, screenPos.z);
		parent.scale(parent.getZoom());
		parent.image(img, 0, 0);
		parent.image(flaresImg, 0, 0);
		parent.popStyle();
		parent.popMatrix();
	}

	public void drawTrail() {
		if (trail.size() > 4) {
			PVector bhPos = bh.getPos();

			parent.pushMatrix();
			parent.pushStyle();
			parent.stroke(parent.color(100, 100, 255));
			parent.strokeWeight(1);
			parent.translate(bhPos.x, bhPos.y, bhPos.z);
			parent.rotateX(parent.getXAng());
			parent.rotateY(parent.getYAng());
			parent.scale(parent.getZoom());

			for (int i = 0; i < trail.size() - 4; i++) {
				PVector trailPos = trail.get(i);
				PVector trailPos2 = trail.get(i + 1);
				parent.line(trailPos.x, trailPos.y, trailPos.z, trailPos2.x,
						trailPos2.y, trailPos2.z);
			}
			parent.popStyle();
			parent.popMatrix();
		}
	}

	public void clearTrail() {
		trail.clear();
	}

	public PVector getScreenPos() {
		return screenPos;
	}
}
