/* Shark.java */

/**
 *  The Shark class defines an object that models the behavior of a Shark
 *	in an ocean object. It inherits fields and methods from the Creature class
 *	It includes a constructors of the form:
 *
 *      public Shark(int starveTime);
 *		AND
 *		public Shark(int starveTime, int feeding);
 *
 *  The first constructor creates a fully-fed shark that will die after 
 *	starveTime timesteps. The second constructor creates a shark with a hunger
 *	level denoted by FEEDING and can go for a maximum of starveTime
 *	timesteps without eating.
 */

public class Shark extends Creature{

	/* ***Shark Fields*** */
	private int starveTimeFull;
	private int timeToDeath;


	/* ***Shark Constructors*** */
	public Shark(int starveTime){
		super(Ocean.SHARK);
		starveTimeFull = starveTime;
		timeToDeath = starveTime;
	}
	
	public Shark(int starveTime, int feeding){
		super(Ocean.SHARK);
		starveTimeFull = starveTime;
		timeToDeath = feeding;
	}


	/* ***Shark Methods*** */
	public void resetHunger(){
		timeToDeath = starveTimeFull;
	}

	public void starve(){
		timeToDeath--;
	}

	public int getTimeToDeath(){
		return timeToDeath;
	}
}