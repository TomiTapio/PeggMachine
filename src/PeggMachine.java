import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Color;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.util.ResourceLoader;

import javax.sound.midi.*;
import java.awt.*;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;
import java.util.Vector;

import static org.lwjgl.opengl.GL11.*;

// "I hereby name my orbs-bounce-off-pegs OpenGL game Dwarven Peggmachine. #DwarvenPeggmachine is set in the world of #DwarfFortress."
//package com.tomitapio.dwarvenpeggmachine;

public class PeggMachine {
  public static boolean VSYNC = true;
  public static int VIEWWIDTH = (int) Math.round((16.0/9.0)*920.0);
  public static int VIEWHEIGHT = 920;
  public static float INIT_CAMERA_Z_DIST = 790.0f;
  public static boolean FULLSCREEN = false;
  protected static boolean running = false;
  protected static TextureLoaderMy textureLoader;

  static boolean show_splashscreen = false;
  static String splashscreen = "-- Dwarven Peggmachine --\nA Java + OpenGL game by TomiTapio.\nMade 2013-11-01--.\n\nESC to quit.";
  static String pause_and_infotext = ""; //if very short text, do not be in pause mode.
  static long worldTimeElapsed = 0; //milliseconds
  static int FPS = 60;
  static int worldTimeIncrement = (int)(1000.0/((double) FPS));
  static long sleeptime;
  static int prev_wti = worldTimeIncrement;
  static long paintTimeMeasured = 0;
  static long logicTimeMeasured = 0;

  static Random randgen;
  static Vector vfxList; //explosions and various temporary gfx, mostly 2D stuff, no collisions.
  static Vector orbList; //dwarven mineral spheres, educational.
  static Vector pegList; //dwarven leather, spidersilk and wood cylinders. Player places pegs to redirect orbs.
  static Vector launcherList; //automated thing that launches orbs.
  static Vector targetList; //hit these with orbs.
  static Vector fsList; //decorative stuff like insects and smoke, 2D flat sprites, no collisions.

  static Vector scoreHistory;

  //MIDI things:
  static Sequencer system_sequencer;
  static MidiDevice system_MIDIDevice;
  static Track global_track;
  static double toomuchnotescounter = 0.0;
  static Vector melodyNotesPile;
  static long micros_per_quarternote = 0;
  static long millis_per_quarternote = 0;


  private static int mouse_x = 0;
  private static int mouse_y = 0;
  private static double camera_x = 0;
  private static double camera_y = 0;
  private static double camera_z = 0;
  private static int pegs_available = 0;
  private static int initial_target_count = 0;
  private static int score = 0;
  private static int level_id = 0;

  UnicodeFont font;
  UnicodeFont font2;

  public static void main(String[] args) throws LWJGLException {
    new PeggMachine().start();
  }



  public static double gimmeRandDouble() {
    return randgen.nextDouble();
  }
  public static int gimmeRandInt(int a) { //ret 0 .. a-1
    return randgen.nextInt(a);
  }

  public static int getWorldTimeIncrement() {
    return worldTimeIncrement; //milliseconds
  }
  public static void gameOverMan() {
    putMelodyNotes(strIntoMelody("ga-over", 15, "") /*Vector of pitches*/, 57 /*core note*/, 1 /*instrument*/, 90, 2.4F /*note duration*/);

      //worldTimeIncrement = 0;
      pause_and_infotext = "--- GAME OVER ---\n";
      try { Thread.sleep(5300); } catch (Exception e) {}
      //initGameMode(mode);

  }
  public static long strToLongSeed(String b) {
    return (long) b.hashCode();
  }
  public static Vector strToMelodyVector(String numbers) {
    Vector ret = new Vector(10,10);
    String[] strArray = numbers.split(",");

    for(int i = 0; i < strArray.length; i++) {
      ret.add(Integer.parseInt(strArray[i]));
    }
    return ret;
  }
  public static Vector strIntoMelody(String in_dna, int len, String enforce_key /*not yet, complex, but increase number until it is in key's numbers*/) {
    //vector of Integers, 0 is pause, 1-127 is midi note pitch but we have relative to corenote(50ish).
    Vector ret = new Vector(len, 15);
    Random tmprand = new Random(strToLongSeed(in_dna)); tmprand.nextBoolean(); tmprand.nextBoolean();
    for (int i = 0; i < len; i++) {
      int note = tmprand.nextInt(20+12); //0..19

      if (note < 12) {
        ret.add(new Integer(0));
      } else {
        ret.add(new Integer(note-12));
      }
    }
    return ret;
  }
  public static void melodyNotesPileAdd(StampedNote s) {
    melodyNotesPile.add(s);
  }
  private boolean checkAndPlayNoteFromQue() { //checks, plays, puts noteoff into que
    if (worldTimeIncrement < 1)
      return false; // the pause.

    boolean ret = false;
    int siz = melodyNotesPile.size();
    StampedNote sn = null;
    if ( siz > 0) {
      for (int i = 0; i < siz; i++) {
        try {
          sn = (StampedNote) melodyNotesPile.elementAt(i);
        } catch (ArrayIndexOutOfBoundsException e) {
          e.printStackTrace();
        }
        if (sn.worldtimestamp < worldTimeElapsed) {
          if (!sn.isNoteOff) {
            //System.out.println("World time "+worldTimeElapsed+", note_on "+sn.toString());
            putMIDINote(sn.instrument, sn.actualnote, false, sn.vol, /*sn.dur_ticks, 0.0,*/ false/*noteoff req*/, true /*override spamming counter*/);

            ret = true;
            //no break, may want chords.

            //put a noteoff into Que
            long noteoff_timestamp = Math.round(sn.worldtimestamp + sn.dur_ticks * millis_per_quarternote);
            melodyNotesPile.add(new StampedNote(noteoff_timestamp , sn.instrument, sn.actualnote, 0, 0, true/*noteoff type*/));

            melodyNotesPile.remove(sn);
            siz = melodyNotesPile.size();
          } else { //noteoff request from pile -- do not generate child noteoff from noteoff
            /* try {
              ShortMessage m = new ShortMessage(); m.setMessage(ShortMessage.NOTE_OFF, 0, sn.actualnote, 0);
              system_MIDIDevice.getReceiver().send(m, -1);
            } catch (Exception e) { e.printStackTrace(); }
            */

            //System.out.println("World time "+worldTimeElapsed+", note_off "+sn.toString());
            putMIDINote(sn.instrument, sn.actualnote/*which key to release*/, true/*noteoff*/, 127, /*sn.dur_ticks, 0.0,*/ false/*noteoff right after*/, true /*override spamming counter*/);
            melodyNotesPile.remove(sn);
            siz = melodyNotesPile.size();
          }
        }
      }
    }
    return ret;
  }
  public static void playDwarfSpeech(int a, int ins /*70 bassoon*/, int vol, int offset) {
    StampedNote s;
    if (a == 1) { // pa-poo-pa!
      s = new StampedNote(worldTimeElapsed,     ins, 37+offset, vol, 0.20, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+250, ins, 32+offset, vol, 0.90, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+600, ins, 36+offset, vol, 0.40, false/*noteoff*/); melodyNotesPile.add(s);
    }
    if (a == 2) { // dun dah, dun doh. //actual: du daa do daa.
      s = new StampedNote(worldTimeElapsed /*what worldtime ms to play at*/, ins, 33+offset, vol, 0.45, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+200 /*what worldtime ms to play at*/, ins, 38+offset, vol, 0.49, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+600 /*what worldtime ms to play at*/, ins, 33+offset, vol, 0.45, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+800 /*what worldtime ms to play at*/, ins, 34+offset, vol, 0.45, false/*noteoff*/); melodyNotesPile.add(s);
    }
    if (a == 3) { // dyy daa-de.
      s = new StampedNote(worldTimeElapsed,     ins, 38+offset, vol, 0.80, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+450, ins, 33+offset, vol, 0.40, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+650, ins, 29+offset, vol, 0.25, false/*noteoff*/); melodyNotesPile.add(s);
    }
    if (a == 4) { // dyy daa-de pok pok.
      s = new StampedNote(worldTimeElapsed,     ins, 39+offset, vol, 0.60, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+420, ins, 33+offset, vol, 0.40, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+650, ins, 30+offset, vol, 0.25, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+1050,ins, 29+offset, vol, 0.25, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+1290,ins, 29+offset, vol, 0.29, false/*noteoff*/); melodyNotesPile.add(s);
    }
    if (a == 5) { // bass babble
      s = new StampedNote(worldTimeElapsed,     ins, 27+offset, vol, 0.15, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+170, ins, 26+offset, vol, 0.15, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+300, ins, 28+offset, vol, 0.25, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+470, ins, 27+offset, vol, 0.25, false/*noteoff*/); melodyNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+590, ins, 27+offset, vol, 0.29, false/*noteoff*/); melodyNotesPile.add(s);
    }

  }
  public static void putMelodyNotes(Vector v /*Vector of pitches and pauses*/, int corenote, int instrument /*instrument*/, int vol, float dur_ticks /*note duration*/) {
    Integer note;
    int actualnote;
    StringBuffer notes_as_string = new StringBuffer(3000);
    try {
      if ( v.size() > 0) {
        for (int i = 0; i < v.size(); i++) {
          note = (Integer) v.elementAt(i);
          actualnote = corenote + note.intValue();
          if (note.intValue() > 0) {
            notes_as_string.append(actualnote + ",");
          } else {
            notes_as_string.append("  ,");
          }

          if (note.intValue() > 0) { //if not a pause, put note.
            //put into my own queue..... because Java Sound device not respect timestamps.
            long worldtimestamp = Math.round(worldTimeElapsed + millis_per_quarternote * dur_ticks * (i+1));
            StampedNote s = new StampedNote(worldtimestamp /*what worldtime to play at*/, instrument, actualnote, vol,  dur_ticks, false/*not noteoff*/);
            melodyNotesPile.add(s);
            //System.out.println("putMelody: inserted " + s.toString());
          }
        }
        System.out.println("playMelody: inst " + instrument + ", dur " + dur_ticks + ", core " + corenote + " at world time " + worldTimeElapsed + ": " + notes_as_string.toString());
      }
    } catch (Exception e) {
      System.out.println("playMelody exception... " +  e.toString());
    }
  }
  private static void allNotesOff() {
    ShortMessage m = new ShortMessage();
    try {
      m.setMessage(ShortMessage.CONTROL_CHANGE, 0, (byte)123, (byte)0);
      system_MIDIDevice.getReceiver().send(m, -1);
    } catch (Exception e) { e.printStackTrace(); }
  }

  public static void putMIDINote(int instrument, int notepitch/*16=20Hz, 32=52Hz, 69=440Hz*/, boolean noteoff_pls,
                                 int volume, /*double dur, double beforewait,*/ boolean followup_noteoff_right_away, boolean override /*override spamming counter*/) {
    int hard_offset = 2;
    int hard_offset_micros = 500;
    //from ticks to microseconds
    float micros_per_quarternote = system_sequencer.getTempoInMPQ();
    //long dur_micros = new Double(dur).longValue() * new Double(micros_per_quarternote).longValue();
    //long beforewait_micros = new Double(beforewait).longValue() * new Double(micros_per_quarternote).longValue();
    /*ticksPerSecond = resolution * (currentTempoInBeatsPerMinute / 60.0);   tickSize = 1.0 / ticksPerSecond;*/


    if (toomuchnotescounter > 5 && !override) {
      System.out.println("putMIDINote: too spammy, inst=" +instrument + " counter="+toomuchnotescounter);
      return;
    }
    //if things are set up okay
    if (system_sequencer != null && system_MIDIDevice != null) {
      try {
        //long tipos = system_sequencer.getTickPosition();
        long tipos = 0;
        long upos = system_sequencer.getMicrosecondPosition();
        long dev_us_pos = system_MIDIDevice.getMicrosecondPosition();

        ShortMessage m = new ShortMessage();
        m.setMessage(ShortMessage.NOTE_ON, 0, notepitch, volume); //xxxx WHEN to noteoff??

        ShortMessage msg_instchange = new ShortMessage();
        msg_instchange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);

        //MidiEvent e = new MidiEvent(m,  tipos+2);         //e.setTick();        //global_track.add(e);        //system_sequencer.getReceiver().send(m,tipos+2);

        system_MIDIDevice.getReceiver().send(msg_instchange, -1 /*dev_us_pos + beforewait_micros*/);
        //if (override) {//melody testing
        if (!noteoff_pls) //if not a noteoff, send noteon message.
          system_MIDIDevice.getReceiver().send(m, -1 /*dev_us_pos + beforewait_micros + hard_offset_micros*/);
        //}
        if (!override)
          toomuchnotescounter = toomuchnotescounter + 1.0;

        //System.out.println("playNote: inst " + instrument + ", pitch "+notepitch+" at world time " + worldTimeElapsed + " and micros requested="+(dev_us_pos + beforewait_micros + hard_offset_micros));

        //xxx or put a noteoff into que.

        if (followup_noteoff_right_away || noteoff_pls) {
          m = new ShortMessage(); m.setMessage(ShortMessage.NOTE_OFF, 0/*channel*/, notepitch/*which key was released*/, volume);
          //m = new ShortMessage(); m.setMessage(ShortMessage.NOTE_ON, 0/*channel*/, notepitch/*which key was released*/, 0);
          system_MIDIDevice.getReceiver().send(m, -1 /*dev_us_pos + beforewait_micros + hard_offset_micros + dur_micros*/);
        }

        //e = new MidiEvent(m,  tipos+64+200);       //global_track.add(e);
      } catch (Exception e) {
        System.out.println("putMIDINote exception: " + e.getMessage());
      }
    } else {
      System.out.println("putMIDINote: global MIDI things aren't set up.");
    }
    //else nothing

  }
  public static MidiDevice chooseMIDIDevice() throws MidiUnavailableException { //borrowed func
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    MidiDevice device = null;
    for (int i = 0; i < infos.length; i++) {
      device = MidiSystem.getMidiDevice(infos[i]); //have four in my Win7: Microsoft MIDI Mapper, Microsoft GS Wavetable Synth, Real Time Sequencer, Java Sound Synthesizer
      System.out.println("MIDI found: " + device.getDeviceInfo().getName().toString()      + "");
      if (device.getDeviceInfo().getName().contains("Java Sound")) {
        System.out.println("MIDI now using: "+device.getDeviceInfo().getName().toString());
        return device;
      }
    }
    System.out.println("MIDI reluctantly using: "+device.getDeviceInfo().getName().toString());
    return device;
  }
  public PeggMachine(){
  }
  public static long getWorldTime() {
    return worldTimeElapsed;
  }

  public void start() throws LWJGLException {
    //init MIDI system
    try {
      MidiDevice md = chooseMIDIDevice(); md.open();
      system_MIDIDevice = md;
      system_sequencer = MidiSystem.getSequencer(); //but which of the four devices is this?

      if (system_sequencer == null || system_MIDIDevice == null) {
        System.out.println("MIDI sequencer getting failed.");
      } else {
        if (!(system_sequencer.isOpen())) {
          system_sequencer.open();
          Sequence mySeq = new Sequence(Sequence.SMPTE_30, 10/*ticks per video frame*/); //MidiSystem.getSequence(myMidiFile);
          system_sequencer.setSequence(mySeq);
          if (system_MIDIDevice.getMicrosecondPosition() == -1) {
            System.out.println("MIDI device does not support timestamps, behaves like a live cable.");
          } else {
            System.out.println("MIDI device current microseconds: "+system_MIDIDevice.getMicrosecondPosition());
          }
        }
      }
    } catch (Exception e) {
      System.out.println("MIDI setup failed.");
      e.printStackTrace();
    }


    // Set up our display
    Display.setTitle("Dwarven Peggmachine by TomiTapio");
    Display.setResizable(false); //whether our window is resizable
    Display.setDisplayMode(new DisplayMode(VIEWWIDTH, VIEWHEIGHT)); //resolution of our display

    //glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA | GLUT_STENCIL);

    Display.setVSyncEnabled(VSYNC); //whether hardware VSync is enabled
    Display.setFullscreen(FULLSCREEN); //whether fullscreen is enabled
    Display.setLocation(0,0);

    Display.create();
    System.out.println("Display vendor: "+glGetString(GL_VENDOR) +" version: " +glGetString(GL_VERSION) + " using "+Display.getAdapter());


    initContextAndResources();
    show_splashscreen = true;

    resize();
    running = true;
    int inputsleep = 0; //to prevent keypress-is-20-presses.


    //core loop
    while (running && !Display.isCloseRequested()) {
      // If the game was resized, we need to update our projection
      if (Display.wasResized())
        resize();

      inputsleep = inputsleep - 1;
      if (inputsleep < 1) { //prevent accidental N-clicks
        inputsleep = 0;
        //read mouse inputs
        mouse_x = Mouse.getX(); // will return the X coordinate on the Display.
        mouse_y = Mouse.getY(); // will return the Y coordinate on the Display.
        if (Mouse.isButtonDown(0)) {
          //to correct... because of camera distance from gameplay plane.
          addPeg(mouse_x, mouse_y, 82.0);
          inputsleep = 22;
        }
        if (Mouse.isButtonDown(1)) {
          addRandomOrbTest();
          inputsleep = 22;
        }

        //read keyboard
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
          running = false;

        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
          if (worldTimeIncrement == 0) {
            worldTimeIncrement = prev_wti; //unpauses
            pause_and_infotext = ""; //clears pause-causing infotext.
            show_splashscreen = false;
          } else {
            prev_wti = worldTimeIncrement;
            worldTimeIncrement = 0; //pauses
          }
          inputsleep = 12;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
          camera_x = camera_x + 35.0;
          inputsleep = 3;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
          camera_x = camera_x - 35.0;
          inputsleep = 3;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
          camera_y = camera_y + 35.0;
          inputsleep = 3;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
          camera_y = camera_y - 35.0;
          inputsleep = 3;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_INSERT)) { //zoom in
          camera_z = camera_z - 35.0;
          inputsleep = 3;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_HOME)) { // zoom out
          camera_z = camera_z + 35.0;
          inputsleep = 3;
        }


        if (Keyboard.isKeyDown(Keyboard.KEY_F1)) {
          //FULLSCREEN = !FULLSCREEN;
          //Display.setFullscreen(FULLSCREEN);
          initContextAndResources();
          level_id = 1; setupLevel();
          //resize();
          inputsleep = 17;
        }

        //speech test: 70 bassoon, 109 bagpipe
        if (Keyboard.isKeyDown(Keyboard.KEY_F2)) {
          playDwarfSpeech(1, 109, 90, -5);
          inputsleep = 17;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F3)) {
          playDwarfSpeech(2, 109, 90, -9);
          inputsleep = 17;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F4)) {
          playDwarfSpeech(3, 109, 90, -6);
          inputsleep = 17;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F5)) {
          playDwarfSpeech(4, 109, 90, -6);
          inputsleep = 17;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F6)) {
          playDwarfSpeech(5, 109, 90, 0);
          inputsleep = 17;
        }

      }

      long na = System.nanoTime();
      if (false) {

          if (worldTimeIncrement > 0)
            System.out.println("nanotime before logic: "+System.nanoTime());
          advance_time();
          if (worldTimeIncrement > 0)
            System.out.println("nanotime after logic: "+System.nanoTime() + ", elapsed "+ (System.nanoTime() - na) / 1000000);
          na = System.nanoTime();
          render();
          if (worldTimeIncrement > 0)
            System.out.println("nanotime after render: "+System.nanoTime() + ", elapsed "+ (System.nanoTime() - na) / 1000000);
          // Flip the buffers and sync to 60 FPS
          na = System.nanoTime();
          Display.update();
          Display.sync(FPS);
          if (worldTimeIncrement > 0)
            System.out.println("nanotime after update and sync-wait: "+System.nanoTime() + ", elapsed "+ (System.nanoTime() - na) / 1000000);
      } else { //without debug time
        advance_time();
        render();
        // Flip the buffers and sync to 60 FPS
        Display.update();
        long seen_wti_nano = System.nanoTime() -na;
        //System.out.println("seen wti "+ seen_wti_nano + " ns, or " + (seen_wti_nano / 1000000.0) + " ms."); // (8.6-11.4 before textures) - 15.7 - 46 ms
        //worldTimeIncrement = -3 + (int) Math.round((seen_wti_nano / 1000000.0));
        Display.sync(FPS);
      }
    }



    // Dispose any resources and destroy our window
    dispose();
    Display.destroy();
  }
  public void setupLevel() {
    System.out.println("Resetting the world... Level "+level_id);
    camera_x = 0.0;    camera_y = 0.0;    camera_z = 0.0; //reset user camera shifts
    worldTimeElapsed = 0; //new world.
    randgen = new Random(); randgen.nextBoolean();
    orbList = new Vector (50, 15);
    pegList = new Vector (15, 15);
    fsList = new Vector (50, 15);

    //    wg = new WorldGrid(102, 56, 1300.0 /*updates, 300=none,600=slow 2500=high*/, 120.0/*bonusrate*/); //161 ms draw time when 260x240. 32ms when 110x60. 27-29ms when 92x57
//    hazardList = new Vector (6, 6);
    vfxList = new Vector (150, 50);
    scoreHistory = new Vector(30,20);

    allNotesOff();
    melodyNotesPile = new Vector(60,60);
    //melody on init?
    //putMelodyNotes(strIntoMelody(in_mode, 6, "") /*Vector of pitches*/, 45 /*core note*/, in_mode.length() /*instrument*/, 3 /*note duration*/);

    if (level_id == 1) {
      //background -- is in render func.
      //set up lights
      try {
        GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, GL11.GL_TRUE);
        initAmbientLight();
        initLight(GL11.GL_LIGHT0, 0.5f/*diffuse*/, 0.5f/*spec*/, 500f, 500f, 300f);
        initLight(GL11.GL_LIGHT1 , 0.15f/*diffuse*/, 0.1f/*spec*/, 900f, 200f, 2000f);
        //GL11.glLighti(GL_LIGHT0, GL_QUADRATIC_ATTENUATION, 1);
        //GL11.glLighti(GL_LIGHT1, GL_QUADRATIC_ATTENUATION, 1);
      } catch (Exception e) {
        e.printStackTrace();
      }


      //how many pegs available
      pegs_available = 6;

      //targets
      initial_target_count = 0;
      targetList = new Vector (25, 5);
      for (int i = 0; i < 800; i=i+35) {
        targetList.add(new Target(25/*xsiz*/,25/*ysiz*/ ,210+i,55+i,12.5, "gneiss.png"));
        initial_target_count++;
      }

      //glued orbs aka obstacles

      //launchers
      launcherList = new Vector (5, 5);
      double minspd = 0.83 * -getGravity();
      //dir 0 is +x, dir pi/2 is +y, dir pi is -x
      //launcherList.add(new Launcher(100/*lau and orb width*/,270,1190,2, minspd,minspd*1.12,VIEWWIDTH-80,0,0,     0.21+Math.PI/2.0, "BW_hex_scaly_leather.png"));
      float LAURAD = 41.0f;
      launcherList.add(new Launcher(LAURAD,270,595,2, minspd,minspd*1.04,
              VIEWWIDTH-80.0,-50.0,LAURAD+2,     0.3+Math.PI/2.0, "gneiss.png"));
      LAURAD = 61.0f;
      launcherList.add(new Launcher(LAURAD,270,1095,2, minspd*1.0,minspd*1.24,
              80.0,-50.0,LAURAD+2,     -0.4+Math.PI/2.0, "gneiss.png"));
    }
    if (level_id == 2) {

    }
  }
  private void addLevelScore(String x) {
    //
    scoreHistory.add(x);
  }

  public void addPeg(int px_x, int px_y, double pegrad) {
    boolean disallow = false;
    Launcher retlau = areCoordsNearLauncher((double)px_x, (double)px_y, 0.0, pegrad);
    if (retlau == null) {

      Peg retpeg = areCoordsNearPeg((double)px_x, (double)px_y, 0.0);
      if (retpeg != null) { //then pick up that peg
        pegList.remove(retpeg);
        pegs_available++;
        changeScore(-1);
      } else if (pegs_available < 1) { //if none available, play sound.
        PeggMachine.melodyNotesPileAdd(new StampedNote(PeggMachine.getWorldTime() /*now*/, 70, 65, 50, 0.3, false));

        addVfx((double) px_x, (double) px_y, 0.0, "NO_PEGS_AVAILABLE", 900, 5.0, null, "", 1.0);

      } else { //try place peg.
        double x = px_x;
        double y = px_y;
        double z = 0.0; //-pegrad/2.0; //FOR HUGE PEGS, they're not spheres! more like half-spheres.
        //Peg newp = new Peg(2.0 /*kg*/, pegrad /*px radius*/, 0.01/*elas*/, x,y,z, "ornate_leather.png", 0.33); //best rubber elasticity 0.01
        Peg newp = new Peg(2.0 /*kg*/, pegrad /*px radius*/, 0.89/*elas*/, x,y,z, "ornate_leather.png", 0.33); //0.01 to 1.00 I guess.

        //for each existing peg, check if blocking

        int listsize = pegList.size();
        for (int j = 0; j < listsize; j++) {
          Peg tar = (Peg) pegList.elementAt(j);
          if (tar != null) {
            if (tar.wouldThisPegTouchYou(newp))
              disallow = true;

          }
        }

        //for each existing target, check if blocking
        listsize = targetList.size();
        for (int j = 0; j < listsize; j++) {
          Target tar = (Target) targetList.elementAt(j);
          if (tar != null) {
            if (tar.wouldThisPegTouchYou(newp))
              disallow = true;

          }
        }



        //MobileThing.isPegPegCollision()
        //if (MobileThing.isPegPegCollision(newp, existingpeg)) {




        if (!disallow) {
          newp.playNoteYouGotPlaced();
          pegList.add(newp);
          pegs_available--;
        }
        //} else { // can't place, existing blocks.
        //PeggMachine.melodyNotesPileAdd(new StampedNote(PeggMachine.getWorldTime() /*now*/, 70, 65, 50, 0.3, false));

        //}
      }
    } else { //too near launcher, error, play sound.
      PeggMachine.melodyNotesPileAdd(new StampedNote(PeggMachine.getWorldTime() /*now*/, 56, 65, 50, 0.38, false));
      PeggMachine.melodyNotesPileAdd(new StampedNote(PeggMachine.getWorldTime()+290 /*now*/, 54, 65, 50, 0.38, false));

      //add vfx, link launcher to indicate that tried place too close.

    }
  }
  public Peg areCoordsNearPeg(double in_x, double in_y, double in_z) {
    int listsize = pegList.size();
    for (int j = 0; j < listsize; j++) {
      Peg p = (Peg) pegList.elementAt(j);
      if (p != null) {
        if (p.areCoordsInsideYou(in_x, in_y, in_z))
          return p;
      }
    }
    return null;
  }
  public Launcher areCoordsNearLauncher(double in_x, double in_y, double in_z, double pegrad) {
    int listsize = launcherList.size();
    for (int j = 0; j < listsize; j++) {
      Launcher la = (Launcher) launcherList.elementAt(j);
      if (la != null) {
        if (la.areCoordsTooNearYou(mouse_x, mouse_y, 0.0, pegrad))
          return la;
      }
    }
    return null;
  }

  public static double getGravity() {

    //// two meters is 1000 distance. 500.0 * -9.81 ??



    return -2250.0;
  }

  // Exit our game loop and close the window
  private void stopRequest() {
    running = false;
  }

  // Called to setup our game and context
  protected void initContextAndResources() {
    if (system_sequencer != null) {
      micros_per_quarternote = new Long(Math.round(system_sequencer.getTempoInMPQ())).longValue();
      millis_per_quarternote = new Long(Math.round(system_sequencer.getTempoInMPQ() / 1000.0)).longValue(); //273 ms per quarternote.
    }

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glEnable(GL_LINE_SMOOTH);
    glHint(GL_LINE_SMOOTH_HINT,  GL_NICEST);
    glEnable(GL_NORMALIZE);
    //GL11.glEnable(GL_STENCIL_TEST); //nope, shadows too hard to code.

    GL11.glClearDepth(1.0f); //xxxxxx
    GL11.glEnable(GL11.GL_DEPTH_TEST);
    GL11.glDepthFunc(GL11.GL_LEQUAL);
    GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);

    GL11.glClearColor(0.0f, 0f, 0f, 0f);

    GL11.glMatrixMode(GL11.GL_PROJECTION); //mode: are changing projection.
    GL11.glLoadIdentity();
    float voo = (float)VIEWWIDTH/(float)VIEWHEIGHT;
    GLU.gluPerspective(60.0f, voo, 9.0f, -9.0f);
    GLU.gluLookAt(VIEWWIDTH/2.0f, VIEWHEIGHT/2.0f, INIT_CAMERA_Z_DIST, //750 too close camera.
            VIEWWIDTH/2.0f, VIEWHEIGHT/2.0f, 0.0f,
            0.0f, 100.0f, 0.0f);
    //2D CAD measurements view:   GL11.glOrtho(0, VIEWWIDTH, 0, VIEWHEIGHT, 9000, -9000); //clip distance -1 to 1 is for 2d games...
    GL11.glMatrixMode(GL11.GL_MODELVIEW); //mode: are changing models.


    //lights setup
    GL11.glEnable(GL_LIGHTING);
    GL11.glEnable(GL_LIGHT0);
    GL11.glEnable(GL_LIGHT1);

    GL11.glEnable(GL_COLOR_MATERIAL);								// enables opengl to use glColor3f to define material color
    GL11.glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);			// tell opengl glColor3f effects the ambient and diffuse properties of material

    //set up texture-loader
    try {
      textureLoader = new TextureLoaderMy();
    } catch (Exception e) {
      e.printStackTrace();
    }

    // init fonts
    initSlickUtilFonts();


    level_id = 1; setupLevel();
  }
  public void addRandomOrbTest() {
    Orb o = new Orb(4.0, 30, 16.0, 7.0 /*friction*/,  gimmeRandomCoord(), 1200.0+gimmeRandomCoord()/10.0, 0.9, "granite_light.png");
    o.setSpeed((gimmeRandDouble() - 0.5) * 37.5,   (gimmeRandDouble() + 0.5) * 17.5,   (gimmeRandDouble() - 0.5) * 57.5);
    orbList.add(o);
  }
  public static void removeOrbFromPlay(Orb o) {
    orbList.remove(o);
  }
  public double gimmeRandomCoord() {
    return Display.getHeight() * randgen.nextDouble();
  }
  protected void render() {
    // Clear the screen and depth buffer
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
    GL11.glLoadIdentity();
    // draw background quad
    drawBackground(1);
    //draw score
    drawText(font, 300, 90, 2, "Level Score: "+score+" Pegs avail: "+pegs_available, Color.white);
/*
    drawText(font, 0, 0, 2,    "foo 0", Color.white);
    drawText(font, 50, 50, 2,   "foo 50", Color.white);
    drawText(font, 200, 200, 2, "foo 200", Color.white);
    drawText(font, 150, 150, 2, "foo 150", Color.white);
    drawText(font, 600, 600, 2, "foo 600", Color.white);
*/
    //camera move
    GL11.glMatrixMode(GL11.GL_PROJECTION); //mode: are changing projection.
    GL11.glLoadIdentity();
    float voo = (float)VIEWWIDTH/(float)VIEWHEIGHT;
    GLU.gluPerspective(60.0f, voo, 9.0f, -9.0f);

    float camera_x_shift = 0.0f; //1.0f*mouse_x - VIEWWIDTH/2.0f;
    float camera_y_shift = 0.0f; //1.0f*mouse_y - VIEWHEIGHT/2.0f;
    camera_x_shift = (float) camera_x;
    camera_y_shift = (float) camera_y;
    float camera_z_shift = (float) camera_z;
    GLU.gluLookAt(camera_x_shift + VIEWWIDTH/2.0f, camera_y_shift + VIEWHEIGHT/2.0f, camera_z_shift + INIT_CAMERA_Z_DIST, //750 too close camera.
            VIEWWIDTH/2.0f, VIEWHEIGHT/2.0f, 0.0f,
            0.0f, 100.0f, 0.0f);
    //2D CAD measurements view:   GL11.glOrtho(0, VIEWWIDTH, 0, VIEWHEIGHT, 9000, -9000); //clip distance -1 to 1 is for 2d games...
    GL11.glMatrixMode(GL11.GL_MODELVIEW); //mode: are changing models.


    // draw cursor quad
    ////////////OR SPRITE
    int x = 0;
    int y = 0;
      x = x + mouse_x;
      y = y + mouse_y;
    GL11.glColor3f(0.5f, 0.5f, 1.0f);
    GL11.glPushMatrix();
    //GL11.glTranslated(0.0, 0.0, (worldTimeElapsed % 500.0)/25.0); //pulsing animation
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(x - 15, y - 5);
    GL11.glVertex2f(x + 15, y - 5);
    GL11.glVertex2f(x + 15, y + 5);
    GL11.glVertex2f(x - 15, y + 5);
    GL11.glEnd();
    GL11.glPopMatrix();


    //shader program test
    //b1.draw(200.0f, 500f, 200f, 50f);

    // draw all 1-4 launchers
    int listsize = launcherList.size();
    for (int j = 0; j < listsize; j++) {
      Launcher p = (Launcher) launcherList.elementAt(j);
      if (p != null)
        p.drawLauncher();
    }

    // draw all targets
    listsize = targetList.size();
    for (int j = 0; j < listsize; j++) {
      Target p = (Target) targetList.elementAt(j);
      if (p != null)
        p.drawTarget();
    }

    // draw all 2D sprites
    listsize = fsList.size();
    for (int j = 0; j < listsize; j++) {
      FlatSprite p = (FlatSprite) fsList.elementAt(j);
      if (p != null)
        p.drawFlatSprite();
    }

    // draw all pegs
    listsize = pegList.size();
    for (int j = 0; j < listsize; j++) {
      Peg p = (Peg) pegList.elementAt(j);
      if (p != null)
        p.drawPeg();
    }

    // draw all orbs
    listsize = orbList.size();
    for (int j = 0; j < listsize; j++) {
      Orb p = (Orb) orbList.elementAt(j);
      if (p != null)
        p.drawOrb();
    }

    //draw all VFX
    int vsize = vfxList.size(); //sorta heavy operation, so outside loop
    for (int i = 0; i < vsize; i++) {
      Vfx v = (Vfx) vfxList.elementAt(i);
      if (v != null) {
        if (v.getExpirationTime() < worldTimeElapsed) {
          vfxList.removeElementAt(i); //dangerous coz...
          vsize--; //coz moved vsize outside loop.
          if (gimmeRandDouble() < 0.01)
            System.out.println("DEBUG: vfx list size is "+vsize);
        } else {
          v.drawVfx(worldTimeElapsed);
        }
      }
    }




    //draw walls???

  }

  public static void addVfx(double x, double y, double z, String in_fx_str, int dur /*ms*/, double s,
                                       MobileThing m, String texture_filename, double in_texturescaling) {
    //verify inputs...

    //new Vfx(0.0, 0.0, 0.0, "TRACER", 1500/*ms*/, 5.0/*size*/, null/*attachedto*/, "texture.png", 1.0)
    Vfx v = new Vfx(x, y, z, in_fx_str, dur, s, m, texture_filename, in_texturescaling);
    if (v != null)
      vfxList.add(v);
  }


  protected void resize() {
    glViewport(0, 0, Display.getWidth(), Display.getHeight());


    // ... update our projection matrices here ...

  }
  public long getTimeMS() {
    return System.nanoTime() / 1000000;
    }
  public long getTime() { //in ms
    return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

  protected void dispose() {

    allNotesOff();
    system_sequencer.close();
    system_MIDIDevice.close(); //otherwise program stays sort-of running in background





  }

  public void advance_time() {
    long time_a = System.currentTimeMillis();
    if (show_splashscreen && worldTimeElapsed > 10) {
      pause_and_infotext = splashscreen;
    }
    //if have infotext, we are paused and draw infotext.
    if (pause_and_infotext.length() > 2) {
      if (worldTimeIncrement > 0) {
        prev_wti = worldTimeIncrement;
        worldTimeIncrement = 0; //pauses like spacebar.
      } //else are already paused.

      drawText(font, VIEWWIDTH*0.3f, VIEWHEIGHT*0.45f, 30.0f, pause_and_infotext, Color.white);
    }

    // move the lights. EVEN WHEN PAUSED.
    moveLight(GL11.GL_LIGHT0);
    //not light 1

    if (worldTimeIncrement < 1) return; // the pause.


    //world goes x milliseconds forward.
    worldTimeElapsed += worldTimeIncrement;

    toomuchnotescounter = toomuchnotescounter - (1.0/8.0); //cut down on sound spamming. // 8 timeticks for new note?
    //play 0-N notes from from melody queue
    if (checkAndPlayNoteFromQue()) {
      toomuchnotescounter = toomuchnotescounter - (1.0/8.0);
    }

    //ambient dwarven sounds: low chance, bass note.
    //note: putmidinot does NOT have a duration -- use stampednote to time a noteoff. or putMelodyNotes().
    if (gimmeRandDouble() < 0.05) { //because 0.001 is hard to reach.
    if (gimmeRandDouble() < 0.003) {
      putMIDINote(12 /*marimba*/, 42, false, 80, /*sn.dur_ticks, 0.0,*/ true, true);
    }
    if (gimmeRandDouble() < 0.003) {
      putMelodyNotes(strIntoMelody("booUrist", 3, "") /*Vector of pitches*/, 27+(int)(4*gimmeRandDouble()) /*core note*/, 52 /*instrument*/, 30, 3.2F /*note duration*/);
    }
    if (gimmeRandDouble() < 0.003) {
      putMIDINote(47 /*timpani*/, 35, false, 80, /*sn.dur_ticks, 0.0,*/ true, true);
    }
    if (gimmeRandDouble() < 0.003) {
      //putMIDINote(60 /*french horn*/, 35, false, 80, 4.0f, 0, false, true);
      putMelodyNotes(strIntoMelody("huriw", 5, "") /*Vector of pitches*/, 26+(int)(4*gimmeRandDouble()) /*core note*/, 60 /*instrument*/, 30, 3.9F /*note duration*/);
    }
    if (gimmeRandDouble() < 0.003) {
      //putMIDINote(75 /*pan flute*/, 45, false, 80, 4.0f, 0, false, true);
      putMelodyNotes(strIntoMelody("fhueiq", 5, "") /*Vector of pitches*/, 34+(int)(4*gimmeRandDouble()) /*core note*/, 75 /*instrument*/, 30, 1.9F /*note duration*/);
    }
    if (gimmeRandDouble() < 0.003) {
      putMIDINote(36 /*slapbass*/, 15, false, 30, /*sn.dur_ticks, 0.0,*/ false, true);
    }
    }


    //time-tick for all Orbs
    try {
      Orb b;
      int bsiz = orbList.size();
      if ( bsiz > 0) {
        for (int i = 0; i < bsiz; i++) {
          b = (Orb) orbList.elementAt(i);
          if (b != null)
            b.advance_time();
          bsiz = orbList.size();
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
    //time-tick for all 1-4 launchers
    try {
      Launcher b;
        for (int i = 0; i < launcherList.size(); i++) {
          b = (Launcher) launcherList.elementAt(i);
          if (b != null)
            b.advance_time();
        }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
    //time-tick for all decorative sprites
    try {
      FlatSprite b;
      for (int i = 0; i < fsList.size(); i++) {
        b = (FlatSprite) fsList.elementAt(i);
        if (b != null)
          b.advance_time();
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
    //time-tick for all tumbling-falling targets
    try {
      Target b;
      for (int i = 0; i < targetList.size(); i++) {
        b = (Target) targetList.elementAt(i);
        if (b != null)
          b.advance_time();
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }

    if (targetList.size() == 0 && initial_target_count > 0) {
      addLevelScore("foo"); //add to score history
      level_id++; setupLevel();
      pause_and_infotext = "Level " + level_id + ". SPACE to unpause.";
    }

    logicTimeMeasured = (System.currentTimeMillis() - time_a);
  }

  public static Vector getPegList() {
    return pegList;
  }
  public static Vector getOrbList() {
    return orbList;
  }
  public static Vector getTargetList() {
    return targetList;
  }

  public static void addToOrbList(Orb o) {
    if (o != null)
      orbList.add(o);
  }

  public static void changeScore(int a) { //negatives for breaking an orb.
    score = score + a;
  }
  public static void removeTarget(Target t) {
    targetList.remove(t);
    //putMelodyNotes(strIntoMelody("kaching, Urist", 8, "") /*Vector of pitches*/, 47+(int)(4*gimmeRandDouble()) /*core note*/, 45 /*instrument*/+offset, vol, 0.2F /*note duration*/);
  }
  public static TextureLoaderMy getTextureLoader() {
    if (textureLoader != null)
      return textureLoader;
    else throw new NullPointerException("argh");

  }
  private void drawBackground(int level) {
    //material:
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] emission = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] amb_diffuse = {1.0f, 1.0f, 1.0f, 1.0f};
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());

    FlatSprite bg = new FlatSprite(VIEWWIDTH, VIEWHEIGHT, 0.0, 0.0, -2.0, "Viscont_White_sharp.png", 12.0);
    bg.setSpeed(0.0, 0.0, 0.0); //no-one updates this flatsprite unless it's in a list.
    bg.drawFlatSprite();
  }


  public static void addToFSList(FlatSprite fs) {
    if (fs != null)
      fsList.add(fs);
  }
  public static int getMouseX() {
    return mouse_x;
  }
  public static int getMouseY() {
    return mouse_y;
  }
  public static double calcBearing(MobileThing a, MobileThing b) { //asker, target
    double bx = b.getX();
    //if (bx < 0.001) bx = 0.001;
    double by = b.getY();
    //if (by < 0.001) by = 0.001;
    double dx = bx - a.getX();
    double dy = - (by - a.getY());
    double ra = Math.atan2(dy, dx); //out comes -pi to +pi
    if (ra < 0)
      ra = Math.abs(ra);
    else //WTF huh
      ra = 2*Math.PI - ra;
    //System.out.println("calcBearing: dx="+dx+ " dy="+dy+" and bearing="+ra );
    return ra;
  }
  public static double calcBearingXY(double ax, double ay, double bx, double by) { //asker, target
    double dx = bx - ax;
    double dy = - (by - ay);
    double ra = Math.atan2(dy, dx); //out comes -pi to +pi
    if (ra < 0)
      ra = Math.abs(ra);
    else
      ra = 2*Math.PI - ra;
    return ra;
  }
  public static double calcDistanceXY(double ax, double ay, double bx, double by) { //asker, target
    return Math.sqrt( (ax - bx)*(ax - bx)  +  (ay - by)*(ay - by) );
  }

  public static void initAmbientLight() {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    GL11.glEnable(GL11.GL_LIGHTING);
    GL11.glShadeModel(GL11.GL_SMOOTH);
    float[] ambientglobal = {0.22f, 0.22f, 0.22f, 1.0f};
    GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, buffer.asFloatBuffer().put(ambientglobal));
  }
  public static void initLight(int light_id, float diffuse_str, float specularmain, float x, float y, float z) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] ambient = {0.1f, 0.01f, 0.02f, 1.0f};
    float[] diffuse = {diffuse_str, diffuse_str, diffuse_str, 1.0f};
    float[] specular = {specularmain +0.3f, specularmain +0.3f, specularmain, 1.0f}; //yellow specular, because fire-light.
    float[] position = {x,y,z, 1.0f}; //xxxx

    GL11.glLight(light_id, GL11.GL_AMBIENT, (FloatBuffer) buffer.asFloatBuffer().put(ambient).flip());
    GL11.glLight(light_id, GL11.GL_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(diffuse).flip());
    GL11.glLight(light_id, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
    GL11.glLight(light_id, GL11.GL_POSITION, (FloatBuffer) buffer.asFloatBuffer().put(position).flip());

    // Attenuation
    //glLighti(light_id, GL_CONSTANT_ATTENUATION, 0);
    //glLighti(light_id, GL_LINEAR_ATTENUATION, 0);
    //glLighti(light_id, GL_QUADRATIC_ATTENUATION, 1); //quadratic attenuation

    // Disable all spot settings
    //glLighti(light_id, GL_SPOT_CUTOFF, 180); // no cutoff
    //glLighti(light_id, GL_SPOT_EXPONENT, 0); // no focussing

    GL11.glEnable(light_id);
  }
  public static void moveLight(int light_id) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    //float[] position = {(float) (-1000 + mouse_x*3), (float) (-1000 + mouse_y*3), 710.0f, 1.0f};
    float[] position = {(float) (mouse_x), (float) (mouse_y), 210.0f, 1.0f};
    GL11.glLight(light_id, GL11.GL_POSITION, (FloatBuffer) buffer.asFloatBuffer().put(position).flip());
  }
  public static void setLightPosition(int light_id, float xi, float yi, float zi) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] position = {xi, yi, zi, 1.0f};
    GL11.glLight(light_id, GL11.GL_POSITION, (FloatBuffer) buffer.asFloatBuffer().put(position).flip());
  }

  public void initSlickUtilFonts() {
    Font awtFont = new Font("Times New Roman", Font.BOLD, 54);
    font = new UnicodeFont(awtFont, 54, false, false);
    try {
      font.addAsciiGlyphs();
      font.addGlyphs(400, 600);
      font.getEffects().add(new ColorEffect(java.awt.Color.ORANGE)); //ColorEffect, FilterEffect, GradientEffect, OutlineEffect, OutlineWobbleEffect, OutlineZigzagEffect, ShadowEffect
      font.loadGlyphs();
    } catch (SlickException e) {

    }

    // load font from a .ttf file
/*
    new UnicodeFont(java.lang.String ttfFileRef, int size, boolean bold, boolean italic)
    try {
      InputStream inputStream	= ResourceLoader.getResourceAsStream("wood sticks.ttf");

      Font awtFont2 = Font.createFont(Font.TRUETYPE_FONT, inputStream);
      awtFont2 = awtFont2.deriveFont(24f); // set font size
      font2 = new UnicodeFont(awtFont2);

    } catch (Exception e) {
      e.printStackTrace();
    }
*/
  }
  private void drawText(UnicodeFont f, double x, double y, double z, String s, Color c) {
    GL11.glPushMatrix();
    GL11.glLoadIdentity();
    // enable texture
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.

    PeggMachine.setMaterial("FONT");

    GL11.glRotated(180, 1.0,0.0,0.0); //x 180deg makes correct side up.
    GL11.glTranslated(x, -y, z);
    if (f != null)
      f.drawString((float)0, (float)0, s, c);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glPopMatrix();

  }
  private void font_test() {
    GL11.glPushMatrix();
    // enable texture
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.
    //glEnable(GL_BLEND);
    //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    //setMaterial();
    for (double mm = 0.0; mm < 100; mm = mm + 15.0) {
      GL11.glRotated(mm, 1.0,1.0,1.0);
      GL11.glTranslated(250.0, 50.0, 200+(worldTimeElapsed % 500.0)/15.0); //pulsing animation

      font.drawString((float)mm, (float)mm, "EEEEEEEEEEEEEEE", Color.white);
    }
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glPopMatrix();

  }
  public static void setMaterial(String s) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    if (s.equals("BASIC")) {
      float[] emission = {0.0f, 0.0f, 0.0f, 1.0f};
      float[] amb_diffuse = {1.0f, 1.0f, 1.0f, 1.0f};
      float[] specular = {0.5f, 0.5f, 0.5f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 70); //0 to 128 //some stones should have zero specular.
    }
    if (s.equals("NOSPECULAR")) {
      float[] emission = {0.0f, 0.0f, 0.0f, 1.0f};
      float[] amb_diffuse = {1.0f, 1.0f, 1.0f, 1.0f};
      float[] specular = {0.02f, 0.02f, 0.02f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 0); //0 to 128 //some stones should have zero specular.
    }
    if (s.equals("FONT")) {
      float[] emission = {0.3f, 0.3f, 0.1f, 1.0f};
      float[] amb_diffuse = {1.0f, 0.9f, 1.0f, 1.0f};
      float[] specular = {0.7f, 0.7f, 0.5f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 120); //0 to 128 //some stones should have zero specular.
    }



  }


}