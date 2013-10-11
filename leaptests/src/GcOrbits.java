import processing.core.PApplet;
import processing.core.PVector;
import processing.data.Table;

public class GcOrbits extends PApplet {
    private static final long serialVersionUID = 1L;

    private BlackHole gcBH;
    private Star[] gcStars;
    private float timeStep = 3 * 3600;
    private int nSteps = 50;
    private boolean drawTrails = false;
    private float zoom = 1;

    public void setup() {
        size(1000, 800, P3D);

        // We will re-scale the spatial dimensions by the following factor
        float scaling = 2.5e-10f;

        // Set the galactic center black hole properties
        PVector bhPos = new PVector(width / 2, height / 2, 0);
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
    }

    public void draw() {
        background(0);

        // Update the stellar coordinates
        for (int i = 0; i < gcStars.length; i++) {
            gcStars[i].update(timeStep, nSteps, zoom);
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
                gcBH.draw(zoom);
                gcBHDrawn = true;
            }

            // Draw the star
            gcStars[starIndex].draw(zoom);
            gcStarDrawn[starIndex] = true;
            starCounter++;
        }

        // Draw the stellar trails
        if (drawTrails) {
            hint(DISABLE_DEPTH_TEST);

            for (int i = 0; i < gcStars.length; i++) {
                gcStars[i].drawTrail(zoom);
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
}
