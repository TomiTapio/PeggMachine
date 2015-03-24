import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

import java.io.IOException;
import java.util.Vector;

/**
 * Date: 2.11.2013
 */
public class Orb extends MobileThing {
  double radius;
  double initial_rotation_angle;
  boolean immobile = false;
  int num_peg_collisions = 0;
  int tracerticks = 0;

  public Orb(double mass, double ra, double ela, double fric, double x, double y, double z, String texturefilename) {
    xcoord = x;
    ycoord = y;
    zcoord = z;
    xspeed = 0.0; //total speed is 20 - 2250 apparently, when gravity is -2550.0
    yspeed = 0.0;
    zspeed = 0.0;
    initial_rotation_angle = PeggMachine.gimmeRandDouble() * 360.0;

    mass_kg = mass;
    elasticity = ela;
    friction = fric;

    radius = ra;
    pixelradius = 40; //xxxx

    unique_id = MobileThing.getNextId();
    //xxx verify inputs

    corenote = 80; // MIDI note 0..127
    MIDI_instrument = 1; //default acoustic piano

    setTexture(texturefilename, 1.0);
    //setTextureScaling(0.2 + 0.8*PeggMachine.gimmeRandDouble());
    //setTextureScaling(0.01);

    tracerticks = PeggMachine.gimmeRandInt(6); //not have all orbs in same tracer-dropping sync!
  }

  public void setGlued(boolean b) {
    immobile = b;
  }
  public void advance_time () {
    int wti = PeggMachine.getWorldTimeIncrement();
    if (wti < 1) return; //zero means pause

    //gravity changes this orb's speed. gravity is -y axis
    yspeed = yspeed + (PeggMachine.getGravity() * (wti/1000.0));

    //xxx possible wind changes xspeed.


    //was bad on launch, immediate slowdown on exiting launcher...
    //if (getSpeed() > getMaxRollingSpeed())
//      reduceSpeed(2.0);

    //move
    if (!immobile) {
      xcoord = xcoord + (xspeed * (wti/1000.0));
      ycoord = ycoord + (yspeed * (wti/1000.0));
      zcoord = zcoord + (zspeed * (wti/1000.0));
      prev_xcoord = xcoord;
      prev_ycoord = ycoord;
      prev_zcoord = zcoord;

      //if (getSpeed() > 0.5*getMaxRollingSpeed())
      //if (Math.abs(yspeed) > 950.0) {
        //if (PeggMachine.gimmeRandDouble() < 0.25)
        if (tracerticks == 4) {
          PeggMachine.addVfx(xcoord, ycoord, zcoord+5.0, "TRACER", 1000, 11.0, null, "32texturecyanx.png", 1.0);
          tracerticks = 0;
          is_inside_thing = false; //periodic reset.
        } else
          tracerticks++;
      //}
    }
    //System.out.println("new orb coords: "+xcoord + "  " + ycoord + "  " +zcoord);

    //check if collide
    MobileThing retmob = collisioncode(this);
    if (retmob != null) {
      if (retmob.getClass().toString().equals("Peg") ) {
        num_peg_collisions++;
      }
    }
  }

  public double getRadius() {
    return radius;
  }

  public boolean areCoordsInsideYou(Vector XYZ) { //easy on a sphere.
    double in_x = (Double) XYZ.get(0);
    double in_y = (Double) XYZ.get(1);
    double in_z = (Double) XYZ.get(2);
    double dist =  Math.sqrt( (in_x - xcoord) * (in_x - xcoord) +  (in_y - ycoord) * (in_y - ycoord) + (in_z - zcoord) * (in_z - zcoord) );
    if (dist < this.radius) {
      return true;
    } else
      return false;
  }


  public void drawOrb() {
    Sphere s = new Sphere();

    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.
    if (textureMy != null)
      textureMy.bind(); //GL11.glBindTexture(GL11.GL_TEXTURE_2D, 1);
    s.setNormals(GLU.GLU_SMOOTH);
    s.setTextureFlag(true); // "specifies if texture coordinates should be generated for quadrics rendered with qobj."

    PeggMachine.setMaterial("BASIC");



    GL11.glPushMatrix();
    GL11.glColor4f(0.9f, 0.85f, 0.8f, 1.0f); //change alpha for ghostly orbs.

    //GL11.glRotatef((float) (1+(ycoord/199.0))/20, 0f, 0f, 1f);

    GL11.glTranslated(xcoord, ycoord, zcoord);
    GL11.glRotated(initial_rotation_angle, 0.0, 0.0, 1.0);
    GL11.glRotated(initial_rotation_angle, 1.0, 0.0, 0.0);
    double rota = yspeed/20.0;
    GL11.glRotated(rota, 0.0, 1.0, 0.0);

    s.setDrawStyle(GLU.GLU_FILL); //fill, line, silhouette, point
    //s.setOrientation();
    s.draw((float)radius, 27,27);
    //PeggMachine.drawString("fooooooooooooooooooooooooooooooooooooooooooo");
    GL11.glPopMatrix();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
  }

}
