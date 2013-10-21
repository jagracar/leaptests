import processing.core.PFont;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.Table;
import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Gesture.Type;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.HandList;
import com.leapmotion.leap.Vector;
import saito.objloader.OBJModel;

public class allTogether extends OrientedApplet {
	private static final long serialVersionUID = 1L;

	private Controller leap;
	private Hand controlHand;
	private Hand lightHand;
	private int controlHandId;
	private int lightHandId;
	private Vector controlHandPos;
	private Vector lightHandPos;
	private int lastCircleEventId;
	private int lastCircleEventTime;

	private OBJModel model;
	private OBJModel herschel;
	private OBJModel itokawa;

	private BlackHole gcBH;
	private Star[] gcStars;
	private float timeStep = 3 * 3600;
	private int nSteps = 50;

	private int layer;

	private PFont titleFont = createFont("SansSerif.plain", 20);
	private PFont descriptionFont = createFont("SansSerif.plain", 15);
	private PImage imgPalm;
	private PImage imgCircle;
	
	public static void main(String[] args) {
		OrientedApplet.main(new String[] { allTogether.class.getName() });
	}
	
	public void setup() {
		size(1900, 1000, P3D);
		background(0);

		// Leap motion setup
		leap = new Controller();
		leap.enableGesture(Gesture.Type.TYPE_CIRCLE);
		leap.enableGesture(Gesture.Type.TYPE_KEY_TAP);
		controlHand = leap.frame().hand(-1);
		lightHand = leap.frame().hand(-1);
		controlHandId = -1;
		lightHandId = -1;
		controlHandPos = new Vector();
		lightHandPos = new Vector();
		lastCircleEventId = -1;
		lastCircleEventTime = 0;

		if (leap.config().setFloat("Gesture.Circle.MinRadius", 10.0f)
				&& leap.config().setFloat("Gesture.Circle.MinArc", TWO_PI)) {
			leap.config().save();
		}

		// Load the obj models
		herschel = new OBJModel(this, "HerschelExport.obj", POLYGON);
		herschel.disableTexture();
		herschel.scale(0.12f);

		itokawa = new OBJModel(this, "itokawa99846.obj", TRIANGLES);
		itokawa.disableTexture();
		itokawa.disableMaterial();
		itokawa.scale(1500f);

		// Galactic center orbits setup
		// We will re-scale the spatial dimensions by the following factor
		float scaling = 6e-10f;

		// Set the galactic center black hole properties
		PVector bhPos = new PVector(width / 2f, height / 2f, 0);
		float bhMass = 4.3e6f * 1.989e30f * 6.67384e-11f * 1e-9f * pow(scaling, 3);
		gcBH = new BlackHole(bhPos, bhMass, this);

		// Set the galactic center star properties
		Table table = loadTable("sstars.csv", "header");
		gcStars = new Star[table.getRowCount()];

		for (int i = 0; i < gcStars.length; i++) {
			PVector starPos = new PVector(table.getFloat(i, "x"), table.getFloat(i, "y"), table.getFloat(i, "z"));
			PVector starVel = new PVector(table.getFloat(i, "vx"), table.getFloat(i, "vy"), table.getFloat(i, "vz"));
			starPos.mult(scaling);
			starVel.mult(scaling);

			gcStars[i] = new Star(starPos, starVel, gcBH, this);
		}

		// Start with the Herschel model
		model = herschel;
		layer = 0;

		imgPalm = loadImage("Leap_Palm_Vectors.png");
		imgPalm.resize(150, 0);
		imgCircle = loadImage("Leap_Gesture_Circle.png");
		imgCircle.resize(150, 0);
	}

	public void draw() {
		// Trick to slow the computation when it's not necessary
		if (leap.frame().hands().count() == 0 && (layer == 0 || layer == 1)) {
			// Reduce the frame rate and don't draw anything
			frameRate(4);
			// background(0);
		} else {
			// Increase the frame rate and clean the screen
			frameRate(60);
			background(0);

			// Get the gestures and act accordingly
			GestureList gestures = leap.frame().gestures();

			for (int i = 0; i < gestures.count(); i++) {
				Gesture gesture = gestures.get(i);

				if (gesture.type() == Type.TYPE_CIRCLE) {
					println("Circle gesture detected.");
					CircleGesture circle = new CircleGesture(gesture);

					if (circle.isValid() && circle.id() != lastCircleEventId && (millis() - lastCircleEventTime) > 3000
							&& circle.progress() > 1 && circle.radius() < 30) {
						println("Valid circle gesture. ID: " + circle.id() + ", circle radius: " + circle.radius());

						// Change the model that will be shown
						//layer = (layer + 1) % 3;

						if (layer == 0) {
							//model = herschel;
						} else if (layer == 1) {
							//model = itokawa;
						}

						lastCircleEventId = circle.id();
						lastCircleEventTime = millis();
					}
				} else if (gesture.type() == Type.TYPE_KEY_TAP) {
					println("Key tap gesture detected.");

					// Clear the stellar trails
					for (int s = 0; s < gcStars.length; s++) {
						gcStars[s].clearTrail();
					}
				}
			}

			// Get the hand that controls the models if it's available
			controlHand = leap.frame().hand(controlHandId);

			if (!controlHand.isValid()) {
				// Use the right most hand if it's available
				Hand rightHand = leap.frame().hands().rightmost();

				if (rightHand.isValid()) {
					controlHand = rightHand;
					controlHandId = controlHand.id();
				}
			}

			if (controlHand.isValid() && controlHand.palmPosition().isValid()) {
				// Update the hand position if the hand is not closed
				if (controlHand.fingers().count() > 1) {
					controlHandPos = controlHand.palmPosition();
				}
			}

			// Get the hand that controls the illumination
			if (lightHandId == controlHandId) {
				lightHandId = -1;
			}

			lightHand = leap.frame().hand(lightHandId);

			if (!lightHand.isValid()) {
				// Use the left most hand if it's available
				Hand leftHand = leap.frame().hands().leftmost();

				if (leftHand.isValid() && leftHand.id() != controlHandId) {
					lightHand = leftHand;
					lightHandId = lightHand.id();
				} else {
					// Loop over the hands and select the first that is available
					HandList hands = leap.frame().hands();

					for (int i = 0; i < hands.count(); i++) {
						Hand hand = hands.get(i);

						if (hand.isValid() && hand.id() != controlHandId) {
							lightHand = hand;
							lightHandId = hand.id();
							break;
						}
					}
				}
			}

			if (lightHand.isValid() && lightHand.fingers().count() > 0) {
				lightHandPos = lightHand.fingers().frontmost().tipPosition();
			}

			if (layer == 0 || layer == 1) {
				// Set the illumination
				float dirY = -(lightHandPos.getY() - 150f) / 100f;
				float dirX = (lightHandPos.getX() + 50f) / 100f;
				directionalLight(255, 255, 255, -dirX, -dirY, -1);

				// Draw the model
				pushMatrix();
				pushStyle();
				fill(255);
				noStroke();
				translate(width / 2f, height / 2f);
				rotateX(PI * (controlHandPos.getY() - 150f) / 150f);
				rotateY(-HALF_PI + PI * controlHandPos.getX() / 150f);
				scale(130f / (130f + max(controlHandPos.getZ(), -130)));
				model.draw();
				popStyle();
				popMatrix();
			} else {
				// Calculate the rotation angles and zoom factor
				xAng = 1.5f * HALF_PI * (controlHandPos.getY() - 150f) / 150f;
				yAng = -HALF_PI + 1.5f * HALF_PI * controlHandPos.getX() / 150f;
				zoom = 75f / (75f + max(controlHandPos.getZ(), -70f));

				// Update the stellar coordinates
				for (int s = 0; s < gcStars.length; s++) {
					gcStars[s].update(timeStep, nSteps);
				}

				// Trick to deal with transparent images:
				// Order the stars according to their z position and draw first those that are more distant
				boolean gcBHDrawn = false;
				boolean[] gcStarDrawn = new boolean[gcStars.length];
				int starCounter = 0;

				while (starCounter < gcStars.length) {
					// Select the most distant star in this iteration
					float minZValue = Float.MAX_VALUE;
					int starIndex = -1;

					for (int i = 0; i < gcStars.length; i++) {
						if (!gcStarDrawn[i] && gcStars[i].getScreenPos().z < minZValue) {
							minZValue = gcStars[i].getScreenPos().z;
							starIndex = i;
						}
					}

					// Check if the black hole needs to be drawn
					if (minZValue > gcBH.getPos().z && !gcBHDrawn) {
						gcBH.draw();
						gcBHDrawn = true;
					}

					// Draw the star
					gcStars[starIndex].draw();
					gcStarDrawn[starIndex] = true;
					starCounter++;
				}

				// Draw the stellar trails
				if (lightHand.isValid()) {
					hint(DISABLE_DEPTH_TEST);

					for (int i = 0; i < gcStars.length; i++) {
						gcStars[i].drawTrail();
					}

					hint(ENABLE_DEPTH_TEST);
				}
			}
		}

		// Draw the text information
		hint(DISABLE_DEPTH_TEST);
		pushStyle();
		noLights();
		fill(color(100, 100, 255));
		fill(255);

		if (layer == 0) {
			textFont(titleFont);
			text("Der Herschel Infrarot-Satellit", 50, 50);
			textFont(descriptionFont);
			float delta = 100;
			text("Bewegen Sie Ihre Hand nach oben und unten und von", 50, delta);
			text("links nach rechts, um das Modell zu drehen.", 50, 25 + delta);
			text("Bewegen Sie Ihre Hand in Richtung des Bildschirms,", 50, 50 + delta);
			text("um das Modell zu vergroessern.", 50, 75 + delta);
			delta = 225;
			text("Zeichnen Sie einen imaginaeren Kreis mit Ihrer Hand,", 50, delta);
			text("um das Objekt zu wechseln.", 50, 25 + delta);
			delta = 350;
			textFont(titleFont);
			text("The Herschel infrared satellite", 50, delta);
			textFont(descriptionFont);
			delta = 400;
			text("Move your right hand up and down, and from left to", 50, delta);
			text("right to rotate the model. Moving your hand in the", 50, 25 + delta);
			text("screen direction will automatically zoom the image.", 50, 50 + delta);
			delta = 500;
			text("Draw an imaginary circle with your hand to move to", 50, delta);
			text("the visualization.", 50, 25 + delta);
		} else if (layer == 1) {
			textFont(titleFont);
			text("Modell des Itokawa-Asteroiden", 50, 50);
			textFont(descriptionFont);
			float delta = 100;
			text("Bewegen Sie Ihre Hand nach oben und unten und von", 50, delta);
			text("links nach rechts, um das Modell zu drehen.", 50, 25 + delta);
			text("Bewegen Sie Ihre Hand in Richtung des Bildschirms,", 50, 50 + delta);
			text("um das Modell zu vergroessern.", 50, 75 + delta);
			delta = 225;
			text("Zeichnen Sie einen imaginaeren Kreis mit Ihrer Hand,", 50, delta);
			text("um das Objekt zu wechseln.", 50, 25 + delta);
			delta = 350;
			textFont(titleFont);
			text("Model of the Itokawa asteroid", 50, delta);
			textFont(descriptionFont);
			delta = 400;
			text("Move your right hand up and down, and from left to", 50, delta);
			text("right to rotate the model. Moving your hand in the", 50, 25 + delta);
			text("screen direction will automatically zoom the image.", 50, 50 + delta);
			delta = 500;
			text("Draw an imaginary circle with your hand to move to", 50, delta);
			text("the visualization.", 50, 25 + delta);
		} else {
			textFont(titleFont);
			text("Sterne in der Naehe des supermassereichen schwarzen Lochs im Zentrum unserer Galaxie", 50, 50);
			textFont(descriptionFont);
			float delta = 100;
			text("Bewegen Sie Ihre Hand nach oben und unten und von", 50, delta);
			text("links nach rechts, um das Modell zu drehen.", 50, 25 + delta);
			text("Bewegen Sie Ihre Hand in Richtung des Bildschirms,", 50, 50 + delta);
			text("um das Modell zu vergroessern.", 50, 75 + delta);
			delta = 225;
			text("Zeichnen Sie einen imaginaeren Kreis mit Ihrer Hand,", 50, delta);
			text("um das Objekt zu wechseln.", 50, 25 + delta);
			delta = 350;
			textFont(titleFont);
			text("Stars around the super massive black hole in our galaxy", 50, delta);
			textFont(descriptionFont);
			delta = 400;
			text("Move your right hand up and down, and from left to", 50, delta);
			text("right to rotate the model. Moving your hand in the", 50, 25 + delta);
			text("screen direction will automatically zoom the image.", 50, 50 + delta);
			delta = 500;
			text("Draw an imaginary circle with your hand to move to", 50, delta);
			text("the visualization.", 50, 25 + delta);
		}

		// image(imgPalm, 100,300);
		// image(imgCircle, 100,400);
		popStyle();
		hint(ENABLE_DEPTH_TEST);
	}
	
	public void mouseClicked(){
		layer = (layer + 1) % 3;

		if (layer == 0) {
			model = herschel;
		} else if (layer == 1) {
			model = itokawa;
		}
	
	}
}
