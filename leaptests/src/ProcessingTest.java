import processing.core.PApplet;

import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Vector;
import com.leapmotion.leap.Gesture.State;

import saito.objloader.OBJModel;

public class ProcessingTest extends PApplet {
	private static final long serialVersionUID = 1L;

	private OBJModel model;
	private OBJModel herschel;
	private OBJModel itokawa;
	private Controller leap;
	private int controlHandId;
	private int lightHandId;
	private Vector controPos;
	private Vector lightPos;
	private int lastCircleEventId;
	private int lastEventTime;

	public void setup() {
		size(800, 800, P3D);
		fill(255);
		noStroke();

		// Load the models
		herschel = new OBJModel(this, "HerschelExport.obj", POLYGON);
		herschel.disableTexture();
		herschel.scale(0.1f);

		itokawa = new OBJModel(this, "itokawa99846.obj", TRIANGLES);
		itokawa.disableTexture();
		itokawa.disableMaterial();
		itokawa.scale(1000f);

		// Start with the Herschel model
		model = herschel;

		// Leap setup
		leap = new Controller();
		leap.enableGesture(Gesture.Type.TYPE_CIRCLE);
		leap.enableGesture(Gesture.Type.TYPE_SWIPE);
		leap.enableGesture(Gesture.Type.TYPE_KEY_TAP);
		leap.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
		controlHandId = -1;
		lightHandId = -1;
		controPos = new Vector();
		lightPos = new Vector();
		lastEventTime = 0;
		lastCircleEventId = -1;

		if (leap.config().setFloat("Gesture.Circle.MinRadius", 10.0f)
				&& leap.config().setFloat("Gesture.Circle.MinArc", TWO_PI)) {
			leap.config().save();
		}
	}

	public void draw() {
        background(0);
        println(frameRate + " fps");

        // Get the gestures
        GestureList gestures = leap.frame().gestures();

        for (int i = 0; i < gestures.count(); i++) {
            Gesture gesture = gestures.get(i);

            switch (gesture.type()) {
            case TYPE_CIRCLE:
                CircleGesture circle = new CircleGesture(gesture);

                if (circle.isValid() && (millis() - lastEventTime) > 3000 && circle.id() != lastCircleEventId && circle.progress() > 1 && circle.radius() < 30) {
                    println("Circle gesture detected!! " + circle.id() + " " + (millis() - lastEventTime) + " " + circle.radius());
                    lastEventTime = millis();
                    lastCircleEventId = circle.id();

                    if (model.equals(herschel)) {
                        model = itokawa;
                    } else {
                        model = herschel;
                    }
                }
                break;
            case TYPE_SWIPE:
                println("Swipe gesture detected!! " + frameCount);
                break;
            case TYPE_KEY_TAP:
                println("Key tap gesture detected!! " + frameCount);
                break;
            case TYPE_SCREEN_TAP:
                println("Screen tap gesture detected!! " + frameCount);
                break;
            default:
                break;
            }
        }

        // Get the hand that controls the models if it's available
        Hand controlHand = leap.frame().hand(controlHandId);

        if (controlHand.isValid()) {
            controPos = controlHand.palmPosition();
        } else {
            controlHandId = leap.frame().hands().rightmost().id();
        }

        // Get the hand that controls the illumination
        Hand lightHand = leap.frame().hand(lightHandId);

        if (lightHand.isValid() && lightHandId != controlHandId) {
            lightPos = lightHand.fingers().frontmost().tipPosition();
        } else {
            // Loop over the hands and select the first that is available
            for (int i = 0; i < leap.frame().hands().count(); i++) {
                if (leap.frame().hands().get(i).id() != controlHandId) {
                    lightHandId = leap.frame().hands().get(i).id();
                    break;
                }
            }
        }

        // Set the illumination
        float dirY = (-(lightPos.getY() - 150) / (float) 100) * 2f;
        float dirX = ((lightPos.getX() + 50) / (float) 200) * 2f;
        directionalLight(255, 255, 255, -dirX, -dirY, -1);

        // Draw the model
        pushMatrix();
        translate(width / 2, height / 2);
        rotateX(TWO_PI * (controPos.getY() - 150) / 150);
        rotateY(-HALF_PI + TWO_PI * controPos.getX() / 300);
        scale(200 / (200 + controPos.getZ()));
        pushStyle();
        model.draw();
        popStyle();
        popMatrix();
    }

	public void keyPressed() {
		if (key == 'i') {
			model = itokawa;
		} else if (key == 'h') {
			model = herschel;
		}
	}
}
