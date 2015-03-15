/* DListNode.java */

/**
 *  The DListNode class defines an object that acts as a node in a doubly-linked
 *	list object DList. A DListNode stores an integer array in its ITEM field, 
 *	the next DListNode in its NEXT field, and the previous DListNode in its
 *	PREV field.
 *	It includes constructors of the form:
 *
 *	public DListNode(int[] item, DListNode prev, DListNode next);
 *	AND
 *	public DListNode(int[] item);
 *	AND
 *	public DListNode();
 *
 *  The first constructor creates a DListNode all appropriate specified fields.
 *	The second constructor creates a DListNode with ITEM stored as this.ITEM
 *	and sets the PREV and NEXT field to be NULL.
 *	The third constructor creates a DListNode object with PREV and NEXT set to
 *	NULL. An integer array of length 3 with elements 
 *	{Ocean.NULL_TYPE, Ocean.NULL_LENGTH, Ocean.NULL_HUNGER} is set to ITEM. The
 *	third constructor is intended to be used to construct the sentinel node in
 *	a DList.
 *	
 *	For the purposes of using DListNode objects in conjunction with 
 *	RunLengthEncoding.java, the element stored at index 0 of ITEM denotes the
 *	type of the run, the element stored at index 1 of ITEM denotes the length
 *	of the run, and the element stored at index 2 of ITEM denotes the hunger
 *	value of the run. 
 */

public class DListNode {

	/* ***DListNode Fields*** */
	private int[] item;
	private DListNode next, prev;


	/* ***DListNode Constructors*** */
	public DListNode(int[] item, DListNode prev, DListNode next){
		if (item == null){
			this.item = new int[3];
			this.item[0] = Ocean.NULL_TYPE;
			this.item[1] = Ocean.NULL_LENGTH;
			this.item[2] = Ocean.NULL_HUNGER;
		} else {
			this.item = item;
		}
		this.next = next;
		this.prev = prev;
	}

	public DListNode(int[] item){
		this(item, null, null);
	}

	public DListNode(){
		this(null, null, null);
	}


	/* ***DListNode Methods*** */
	public int[] getElement(){
		return item;
	}

	public DListNode getPrev(){
		return prev;
	}

	public DListNode getNext(){
		return next;
	}

	public void setElement(int[] n){
		item = n;
	}

	public void setPrev(DListNode newPrev){
		prev = newPrev;
	}

	public void setNext(DListNode newNext){
		next = newNext;
	}

	/* ***DListNode Methods for RunLengthEncoding*** */
	public void setType(int newType){
		if (item.length >= 1){
			item[0] = newType;	
		}
	}

	public void setRunLength(int newLength){
		if (item.length >= 2){
			item[1] = newLength;
		}
	}

	public void setHunger(int newHunger){
		if (item.length >= 3){
			item[2] = newHunger;
		}
	}

	public int getRunType(){
		if (item.length >= 1){
			return item[0];
		} else {
			return Ocean.NULL_TYPE;
		}
	}

	public int getRunLength(){
		if (item.length >= 2){
			return item[1];
		} else {
			return Ocean.NULL_LENGTH;
		}
	}

	public int getRunHunger(){
		if (item.length >= 1){
			return item[2];
		} else {
			return Ocean.NULL_HUNGER;
		}
	}
}