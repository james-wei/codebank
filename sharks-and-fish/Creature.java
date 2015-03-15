/* Creature.java */

/**
 *  The Creature class defines an object that models the behavior of a Creature
 *	in an Ocean object. They include a constructor of the form
 *
 *      public Creature(int type);
 *
 *  that creates a Creature of type EMPTY, FISH, or SHARK.
 */

public class Creature {

	/* ***Creature Fields*** */
	private int type;


	/* ***Creature Constructor*** */
	public Creature() {
		this.type = Ocean.EMPTY;
	}

	public Creature(int type) {
		this.type = type;
	}


	/* ***Creature Methods*** */
	public int getType() {
		return type;
	}
}