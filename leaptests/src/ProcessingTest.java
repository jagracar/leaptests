import processing.core.PApplet;
import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.HandList;
import com.leapmotion.leap.Vector;
import saito.objloader.OBJModel;

public class ProcessingTest extends PApplet {
    private static final long serialVersionUID = 1L;

    private OBJModel model;
    private OBJModel herschel;
    private OBJModel itokawa;
    private Controller leap;
    private Hand controlHand;
    private Hand lightHand;
    private int controlHandId;
    private int lightHandId;
    private Vector controlHandPos;
    private Vector lightHandPos;
    private int lastCircleEventId;
    private int lastCircleEventTime;

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

        // Leap motion setup
        leap = new Controller();
        leap.enableGesture(Gesture.Type.TYPE_CIRCLE);
        leap.enableGesture(Gesture.Type.TYPE_SWIPE);
        leap.enableGesture(Gesture.Type.TYPE_KEY_TAP);
        leap.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
        controlHand = leap.frame().hand(-1);
        lightHand = leap.frame().hand(-1);
        controlHandId = -1;
        lightHandId = -1;
        controlHandPos = new Vector();
        lightHandPos = new Vector();
        lastCircleEventId = -1;
        lastCircleEventTime = 0;

        if (leap.config().setFloat("Gesture.Circle.MinRadius", 10.0f) && leap.config().setFloat("Gesture.Circle.MinArc", TWO_PI)) {
            leap.config().save();
        }
    }

    public void draw() {
        background(0);

        // Get the gestures
        GestureList gestures = leap.frame().gestures();

        for (int i = 0; i < gestures.count(); i++) {
            Gesture gesture = gestures.get(i);

            switch (gesture.type()) {
            case TYPE_CIRCLE:
                CircleGesture circle = new CircleGesture(gesture);

                if (circle.isValid() && circle.id() != lastCircleEventId && (millis() - lastCircleEventTime) > 3000
                        && circle.progress() > 1 && circle.radius() < 30) {
                    println("Circle gesture detected. ID: " + circle.id() + ", circle radius: " + circle.radius());
                    lastCircleEventId = circle.id();
                    lastCircleEventTime = millis();

                    // Change the model that will be shown
                    if (model.equals(herschel)) {
                        model = itokawa;
                    } else {
                        model = herschel;
                    }
                }
                break;
            case TYPE_SWIPE:
                println("Swipe gesture detected.");
                break;
            case TYPE_KEY_TAP:
                println("Key tap gesture detected.");
                break;
            case TYPE_SCREEN_TAP:
                println("Screen tap gesture detected.");
                break;
            default:
                break;
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
            controlHandPos = controlHand.palmPosition();
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

        if (lightHand.isValid() && lightHand.fingers().frontmost().tipPosition().isValid()) {
            lightHandPos = lightHand.fingers().frontmost().tipPosition();
        }

        // Set the illumination
        float dirY = -(lightHandPos.getY() - 150f) / 100f;
        float dirX = (lightHandPos.getX() + 50f) / 100f;
        directionalLight(255, 255, 255, -dirX, -dirY, -1);

        // Draw the model
        pushMatrix();
        pushStyle();
        translate(width / 2f, height / 2f);
        rotateX(TWO_PI * (controlHandPos.getY() - 150f) / 150f);
        rotateY(-HALF_PI + TWO_PI * controlHandPos.getX() / 150f);
        scale(200f / (200f + controlHandPos.getZ()));
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
