import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;
import org.lwjgl.util.glu.GLU;

public class Target extends MobileThing {

  double xsize, ysize, y_rota;
  boolean immobile = true;
  boolean cancollide = true;
  int scoringvalue = 1;




  public Target(double xsize1, double ysize1, double x, double y, double z, String texturefilename) {
    xcoord = x;
    ycoord = y;
    zcoord = z;
    xspeed = 0.0;
    yspeed = 0.0;
    zspeed = 0.0;
    xsize = xsize1;
    ysize = ysize1;
    y_rota = 0.0;

    setTexture(texturefilename, 1.0);
  }
  public void setDislodged() {
    immobile = false;
    cancollide = false;
    PeggMachine.melodyNotesPileAdd(new StampedNote(PeggMachine.getWorldTime() /*now*/, 115, 55+PeggMachine.gimmeRandInt(3), 50, 0.3, false));
    PeggMachine.changeScore(getScoringvalue());
  }
  public boolean canCollide() {
    return cancollide;
  }


  public int getScoringvalue() {
    return scoringvalue;
  }
  public double getMaxSpeed() {
    return 0.6 * Math.abs(PeggMachine.getGravity());
  }


  public void advance_time () {
    int wti = PeggMachine.getWorldTimeIncrement();
    if (wti < 1) return; //zero means pause

    //move
    if (!immobile) {
      yspeed = yspeed + (PeggMachine.getGravity() * (wti/1000.0));
      y_rota = y_rota + 0.01*yspeed;

      if (getSpeed() > this.getMaxSpeed())
      reduceSpeed(0.90);

      xcoord = xcoord + (xspeed * (wti/1000.0));
      ycoord = ycoord + (yspeed * (wti/1000.0));
      zcoord = zcoord + (zspeed * (wti/1000.0));

      if (ycoord < -500.0)
        PeggMachine.removeTarget(this);
    } else {

      //xxx occasional shimmer VFX.
    }

    //xxxxxxxxxxxx collisions, cascades
    //this.collisioncode(this);
  }


  public void drawTarget() {
    Cylinder cyl = new Cylinder();

    // enable texture
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.

    PeggMachine.setMaterial("NOSPECULAR");

    if (textureMy != null)
      textureMy.bind();
    //GL11.glBindTexture(GL11.GL_TEXTURE_2D, 1);

    cyl.setNormals(GLU.GLU_SMOOTH);
    cyl.setTextureFlag(true); // "specifies if texture coordinates should be generated for quadrics rendered with qobj."

    GL11.glPushMatrix();
    GL11.glColor4f(0.9f, 0.9f, 1.0f, 1.0f);
    //GL11.glRotatef((float) (1+(ycoord/199.0))/20, 0f, 0f, 1f);
    GL11.glTranslated(xcoord, ycoord, zcoord);
    GL11.glScaled(xsize/ysize, 1.0, 1.0);
    GL11.glRotated(90, 2.0, 0.0, 0.0);
    GL11.glRotated(90, 0.0, 0.0, 1.0);

    if (getSpeed() > 0.01) {
      //GL11.glRotated(initial_rotation_angle, 0.0, 0.0, 1.0);
      y_rota = yspeed/20.0;
      GL11.glRotated(y_rota, 0.0, 1.0, 0.0);
      //GL11.glRotated(2*rota, 0.0, 0.0, 1.0);
      GL11.glTranslated(0.0, 0.0, yspeed/10.0);
    }
    cyl.setDrawStyle(GLU.GLU_FILL); //fill, line, silhouette, point
    //s.setOrientation();
    cyl.draw((float) xsize, (float) (xsize * 0.8), 30.0f, 12, 5);

    GL11.glPopMatrix();
    GL11.glDisable(GL11.GL_TEXTURE_2D);

  }

  public boolean isOrbCollision(Orb o) {
    if (calcDistanceXY(this, o) < (xsize/2.0 +o.getRadius() ))
      return true;
    else
      return false;

  }
  public boolean wouldThisPegTouchYou(Peg p) {
    if (calcDistanceXY(this, p) < (xsize*1.25 +p.getRadius() ))
      return true;
    else
      return false;
  }



}
