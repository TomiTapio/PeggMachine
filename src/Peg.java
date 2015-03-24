import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

public class Peg extends MobileThing { //a non-mineral cylinder that player places. Leather.
  //they're not spheres! more like half-spheres.
  double radius;
  double cylheight;


  public Peg(double mass, double ra, double ela, double x, double y, double z, String texturefilename, double texturescaling) {
    xcoord = x;
    ycoord = y;
    zcoord = z;
    xspeed = 0.0; //never moves, never call setSpeed on a peg.
    yspeed = 0.0;
    zspeed = 0.0;

    mass_kg = mass;
    elasticity = ela;

    radius = ra;
    cylheight = 40.0;
    pixelradius = 40; //xxxx

    unique_id = MobileThing.getNextId();
    //xxx verify inputs

    corenote = 54; // MIDI note 0..127
    MIDI_instrument = 1; //default acoustic piano

    setTexture(texturefilename, texturescaling);

  }
  public double getRadius() {
    return radius;
  }

  public void playNoteYouGotPlaced() {
    playNote(2,15,4);


  }

  public void playNoteOrbHitYou(int strength/*0..95*/) {
    int vol = 30+strength;
    if (vol > 127)
      vol = 127;
    PeggMachine.putMIDINote(47 /*timpani*/, 45, false, vol, /*sn.dur_ticks, 0.0,*/ false/*foll noteoff*/, true);
  }

  public boolean areCoordsInsideYou(double in_x, double in_y, double in_z) {
    double dist = Math.sqrt( (in_x - xcoord) * (in_x - xcoord) +  (in_y - ycoord) * (in_y - ycoord) + (in_z - zcoord) * (in_z - zcoord) );
    if (dist < this.radius) {
      return true;
    } else
      return false;
  }

  public boolean wouldThisPegTouchYou(Peg p) {
    if (calcDistanceXY(this, p) < (radius*0.98 +p.getRadius() ))
      return true;
    else
      return false;
  }


  public void drawPeg() {
    Sphere s = new Sphere();

    // enable texture
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    //glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.

    PeggMachine.setMaterial("BASIC");

    if (textureMy != null)
      textureMy.bind();
    //GL11.glBindTexture(GL11.GL_TEXTURE_2D, 1);

    s.setNormals(GLU.GLU_SMOOTH);
    s.setTextureFlag(true); // "specifies if texture coordinates should be generated for quadrics rendered with qobj."

    GL11.glPushMatrix();
    GL11.glColor4f(0.9f, 0.9f, 1.0f, 1.0f);
    //GL11.glRotatef((float) (1+(ycoord/199.0))/20, 0f, 0f, 1f);

    GL11.glTranslated(xcoord, ycoord, zcoord);
    //GL11.glRotated((100 + ycoord+xcoord) / 90.0, 1.0, 0.5, 0.1);

    //s.setDrawStyle(GLU.GLU_FILL); //fill, line, silhouette, point
    //s.setOrientation();
    s.draw((float)radius, 16,16);
    GL11.glPopMatrix();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
  }


}
