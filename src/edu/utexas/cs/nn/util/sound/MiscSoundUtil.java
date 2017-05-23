package edu.utexas.cs.nn.util.sound;
import javax.sound.sampled.Clip;

//for playing midi sound files on some older systems
import java.applet.Applet;
import java.applet.AudioClip;
import java.net.MalformedURLException;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
/**
 *  <i>Standard audio</i>. This class provides a basic capability for
 *  creating, reading, and saving audio. 
 *  <p>
 *  The audio format uses a sampling rate of 44,100 (CD quality audio), 16-bit, monaural.
 *
 *  <p>
 *  For additional documentation, see <a href="http://introcs.cs.princeton.edu/15inout">Section 1.5</a> of
 *  <i>Computer Science: An Interdisciplinary Approach</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public final class MiscSoundUtil {

	/**
	 *  The sample rate - 44,100 Hz for CD quality audio.
	 */
	public static final int SAMPLE_RATE = 44100;

	public static final int BYTES_PER_SAMPLE = 2;                // 16-bit audio
	public static final int BITS_PER_SAMPLE = 16;                // 16-bit audio
	private static final double MAX_16_BIT = Short.MAX_VALUE;     // 32,767
	private static final int SAMPLE_BUFFER_SIZE = 4096;


	private static SourceDataLine line;   // to play the sound
	private static byte[] buffer;         // our internal buffer
	private static int bufferSize = 0;    // number of samples currently in internal buffer

	private static boolean playing = false;
	private static boolean available = true;

	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;
	public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

	private MiscSoundUtil() {
		// can not instantiate
	}

	// static initializer
	static {
		init();
	}

	// open up an audio stream
	private static void init() {
		try {
			// 44,100 samples per second, 16-bit audio, mono, signed PCM, little Endian
			AudioFormat format = new AudioFormat((float) SAMPLE_RATE, BITS_PER_SAMPLE, 1, true, false);
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format, SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE);

			// the internal buffer is a fraction of the actual buffer size, this choice is arbitrary
			// it gets divided because we can't expect the buffered data to line up exactly with when
			// the sound card decides to push out its samples.
			buffer = new byte[SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE/3];
		}
		catch (LineUnavailableException e) {
			System.out.println(e.getMessage());
		}

		// no sound gets made before this call
		line.start();
	}


	/**
	 * Closes standard audio.
	 */
	public static void close() {
		line.drain();
		line.stop();
	}

	/**
	 * Writes one sample (between -1.0 and +1.0) to standard audio.
	 * If the sample is outside the range, it will be clipped.
	 *
	 * @param  sample the sample to play
	 * @throws IllegalArgumentException if the sample is {@code Double.NaN}
	 */
	public static void playDouble(double sample) {

		// clip if outside [-1, +1]
		if (Double.isNaN(sample)) throw new IllegalArgumentException("sample is NaN");
		if (sample < -1.0) sample = -1.0;
		if (sample > +1.0) sample = +1.0;

		// convert to bytes
		short s = (short) (MAX_16_BIT * sample);
		buffer[bufferSize++] = (byte) s;
		buffer[bufferSize++] = (byte) (s >> 8);   // little Endian

		// send to sound card if buffer is full        
		if (bufferSize >= buffer.length) {
			line.write(buffer, 0, buffer.length);
			bufferSize = 0;
		}
	}

	/**
	 * Writes the array of samples (between -1.0 and +1.0) to standard audio.
	 * If a sample is outside the range, it will be clipped.
	 *
	 * @param  samples the array of samples to play
	 * @throws IllegalArgumentException if any sample is {@code Double.NaN}
	 * @throws IllegalArgumentException if {@code samples} is {@code null}
	 */
	public static void playDoubleArray(double[] samples) {
		if (samples == null) throw new IllegalArgumentException("argument to play() is null");
		playing = false; // Disable any previously playing sample
		while(!available) { // Wait until previous sample finishes playing
			try {
				Thread.sleep(1); // short pause to wait for sound line to become available
			} catch (InterruptedException e) {
				e.printStackTrace(); // Should not happen?
			}
		}
		// Play sound in its own Thread
		new Thread() {
			public void run() {
				playing = true;
				available = false;
				for (int i = 0; playing && i < samples.length; i++) {
					playDouble(samples[i]);
				}				
				available = true;
			}
		}.start();
	}

	/**
	 * Reads audio samples from a file (in .wav or .au format) and returns
	 * them as a double array with values between -1.0 and +1.0.
	 *
	 * @param  filename the name of the audio file
	 * @return the array of samples
	 */
	public static double[] read(String filename) {
		byte[] data = readByte(filename);
		int n = data.length;
		double[] d = new double[n/2];
		for (int i = 0; i < n/2; i++) {
			d[i] = ((short) (((data[2*i+1] & 0xFF) << 8) + (data[2*i] & 0xFF))) / ((double) MAX_16_BIT);
		}
		return d;
	}

	/**
	 * Reads in data from audio file and returns it as an array of bytes. 
	 * 
	 * @param filename string reference to audio file being used
	 * @return byte array containing data from audio file 
	 */
	public static byte[] readByte(String filename) {
		byte[] data = null;
		AudioInputStream ais = null;
		try {

			// try to read from file
			File file = new File(filename);
			if (file.exists()) {
				ais = AudioSystem.getAudioInputStream(file);
				int bytesToRead = ais.available();
				data = new byte[bytesToRead];
				int bytesRead = ais.read(data);
				if (bytesToRead != bytesRead)
					throw new IllegalStateException("read only " + bytesRead + " of " + bytesToRead + " bytes"); 
			}

			// try to read from URL
			else {
				URL url = MiscSoundUtil.class.getResource(filename);
				ais = AudioSystem.getAudioInputStream(url);
				int bytesToRead = ais.available();
				data = new byte[bytesToRead];
				int bytesRead = ais.read(data);
				if (bytesToRead != bytesRead)
					throw new IllegalStateException("read only " + bytesRead + " of " + bytesToRead + " bytes"); 
			}
		}
		catch (IOException e) {
			throw new IllegalArgumentException("could not read '" + filename + "'", e);
		}

		catch (UnsupportedAudioFileException e) {
			throw new IllegalArgumentException("unsupported audio format: '" + filename + "'", e);
		}

		return data;
	}

	/**
	 * Saves the double array as an audio file (using .wav or .au format).
	 *
	 * @param  filename the name of the audio file
	 * @param  samples the array of samples
	 * @throws IllegalArgumentException if unable to save {@code filename}
	 * @throws IllegalArgumentException if {@code samples} is {@code null}
	 */
	public static void save(String filename, double[] samples) {
		if (samples == null) {
			throw new IllegalArgumentException("samples[] is null");
		}

		// assumes 44,100 samples per second
		// use 16-bit audio, mono, signed PCM, little Endian
		AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
		byte[] data = new byte[2 * samples.length];
		for (int i = 0; i < samples.length; i++) {
			int temp = (short) (samples[i] * MAX_16_BIT);
			data[2*i + 0] = (byte) temp;
			data[2*i + 1] = (byte) (temp >> 8);
		}

		// now save the file
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			AudioInputStream ais = new AudioInputStream(bais, format, samples.length);
			if (filename.endsWith(".wav") || filename.endsWith(".WAV")) {
				AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
			}
			else if (filename.endsWith(".au") || filename.endsWith(".AU")) {
				AudioSystem.write(ais, AudioFileFormat.Type.AU, new File(filename));
			}
			else {
				throw new IllegalArgumentException("unsupported audio format: '" + filename + "'");
			}
		}
		catch (IOException ioe) {
			throw new IllegalArgumentException("unable to save file '" + filename + "'", ioe);
		}
	}



	/**
	 * Plays an audio file (in .wav, .mid, or .au format) in a background thread.
	 *
	 * @param filename the name of the audio file
	 * @throws IllegalArgumentException if unable to play {@code filename}
	 * @throws IllegalArgumentException if {@code filename} is {@code null}
	 */
	public static synchronized void play(final String filename) {
		if (filename == null) throw new IllegalArgumentException();

		InputStream is = MiscSoundUtil.class.getResourceAsStream(filename);
		if (is == null) {
			throw new IllegalArgumentException("could not read '" + filename + "'");
		}

		// code adapted from: http://stackoverflow.com/questions/26305/how-can-i-play-sound-in-java
		try {
			// check if file format is supported
			// (if not, will throw an UnsupportedAudioFileException)
			AudioInputStream ais = AudioSystem.getAudioInputStream(is); // unnecessary code?

			new Thread(new Runnable() {
				@Override
				public void run() {
					stream(filename);
				}
			}).start();
		}

		// let's try Applet.newAudioClip() instead
		catch (UnsupportedAudioFileException e) {
			playApplet(filename);
			return;
		}

		// something else went wrong
		catch (IOException ioe) {
			throw new IllegalArgumentException("could not play '" + filename + "'", ioe);
		}

	}

	/**
	 * Plays sound using Applet.newAudioClip()
	 * 
	 * @param filename string reference to audio file being played
	 */
	private static void playApplet(String filename) {
		URL url = null;
		try {
			File file = new File(filename);
			if(file.canRead()) url = file.toURI().toURL();
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException("could not play '" + filename + "'", e);
		}

		// URL url = StdAudio.class.getResource(filename);
		if (url == null) {
			throw new IllegalArgumentException("could not play '" + filename + "'");
		}

		AudioClip clip = Applet.newAudioClip(url);
		clip.play();
	}

	/**
	 * Method used to play wav or aif file. (Fails on long clips?)
	 * 
	 * @param filename string reference to audio file being played
	 */
	private static void stream(String filename) {
		SourceDataLine line = null;
		int BUFFER_SIZE = 4096; // 4K buffer

		try {
			InputStream is = MiscSoundUtil.class.getResourceAsStream(filename);
			AudioInputStream ais = AudioSystem.getAudioInputStream(is);
			AudioFormat audioFormat = ais.getFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
			line.start();
			byte[] samples = new byte[BUFFER_SIZE];
			int count = 0;
			while ((count = ais.read(samples, 0, BUFFER_SIZE)) != -1) {
				line.write(samples, 0, count);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
		catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		finally {
			if (line != null) {
				line.drain();
				line.close();
			}
		}
	}

	/**
	 * Loops an audio file (in .wav, .mid, or .au format) in a background thread.
	 *
	 * @param filename the name of the audio file
	 * @throws IllegalArgumentException if {@code filename} is {@code null}
	 */
	public static synchronized void loop(String filename) {
		if (filename == null) throw new IllegalArgumentException();

		// code adapted from: http://stackoverflow.com/questions/26305/how-can-i-play-sound-in-java
		try {
			Clip clip = AudioSystem.getClip();
			InputStream is = MiscSoundUtil.class.getResourceAsStream(filename);
			AudioInputStream ais = AudioSystem.getAudioInputStream(is);
			clip.open(ais);
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		catch (UnsupportedAudioFileException e) {
			throw new IllegalArgumentException("unsupported audio format: '" + filename + "'", e);
		}
		catch (LineUnavailableException e) {
			throw new IllegalArgumentException("could not play '" + filename + "'", e);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("could not play '" + filename + "'", e);
		}
	}

	/**
	 * Creates notes based on frequency, duration, and amplitude inputs. 
	 * 
	 * @param hz input frequency of note
	 * @param duration input duration of note
	 * @param amplitude input amplitude of note
	 * @return double array containing notes
	 */
	private static double[] note(double hz, double duration, double amplitude) {
		int n = (int) (MiscSoundUtil.SAMPLE_RATE * duration);
		double[] a = new double[n+1];
		for (int i = 0; i <= n; i++)
			a[i] = amplitude * Math.sin(2 * Math.PI * i * hz / MiscSoundUtil.SAMPLE_RATE);
		return a;
	}

	public static void MIDIData(File audioFile) {
		Sequence sequence;
		try {
			sequence = MidiSystem.getSequence(audioFile);
			int trackNumber = 0;
			for (Track track :  sequence.getTracks()) {
				trackNumber++;
				System.out.println("Track " + trackNumber + ": size = " + track.size());
				System.out.println();
				for (int i=0; i < track.size(); i++) { 
					MidiEvent event = track.get(i);
					System.out.print("@" + event.getTick() + " ");
					MidiMessage message = event.getMessage();
					if (message instanceof ShortMessage) {
						ShortMessage sm = (ShortMessage) message;
						//System.out.print("Channel: " + sm.getChannel() + " ");
						if (sm.getCommand() == NOTE_ON) {
							int key = sm.getData1();
							int octave = (key / 12)-1;
							int note = key % 12;
							String noteName = NOTE_NAMES[note];
							int velocity = sm.getData2();
							System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
						} else if (sm.getCommand() == NOTE_OFF) {
							int key = sm.getData1();
							int octave = (key / 12)-1;
							int note = key % 12;
							String noteName = NOTE_NAMES[note];
							int velocity = sm.getData2();
							System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
						} else {
							System.out.println("Command:" + sm.getCommand());
						}
					} else {
						System.out.println("Other message: " + message.getClass());
					}
				}

				System.out.println();
			}
		} catch (InvalidMidiDataException | IOException e) {
			e.printStackTrace();
		}

		

	}


}