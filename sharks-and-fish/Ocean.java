/* Ocean.java */

/**
 *  The Ocean class defines an object that models an ocean full of sharks and
 *  fish.  Descriptions of the methods you must implement appear below.  They
 *  include a constructor of the form
 *
 *      public Ocean(int i, int j, int starveTime);
 *
 *  that creates an empty ocean having width i and height j, in which sharks
 *  starve after starveTime timesteps.
 *
 *  See the README file accompanying this project for additional details.
 */

public class Ocean {

  /**
   *  Do not rename these constants.  WARNING:  if you change the numbers, you
   *  will need to recompile Test4.java.  Failure to do so will give you a very
   *  hard-to-find bug.
   */

  public final static int EMPTY = 0;
  public final static int SHARK = 1;
  public final static int FISH = 2;

  public final static int NULL_TYPE = -10;
  public final static int NULL_LENGTH = -10;
  public final static int NULL_HUNGER = -10;

  /**
   *  Define any variables associated with an Ocean object here.  These
   *  variables MUST be private.
   */

  private Creature[][] ocean_array;
  private int starveTime;
  private int width;
  private int height;

  /**
   *  The following methods are required for Part I.
   */

  /**
   *  Ocean() is a constructor that creates an empty ocean having width i and
   *  height j, in which sharks starve after starveTime timesteps.
   *  @param i is the width of the ocean.
   *  @param j is the height of the ocean.
   *  @param starveTime is the number of timesteps sharks survive without food.
   */

  public Ocean(int i, int j, int starveTime) {
    // Your solution here.
    this.starveTime = starveTime;
    width = i;
    height = j;
    ocean_array = new Creature[i][j];

    for (int m = 0; m < width; m++) {        // Fill Ocean with EMPTY creatures
      for (int n = 0; n < height; n++){
        ocean_array[m][n] = new Creature(EMPTY);
      }
    }
  }

  /**
   *  width() returns the width of an Ocean object.
   *  @return the width of the ocean.
   */

  public int width() {
    // Replace the following line with your solution.
    return width;
  }

  /**
   *  height() returns the height of an Ocean object.
   *  @return the height of the ocean.
   */

  public int height() {
    // Replace the following line with your solution.
    return height;
  }

  /**
   *  starveTime() returns the number of timesteps sharks survive without food.
   *  @return the number of timesteps sharks survive without food.
   */

  public int starveTime() {
    // Replace the following line with your solution.
    return starveTime;
  }

  /**
   *  addFish() places a fish in cell (x, y) if the cell is empty.  If the
   *  cell is already occupied, leave the cell as it is.
   *  @param x is the x-coordinate of the cell to place a fish in.
   *  @param y is the y-coordinate of the cell to place a fish in.
   */

  public void addFish(int x, int y) {
    // Your solution here.
    if (cellContents(x,y) == EMPTY){
      ocean_array[convertXCoord(x)][convertYCoord(y)] = new Fish();
    }
  }

  /**
   *  addShark() (with two parameters) places a newborn shark in cell (x, y) if
   *  the cell is empty.  A "newborn" shark is equivalent to a shark that has
   *  just eaten.  If the cell is already occupied, leave the cell as it is.
   *  @param x is the x-coordinate of the cell to place a shark in.
   *  @param y is the y-coordinate of the cell to place a shark in.
   */

  public void addShark(int x, int y) {
    // Your solution here.
    if (cellContents(x,y) == EMPTY){
      ocean_array[convertXCoord(x)][convertYCoord(y)] = new Shark(starveTime);
    }
  }

  /**
   *  cellContents() returns EMPTY if cell (x, y) is empty, FISH if it contains
   *  a fish, and SHARK if it contains a shark.
   *  @param x is the x-coordinate of the cell whose contents are queried.
   *  @param y is the y-coordinate of the cell whose contents are queried.
   */

  public int cellContents(int x, int y) {
    // Replace the following line with your solution.
    return ocean_array[convertXCoord(x)][convertYCoord(y)].getType();
  }

  /**
   *  timeStep() performs a simulation timestep as described in README.
   *  @return an ocean representing the elapse of one timestep.
   */

  public Ocean timeStep() {
    // Replace the following line with your solution.
    Ocean afterOcean = new Ocean(width, height, starveTime);

    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++){

        afterOcean.ocean_array[i][j] = ocean_array[i][j];

        switch(ocean_array[i][j].getType()){
          
          case SHARK:
            Creature currShark = afterOcean.ocean_array[i][j];
            if (numNeighborType(i, j, FISH) > 0){                   // Implement RULE 1
              ((Shark)currShark).resetHunger();
            } else {                                                // Implement RULE 2
              ((Shark)currShark).starve();
              if (((Shark)currShark).getTimeToDeath() < 0){
                afterOcean.ocean_array[i][j] = new Creature(EMPTY); // Shark dies
              }
            }
            break;

          case FISH:
            if (numNeighborType(i, j, SHARK) == 0) {                // Implement RULE 3
              break;
            } else if (numNeighborType(i, j, SHARK) == 1) {         // Implement RULE 4
              afterOcean.ocean_array[i][j] = new Creature(EMPTY);   // Fish is eaten
            } else if (numNeighborType(i, j, SHARK) >= 2){          // Implement RULE 5
              afterOcean.ocean_array[i][j] = new Shark(starveTime);
            }
            break;

          case EMPTY:
            if (numNeighborType(i, j, FISH) < 2){                   // Implement RULE 6
              break;
            } else if (numNeighborType(i, j, FISH) >= 2 && 
                       numNeighborType(i, j, SHARK) <= 1){          // Implement RULE 7
              afterOcean.ocean_array[i][j] = new Fish();
            } else if (numNeighborType(i, j, FISH) >= 2 && 
                       numNeighborType(i, j, SHARK) >= 2){          // Implement RULE 8
              afterOcean.ocean_array[i][j] = new Shark(starveTime);
            }
            break;
        }
      }
    }
    return afterOcean;
  }

  /**
   *  The following method is required for Part II.
   */

  /**
   *  addShark() (with three parameters) places a shark in cell (x, y) if the
   *  cell is empty.  The shark's hunger is represented by the third parameter.
   *  If the cell is already occupied, leave the cell as it is.  You will need
   *  this method to help convert run-length encodings to Oceans.
   *  @param x is the x-coordinate of the cell to place a shark in.
   *  @param y is the y-coordinate of the cell to place a shark in.
   *  @param feeding is an integer that indicates the shark's hunger.  You may
   *         encode it any way you want; for instance, "feeding" may be the
   *         last timestep the shark was fed, or the amount of time that has
   *         passed since the shark was last fed, or the amount of time left
   *         before the shark will starve.  It's up to you, but be consistent.
   */

  public void addShark(int x, int y, int feeding) {
    // Your solution here.
    if (cellContents(x,y) == EMPTY){
      ocean_array[convertXCoord(x)][convertYCoord(y)] = new Shark(starveTime, feeding);
    }
  }

  /**
   *  The following method is required for Part III.
   */

  /**
   *  sharkFeeding() returns an integer that indicates the hunger of the shark
   *  in cell (x, y), using the same "feeding" representation as the parameter
   *  to addShark() described above.  If cell (x, y) does not contain a shark,
   *  then its return value is undefined--that is, anything you want.
   *  Normally, this method should not be called if cell (x, y) does not
   *  contain a shark.  You will need this method to help convert Oceans to
   *  run-length encodings.
   *  @param x is the x-coordinate of the cell whose contents are queried.
   *  @param y is the y-coordinate of the cell whose contents are queried.
   */

  public int sharkFeeding(int x, int y){
    // Replace the following line with your solution.
  	if (cellContents(x, y) == SHARK){
  		return ((Shark) ocean_array[x][y]).getTimeToDeath();
  	} else {
  		return NULL_HUNGER;
  	}
  }


  /* ***Helper Functions*** */

  /**
   *  convertXCoord() (with one parameter) performs ocean "wrapping" by 
   *  returning the equivalent of a given x-coordinate that is less than the
   *  original ocean width.
   *  @param x is the x-coordinate to be converted.
   *  @return an equivalent x-coordinate that is less than the width.
   */

  private int convertXCoord(int x){
    int modded = x % width;
    if (modded < 0){
      modded += width;  
    }   
    return modded;
  }

  /**
   *  convertYCoord() (with one parameter) performs ocean "wrapping" by 
   *  returning the equivalent of a given y-coordinate that is less than the
   *  original ocean height.
   *  @param y is the y-coordinate to be converted.
   *  @return an equivalent y-coordinate that is less than the height.
   */

  private int convertYCoord(int y){
    int modded = y % height;
    if (modded < 0){
      modded += height;  
    }   
    return modded;
  }

  /**
   *  numNeighborType() (with three parameters) returns the number of 
   *  neighboring cells with a certain type.
   *  @param x is the x-coordinate of the current cell
   *  @param y is the y-coordinate of the current cell
   *  @param type is the type of Creature to count (EMPTY, FISH, or SHARK)
   *  @return the number of TYPE creatures neighboring the current cell
   */

  private int numNeighborType(int x, int y, int type){
    x = convertXCoord(x);
    y = convertYCoord(y);

    int typeCounter = 0;

    for (int i = x-1; i <= x+1; i++){ // Parse through all neighboring cells
      for (int j = y-1; j <= y+1; j++){
        if (ocean_array[convertXCoord(i)][convertYCoord(j)].getType() == type){
          typeCounter++;
        }
      }
    }    
    return typeCounter;
  }

}
