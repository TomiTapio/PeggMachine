import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

import java.io.IOException;

public class Launcher extends MobileThing {

  double how_often;
  double timevariance;
  double minvelo;
  double maxvelo;
  double xsize, ysize, orbrad, launchdir;

  int prev_launch_worldtime = 0;




  public Launcher(double xsize1, double ysize1, double t1, double oftenvari, /*double orbrad_in*/ double minv, double maxv,
                  double x, double y, double z, double in_launchdir, String texturefilename) {
    xcoord = x;
    ycoord = y;
    zcoord = z;
    xspeed = 0.0;
    yspeed = 0.0;
    zspeed = 0.0;
    xsize = xsize1;
    ysize = ysize1;

    orbrad = xsize1 - 1.0;

    how_often = t1;
    timevariance = oftenvari;
    minvelo = minv;
    maxvelo = maxv;
    launchdir = in_launchdir;
    mass_kg = 15.0;


    unique_id = MobileThing.getNextId();
    //xxx verify inputs

    corenote = 80; // MIDI note 0..127
    MIDI_instrument = 1; //default acoustic piano

    setTexture(texturefilename, 1.0);


  }

  public void advance_time() {
    if (PeggMachine.getWorldTimeIncrement() < 1) return; //zero means pause

    if (((long)PeggMachine.getWorldTime()) > (prev_launch_worldtime + how_often) ) {
      //use timevariance


      if (PeggMachine.getOrbList().size() < 40) {
        //FlatSprite fs = new FlatSprite(90,70, xcoord+(xsize/2), ycoord+(ysize), zcoord, "gneiss.png");
        //fs.setSpeed(-70.0, minvelo + PeggMachine.gimmeRandDouble() * (maxvelo - minvelo), 0.0);

        Orb o = new Orb(4.0,orbrad,27.0/*ela*/,500.0/*fric*/, xcoord+(xsize/2.9)-6.0, ycoord+(ysize), zcoord, "mohave_marble_mywrap.png");
        //o.setSpeed(-70.0, minvelo + PeggMachine.gimmeRandDouble() * (maxvelo-minvelo), 0.0);
        o.setSpeed_mag_dirXY(minvelo + PeggMachine.gimmeRandDouble() * (maxvelo-minvelo), launchdir);
        PeggMachine.addToOrbList(o);
        //PeggMachine.addToFSList(fs);
        //System.out.println("Launched orb at time "+PeggMachine.getWorldTime() + " it has Ek "+o.getKineticEnergy());
        prev_launch_worldtime = (int)PeggMachine.getWorldTime();
        //play launch sound, 105 banjo
        PeggMachine.melodyNotesPileAdd(new StampedNote(PeggMachine.getWorldTime() /*now*/, 105, 42, 40, 0.4, false));
      }
    }
  }

  public void drawLauncher() {
    //Sphere s = new Sphere();
    Cylinder s = new Cylinder();
    // enable texture
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    //glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.

    PeggMachine.setMaterial("NOSPECULAR");

    if (textureMy != null)
      textureMy.bind();
    s.setNormals(GLU.GLU_SMOOTH);
    s.setTextureFlag(true); // "specifies if texture coordinates should be generated for quadrics rendered with qobj."

    //System.out.println("Trying to draw Launcher at "+xcoord + "  " + ycoord + "  " +zcoord);
    GL11.glPushMatrix();

    GL11.glColor3f(1.0f, 1.0f, 1.0f);
    GL11.glLoadIdentity();
    GL11.glTranslated(xcoord, ycoord, zcoord);
    GL11.glTranslated(0, ysize, 0);


    GL11.glScaled(1.2,1.9,1.0);
    GL11.glRotated(90, 1.0, 0.0, 0.0);
    GL11.glRotated(90, 0.0, 0.0, 1.0);



    //sphere: s.draw((float)xsize, 42,42);
    s.draw((float)xsize, (float)xsize, (float) ysize, 15,15);
    GL11.glPopMatrix();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
  }


  public boolean areCoordsTooNearYou(int mouse_x, int mouse_y, double v, double peg_radius) {
    double dist = Math.sqrt( (mouse_x - xcoord) * (mouse_x - xcoord) +  (mouse_y - ycoord) * (mouse_y - ycoord) + (v - zcoord) * (v - zcoord) );

    double ydist = Math.abs(mouse_y - ycoord);
    double xdist = Math.abs(mouse_x - xcoord);
    if ((xdist < (peg_radius/2.0 + 1.3*xsize))
      && (ydist < (peg_radius + 2.2*ysize)))
      return true;

    return false;
/*
    if (dist < 2.0*(this.ysize + this.xsize)) {
      return true;
    } else
      return false;
*/
  }
}
