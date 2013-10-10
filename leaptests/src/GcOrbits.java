import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.Table;

public class GcOrbits extends PApplet {
	private static final long serialVersionUID = 1L;

	BlackHole bh;
	Star[] stars;
	float dt = 3 * 3600;
	float elapsedTime = 0;
	int nSteps = 0;
	float zoom = 3;
	boolean drawTrails = false;

	public void setup() {
		size(1000, 800, P3D);

		// Set the galactic center position and mass
		float scaling = 2.5e-10f;
		bh = new BlackHole(new PVector(0, 0, 0), 4.3e6f * 1.989e30f
				* 6.67384e-11f * 1e-9f * pow(scaling, 3));

		// Create the stars
		Table table = loadTable("sstars.csv", "header");
		stars = new Star[table.getRowCount()];

		for (int i = 0; i < stars.length; i++) {
			stars[i] = new Star(new PVector(table.getFloat(i, "x") * scaling,
					table.getFloat(i, "y") * scaling, table.getFloat(i, "z")
							* scaling), new PVector(table.getFloat(i, "vx")
					* scaling, table.getFloat(i, "vy") * scaling,
					table.getFloat(i, "vz") * scaling), bh);

		}
	}

	public void draw() {
		background(0);
		lights();

		// Update the star coordinates
		for (int i = 0; i < stars.length; i++) {
			// Speed up things a bit
			for (int j = 0; j < 50; j++) {
				stars[i].update(dt);
			}

			// Calculate the screen position
			stars[i].calculateScreenPos();

			if (nSteps > 5) {
				stars[i].addTrailPoint();
			}
		}

		if (nSteps > 5) {
			nSteps = 0;
		} else {
			nSteps++;
		}

		elapsedTime += 50 * dt;
		println(frameRate + " elapsedTime =" + elapsedTime
				/ (365.25 * 24 * 3600));

		// Trick to deal with transparent images:
		// Order the stars according to their z position and draw first those
		// that
		// are more distant
		boolean[] alreadyDrawn = new boolean[stars.length];
		int counter = 0;
		boolean bhDrawn = false;

		while (counter < stars.length) {
			float minZValue = Float.MAX_VALUE;
			int starIndex = -1;

			// Select the most distant star in this iteration
			for (int i = 0; i < stars.length; i++) {
				if (!alreadyDrawn[i]) {
					PVector screenPos = stars[i].getScreenPos();

					if (screenPos.z < minZValue) {
						minZValue = screenPos.z;
						starIndex = i;
					}
				}
			}

			if (minZValue > 0 && !bhDrawn) {
				bh.draw();
				bhDrawn = true;
			} else {
				// Draw the star
				stars[starIndex].draw();
				alreadyDrawn[starIndex] = true;
				counter++;
			}
		}

		if (drawTrails) {
			hint(DISABLE_DEPTH_TEST);
			for (int i = 0; i < stars.length; i++) {
				stars[i].drawTrail();
			}
			hint(ENABLE_DEPTH_TEST);
		}
	}

	public void mousePressed() {
		if (mouseButton == LEFT) {
			zoom *= 1.2;
		} else if (mouseButton == RIGHT) {
			zoom /= 1.2;
		} else if (mouseButton == CENTER) {
			drawTrails = !drawTrails;
		}
	}

	public class BlackHole {
		private PVector pos;
		private float mass;
		private int radius;
		private PImage img;

		public BlackHole(PVector pos, float mass) {
			this.pos = pos.get();
			this.mass = mass;

			// Create the image that will be used to draw the black hole
			radius = 5;
			img = createImage(3 * radius, 3 * radius, ARGB);

			img.loadPixels();
			for (int y = 0; y < img.height; y++) {
				for (int x = 0; x < img.width; x++) {
					float relDistSq = (sq(x - img.width / 2) + sq(y
							- img.height / 2))
							/ sq(radius);
					img.pixels[x + y * img.width] = color(0,
							max(0, min(255, 255 * (1.3f - sqrt(relDistSq)))));
				}
			}
			img.updatePixels();
		}

		void draw() {
			pushMatrix();
			imageMode(CENTER);
			translate(width / 2, height / 2, 0);
			scale(zoom);
			// ellipse(0, 0, 10, 10);
			image(img, 0, 0);
			popMatrix();
		}

		PVector getPos() {
			return pos.get();
		}

		float getMass() {
			return mass;
		}
	}

	public class Star {
		private PVector pos;
		private PVector vel;
		private BlackHole bh;
		private PVector acc;
		private PVector screenPos;
		private float radius;
		private PImage img;
		private PImage flaresImg;
		private float seed;
		private ArrayList<PVector> trail;

		public Star(PVector pos, PVector vel, BlackHole bh) {
			this.pos = pos.get();
			this.vel = vel.get();
			this.bh = bh;

			// Calculate the acceleration vector
			PVector r = PVector.sub(this.pos, this.bh.getPos());
			float r3 = r.mag() * r.magSq();
			acc = PVector.mult(r, -bh.getMass() / r3);

			// Calculate the current position of the star in the screen
			screenPos = new PVector(0, 0, 0);
			calculateScreenPos();

			// Create the image that will be used to draw the star
			radius = 6;
			img = createImage(6 * ((int) radius), 6 * ((int) radius), ARGB);

			img.loadPixels();
			for (int y = 0; y < img.height; y++) {
				for (int x = 0; x < img.width; x++) {
					float relDistSq = (sq(x - img.width / 2) + sq(y
							- img.height / 2))
							/ sq(radius);
					float grey = max(0, 255 * (1 - relDistSq));
					float alpha = max(0, min(255, 255 * (1.2f - relDistSq)));
					img.pixels[x + y * img.width] = color(grey, alpha);
				}
			}
			img.updatePixels();

			// Create the flares image and the seed for the flares
			flaresImg = createImage(img.width, img.height, ARGB);
			seed = random(1000);

			// The trail
			trail = new ArrayList<PVector>(1000);
		}

		public void update(float dt) {
			pos.add(PVector.mult(vel, dt));
			pos.add(PVector.mult(acc, dt * dt / 2));

			vel.add(PVector.mult(acc, dt / 2));

			PVector r = PVector.sub(pos, bh.getPos());
			float r3 = r.mag() * r.magSq();
			acc = PVector.mult(r, -bh.getMass() / r3);

			vel.add(PVector.mult(acc, dt / 2));
		}

		public void calculateScreenPos() {
			PVector bhPos = bh.getPos();

			pushMatrix();
			translate(width / 2, height / 2, 0);
			scale(zoom);
			translate(bhPos.x, bhPos.y, bhPos.z);
			rotateX(TWO_PI * mouseY / height);
			rotateY(TWO_PI * mouseX / width);
			translate(pos.x - bhPos.x, pos.y - bhPos.y, pos.z - bhPos.y);

			// Save the screen position
			screenPos = new PVector(modelX(0, 0, 0), modelY(0, 0, 0), modelZ(0,
					0, 0));
			popMatrix();
		}

		void draw() {
			pushMatrix();
			imageMode(CENTER);
			translate(screenPos.x, screenPos.y, screenPos.z);
			scale(zoom);
			image(img, 0, 0);
			updateFlareImage();
			image(flaresImg, 0, 0);
			noStroke();
			// fill(255);
			// sphere(0.8*radius);
			// fill(255, 100);
			// sphere(0.9*radius);
			// sphere(1.0*radius);
			// sphere(1.1*radius);
			popMatrix();
		}

		void updateFlareImage() {
			// Prepare the flares image for the next iteration
			seed += 0.1;

			flaresImg.loadPixels();
			for (int y = 0; y < flaresImg.height; y++) {
				for (int x = 0; x < flaresImg.width; x++) {
					float dist = sqrt(sq(x - flaresImg.width / 2)
							+ sq(y - flaresImg.height / 2));

					if (dist < 0.9 * radius) {
						float ang = (atan2((float) (y - flaresImg.height / 2),
								(float) (x - flaresImg.width / 2)) + noise(x))
								/ TWO_PI;
						flaresImg.pixels[x + y * flaresImg.width] = color(255 * noise(
								0.1f * (dist - seed), 3 * ang));
					}
				}
			}

			// Make the changes in a temporal array
			int[] tempFlaresImg = new int[flaresImg.pixels.length];

			for (int y = 2; y < flaresImg.height - 2; y++) {
				for (int x = 2; x < flaresImg.width - 2; x++) {
					float distSq = sq(x - flaresImg.width / 2)
							+ sq(y - flaresImg.height / 2);
					float greySum = 0;
					float counter = 0;

					for (int i = -2; i < 3; i++) {
						for (int j = -2; j < 3; j++) {
							greySum += red(flaresImg.pixels[x + i + (y + j)
									* flaresImg.width]);
							counter++;
						}
					}

					float newGrey = greySum / counter;
					tempFlaresImg[x + y * flaresImg.width] = color(newGrey,
							newGrey);
				}
			}

			// Replace the flares image pixels with the temporal array
			flaresImg.pixels = tempFlaresImg;
			flaresImg.updatePixels();
		}

		public void addTrailPoint() {
			trail.add(pos.get());

			if (trail.size() > 1000) {
				trail.remove(0);
			}
		}

		public void drawTrail() {
			if (trail.size() > 4) {
				PVector bhPos = bh.getPos();

				pushMatrix();
				pushStyle();
				stroke(color(100, 100, 255));
				strokeWeight(1);
				translate(width / 2, height / 2, 0);
				translate(bhPos.x, bhPos.y, bhPos.z);
				rotateX(TWO_PI * mouseY / height);
				rotateY(TWO_PI * mouseX / width);
				scale(zoom);

				for (int i = 0; i < trail.size() - 4; i++) {
					PVector trailPos = trail.get(i);
					PVector trailPos2 = trail.get(i + 1);
					line(trailPos.x - bhPos.x, trailPos.y - bhPos.y, trailPos.z
							- bhPos.y, trailPos2.x - bhPos.x, trailPos2.y
							- bhPos.y, trailPos2.z - bhPos.y);
				}
				popStyle();
				popMatrix();
			}
		}

		public PVector getScreenPos() {
			return screenPos;
		}
	}

}
