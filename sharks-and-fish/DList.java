/* DList.java */

/**
 *  The DList class functions as a manager for a circularly-linked doubly-linked
 *	list. A DList contains a sentinel node to keep track of the first and last
 *	nodes in a DList. A DList also tracks the number of nodes in the 
 *	doubly-linked list. It includes the constructor:
 *
 *		public DList();
 *
 *	The constructor creates an empty DList.
 *	
 *	For the purposes of using a DList in conjunction with RunLengthEncoding.java,
 *	a private field runTracker keeps track of the next run to be returned and
 *	numRunsReturned tracks the number of runs returned.
 */


public class DList {

	/* ***DList Fields*** */
	private int size;
	private DListNode sentinel, runTracker;
	private int numRunsReturned;


	/* ***DList Constructor*** */
	public DList(){
		size = 0;
		sentinel = new DListNode();
		sentinel.setNext(sentinel);
		sentinel.setPrev(sentinel);
		runTracker = sentinel;
		numRunsReturned = 0;
	}

	/* ***DList Methods*** */
	public int getSize(){
		return size;
	}

	public boolean isEmpty(){
		return size == 0;
	}

	public DListNode getFirst(){
		return sentinel.getNext();
	}

	public DListNode getLast(){
		return sentinel.getPrev();
	}

	public DListNode getPrev(DListNode n){
		return n.getPrev();
	}

	public DListNode getNext(DListNode n){
		return n.getNext();
	}

  	public void addBefore(DListNode currNode, DListNode newNode){
  		/* Adds newNode before currNode */
	    DListNode prevNode = getPrev(currNode);
	    newNode.setNext(currNode);
	    newNode.setPrev(prevNode);
	    prevNode.setNext(newNode);
	    currNode.setPrev(newNode);
	    size++;
    }   

	public void addAfter(DListNode currNode, DListNode newNode){
		/* Adds newNode after currNode */
	    DListNode nextNode = getNext(currNode);
	    newNode.setNext(nextNode);
	    newNode.setPrev(currNode);
	    nextNode.setPrev(newNode);
	    currNode.setNext(newNode);
	    size++;
	}
	
	public void addLast(DListNode newLastNode){
		/* Add newLastNode to the end of the DList */		
		DListNode currLastNode = getPrev(sentinel);
		newLastNode.setNext(sentinel);
		newLastNode.setPrev(currLastNode);
		currLastNode.setNext(newLastNode);
		sentinel.setPrev(newLastNode);
		size++;
	}
	
  	public void addFirst(DListNode newFirstNode){
  		/* Adds newFirstNode to the front of the DList */
		DListNode currFirstNode = getNext(sentinel);
		newFirstNode.setPrev(sentinel);
		newFirstNode.setNext(currFirstNode);
		currFirstNode.setPrev(newFirstNode);
		sentinel.setNext(newFirstNode);
		size++;
	}
		
	public void removeNode(DListNode n){
		/* Removes the DListNode n from the list */
		if (n == getNext(sentinel)){
			removeHead();
		} else if (n == getPrev(sentinel)){
			removeTail();
		} else {
			DListNode prevNode = getPrev(n);
			DListNode nextNode = getNext(n);
			prevNode.setNext(nextNode);
			nextNode.setPrev(prevNode);
			n.setPrev(null);
			n.setNext(null);
			size--;
		}
	}

	public void removeHead(){
		/* Removes the first node */
		DListNode remNode = getNext(sentinel);
		DListNode newHead = getNext(remNode);
		sentinel.setNext(newHead);
		newHead.setPrev(sentinel);
		remNode.setNext(null);
		remNode.setPrev(null);
		size--;
	}

	public void removeTail(){
		/* Removes the last node */
		DListNode remNode = getPrev(sentinel);
		DListNode newTail = getPrev(remNode);
		sentinel.setPrev(newTail);
		newTail.setNext(sentinel);
		remNode.setNext(null);
		remNode.setPrev(null);
		size--;
	}
	

	/* ***DList Methods for RunLengthEncoding*** */
	public DListNode trackerValue(){
		if (runTracker == sentinel){ // Upon tracking the first run
			runTracker = getNext(sentinel);
		}
		DListNode s = runTracker;
		incrementTracker();
		return s;
	}
	
	private void incrementTracker(){
		runTracker = getNext(runTracker);
		numRunsReturned++;
	}
	
	public void resetTracker(){
		runTracker = sentinel;
		numRunsReturned = 0;
	}
	
	public boolean isNextRun(){
		/* Returns true if there is a proceeding run to be parsed */
		return numRunsReturned < size;
	}
}