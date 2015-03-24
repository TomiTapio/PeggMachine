import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

import java.io.IOException;
import java.util.Vector;

import static org.lwjgl.opengl.GL11.glNormal3f;

public class FlatSprite extends MobileThing {
  double width, height, texturescaling;

  public FlatSprite(double in_width, double in_height, double x, double y, double z, String texturefilename, double in_texturescaling) {
    xcoord = x;
    ycoord = y;
    zcoord = z;
    xspeed = 0.0;
    yspeed = 0.0;
    zspeed = 0.0;

    width = in_width;
    height = in_height;
    mass_kg = 0.0;
    unique_id = MobileThing.getNextId();
    //xxx verify inputs

    corenote = 90; // MIDI note 0..127
    MIDI_instrument = 1; //default acoustic piano

    setTexture(texturefilename, 1.0);
    texturescaling = in_texturescaling;
  }
  public void advance_time () {
    int wti = PeggMachine.getWorldTimeIncrement();
    if (wti < 1) return; //zero means pause

    yspeed = yspeed + 0.1*(PeggMachine.getGravity() * (wti/1000.0));
    //move
    xcoord = xcoord + (xspeed * (wti/1000.0));
    ycoord = ycoord + (yspeed * (wti/1000.0));
    zcoord = zcoord + (zspeed * (wti/1000.0));
  }
  public void drawFlatSprite() { //preferably has much alpha channel, to appear non-square.
    // enable texture
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.

    //setMaterial();

    if (textureMy != null)
      textureMy.bind();
    //System.out.println("Trying to draw sprite at "+xcoord + "  " + ycoord + "  " +zcoord);
    GL11.glPushMatrix();

    GL11.glColor4f(0.9f, 0.9f, 1.0f, 1.0f);
    GL11.glTranslated(xcoord, ycoord, zcoord);

    GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2d(0, 0);
      GL11.glVertex2d(0, 0);
      GL11.glNormal3d(0.0, 0.0, 1.0);
      GL11.glTexCoord2d(0, texturescaling * textureMy.getHeight());
      GL11.glVertex2d(0, height);
      GL11.glNormal3d(0.0, 0.0, 1.0);
      GL11.glTexCoord2d(texturescaling * textureMy.getWidth(), texturescaling * textureMy.getHeight());
      GL11.glVertex2d(width, height);
      GL11.glNormal3d(0.0, 0.0, 1.0);
      GL11.glTexCoord2d(texturescaling * textureMy.getWidth(), 0);
      GL11.glVertex2d(width, 0);
      GL11.glNormal3d(0.0, 0.0, 1.0);
    GL11.glEnd();

    GL11.glPopMatrix();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
  }
  public static void drawFlatLine(double x1, double y1, double z1, double x2, double y2, double z2, double width) {




  }

}
