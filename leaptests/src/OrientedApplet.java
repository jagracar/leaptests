import processing.core.PApplet;

public class OrientedApplet extends PApplet {
	private static final long serialVersionUID = 1L;

	protected float xAng = 0;
	protected float yAng = 0;
	protected float zoom = 1;

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
