import org.lwjgl.opengl.GL11;
import java.io.IOException;

/* creation date: 15.6.2012 for TomiTapio's Missilemada */
public class Vfx { //similar to FlatSprite but mostly doesn't move, and expires at designated time.
  double xcoord, ycoord, zcoord;
  double width, height, texturescaling;
  TextureMy textureMy = null;
  boolean hastexture = false;
  double sizeness;
  long expiration_time; //in worldtime milliseconds when to delete.
  String effect;
  MobileThing attachedTo = null;
  int host_x = 0;
  int host_y = 0;
  int host_pixelradius = 2;

  public Vfx (double x, double y, double z, String fx_str, int duration, double si, MobileThing m, String texture_filename, double in_texturescaling) {
    expiration_time = PeggMachine.getWorldTime() + duration;
    xcoord = x;
    ycoord = y;
    zcoord = z;
    effect = new String(fx_str);
    sizeness = si;
    attachedTo = m;

    try {
      if (texture_filename.length() > 0) {
        textureMy = PeggMachine.getTextureLoader().getTexture(texture_filename);
        texturescaling = in_texturescaling;
        hastexture = true;
      } else {
        hastexture = false;

      }
    } catch (IOException e) {
      System.out.println("Vfx: Unable to load texture file: "+texture_filename);
      e.printStackTrace();
    }




  }
  public long getExpirationTime() {
    return expiration_time;
  }
  public void drawVfx(long wtime) {
    int xp = (int) Math.round( xcoord );
    int yp = (int) Math.round( ycoord );

    if (attachedTo != null) {
      host_x = (int) Math.round( attachedTo.getX() );
      host_y = (int) Math.round( attachedTo.getY() );
      host_pixelradius = attachedTo.getPixelRadius(); // defaults to 2
    }
    if (effect.equals("TRACER") || effect.equals("HIT_PEG")) {
      width = sizeness * 2.5;
      height = sizeness * 2.5;
      //move some of opengl outsize which_effect.
      //setMaterial();
      if (hastexture) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.
        if (textureMy != null)
          textureMy.bind();
      } else {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
      }

      GL11.glPushMatrix();
      GL11.glColor4f(0.9f, 0.9f, 1.0f, 1.0f);
      GL11.glTranslated(xcoord, ycoord, zcoord);

      GL11.glBegin(GL11.GL_QUADS);
      if (hastexture)
        GL11.glTexCoord2d(0, 0);
      GL11.glVertex2d(0, 0);
      GL11.glNormal3d(0.0, 0.0, 1.0);
      if (hastexture)
        GL11.glTexCoord2d(0, texturescaling * textureMy.getHeight());
      GL11.glVertex2d(0, height);
      GL11.glNormal3d(0.0, 0.0, 1.0);
      if (hastexture)
        GL11.glTexCoord2d(texturescaling * textureMy.getWidth(), texturescaling * textureMy.getHeight());
      GL11.glVertex2d(width, height);
      GL11.glNormal3d(0.0, 0.0, 1.0);
      if (hastexture)
        GL11.glTexCoord2d(texturescaling * textureMy.getWidth(), 0);
      GL11.glVertex2d(width, 0);
      GL11.glNormal3d(0.0, 0.0, 1.0);
      GL11.glEnd();

      GL11.glPopMatrix();
      if (hastexture)
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    } else {
      System.out.println("Vfx: unknown draw request "+effect);
    }
  }
}