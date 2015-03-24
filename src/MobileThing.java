import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

public class MobileThing
{
  TextureMy textureMy;
  //int texture_width;
  //int texture_height;

  double xcoord;
  double ycoord;
  double zcoord;
  double prev_xcoord;
  double prev_ycoord;
  double prev_zcoord;

  double xspeed; //positive is right
  double yspeed; //positive is up
  double zspeed; //positive z is towards camera.

  double mass_kg;
  double density;
  double elasticity; //see http://en.wikipedia.org/wiki/Young%27s_modulus and http://www.kayelaby.npl.co.uk/general_physics/2_2/2_2_2.html
  //very bouncy rubber 0.01 GPa, pine 9, lead 16, steel 210 GPa
  double friction = 0.001; //dummy value
  public boolean is_inside_thing = false;
  public int count_touching_others = 0;
  
  int pixelradius = 4;  
  long unique_id = 0;
  static long next_uid = 0;

  int corenote = 80; // MIDI note 0..127, asteroids and ships low, bonuses high, missiles nothing.
  int MIDI_instrument = 1; //default acoustic piano

  public String getString() {
    return this.getClass().toString() + " Id "+unique_id + " speed "+getSpeed() + " XYdir "+getBearingXY();
  }
  public double getMaxRollingSpeed() {
    return 19000.0 / friction;
  }
  public double getMaxAirSpeed() {
    return 19000.0 / friction;
  }

  public void setTexture(String filename, double scaling) {
    try {
      textureMy = PeggMachine.getTextureLoader().getTexture(filename);
      //texture_width = (int) Math.round (scaling * textureMy.getImageWidth());
      //texture_height = (int) Math.round (scaling * textureMy.getImageHeight());
    } catch (IOException e) {
      System.out.println("MobileThing: Unable to load texture file: "+filename);
      e.printStackTrace();
    }

    //float mcolor[] = { 1.0f, 0.0f, 0.0f, 1.0f };
    //glMaterialfv(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, mcolor);



  }
  public void setTextureScaling(double scaling) {
    //NOT WORKING



    //int neww = (int) Math.round(scaling * getTextureWidth());
    //int newh = (int) Math.round(scaling * getTextureHeight());
    //textureMy.setWidth(neww);
    //textureMy.setHeight(newh);
  }
  public int getTextureWidth() {
    return textureMy.getImageWidth();
  }
  public int getTextureHeight() {
    return textureMy.getImageHeight();
  }
  public double getKineticEnergy() {
    //speed is in sort-of-pixels per second. Let's say 1000px is two meters.

    return 0.5 * mass_kg * (getSpeed()/500.0) * (getSpeed()/500.0);
  }
  public double getMomentum() {
    return mass_kg * (getSpeed()/500.0);
  }

  public double getSpeed() {
    return Math.sqrt(xspeed*xspeed + yspeed*yspeed + zspeed*zspeed);
  }
  public double getBearingXY() {
    //xxx unverified

    double dir_rad = Math.atan2(yspeed, xspeed); //out comes -pi to +pi
    if (dir_rad < 0)
      dir_rad = Math.abs(dir_rad);
    else
      dir_rad = 2*Math.PI - dir_rad;
    //System.out.println("dir_rad = Math.atan2(yspeed, xspeed) = " + dir_rad);
    return dir_rad;
  }
  public void reduceSpeed(double s) {
    xspeed = xspeed * s;
    yspeed = yspeed * s;
    zspeed = zspeed * s;
  }
  public double getMass() {
    return mass_kg;
  }
  public int getPixelRadius() {
    return pixelradius; //a hack, for attached Vfxs
  }
  public long getId() {
    return unique_id;
  }
  public static long getNextId() {
    next_uid++;
    return next_uid;
  }
  public double getElasticity() {
    return elasticity;
  }
  public double getX() {
    return xcoord;
  }
  public double getY() {
    return ycoord;
  }
  public double getZ() {
    return zcoord;
  }

  public void setX(double a) {
    xcoord = a;
  }
  public void setY(double a) {
    ycoord = a;
  }
  public void setZ(double a) {
    zcoord = a;
  }
  public void setXSpeed(double a) {
    xspeed = a;
  }
  public void setYSpeed(double a) {
    yspeed = a;
  }
  public void setZSpeed(double a) {
    zspeed = a;
  }
  public double getXSpeed() {
    return xspeed;
  }
  public double getYSpeed() {
    return yspeed;
  }
  public double getZSpeed() {
    return zspeed;
  }

  public void setCoreNote(int a) {
    corenote = a;
  }
  public int getCoreNote() {
    return corenote;
  }
  public void setMIDIInstrument(int a) {
    MIDI_instrument = a;
  }
  public int getMIDIInstrument() {
    return MIDI_instrument;
  }
  public void playNote(int offset, int volume, double dur_ticks) {
    PeggMachine.melodyNotesPileAdd(new StampedNote(PeggMachine.getWorldTime() /*now*/, MIDI_instrument, corenote + offset, volume, dur_ticks, false));
  }
  public void playNoteDelayed(int offset, int volume, double dur_ticks, int wait) { //please, offset -10 to 10, volume 10..127
    PeggMachine.melodyNotesPileAdd(new StampedNote((PeggMachine.getWorldTime() +wait) /*now plus delay*/, MIDI_instrument, corenote + offset, volume, dur_ticks, false));
  }
  public void setSpeed(double x, double y, double z) {
    xspeed = x;
    yspeed = y;
    zspeed = z;
  }
  public void setSpeed_mag_dirXY(double mag, double dir /*0..2PI*/) {
    xspeed = mag * Math.cos(dir);
    yspeed = mag * Math.sin(dir);
    zspeed = 0.0;
  }
  public double getDirXY() { //for setSpeed_KE_dirXY()
    return PeggMachine.calcBearingXY(xcoord, ycoord, xcoord+xspeed, ycoord+yspeed);
  }
  public void setSpeed_KE_dirXY(double kinetic_energy, double dir /*0..2PI*/) {
    //speed is in sort-of-pixels per second. Let's say 1000px is two meters.
    // 0.5 * mass_kg * (getSpeed()/500.0) * (getSpeed()/500.0);


    double spe = 500.0 * Math.sqrt((2*kinetic_energy)/mass_kg); // two meters is 1000 distance.
    xspeed = spe * Math.cos(dir);
    yspeed = spe * Math.sin(dir);
    zspeed = 0.0;
  }
  public static double calcDistanceXY(MobileThing a, MobileThing b) {
    return Math.sqrt( (a.getX() - b.getX())
                         *(a.getX() - b.getX()) +
                          (a.getY() - b.getY())
                         *(a.getY() - b.getY()) );
  }
  public static double calcDistanceXYZ(MobileThing a, MobileThing b) {
    return Math.sqrt( (a.getX() - b.getX())*(a.getX() - b.getX())
            +  (a.getY() - b.getY())*(a.getY() - b.getY())
            + (a.getZ() - b.getZ())*(a.getZ() - b.getZ()) );
  }



  public static boolean isPegPegCollision(Peg o, Peg p) {
    double deltaX = p.getX() - o.getX();
    double deltaY = p.getY() - o.getY();
    double deltaZ = p.getZ() - o.getZ();

    double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

    if (dist < 1.02*o.getRadius() + 1.05*p.getRadius()) {
      return true;
    } else {
      return false;
    }

  }

  public static boolean isOrbPegCollision(Orb o, Peg p) {
    double deltaX = p.getX() - o.getX();
    double deltaY = p.getY() - o.getY();
    double deltaZ = p.getZ() - o.getZ();

    double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

    if (dist < 0.98*o.getRadius() + 0.93*p.getRadius()) {
      return true;
    } else {
      return false;
    }

  }
  public static boolean isOrbOrbCollision(Orb o, Orb p) {
    double deltaX = p.getX() - o.getX();
    double deltaY = p.getY() - o.getY();
    double deltaZ = p.getZ() - o.getZ();

    double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

    if (dist < 0.99*o.getRadius() + 0.99*p.getRadius()) {
      return true;
    } else {
      return false;
    }

  }
 /* public boolean isOrbTargetCollision(Orb o, Target t) {
    double deltaX = t.getX() - o.getX();
    double deltaY = t.getY() - o.getY();
    double deltaZ = t.getZ() - o.getZ();

    double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

    if (dist < o.getRadius() + t.getRadius()) {
      return true;
    } else {
      return false;
    }

  }*/

  public MobileThing collisioncode(MobileThing a) { //a vs all objects in the world. a is usually orb.
    MobileThing retmob = null; // return the obj we collided with.
    double elas_factor = 0.0;
    a.count_touching_others = 0; //might touch 2 peg 3-4 orbs at once.
    double zed_rollout = 9.0; //roll towards camera to solve crowded.

    //xxx replace xspeed wordings with a.getxspeed.....

    //xxx if a aint orb, crasssssshhhhhhhh



    //with pegs
    Vector li = PeggMachine.getPegList();
    try {
      Peg pe;
      int bsiz = li.size();
      if ( bsiz > 0) {
        for (int i = 0; i < bsiz; i++) {
          pe = (Peg) li.elementAt(i);
          if (pe != null) {

            if (a.getClass().getName().equals("Orb")) {
            if (isOrbPegCollision((Orb)a, pe)) {
              retmob = pe;
              a.count_touching_others++;
              //System.out.println("Orb-Peg: before collision: a.Ek "+a.getKineticEnergy());
              elas_factor = (1.000001/(1.0+pe.getElasticity()));
              if (elas_factor > 1.0) {
                elas_factor = 1.0;
                System.out.println("Error: Orb on peg impact, elas_factor over unity");
              }

              //this.playNote(-8, 20, 2);
              int str = (int) Math.round( (a.getSpeed()/2250 /*a.getMaxRollingSpeed()*/) * 95 );

              if (str > 6)
                pe.playNoteOrbHitYou(str/2);

              PeggMachine.addVfx((pe.getX() + a.getX())/2.0,   (pe.getY() + a.getY())/2.0,   45.0+(pe.getZ() + a.getZ())/2.0,
                      "HIT_PEG", 1000, 21.0, null, "32texturelimex.png", 1.0);




              if (this.getX() < pe.getX()) { //if hits left side of peg (peg's "top" surface)
                //xspeed = xspeed + 0.2*PeggMachine.getGravity();
                xspeed = -getSpeed() * 0.45 * elas_factor;
              } else { //right
                //xspeed = xspeed - 0.2*PeggMachine.getGravity();
                xspeed = getSpeed() * 0.45 * elas_factor;
              }
              //oh, it is the gravity that keeps orbs moving atop pegs. x-wobble above.





              if (xspeed > 920.0)
                xspeed = 920.0;
              if (xspeed < -920.0)
                xspeed = -920.0;

             //xxxx xspeed change should be of collision angle and peg elasticity.


              a.setSpeed(getXSpeed(), (-getYSpeed()*0.95 * elas_factor), getZSpeed()); //was: horrible bug, ydir didn't change...

              //System.out.println("Orb-Peg: after collision: a.Ek "+a.getKineticEnergy());



              //while??
              if (this.getY() > pe.getY() && isOrbPegCollision((Orb)a,pe)) { //prevent "be inside peg". rise to top.
                //reset to previous location
                if (!a.is_inside_thing) {
                  xcoord = prev_xcoord;
                  ycoord = prev_ycoord;
                  zcoord = prev_zcoord;
                }

                ycoord = ycoord + 1.5;
                //a.reduceSpeed(0.90);
                a.is_inside_thing = true;
                a.setZ(a.getZ() + 4.0); //roll towards camera.
                a.setZSpeed(a.getZSpeed() + zed_rollout);

              }
              if (this.getY() < pe.getY() && isOrbPegCollision((Orb)a,pe)) { //prevent "be inside peg". to bottom.

                //reset to previous location
                if (!a.is_inside_thing) {
                  xcoord = prev_xcoord;
                  ycoord = prev_ycoord;
                  zcoord = prev_zcoord;
                }

                ycoord = ycoord - 1.5;

                //a.reduceSpeed(0.90);
                a.is_inside_thing = true;
                a.setZ(a.getZ() + 4.0); //roll towards camera.
                a.setZSpeed(a.getZSpeed() + zed_rollout);

              }

              System.out.println("Orb on peg impact int " + str + " and orb speed was "+ a.getSpeed() + " and resulting XY bearing is " + a.getBearingXY());
            }
            }
          }
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }


    //with other orbs
    li = PeggMachine.getOrbList();
    try {
      Orb b;
      int bsiz = li.size();
      if ( bsiz > 0) {
        for (int i = 0; i < bsiz; i++) {
          b = (Orb) li.elementAt(i);
          if (a.getClass().getName().equals("Orb")) {
          if (b != null && b != a) { //dont self-self collide.
            if (isOrbOrbCollision((Orb) a,b)) {
              elas_factor = (1.000001/(1.0+b.getElasticity()));
              if (elas_factor > 1.0) {
                elas_factor = 1.0;
                System.out.println("Error: Orb on orb impact, elas_factor over unity");
              }

              System.out.println("Orb-Orb: before collision: a.Ek "+a.getKineticEnergy()+ " b.Ek "+b.getKineticEnergy()
                      + " total "+(a.getKineticEnergy()+b.getKineticEnergy()));
              retmob = b;
              a.count_touching_others++;

              int str = (int) Math.round( (a.getSpeed()/2250 /*a.getMaxRollingSpeed()*/) * 95 );
              //System.out.println("Orb on orb impact int " + str + " and active orb speed was "+ a.getSpeed());
              //public void playNoteOrbHitYou(int strength/*0..95*/) {
              int vol = 30+str;
              if (vol > 127)
                vol = 127;
              if (vol > 36)
                PeggMachine.putMIDINote(98 /*FX:crystal*/, 45, false, vol, /*sn.dur_ticks, 0.0,*/ true/*foll noteoff*/, true);

              double total_ke = a.getKineticEnergy() + b.getKineticEnergy();
              //gain xspeed, because gravity pulling while resting(or not) on fellow smooth orb.
              if (a.getX() < b.getX()) {
                a.setSpeed(a.getXSpeed() + 0.1*PeggMachine.getGravity() * Math.cos(PeggMachine.calcBearing(b,a)), a.getYSpeed(), a.getZSpeed());
              } else {
                a.setSpeed(a.getXSpeed() - 0.1*PeggMachine.getGravity() * Math.cos(PeggMachine.calcBearing(b,a)), a.getYSpeed(), a.getZSpeed());

              }





              //xspeed = getSpeed() * 0.45 + (0.01/pe.getElasticity());
              //if (this.getX() < b.getX())
///                xspeed = xspeed - 0.23*getSpeed();
//              else
//                xspeed = xspeed + 0.23*getSpeed();
              if (a.getXSpeed() > 920.0)
                a.setXSpeed(920.0);
              if (a.getXSpeed() < -920.0)
                a.setXSpeed(-920.0);

//              yspeed = -(this.getSpeed() + b.getSpeed()) / 2.3;
              //b.reduceSpeed(0.6);

              //a.setSpeed(getXSpeed(), (-getYSpeed()*0.95 * elas_factor), getZSpeed());

              //xxxxxx use momentum
              a.setSpeed_KE_dirXY(     total_ke/3.8, PeggMachine.calcBearingXY(b.getX(), b.getY(), a.getX(), a.getY() )); //from b to a dir.
              if (a.getKineticEnergy() > 0.05)
                b.setSpeed_KE_dirXY(     total_ke/3.8, PeggMachine.calcBearingXY(a.getX(), a.getY(), b.getX(), b.getY() )); //from b to a dir. //PeggMachine.calcBearingXY(a.getX(), a.getY(), b.getX(), b.getY() )
              else //a puny, uhh what
                b.setSpeed_KE_dirXY(     b.getKineticEnergy() * 0.75, PeggMachine.calcBearingXY(a.getX(), a.getY(), b.getX(), b.getY() )); //from b to a dir.


              //while??
              if (a.getY() > b.getY() && isOrbOrbCollision((Orb)a,b)) { //prevent "be inside orb".
                a.setY(a.getY() + 0.9);
                //a.reduceSpeed(0.90);
                //b.reduceSpeed(0.90);
                a.is_inside_thing = true;
                a.setZ(a.getZ() + 4.0);
                a.setZSpeed(a.getZSpeed() + zed_rollout);
              }
              if (a.getY() < b.getY() && isOrbOrbCollision((Orb)a,b)) { //prevent "be inside orb".
                //ycoord = ycoord + 1.5; //slide up, not down through things.
                //a.reduceSpeed(0.90);
                //b.reduceSpeed(0.90);
                a.is_inside_thing = true;
                a.setZ(a.getZ() + 4.0);
                a.setZSpeed(a.getZSpeed() + zed_rollout);
              }

              System.out.println("Orb-Orb: after collision: a.Ek "+a.getKineticEnergy()+ " b.Ek "+b.getKineticEnergy()
                      + " total "+(a.getKineticEnergy()+b.getKineticEnergy()));

            }
          }
          }
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }



    //with targets
    li = PeggMachine.getTargetList();
    try {
      Target tar;
      int bsiz = li.size();
      for (int i = 0; i < bsiz; i++) {
        tar = (Target) li.elementAt(i);
        if (tar != null) {



          if (a.getClass().getName().equals("Orb")) {
          if (tar.isOrbCollision((Orb) a) && tar.canCollide()) {
            retmob = tar;
            PeggMachine.changeScore(tar.getScoringvalue());
            tar.setDislodged(); //it is dislodged, it tumbles down.
            tar.setZ(a.getZ() + 2.0);
            tar.setSpeed_mag_dirXY(a.getSpeed()*0.9, -Math.PI/2.0);

            /*bsiz = li.size();
            if (a.getX() < tar.getX())
              a.setXSpeed(a.getXSpeed() - 0.23*a.getSpeed());
            else
              a.setXSpeed(a.getXSpeed() + 0.23*a.getSpeed());
            if (a.getXSpeed() > 120.0)
              a.setXSpeed(120.0);
            if (a.getXSpeed() < -120.0)
              a.setXSpeed(-120.0);

            a.setYSpeed( -(a.getSpeed()) / 3.3 );
            */
            a.reduceSpeed(0.85);




          }
          }
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }






    //with play-area walls???
    if (a.getY() < -800.0 && a.getClass().getName().equals("Orb")) {
      PeggMachine.removeOrbFromPlay((Orb) a);
    }

    //nerf wobbling by reducing speed. But gravity gives more speed.
    if (count_touching_others > 1)
      reduceSpeed(0.96);
    if (count_touching_others > 2)
      reduceSpeed(0.85);

    //give my new coords to TRAIL object //done as Vfx in Orb's movement.




   return retmob;
  }


}