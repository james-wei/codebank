/* RunLengthEncoding.java */

/**
 *  The RunLengthEncoding class defines an object that run-length encodes an
 *  Ocean object.  Descriptions of the methods you must implement appear below.
 *  They include constructors of the form
 *
 *      public RunLengthEncoding(int i, int j, int starveTime);
 *      public RunLengthEncoding(int i, int j, int starveTime,
 *                               int[] runTypes, int[] runLengths) {
 *      public RunLengthEncoding(Ocean ocean) {
 *
 *  that create a run-length encoding of an Ocean having width i and height j,
 *  in which sharks starve after starveTime timesteps.
 *
 *  The first constructor creates a run-length encoding of an Ocean in which
 *  every cell is empty.  The second constructor creates a run-length encoding
 *  for which the runs are provided as parameters.  The third constructor
 *  converts an Ocean object into a run-length encoding of that object.
 *
 *  See the README file accompanying this project for additional details.
 */

public class RunLengthEncoding {

  /**
   *  Define any variables associated with a RunLengthEncoding object here.
   *  These variables MUST be private.
   */
		
	private int width;
	private int height;
	private int starveTime;
	private DList runs;

  /**
   *  The following methods are required for Part II.
   */

  /**
   *  RunLengthEncoding() (with three parameters) is a constructor that creates
   *  a run-length encoding of an empty ocean having width i and height j,
   *  in which sharks starve after starveTime timesteps.
   *  @param i is the width of the ocean.
   *  @param j is the height of the ocean.
   *  @param starveTime is the number of timesteps sharks survive without food.
   */

  public RunLengthEncoding(int i, int j, int starveTime) {
    // Your solution here.
  	width = i;
  	height = j;
  	this.starveTime = starveTime;
  	
  	runs = new DList();

    int[] item = {Ocean.EMPTY, width*height};
  	DListNode emptyOceanNode = new DListNode(item);
  	runs.addLast(emptyOceanNode);
  }

  /**
   *  RunLengthEncoding() (with five parameters) is a constructor that creates
   *  a run-length encoding of an ocean having width i and height j, in which
   *  sharks starve after starveTime timesteps.  The runs of the run-length
   *  encoding are taken from two input arrays.  Run i has length runLengths[i]
   *  and species runTypes[i].
   *  @param i is the width of the ocean.
   *  @param j is the height of the ocean.
   *  @param starveTime is the number of timesteps sharks survive without food.
   *  @param runTypes is an array that represents the species represented by
   *         each run.  Each element of runTypes is Ocean.EMPTY, Ocean.FISH,
   *         or Ocean.SHARK.  Any run of sharks is treated as a run of newborn
   *         sharks (which are equivalent to sharks that have just eaten).
   *  @param runLengths is an array that represents the length of each run.
   *         The sum of all elements of the runLengths array should be i * j.
   */

  public RunLengthEncoding(int i, int j, int starveTime,
                           int[] runTypes, int[] runLengths) {
    // Your solution here.
  	width = i;
  	height = j;
  	this.starveTime = starveTime;
  	
  	runs = new DList();
  	
  	for (int k = 0; k < runTypes.length && k < runLengths.length; k++){
  		int[] item = new int[3];
      if (runTypes[k] == Ocean.SHARK){
      	item[0] = runTypes[k];
        item[1] = runLengths[k];
        item[2] = starveTime;
  		} else {
      	item[0] = runTypes[k];
        item[1] = runLengths[k];
        item[2] = Ocean.NULL_HUNGER;
  		}
    	DListNode runNode = new DListNode(item);
    	runs.addLast(runNode);
  	}  	
  }

  /**
   *  restartRuns() and nextRun() are two methods that work together to return
   *  all the runs in the run-length encoding, one by one.  Each time
   *  nextRun() is invoked, it returns a different run (represented as an
   *  array of two ints), until every run has been returned.  The first time
   *  nextRun() is invoked, it returns the first run in the encoding, which
   *  contains cell (0, 0).  After every run has been returned, nextRun()
   *  returns null, which lets the calling program know that there are no more
   *  runs in the encoding.
   *
   *  The restartRuns() method resets the enumeration, so that nextRun() will
   *  once again enumerate all the runs as if nextRun() were being invoked for
   *  the first time.
   *
   *  (Note:  Don't worry about what might happen if nextRun() is interleaved
   *  with addFish() or addShark(); it won't happen.)
   */

  /**
   *  restartRuns() resets the enumeration as described above, so that
   *  nextRun() will enumerate all the runs from the beginning.
   */

  public void restartRuns() {
    // Your solution here.
  	runs.resetTracker();
  }

  /**
   *  nextRun() returns the next run in the enumeration, as described above.
   *  If the runs have been exhausted, it returns null.  The return value is
   *  an array of two ints (constructed here), representing the type and the
   *  size of the run, in that order.
   *  @return the next run in the enumeration, represented by an array of
   *          two ints.  The int at index zero indicates the run type
   *          (Ocean.EMPTY, Ocean.SHARK, or Ocean.FISH).  The int at index one
   *          indicates the run length (which must be at least 1).
   */

  public int[] nextRun() {
    // Replace the following line with your solution.
  	if (runs.isNextRun()){
      DListNode returnRun = runs.trackerValue();
      int runType = returnRun.getRunType();
      int runLength = returnRun.getRunLength();
   		int[] runArray = {runType,runLength};
  		return runArray;
  	} else {
  		return null;
  	}
  }

  /**
   *  toOcean() converts a run-length encoding of an ocean into an Ocean
   *  object.  You will need to implement the three-parameter addShark method
   *  in the Ocean class for this method's use.
   *  @return the Ocean represented by a run-length encoding.
   */

  public Ocean toOcean() {
    // Replace the following line with your solution.
  	
  	Ocean convertedOcean = new Ocean(width, height, starveTime);
  	
  	DListNode currNode = runs.getFirst();
  	
  	int insertX = 0;
  	int insertY = 0;
  	
  	for (int i = 0; i < runs.getSize(); i++){
  		switch (currNode.getRunType()){
  			case Ocean.EMPTY:
  				insertX += currNode.getRunLength();
  				while(insertX > width){
  					insertX -= width;
  					insertY++;
  				}
  				break;
  		
  			case Ocean.FISH:
  				int numFishToAdd = currNode.getRunLength();
  				while (numFishToAdd > 0){
  					if (insertX >= width){
  						insertX -= width;
  						insertY++;
  					}
  					convertedOcean.addFish(insertX, insertY);
  					insertX++;
  					numFishToAdd--;
  				}
  				break;
  		
  			case Ocean.SHARK:
  				int numSharksToAdd = currNode.getRunLength();
  				while (numSharksToAdd > 0){
  					if (insertX >= width){
  						insertX -= width;
  						insertY++;
  					}
  					convertedOcean.addShark(insertX, insertY, currNode.getRunHunger());
  					insertX++;
  					numSharksToAdd--;
  				}
  				break;
  		}
  		currNode = runs.getNext(currNode);
  	}
  	return convertedOcean;
  }

  /**
   *  The following method is required for Part III.
   */

  /**
   *  RunLengthEncoding() (with one parameter) is a constructor that creates
   *  a run-length encoding of an input Ocean.  You will need to implement
   *  the sharkFeeding method in the Ocean class for this constructor's use.
   *  @param sea is the ocean to encode.
   */

  public RunLengthEncoding(Ocean sea) {
    // Your solution here, but you should probably leave the following line
    // at the end.
  	
  	width = sea.width();
  	height = sea.height();
  	starveTime = sea.starveTime();
  	
  	runs = new DList();
    
  	int currEncodeType = sea.cellContents(0, 0);
  	int currEncodeLength = 0;
  	int currSharkHunger = sea.sharkFeeding(0,0);
  	  	  	
  	for (int y = 0; y < sea.height(); y++){
  		for (int x = 0; x < sea.width(); x++){
  			if (sea.cellContents(x, y) == currEncodeType && sea.sharkFeeding(x,y) == currSharkHunger){
  				currEncodeLength++;
  			} else {
          //Make DListNode for previous count/type
          int[] item = {currEncodeType, currEncodeLength, currSharkHunger};
  	    	DListNode runNode = new DListNode(item);
  	    	runs.addLast(runNode);
  				
  				//Initialize new count
  		  	currEncodeType = sea.cellContents(x, y);
  		  	currEncodeLength = 1;
  		  	currSharkHunger = sea.sharkFeeding(x,y);
  			}
  		}
  	}

  	if (currEncodeLength > 0){
        int[] item = {currEncodeType, currEncodeLength, currSharkHunger};
        DListNode runNode = new DListNode(item);
        runs.addLast(runNode);
  	}

  	check();
  }

  /**
   *  The following methods are required for Part IV.
   */

  /**
   *  addFish() places a fish in cell (x, y) if the cell is empty.  If the
   *  cell is already occupied, leave the cell as it is.  The final run-length
   *  encoding should be compressed as much as possible; there should not be
   *  two consecutive runs of sharks with the same degree of hunger.
   *  @param x is the x-coordinate of the cell to place a fish in.
   *  @param y is the y-coordinate of the cell to place a fish in.
   */

  public void addFish(int x, int y) {
    // Your solution here, but you should probably leave the following line
    // at the end.
    int encodedPosition = getEncodedPosition(x, y);

    DListNode currRun = runs.getFirst();
    while (encodedPosition > 0){
      encodedPosition -= currRun.getRunLength();
      currRun = runs.getNext(currRun);
    }

    if (encodedPosition < 0){
      currRun = runs.getPrev(currRun);
    }

    //Check if the current run is empty
    if (currRun.getRunType() != Ocean.EMPTY){
      check();
      return;
    }

    //CASE: the length of the current run is exactly 1
    if (currRun.getRunLength() == 1) {
      
      int prevRunType = (runs.getPrev(currRun)).getRunType();
      int nextRunType = (runs.getNext(currRun)).getRunType();

        //Compress forwards and backwards
      if (prevRunType == Ocean.FISH && nextRunType == Ocean.FISH){
        int newRunLength = 1 + (runs.getPrev(currRun)).getRunLength() + 
                               (runs.getNext(currRun)).getRunLength();
        
        runs.removeNode(runs.getPrev(currRun));
        runs.removeNode(runs.getNext(currRun));
        
        currRun.setType(Ocean.FISH);
        currRun.setRunLength(newRunLength);
        currRun.setHunger(Ocean.NULL_HUNGER);

        check();
        return;
      }
        //Compress only forwards
      else if (nextRunType == Ocean.FISH){
        int newRunLength = 1 + (runs.getNext(currRun)).getRunLength();
        
        runs.removeNode(runs.getNext(currRun));
        
        currRun.setType(Ocean.FISH);
        currRun.setRunLength(newRunLength);
        currRun.setHunger(Ocean.NULL_HUNGER);

        check();
        return;
      }
        //Compress only backwards
      else if (prevRunType == Ocean.FISH){
        int newRunLength = 1 + (runs.getPrev(currRun)).getRunLength();
        
        runs.removeNode(runs.getPrev(currRun));
        
        currRun.setType(Ocean.FISH);
        currRun.setRunLength(newRunLength);
        currRun.setHunger(Ocean.NULL_HUNGER);

        check();
        return;
      }
        //No compression -- change type of current run
      else{
        currRun.setType(Ocean.FISH);
        currRun.setHunger(Ocean.NULL_HUNGER);

        check();
        return;
      }
    }

    //CASE: the length of the current run is greater than 1
    else{
      //Subcase: Inserting at the beginning of a run
      if (encodedPosition == 0){
        int prevRunType = (runs.getPrev(currRun)).getRunType();
        
        //Compress backwards
        if (prevRunType == Ocean.FISH){
          int newFishRunLength = 1 + (runs.getPrev(currRun)).getRunLength();
          (runs.getPrev(currRun)).setRunLength(newFishRunLength);
          currRun.setRunLength(currRun.getRunLength() - 1);
          check();
          return;
        }
        
        //Break up the current run
        else {
          int[] newFishData = {Ocean.FISH, 1, Ocean.NULL_HUNGER};
          DListNode newFishNode = new DListNode(newFishData);
          runs.addBefore(currRun, newFishNode);
          currRun.setRunLength(currRun.getRunLength() - 1);
          check();
          return;
        }
      }
      
      //Subcase: Inserting at the end of a run
      else if (encodedPosition == -1){
        int nextRunType = (runs.getNext(currRun)).getRunType();
        
        //Compress forwards
        if (nextRunType == Ocean.FISH){
          int newFishRunLength = 1 + (runs.getNext(currRun)).getRunLength();
          (runs.getNext(currRun)).setRunLength(newFishRunLength);
          currRun.setRunLength(currRun.getRunLength() - 1);
          check();
          return;
        }
        
        //Break up the current run
        else {
          int[] newFishData = {Ocean.FISH, 1, Ocean.NULL_HUNGER};
          DListNode newFishNode = new DListNode(newFishData);
          runs.addAfter(currRun, newFishNode);
          currRun.setRunLength(currRun.getRunLength() - 1);
          check();
          return;
        }
      }

      //Subcase: Inserting in the middle of a run
      else{ //Must break up the current run
        int firstSplitLength = currRun.getRunLength() + encodedPosition;
        int secondSplitLength = currRun.getRunLength() - firstSplitLength - 1;

        int[] firstSplitData = {Ocean.EMPTY, firstSplitLength, Ocean.NULL_HUNGER};
        int[] secondSplitData = {Ocean.EMPTY, secondSplitLength, Ocean.NULL_HUNGER};

        DListNode firstSplit = new DListNode(firstSplitData);
        DListNode secondSplit = new DListNode(secondSplitData);

        currRun.setType(Ocean.FISH);
        currRun.setRunLength(1);
        currRun.setHunger(Ocean.NULL_HUNGER);

        runs.addBefore(currRun, firstSplit);
        runs.addAfter(currRun,secondSplit);

        check();
        return;
      }
    }
  }

  /**
   *  addShark() (with two parameters) places a newborn shark in cell (x, y) if
   *  the cell is empty.  A "newborn" shark is equivalent to a shark that has
   *  just eaten.  If the cell is already occupied, leave the cell as it is.
   *  The final run-length encoding should be compressed as much as possible;
   *  there should not be two consecutive runs of sharks with the same degree
   *  of hunger.
   *  @param x is the x-coordinate of the cell to place a shark in.
   *  @param y is the y-coordinate of the cell to place a shark in.
   */

  public void addShark(int x, int y) {
    // Your solution here, but you should probably leave the following line
    // at the end.

    int encodedPosition = getEncodedPosition(x, y);

    DListNode currRun = runs.getFirst();
    while (encodedPosition > 0){
      encodedPosition -= currRun.getRunLength();
      currRun = runs.getNext(currRun);
    }

    if (encodedPosition < 0){
      currRun = runs.getPrev(currRun);
    }

    //Check if the current run is empty
    if (currRun.getRunType() != Ocean.EMPTY){
      check();
      return;
    }

    //CASE: the length of the current run is exactly 1
    if (currRun.getRunLength() == 1) {
      
      int prevRunType = (runs.getPrev(currRun)).getRunType();
      int nextRunType = (runs.getNext(currRun)).getRunType();
      int prevRunHunger = (runs.getPrev(currRun)).getRunHunger();
      int nextRunHunger = (runs.getNext(currRun)).getRunHunger();

        //Compress forwards and backwards
      if (prevRunType == Ocean.SHARK && prevRunHunger == starveTime &&
          nextRunType == Ocean.SHARK && nextRunHunger == starveTime){
        int newRunLength = 1 + (runs.getPrev(currRun)).getRunLength() + 
                               (runs.getNext(currRun)).getRunLength();
        
        runs.removeNode(runs.getPrev(currRun));
        runs.removeNode(runs.getNext(currRun));
        
        currRun.setType(Ocean.SHARK);
        currRun.setRunLength(newRunLength);
        currRun.setHunger(starveTime);

        check();
        return;
      }
        //Compress only forwards
      else if (nextRunType == Ocean.SHARK && nextRunHunger == starveTime){
        int newRunLength = 1 + (runs.getNext(currRun)).getRunLength();
        
        runs.removeNode(runs.getNext(currRun));
        
        currRun.setType(Ocean.SHARK);
        currRun.setRunLength(newRunLength);
        currRun.setHunger(starveTime);

        check();
        return;
      }
        //Compress only backwards
      else if (prevRunType == Ocean.SHARK && prevRunHunger == starveTime){
        int newRunLength = 1 + (runs.getPrev(currRun)).getRunLength();
        
        runs.removeNode(runs.getPrev(currRun));
        
        currRun.setType(Ocean.SHARK);
        currRun.setRunLength(newRunLength);
        currRun.setHunger(starveTime);

        check();
        return;
      }
        //No compression -- change type of current run
      else{
        currRun.setType(Ocean.SHARK);
        currRun.setHunger(starveTime);

        check();
        return;
      }
    }

    //CASE: length of run is greater than 1
    else{
      
      //Subcase: Inserting at the beginning of a run
      if (encodedPosition == 0){
        int prevRunType = (runs.getPrev(currRun)).getRunType();
        int prevRunHunger = (runs.getPrev(currRun)).getRunHunger();
        
        //Compress backwards
        if (prevRunType == Ocean.SHARK && prevRunHunger == starveTime){
          int newSharkRunLength = 1 + (runs.getPrev(currRun)).getRunLength();
          (runs.getPrev(currRun)).setRunLength(newSharkRunLength);
          currRun.setRunLength(currRun.getRunLength() - 1);
          check();
          return;
        }
        
        //Break up current run
        else {
          int[] newSharkData = {Ocean.SHARK, 1, starveTime};
          DListNode newSharkNode = new DListNode(newSharkData);
          runs.addBefore(currRun, newSharkNode);
          currRun.setRunLength(currRun.getRunLength() - 1);
          check();
          return;
        }
      }
      //Subcase: Inserting at the end of run
      else if (encodedPosition == -1){
        int nextRunType = (runs.getNext(currRun)).getRunType();
        int nextRunHunger = (runs.getNext(currRun)).getRunHunger();
        
        //Compress forwards
        if (nextRunType == Ocean.SHARK && nextRunHunger == starveTime){
          int newSharkRunLength = 1 + (runs.getNext(currRun)).getRunLength();
          (runs.getNext(currRun)).setRunLength(newSharkRunLength);
          currRun.setRunLength(currRun.getRunLength() - 1);
          check();
          return;
        }
        
        //Break up current run
        else {
          int[] newSharkData = {Ocean.SHARK, 1, starveTime};
          DListNode newSharkNode = new DListNode(newSharkData);
          runs.addAfter(currRun, newSharkNode);
          currRun.setRunLength(currRun.getRunLength() - 1);
          check();
          return;
        }
      }

      //Subcase: Inserting in the middle of a run
      else{ //Must break up the current run
        int firstSplitLength = currRun.getRunLength() + encodedPosition;
        int secondSplitLength = currRun.getRunLength() - firstSplitLength - 1;

        int[] firstSplitData = {Ocean.EMPTY, firstSplitLength, Ocean.NULL_HUNGER};
        int[] secondSplitData = {Ocean.EMPTY, secondSplitLength, Ocean.NULL_HUNGER};

        DListNode firstSplit = new DListNode(firstSplitData);
        DListNode secondSplit = new DListNode(secondSplitData);

        currRun.setType(Ocean.SHARK);
        currRun.setRunLength(1);
        currRun.setHunger(starveTime);

        runs.addBefore(currRun, firstSplit);
        runs.addAfter(currRun,secondSplit);

        check();
        return;
      }
    }
  }

  /**
   *  check() walks through the run-length encoding and prints an error message
   *  if two consecutive runs have the same contents, or if the sum of all run
   *  lengths does not equal the number of cells in the ocean.
   */

  private void check() {

  	int numCells = 0;
  	int total_runs = runs.getSize();
  	
  	DListNode currRun = runs.getFirst();
  	
    for (int i = 0; i < total_runs; i++){ // Loop through all runs
      numCells += currRun.getRunLength();

      /* Check that run-lengths are greater than 1 */
  		if (currRun.getRunLength() < 1){
  			printError(4);
  		}

      /* Check for uncompressed elements */
    	DListNode nextRun = runs.getNext(currRun);
  		if (currRun.getRunType() == Ocean.SHARK &&
  			  nextRun.getRunType() == Ocean.SHARK &&
  			  currRun.getRunHunger() == nextRun.getRunHunger()){
  			printError(1);
  		} else if (currRun.getRunType() == Ocean.FISH &&
  							 nextRun.getRunType() == Ocean.FISH){
  			printError(2);
    	} else if (currRun.getRunType() == Ocean.EMPTY &&
					 			 nextRun.getRunType() == Ocean.EMPTY){
    		printError(3);
    	}

      /* Check for appropriate hunger values */
      if (currRun.getRunType() == Ocean.SHARK && currRun.getRunHunger() < 0){
        printError(6);
      } else if ((currRun.getRunType() == Ocean.EMPTY || 
                  currRun.getRunType() == Ocean.FISH) && 
                  currRun.getRunHunger() != Ocean.NULL_HUNGER){
        printError(7);
      }

  		currRun = runs.getNext(currRun);  // Check the validity of the next run
  	}
  	
    /* Check that the number of compressed cells equals the number of original cells */
    if (numCells != width * height){
  		printError(5);
  	}
  }
  
  
  /* ***Helper Functions*** */
  
  /**
   *  printError() (with one parameter) takes an integer and prints out the 
   *  corresponding error message. printError() is called in the check() 
   *  function.
   */

  private void printError(int n){
  	switch(n){
	  	case 1:
	    	System.out.println("RunLengthEncoding Error: Consecutive sharks of the same hunger levels not correctly compressed.");
	    	break;
	  	case 2:
	    	System.out.println("RunLengthEncoding Error: Consecutive fish not correctly compressed.");
	  		break;
	  	case 3:
	    	System.out.println("RunLengthEncoding Error: Consecutive empty spaces not correctly compressed.");
	  		break;
	  	case 4:
	    	System.out.println("RunLength Error: the length of a run is less than 1.");
	  		break;
	  	case 5:
	  		System.out.println("RunLengthEncoding Error: Number of compressed cells doesn't equal number of original cells.");
	  		break;
      case 6:
        System.out.println("RunLengthEncoding Error: Shark with negative hunger.");
        break;
      case 7:
        System.out.println("RunLengthEncoding Error: FISH or EMPTY with hunger value that is not NULL_HUNGER.");
        break;
      default:
	  		System.out.println("RunLengthEncoding Error");
	  		break; 		
  	}
  }

  /**
   *  getEncodedPosition() (with two parameters) takes a position denoted by x 
   *  and y and returns the corresponding encoded position in a run-length
   *  encoding.
   *  @param x is the x-coordinate of the position
   *  @param y is the y-coordinate of the position
   *  @return the run-length encoding position corresponding to (x, y)
   */

  private int getEncodedPosition(int x, int y){
    int encodedPosition = 0;

    //Convert x and y coordinates
    x = x % width;
    if (x < 0){
      x += width;
    }
    
    y = y % height;
    if (y < 0){
      y += height;
    }

    while (y > 0){
      encodedPosition += width;
      y--;
    }
    encodedPosition += x;
    return encodedPosition;
  }

}
