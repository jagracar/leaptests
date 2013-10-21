import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Gesture.Type;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Vector;
import processing.core.PVector;
import processing.data.Table;

public class GcOrbits extends OrientedApplet {
	private static final long serialVersionUID = 1L;

	private Controller leap;
	private Hand controlHand;
	private int controlHandId;
	public Vector controlHandPos;
	private int lastCircleEventId;
	private int lastCircleEventTime;

	private BlackHole gcBH;
	private Star[] gcStars;
	private float timeStep = 3 * 3600;
	private int nSteps = 50;
	private boolean drawTrails = false;
	private float xAng = 0;
	private float yAng = 0;
	private float zoom = 1;

	public void setup() {
		size(1900, 1000, P3D);

		// Leap motion setup
		leap = new Controller();
		leap.enableGesture(Gesture.Type.TYPE_CIRCLE);
		controlHand = leap.frame().hand(-1);
		controlHandId = -1;
		controlHandPos = new Vector();
		lastCircleEventId = -1;
		lastCircleEventTime = 0;

		// We will re-scale the spatial dimensions by the following factor
		float scaling = 6e-10f;

		// Set the galactic center black hole properties
		PVector bhPos = new PVector(width / 2, height / 2, 0);
		float bhMass = 4.3e6f * 1.989e30f * 6.67384e-11f * 1e-9f
				* pow(scaling, 3);
		gcBH = new BlackHole(bhPos, bhMass, this);

		// Set the galactic center star properties
		Table table = loadTable("sstars.csv", "header");
		gcStars = new Star[table.getRowCount()];

		for (int i = 0; i < gcStars.length; i++) {
			PVector starPos = new PVector(table.getFloat(i, "x"),
					table.getFloat(i, "y"), table.getFloat(i, "z"));
			PVector starVel = new PVector(table.getFloat(i, "vx"),
					table.getFloat(i, "vy"), table.getFloat(i, "vz"));
			starPos.mult(scaling);
			starVel.mult(scaling);

			gcStars[i] = new Star(starPos, starVel, gcBH, this);
		}
	}

	public void draw() {
		background(0);

		// Get the gestures
		GestureList gestures = leap.frame().gestures();

		for (int i = 0; i < gestures.count(); i++) {
			if (gestures.get(i).type() == Type.TYPE_CIRCLE) {
				CircleGesture circle = new CircleGesture(gestures.get(i));

				if (circle.isValid() && circle.id() != lastCircleEventId
						&& (millis() - lastCircleEventTime) > 3000
						&& circle.progress() > 1 && circle.radius() < 30) {
					println("Circle gesture detected. ID: " + circle.id()
							+ ", circle radius: " + circle.radius());
					lastCircleEventId = circle.id();
					lastCircleEventTime = millis();

					drawTrails = !drawTrails;
				}
			}
		}

		// Get the control hand if it's available
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
			controlHandPos = controlHand.palmPosition();
		}

		// Calculate the rotation angles and zoom factor
		xAng = PI * (controlHandPos.getY() - 150f) / 150f;
		yAng = -HALF_PI + PI * controlHandPos.getX() / 150f;
		zoom = 100f / (100f + controlHandPos.getZ());

		// Update the stellar coordinates
		for (int i = 0; i < gcStars.length; i++) {
			gcStars[i].update(timeStep, nSteps);
		}

		// Trick to deal with transparent images:
		// Order the stars according to their z position and draw first those
		// that are more distant
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
		if (drawTrails) {
			hint(DISABLE_DEPTH_TEST);

			for (int i = 0; i < gcStars.length; i++) {
				gcStars[i].drawTrail();
			}

			hint(ENABLE_DEPTH_TEST);
		}
		

		hint(DISABLE_DEPTH_TEST);
		text("Hola "+ frameRate, 100, 100);
		hint(ENABLE_DEPTH_TEST);
	}

	public float getXAng() {
		return xAng;
	}

	public float getYAng() {
		return yAng;
	}

	public float getZoom() {
		return zoom;
	}
}
