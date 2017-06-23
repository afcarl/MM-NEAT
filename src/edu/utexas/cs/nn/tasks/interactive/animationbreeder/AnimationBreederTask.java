package edu.utexas.cs.nn.tasks.interactive.animationbreeder;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.utexas.cs.nn.MMNEAT.MMNEAT;
import edu.utexas.cs.nn.evolution.genotypes.Genotype;
import edu.utexas.cs.nn.networks.Network;
import edu.utexas.cs.nn.parameters.Parameters;
import edu.utexas.cs.nn.scores.Score;
import edu.utexas.cs.nn.tasks.interactive.InteractiveEvolutionTask;
import edu.utexas.cs.nn.util.graphics.AnimationUtil;

/**
 * Interface that interactively evolves originally generated animations
 * from a CPPN. Uses the interactive evolution interface to complete this.
 * 
 * @author Isabel Tweraser
 *
 * @param <T>
 */
public class AnimationBreederTask<T extends Network> extends InteractiveEvolutionTask<T>{

	protected JSlider animationLength;
	protected JSlider pauseLength;
	protected JSlider pauseLengthBetweenFrames;

	protected boolean alwaysAnimate = Parameters.parameters.booleanParameter("alwaysAnimate");

	protected BufferedImage[] getAnimationImages(T cppn, int startFrame, int endFrame, boolean beingSaved) {
		return AnimationUtil.imagesFromCPPN(cppn, picSize, picSize, startFrame, endFrame, getInputMultipliers());
	}

	// use private inner class to run animation in a loop
	protected class AnimationThread extends Thread {
		private int imageID;
		private boolean abort;


		public AnimationThread(int imageID) {
			this.imageID = imageID;
			this.abort = false;
		}


		public void run() {
			if(showNetwork) {
				stopAnimation();
//				if(alwaysAnimate) {
//				for(int x = 0; x < animationThreads.length; x++) {
//					if(animationThreads[x] != null) animationThreads[x].stopAnimation();
//				}
//				for(int i = 0; i < animations.length; i++) {
//					// Cannot clear animation if being loaded
//					synchronized(animations[i]) {
//						animations[i].clear();
//					}
//				}
//			}
			}
			int end = Parameters.parameters.integerParameter("defaultAnimationLength");
			// Only one thread can add frames at a time
			synchronized(animations[imageID]) {
				//adds images to array at index of specified button (imageID)
				if(animations[imageID].size() < Parameters.parameters.integerParameter("defaultAnimationLength")) {
					int start = animations[imageID].size();
					BufferedImage[] newFrames = getAnimationImages(scores.get(imageID).individual.getPhenotype(), start, end, false);
					for(BufferedImage bi : newFrames) {
						if(abort) break; // stop loading if animation is aborted
						animations[imageID].add(bi);
					}
				}
			}

			while(!abort) {
				// One animation loop
				for(int frame = 0; !abort && frame < end; frame++) {
					// set button over and over
					setButtonImage(animations[imageID].get(frame), imageID);
					try {
						// pause between frames (each image in animation)
						Thread.sleep(Parameters.parameters.integerParameter("defaultFramePause"));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}		
				}
				try {
					// pause between animations
					Thread.sleep(Parameters.parameters.integerParameter("defaultPause"));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}		
			}
		}

		public void stopAnimation() {
			abort = true;
		}		
	}

	public static final int CPPN_NUM_INPUTS	= 5;
	public static final int CPPN_NUM_OUTPUTS = 3;

	// stores all animations in an array with a different button's animation at each index
	public ArrayList<BufferedImage>[] animations;
	protected AnimationThread[] animationThreads;

	public AnimationBreederTask() throws IllegalAccessException {
		this(true);
	}

	/**
	 * Constructor - all sliders are added here and mouse listening is enabled for hovering over the buttons
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public AnimationBreederTask(boolean justAnimationBreeder) throws IllegalAccessException {
		super();
		animationThreads = new AnimationBreederTask.AnimationThread[Parameters.parameters.integerParameter("mu")];
		if(justAnimationBreeder) {
			//Construction of JSlider for desired animation length

			animationLength = new JSlider(JSlider.HORIZONTAL, Parameters.parameters.integerParameter("minAnimationLength"), Parameters.parameters.integerParameter("maxAnimationLength"), Parameters.parameters.integerParameter("defaultAnimationLength"));

			Hashtable<Integer,JLabel> animationLabels = new Hashtable<>();
			animationLength.setMinorTickSpacing(20);
			animationLength.setPaintTicks(true);
			animationLabels.put(Parameters.parameters.integerParameter("minAnimationLength"), new JLabel("Short"));
			animationLabels.put(Parameters.parameters.integerParameter("maxAnimationLength"), new JLabel("Long"));
			animationLength.setLabelTable(animationLabels);
			animationLength.setPaintLabels(true);
			animationLength.setPreferredSize(new Dimension(150, 40));

			/**
			 * Implements ChangeListener to adjust animation length. When animation length is specified, 
			 * input length is used to reset and redraw buttons. 
			 */
			animationLength.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					// get value
					JSlider source = (JSlider)e.getSource();
					if(!source.getValueIsAdjusting()) {
						int newLength = (int) source.getValue();
						Parameters.parameters.setInteger("defaultAnimationLength", newLength);
						// reset buttons
						resetButtons(false); // do not clear out cached animation frames
					}
				}
			});

			//Construction of JSlider for desired length of pause between each iteration of animation

			pauseLength = new JSlider(JSlider.HORIZONTAL, Parameters.parameters.integerParameter("minPause"), Parameters.parameters.integerParameter("maxPause"), Parameters.parameters.integerParameter("defaultPause"));

			Hashtable<Integer,JLabel> pauseLabels = new Hashtable<>();
			pauseLength.setMinorTickSpacing(75);
			pauseLength.setPaintTicks(true);
			pauseLabels.put(Parameters.parameters.integerParameter("minPause"), new JLabel("No pause"));
			pauseLabels.put(Parameters.parameters.integerParameter("maxPause"), new JLabel("Long pause"));
			pauseLength.setLabelTable(pauseLabels);
			pauseLength.setPaintLabels(true);
			pauseLength.setPreferredSize(new Dimension(100, 40));

			/**
			 * Implements ChangeListener to adjust pause length. When pause length is specified, 
			 * input length is used to reset and redraw buttons. 
			 */
			pauseLength.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					// get value
					JSlider source = (JSlider)e.getSource();
					if(!source.getValueIsAdjusting()) {
						int newLength = (int) source.getValue();
						Parameters.parameters.setInteger("defaultPause", newLength);
						// reset buttons: necessary?
						//resetButtons();
					}
				}
			});

			//Add all JSliders to individual panels so that they can be titled with JLabels

			//Animation slider
			JPanel animation = new JPanel();
			animation.setLayout(new BoxLayout(animation, BoxLayout.Y_AXIS));
			JLabel animationLabel = new JLabel();
			animationLabel.setText("Animation length");
			animation.add(animationLabel);
			animation.add(animationLength);

			//Pause (between animations) slider
			JPanel pause = new JPanel();
			pause.setLayout(new BoxLayout(pause, BoxLayout.Y_AXIS));
			JLabel pauseLabel = new JLabel();
			pauseLabel.setText("Pause between animations");
			pause.add(pauseLabel);
			pause.add(pauseLength);


			//Add all panels to interface
			top.add(animation);
			top.add(pause);
		}

		//Construction of JSlider for desired length of pause between each frame within an animation

		pauseLengthBetweenFrames = new JSlider(JSlider.HORIZONTAL, Parameters.parameters.integerParameter("minPause"), Parameters.parameters.integerParameter("maxPause"), Parameters.parameters.integerParameter("defaultFramePause"));

		Hashtable<Integer,JLabel> framePauseLabels = new Hashtable<>();
		pauseLengthBetweenFrames.setMinorTickSpacing(75);
		pauseLengthBetweenFrames.setPaintTicks(true);
		framePauseLabels.put(Parameters.parameters.integerParameter("minPause"), new JLabel("No pause"));
		framePauseLabels.put(Parameters.parameters.integerParameter("maxPause"), new JLabel("Long pause"));
		pauseLengthBetweenFrames.setLabelTable(framePauseLabels);
		pauseLengthBetweenFrames.setPaintLabels(true);
		pauseLengthBetweenFrames.setPreferredSize(new Dimension(100, 40));

		/**
		 * Implements ChangeListener to adjust frame pause length. When frame pause length is specified, 
		 * input length is used to reset and redraw buttons. 
		 */
		pauseLengthBetweenFrames.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// get value
				JSlider source = (JSlider)e.getSource();
				if(!source.getValueIsAdjusting()) {
					int newLength = (int) source.getValue();
					Parameters.parameters.setInteger("defaultFramePause", newLength);
					// reset buttons: necessary?
					//resetButtons();
				}
			}
		});		

		//Pause (between frames) slider
		JPanel framePause = new JPanel();
		framePause.setLayout(new BoxLayout(framePause, BoxLayout.Y_AXIS));
		JLabel framePauseLabel = new JLabel();
		framePauseLabel.setText("Pause between frames");
		framePause.add(framePauseLabel);
		framePause.add(pauseLengthBetweenFrames);

		if(!simplifiedInteractiveInterface) {
			top.add(framePause);
		}

		if(!alwaysAnimate) {
			//Enables MouseListener so that animation on a button will play when mouse is hovering over it
			for(JButton button: buttons) {
				button.addMouseListener(new MouseListener() {

					AnimationThread animation;

					@Override
					public void mouseClicked(MouseEvent e) { } // Do not use
					@Override
					public void mousePressed(MouseEvent e) { } // Do not use
					@Override
					public void mouseReleased(MouseEvent e) { } // Do not use

					@Override
					public void mouseEntered(MouseEvent e) {
						// ugly handling of button id extraction.
						Scanner id = new Scanner(e.toString());
						id.next(); // throw away tokens in string name
						id.next(); // the third will be the id number
						animation = new AnimationThread(id.nextInt());
						id.close();
						animation.start();
					}

					@Override
					public void mouseExited(MouseEvent e) {
						animation.stopAnimation(); //exits loop in which animation is played
					}
				});
			}
		}
	}

	@Override
	public String[] sensorLabels() {
		return new String[] { "X-coordinate", "Y-coordinate", "distance from center", "time", "bias" };
	}

	@Override
	public String[] outputLabels() {
		return new String[] { "hue-value", "saturation-value", "brightness-value" };
	}

	@Override
	protected String getWindowTitle() {
		return "AnimationBreeder";
	}

	@Override
	protected void save(int i) {
		// Use of imageHeight and imageWidth allows saving a higher quality image than is on the button
		BufferedImage[] toSave = getAnimationImages(scores.get(i).individual.getPhenotype(), 0, Parameters.parameters.integerParameter("defaultAnimationLength"), true);
		JFileChooser chooser = new JFileChooser();//used to get save name 
		chooser.setApproveButtonText("Save");
		FileNameExtensionFilter filter = new FileNameExtensionFilter("GIF", "gif");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(frame);
		if(returnVal == JFileChooser.APPROVE_OPTION) {//if the user decides to save the image
			System.out.println("You chose to call the image: " + chooser.getSelectedFile().getName());
			try {
				//saves gif to chosen file name
				AnimationUtil.createGif(toSave, Parameters.parameters.integerParameter("defaultFramePause"), chooser.getCurrentDirectory() + "\\" + chooser.getSelectedFile().getName() + ".gif");
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("image " + chooser.getSelectedFile().getName() + " was saved successfully");
		} else { //else image dumped
			System.out.println("image not saved");
		}
	}

	@Override
	protected BufferedImage getButtonImage(T phenotype, int width, int height, double[] inputMultipliers) {
		// Just get first frame for button. Slightly inefficent though, since all animation frames were pre-computed
		return AnimationUtil.imagesFromCPPN(phenotype, picSize, picSize, 0, 1, getInputMultipliers())[0];
	}

	@Override
	protected void additionalButtonClickAction(int scoreIndex, Genotype<T> individual) {
		// do nothing
	}

	public ArrayList<Score<T>> evaluateAll(ArrayList<Genotype<T>> population) {
		clearAnimations(population.size());
		return super.evaluateAll(population); // wait for user choices
	}

	@SuppressWarnings("unchecked")
	private void clearAnimations(int num) {
		// Load all Image arrays with animations
		animations = new ArrayList[num];
		for(int i = 0; i < animations.length; i++) {
			animations[i] = new ArrayList<BufferedImage>();
		}		
	}

	protected void setUndo() {
		clearAnimations(scores.size());
		if(alwaysAnimate) {
			for(int x = 0; x < animationThreads.length; x++) {
				if(animationThreads[x] != null) animationThreads[x].stopAnimation();
			}
		}
		super.setUndo();
	}

	@Override
	public void resetButtons(boolean hardReset) {
		super.resetButtons(hardReset);
		if(alwaysAnimate) {
			for(int x = 0; x < animationThreads.length; x++) {
				if(animationThreads[x] != null) animationThreads[x].stopAnimation();
			}
		}
		if(hardReset) {
			//Clears out all pre-computed animations so that checking/unchecking boxes actually creates new animations
			for(int i = 0; i < animations.length; i++) {
				// Cannot clear animation if being loaded
				synchronized(animations[i]) {
					animations[i].clear();
				}
			}
		}
		if(alwaysAnimate) {
			for(int x = 0; x < animationThreads.length; x++) {
				animationThreads[x] = new AnimationThread(x);
				animationThreads[x].start();
			}
		}
	}

	@Override
	protected void resetButton(Genotype<T> individual, int x) {
		super.resetButton(individual, x);
		if(alwaysAnimate) {
			if(animationThreads[x] != null) animationThreads[x].stopAnimation();
			animationThreads[x] = new AnimationThread(x);
			animationThreads[x].start();
		}
	}

	@Override
	protected void reset() {
		if(alwaysAnimate) {
			for(int x = 0; x < animationThreads.length; x++) {
				if(animationThreads[x] != null) animationThreads[x].stopAnimation();
			}
		}
		//Clears out all pre-computed animations so that checking/unchecking boxes actually creates new animations
		for(int i = 0; i < animations.length; i++) {
			// Cannot clear animation if being loaded
			synchronized(animations[i]) {
				animations[i].clear();
			}
		}
		super.reset();
	}

	@Override
	protected void evolve() {
		super.evolve();
		if(alwaysAnimate) {
			for(int x = 0; x < animationThreads.length; x++) {
				if(animationThreads[x] != null) animationThreads[x].stopAnimation();
			}
		}
	}

	@Override
	public int numCPPNInputs() {
		return CPPN_NUM_INPUTS;
	}

	@Override
	public int numCPPNOutputs() {
		return CPPN_NUM_OUTPUTS;
	}

	/**
	 * Allows for quick and easy launching without saving any files
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MMNEAT.main(new String[]{"runNumber:5","randomSeed:5","trials:1","mu:16","maxGens:500","io:false","netio:false","mating:true","task:edu.utexas.cs.nn.tasks.interactive.animationbreeder.AnimationBreederTask","allowMultipleFunctions:true","ftype:0","netChangeActivationRate:0.3","cleanFrequency:-1","recurrency:false","ea:edu.utexas.cs.nn.evolution.selectiveBreeding.SelectiveBreedingEA","imageWidth:500","imageHeight:500","imageSize:200"});
		} catch (FileNotFoundException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
}
