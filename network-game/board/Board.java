/* Board.java */

package board;
import player.MachinePlayer;

/**
 *  An implementation of a Network game Board. Keeps track of positions of 
 *  pieces. Enforces legal moves.
 */
public class Board {

  /* ***Public Static Final Constants*** */

  public static final int DIMENSION = 8; // Number of rows/columns in a Board


  /* ***Protected Static Final Constants*** */

  // Constants representing the goal a piece is in:
  protected static final int NONE = -1;
  protected static final int LEFT = 0;
  protected static final int RIGHT = 1;
  protected static final int TOP = 2;
  protected static final int BOTTOM = 3;


  /* ***Private Static Final Constants*** */

  // A Position array of the different directions to move along while searching
  // for a connection (the first parameter represents the number of units to 
  // move along the x-axis, and the second parameter represents the number of 
  // units to move along the y-axis)
  private static final Position[] CONNECTION_STEPS =
    {new Position(0, -1), new Position(1, -1),
     new Position(1, 0),  new Position(1, 1),
     new Position(0, 1),  new Position(-1, 1),
     new Position(-1, 0), new Position(-1, -1)};
  
  private static final int MIN_PARTIAL_LEN = 2; // Min length of partial path


  /* ***Private Board Fields*** */

  private Position[][] grid; // Stores all the Positions in the Board

  // For each goal (specified by LEFT, RIGHT, TOP, BOTTOM), goals[goal] stores
  // the Positions in the goal (the Positions are also contained in grid)
  private Position[][] goals;

  // The number of white and black pieces
  private int numWhite;
  private int numBlack;


  /* ***Board Constructor*** */

  /**
   *  Board constructor. Creates an empty Network board of width DIMENSION and
   *  height DIMENSION. By default, each (x, y) position is set to
   *  MachinePlayer.EMPTY.
   */
  public Board() {
    grid = new Position[DIMENSION][DIMENSION];
    goals = new Position[4][DIMENSION - 2];
    numWhite = 0;
    numBlack = 0;

    // Create each possible Position p = (0..DIMENSION - 1, 0..DIMENSION - 1)
    for (int i = 0; i < DIMENSION; i++) {
      for (int j = 0; j < DIMENSION; j++) {
        Position p = new Position(i, j);
        // Put p in the appropriate place in grid
        grid[i][j] = p;
        if (notInCorner(i, j)) {
          // If possible, put p in the appropriate place in goals
          int goal = p.getGoal();
          if (goal == LEFT || goal == RIGHT) {
            goals[goal][j - 1] = p;
          }
          if (goal == TOP || goal == BOTTOM) {
            goals[goal][i - 1] = p;
          }
        }
      }
    }
  }


  /* ***Board Methods*** */

  /**
   *  canPlace() checks if a COLOR chip can be placed at position (X, Y) on the
   *  board.
   *  @param color is the color of the chip (either MachinePlayer.WHITE,
   *         MachinePlayer.BLACK, or MachinePlayer.EMPTY).
   *  @param x (an integer) is the x-coordinate of the position.
   *  @param y (an integer) is the y-coordinate of the position.
   *  @return a boolean true if a COLOR chip can be placed at (X, Y) and false 
   *          otherwise.
   */
  public boolean canPlace(int color, int x, int y) {
    return notOffBoard(x, y) &&
           notInCorner(x, y) &&
           notInOppositeGoal(color, x, y) &&
           notOccupied(x, y) &&
           notGroupOfThree(color, x, y) &&
           notTooManyChips(color);
  }

  /**
   *  notOffBoard() checks if a position (X, Y) is on the board.
   *  @param x (an integer) is the x-coordinate of the position.
   *  @param y (an integer) is the y-coordinate of the position.
   *  @return a boolean of whether (X, Y) is located on the board.
   */
  private boolean notOffBoard(int x, int y) {
    return (x >= 0) && (x < DIMENSION) && (y >= 0) && (y < DIMENSION);
  }

  /**
   *  notInCorner() checks if a position (X, Y) is NOT in a corner.
   *  @param x (an integer) is the x-coordinate of the position.
   *  @param y (an integer) is the y-coordinate of the position.
   *  @return a boolean of whether (X, Y) is NOT in a corner.
   */
  private boolean notInCorner(int x, int y) {
    return  !(x == 0 && y == 0) &&
            !(x == 0 && y == DIMENSION - 1) &&
            !(x == DIMENSION - 1 && y == 0) &&
            !(x == DIMENSION - 1 && y == DIMENSION - 1);
  }

  /**
   *  notInOppositeGoal() checks to make sure that a COLOR chip is NOT placed
   *  in the goal area of the opposite color.
   *  @param color (an integer) represents the color of the chip.
   *  @param x (an integer) is the x-coordinate of the position.
   *  @param y (an integer) is the y-coordinate of the position.
   *  @return a boolean of whether a COLOR chip is NOT placed in the goal area
   *          of the opposite color.
   */
  private boolean notInOppositeGoal(int color, int x, int y) {
    if (color == MachinePlayer.WHITE) {
      if (y == 0 || y == DIMENSION - 1) {
        return false;
      }
    } else if (color == MachinePlayer.BLACK) {
      if (x == 0 || x == DIMENSION - 1) {
        return false;
      }
    }
    return true;
  }
  
  /**
   *  notOccupied() checks if a position (X, Y) is EMPTY (i.e. not occupied by
   *  another chip).
   *  @param x (an integer) is the x-coordinate of the position.
   *  @param y (an integer) is the y-coordinate of the position.
   *  @return a boolean of whether (X, Y) is EMPTY.
   */
  private boolean notOccupied(int x, int y) {
    return grid[x][y].color == MachinePlayer.EMPTY;
  }

  /**
   *  notGroupOfThree() checks if placing a COLOR piece at position (X, Y) does
   *  not form a cluster of three. If there are no neighbors around position 
   *  (X, Y), then a cluster will not be formed. If there are two or more 
   *  neighbors around position (X, Y), a cluster will be formed. If there is 
   *  exactly one neighbor surrounding (X, Y), count how many neighbors it has.
   *  If the neighbor of (X, Y) has other neighbors, then placing a COLOR piece
   *  at (X, Y) will result in a cluster. If the neighbor of (X, Y) doesn't 
   *  have other neighbors, then placing a COLOR piece at (X, Y) will not 
   *  result in a cluster.
   *  @param color of the piece (MachinePlayer.WHITE or MachinePlayer.BLACK).
   *  @param X coordinate of position.
   *  @param Y coordinate of position.
   *  @return a boolean true if a cluster will NOT be formed; false otherwise.
   */
  private boolean notGroupOfThree(int color, int x, int y) {
    int neighborQty = numNeighbors(color, x, y);

    // [Case #1]: No neighbors
    if (neighborQty == 0) {
      return true;
    }

    // [Case #2]: Exactly one neighbor
    else if (neighborQty == 1) {
      for (int y2 = y - 1; y2 <= y + 1; y2++) {
        for (int x2 = x - 1; x2 <= x + 1; x2++) {
          if (notOffBoard(x2, y2) && grid[x2][y2].color == color) {
            return numNeighbors(color, x2, y2) == 0;
          }
        }
      }
    }

    // [Case #3]: Too many neighbors
    return false;
  }

  /**
   *  numNeighbors() counts the number of neighboring pieces that are of the
   *  same color. 
   *  @param color (an integer) represents the color of the chip.
   *  @param x (an integer) is the x-coordinate of the position.
   *  @param y (an integer) is the y-coordinate of the position.
   *  @return the number (an integer) of neighboring pieces of the same color.
   */
  private int numNeighbors(int color, int x, int y) {
    int neighborCount = 0;
    for (int y2 = y - 1; y2 <= y + 1; y2++) {
      for (int x2 = x - 1; x2 <= x + 1; x2++) {
        if (notOffBoard(x2, y2) && grid[x2][y2].color == color &&
           (x != x2 || y != y2)) {
          neighborCount++;
        }
      }
    }
    return neighborCount;
  }

  /**
   *  notTooManyChips() checks if the number of chips of the specified color is
   *  less than 10 such that further ADD moves are permitted.
   *  @param color (an integer) represents the color of the chip.
   *  @return a boolean of whether there is less than 10 chips of the specified
   *          color already on the board.
   */
  private boolean notTooManyChips(int color) {
    if (color == MachinePlayer.WHITE) {
      return numWhite < 10;
    } else {
      return numBlack < 10;
    }
  }

  /**
   *  canMove() checks if a COLOR chip can be moved from a position (XI, YI) on
   *  the board to another position (XF, YF) on the board. This method 
   *  temporarily sets the color of position (XI, YI) to MachinePlayer.EMPTY 
   *  and checks if a chip of the specified color can be placed at Position 
   *  (XF, YF).
   *  @param color (an intger) is the color of the chip (either 
   *         MachinePlayer.WHITE, MachinePlayer.BLACK, or MachinePlayer.EMPTY).
   *  @param xi (an integer) is the x-coordinate of the initial position.
   *  @param yi (an integer) is the y-coordinate of the initial position.
   *  @param xf (an integer) is the x-coordinate of the final position.
   *  @param yf (an integer) is the y-coordinate of the final position.
   *  @return a boolean true if a COLOR chip can be moved from (XI, YI) to 
   *          (XF, YF) and false otherwise.
   */
  public boolean canMove(int color, int xi, int yi, int xf, int yf) {
    if ((grid[xi][yi].color != color) || (xi == xf && yi == yf) || 
        (countColor(color) != 10)) {
      return false;
    }
    grid[xi][yi].color = MachinePlayer.EMPTY;
    changeColorCounter(color, -1);
    boolean b = canPlace(color, xf, yf);
    grid[xi][yi].color = color;
    changeColorCounter(color, 1);
    return b;
  }

  /**
   *  getChip() returns the color of the chip at position (X, Y) on the board.
   *  @param x (an integer) is the x-coordinate of the position.
   *  @param y (an integer) is the y-coordinate of the position.
   *  @return an integer representing the color of the chip at position (X, Y)
   *          (either MachinePlayer.WHITE, MachinePlayer.BLACK, or
   *          MachinePlayer.EMPTY).
   */
  public int getChip(int x, int y) {
    if (notOffBoard(x, y)) {
      return grid[x][y].color;
    }
    return MachinePlayer.EMPTY;
  }

  /**
   *  placeChip() places a COLOR chip at position (X, Y) on the board if it is
   *  a legal move.
   *  @param color is the color of the chip (either MachinePlayer.WHITE,
   *         MachinePlayer.BLACK, or MachinePlayer.EMPTY).
   *  @param x is the x-coordinate of the position.
   *  @param y is the y-coordinate of the position.
   */
  public void placeChip(int color, int x, int y) {
    if (color == MachinePlayer.EMPTY) {
      removeChip(x, y);
    } else if (canPlace(color, x, y)) {
      Position currPosition = grid[x][y];
      currPosition.color = color;
      changeColorCounter(color, 1);

      // Establish connections for current chip
      Position[] connections = getConnections(currPosition);
      for (int i = 0; i < 8; i++) {
        if (connections[i] != null) {
          currPosition.connections[i] = connections[i];
          connections[i].connections[(i + 4) % 8] = currPosition;
        }
      }
    }
  }

  /**
   *  removeChip() removes a COLOR chip at position (X, Y) on the board, sets 
   *  the position (X, Y) on the board to be MachinePlayer.EMPTY, and returns
   *  the color of the chip originally at (X, Y).
   *  @param x is the x-coordinate of the position.
   *  @param y is the y-coordinate of the position.
   *  @return an integer representing the color of the chip originally at the
   *          position (X, Y) on the board (either MachinePlayer.WHITE,
   *          MachinePlayer.BLACK, or MachinePlayer.EMPTY).
   */
  public int removeChip(int x, int y) {
    int currChipColor = MachinePlayer.EMPTY;
    if (notOffBoard(x, y)) {
      Position currPosition = grid[x][y];
      currChipColor = currPosition.color;

      // Update connections of previously-connected chips if applicable
      if (currChipColor != MachinePlayer.EMPTY) {
        changeColorCounter(currChipColor, -1);
        currPosition.color = MachinePlayer.EMPTY;
        Position[] connections = getConnections(currPosition);
        for (int i = 0; i < 4; i++) {
          currPosition.connections[i] = null;
          currPosition.connections[i + 4] = null;
          Position connection1 = connections[i];
          Position connection2 = connections[i + 4];
          if (connection1 != null) {
            connection1.connections[i + 4] = connection2;
          }
          if (connection2 != null) {
            connection2.connections[i] = connection1;
          }
        }
      }
    }
    return currChipColor;
  }

  /**
   *  changeColorCounter() updates the numWhite and numBlack fields of a Board 
   *  object. This method is called when placing and removing chips.
   *  @param color (an integer) is the color of the counter to change.
   *  @param value (an integer) is the value by which the color counter should 
   *         change.
   */
  private void changeColorCounter(int color, int value) {
    if (color == MachinePlayer.WHITE) {
      numWhite += value;
    } else if (color == MachinePlayer.BLACK) {
      numBlack += value;
    }
  }

  /**
   *  getConnections() returns an array of the 8 Positions found by following
   *  CONNECTION_STEPS (or null if a Position is not found).
   *  @param p the initial Position.
   *  @return the connected Positions from p.
   */
  private Position[] getConnections(Position p) {
    Position[] connections = new Position[8];
    for (int i = 0; i < 8; i++) {
      connections[i] = getConnection(p, CONNECTION_STEPS[i]);
    }
    return connections;
  }

  /**
   *  getConnection() returns a single Position found by following a connection
   *  step in CONNECTION_STEPS (or null if a Position is not found).
   *  @param p the initial Position.
   *  @param connectionStep the value from CONNECTION_STEPS
   *  @return the connected Position from p.
   */
  private Position getConnection(Position p, Position connectionStep) {
    int x = p.getX();
    int y = p.getY();
    while (true) {
      x += connectionStep.getX();
      y += connectionStep.getY();
      if (!(notOffBoard(x, y))) {
        break;
      }
      if (grid[x][y].color != MachinePlayer.EMPTY) {
        return grid[x][y];
      }
    }
    return null;
  }

  /**
   *  countChips() returns the number of colored (non-empty) chips on the 
   *  board.
   *  @return an integer representing the number of colored chips on the board.
   */
  public int countChips() {
    return numWhite + numBlack;
  }

  /**
   *  countColor() returns the number of chips of the specified COLOR on the 
   *  board.
   *  @param an integer representing the color of the chips to be counted
   *  @return an integer representing the number of COLOR chips on the board.
   */
  public int countColor(int color) {
    if (color == MachinePlayer.WHITE) {
      return numWhite;
    } else if (color == MachinePlayer.BLACK) {
      return numBlack;
    } else if (color == MachinePlayer.EMPTY) {
      return DIMENSION * DIMENSION - numBlack - numWhite;
    } else {
      return countChips();
    }
  }

  /**
   *  hasNetwork() returns whether the board has a network of color chips.
   *  @param color is the color of the desired network.
   *  @return a boolean true if there is a color Network, and false otherwise.
   */
  public boolean hasNetwork(int color) {
    // Set goal to LEFT (if white) or TOP (if black)
    int goal;
    if (color == MachinePlayer.WHITE) {
      goal = LEFT;
    } else {
      goal = TOP;
    }
    // For each valid start Position in goal, perform dfsFull() search starting
    // at that Position. Return whether any search returns true.
    for (Position start: goals[goal]) {
      if (start.color == color) {
        Path path = new Path(goal);
        path.append(start);
        if (dfsFull(color, path)) {
          path.pop();
          return true;
        }
        path.pop();
      }
    }
    return false;
  }

  /**
   *  dfsFull() returns whether Positions can be connected to the end of path
   *  to build a network.
   *  @param color is the color of the Positions to be appended.
   *  @param path is the Path to which Positions are appended.
   *  @return a boolean true if path can be extended to form a COLOR Network,
   *  and false otherwise.
   */
  private boolean dfsFull(int color, Path path) {
    if (path.isNetwork()) {
      return true;
    }
    for (Position p: getConnections(path.end())) {
      if (p != null && p.color == color && path.append(p)) {
        if (dfsFull(color, path)) {
          path.pop();
          return true;
        }
        path.pop();
      }
    }
    return false;
  }

  /**
   *  boardEvaluate() returns a minimax score for the current board. This 
   *  method is only called when it is known that the current board has no
   *  network. Note that BLACK is always the maximizer and WHITE is always the
   *  minimizer.
   *  @return a double that is the minimax score for the current board.
   */
  public double boardEvaluate() {

    // Determine the sum of all the total partial paths:
    double sumBlack = sumPartialPaths(MachinePlayer.BLACK);
    double sumWhite = sumPartialPaths(MachinePlayer.WHITE);

    // Determine the number of chips in each goal area:
    int blackTopGoalChips = countGoal(MachinePlayer.BLACK, 0);
    int blackBottomGoalChips = countGoal(MachinePlayer.BLACK, DIMENSION - 1);
    int whiteLeftGoalChips = countGoal(MachinePlayer.WHITE, 0);
    int whiteRightGoalChips = countGoal(MachinePlayer.BLACK, DIMENSION - 1);

    // Disincentivize unbalanced goals:
    if (blackTopGoalChips + blackBottomGoalChips > 1) {
      if (blackTopGoalChips > blackBottomGoalChips) {
        sumBlack = sumBlack*(blackBottomGoalChips/blackTopGoalChips);
      } else if (blackTopGoalChips < blackBottomGoalChips) {
        sumBlack = sumBlack*(blackTopGoalChips/blackBottomGoalChips);
      }
    }
    if (whiteLeftGoalChips + whiteRightGoalChips > 1) {
      if (whiteLeftGoalChips > whiteRightGoalChips) {
        sumWhite = sumWhite*(whiteRightGoalChips/whiteLeftGoalChips);
      } else if (whiteLeftGoalChips < whiteRightGoalChips) {
        sumWhite = sumWhite*(whiteLeftGoalChips/whiteRightGoalChips);
      }
    }

    return scale(sumBlack, sumWhite);
  }

  /**
   *  sumPartialPaths() returns the sum of the lengths of all partial paths 
   *  from the two goal areas of the specified color that have length greater
   *  than MIN_PARTIAL_LEN.
   *  @param the color the paths.
   *  @return the sum of the lengths of all partial paths.
   */
  private int sumPartialPaths(int color) {
    if (color == MachinePlayer.WHITE) {
      return sumPathsFromGoal(LEFT, color) + sumPathsFromGoal(RIGHT, color);
    } else {
      return sumPathsFromGoal(TOP, color) + sumPathsFromGoal(BOTTOM, color);
    }
  }

  /**
   *  sumPathsFromGoal() returns the sum of the lengths of all partial paths 
   *  from the a specified goal area of the specified color that have length
   *  greater than MIN_PARTIAL_LEN.
   *  @param an integer denoting the goal from which to count partial paths
   *  @param the color the paths.
   *  @return the sum of the lengths of the partial paths from the goal.
   */
  private int sumPathsFromGoal(int startGoal, int color) {
    int totalLength = 0;
    for (Position start: goals[startGoal]) {
      if (start.color == color) {
        Path path = new Path(startGoal);
        path.append(start);
        totalLength += dfsPartial(color, path);
        path.pop();
      }
    }
    return totalLength;
  }

  /**
   *  dfsPartial() returns the sum of the lengths of all partial paths from the
   *  a specified Position that have length greater than MIN_PARTIAL_LEN. This 
   *  method is called tree recursively until the end of a partial path is 
   *  found. At that point, the function returns the length of that particular
   *  path and passes this information back up the recursive calls.
   *  @param the color the paths.
   *  @param the current Path of interest.
   *  @return the sum of the lengths of the partial paths from a specified 
   *          Position.
   */
  private int dfsPartial(int color, Path path) {
    boolean flag = false;
    int runningTotal = 0;
    
    // Call dfsPartial() with each possible Position appended to path, and add
    // the result to runningTotal
    for (Position p: getConnections(path.end())) {
      if (p != null && p.color == color && path.append(p)) {
        flag = true;
        runningTotal += dfsPartial(color, path);
        path.pop();
      }
    }
    
    // If no Positions can be appended to path and path is sufficiently long,
    // return the sum of runningTotal and the length of path; otherwise, return
    // runningTotal
    int length = path.length();
    if (!flag && length >= MIN_PARTIAL_LEN) {
      return runningTotal + length;
    } else {
      return runningTotal;
    }
  }

  /**
   *  countGoal() returns the number of chips in a specified goal area.
   *  @param the color of the goal area.
   *  @param the start position (to specify which of the two goals to count).
   *  @return the number of chips in a goal area.
   */
  private int countGoal(int color, int start) {
    int numChips = 0;
    if (color == MachinePlayer.WHITE) {
      for (int i = 0; i < DIMENSION; i++) {
        if (getChip(start, i) == color) {
          numChips++;
        }
      }
    } else if (color == MachinePlayer.BLACK) {
      for (int i = 0; i < DIMENSION; i++) {
        if (getChip(i, start) == color) {
          numChips++;
        }
      }
    }
    return numChips;
  }

  /**
   *  scale() returns a minimax score for a board with no network. Given a 
   *  proportional black score and a proportional white score, this method 
   *  returns a double between 1 and -1 that indicates to what extent this 
   *  board favors the WHITE player or the BLACK player. A score of 0.0 
   *  indicates that this board does not favor any one player.
   *  @param a proportional score (a double) for the black player.
   *  @param a proportional score (a double) for the white player.
   *  @return a double that is the minimax score for the current board.
   */
  private double scale(double blackScore, double whiteScore) {
    double x = 0.0;
    if (blackScore > whiteScore) {
      x = blackScore - 0.83*whiteScore;
      return x / (x + 1.);
    } else if (whiteScore > blackScore) {
      x = whiteScore - 0.83*blackScore;
      return -x / (x + 1.);
    }
    return x;
  }

  /**
   *  toString() returns a String representation of a Board object.
   *  @return a String representation of a Board object.
   */
  public String toString() {
    String boardRepr = "------------------- \n";
    for (int y = 0; y < DIMENSION; y++) {
      boardRepr += "| ";
      for (int x = 0; x < DIMENSION; x++) {
        boardRepr += pieceRepr(x, y);
      }
      boardRepr += "|\n";
    }
    boardRepr += "-------------------";
    return boardRepr;
  }

  /**
   *  pieceRepr() returns a String representation of a chip on a Board.
   *  @return a String representation of a chip on a Board.
   */
  private String pieceRepr(int x, int y) {
    int color = grid[x][y].color;
    if (!(notInCorner(x, y)) && color == MachinePlayer.EMPTY) {
      return "X ";
    } else if (color == MachinePlayer.WHITE) {
      return "W ";
    } else if (color == MachinePlayer.BLACK) {
      return "B ";
    } else {
      return ". ";
    }
  }

  /* ************************************************************************ *
   * *                            TESTING MODULE                            * *
   * ************************************************************************ */
  
   /* // <-------------------------------[comment/uncomment to access test code]


  public static void main(String args[]) {
    Board b1 = new Board();
    testAddConnections();
    checkInvariants(b1);
    boardTestDetailed(b1);
    try {
      int numPasses = Integer.parseInt(args[0]);
      boardTestRandom(b1, numPasses);
    } catch (Exception e) {
      boardTestRandom(b1, 100);
    }
    testHasNetwork();
    boardTest2();
  }

  private static void boardAssert(boolean flag) {
    if (!flag) {
      System.out.println("******INVARIANT FAILED******");
      Thread.dumpStack();
      System.exit(0);
    }
  }

  private static void checkInvariants(Board b) {

    // Check accuracy of counters
    boardAssert(b.numBlack == lookFor(b, MachinePlayer.BLACK));
    boardAssert(b.numWhite == lookFor(b, MachinePlayer.WHITE));
    boardAssert(DIMENSION * DIMENSION - b.numBlack - b.numWhite == lookFor(b, MachinePlayer.EMPTY));
    boardAssert(b.countChips() == lookFor(b, MachinePlayer.BLACK) + lookFor(b, MachinePlayer.WHITE));
    boardAssert(b.numBlack == lookFor(b, MachinePlayer.BLACK));
    boardAssert(b.numWhite == lookFor(b, MachinePlayer.WHITE));

    if (b.isEmpty()) {
      boardAssert(b.numBlack == 0);
      boardAssert(b.numWhite == 0);
    }

    // Check that no more than 10 white and 10 black pieces are on the board
    boardAssert(b.numWhite <= 10);
    boardAssert(b.numBlack <= 10);

    // Loop through every position
    for (int y = 0; y < Board.DIMENSION; y++) {
      for (int x = 0; x < Board.DIMENSION; x++) {

        //Check that corners are empty
        if (!b.notInCorner(x, y)) { 
          boardAssert(b.grid[x][y].color == MachinePlayer.EMPTY);
        }

        // Check that there are no clusters
        if (b.grid[x][y].color == MachinePlayer.WHITE || b.grid[x][y].color == MachinePlayer.BLACK) {
          boardAssert(b.numNeighbors(b.grid[x][y].color, x, y) <= 1);
        }

        // Check that there are no black/white chips in white/black goals
        boardAssert(b.notInOppositeGoal(b.grid[x][y].color, x, y));

        // Check that connections behave as expected
        if (b.grid[x][y].color != MachinePlayer.EMPTY) {
          Position[] connections = b.getConnections(b.grid[x][y]);
          for (int i = 0; i < 8; i++) {
            boardAssert(b.grid[x][y].connections[i] == connections[i]);
          }
        }
      }
    }
  }

  boolean isEmpty() {
    for (int y = 0; y < DIMENSION; y++) {
      for (int x = 0; x < DIMENSION; x++) {
        if (grid[x][y].color != MachinePlayer.EMPTY) {
          return false;
        }
      }
    }
    return true;
  }

  void makeEmpty() {
    for (int y = 0; y < DIMENSION; y++) {
      for (int x = 0; x < DIMENSION; x++) {
        grid[x][y].color = MachinePlayer.EMPTY;
        for (int i = 0; i < 8; i++) {
          grid[x][y].connections[i] = null;
        }
      }
    }
    numWhite = 0;
    numBlack = 0;
  }

  private static void testAddConnections() {
    Board b = new Board();
    b.placeChip(MachinePlayer.WHITE, 4, 4);
    boardAssert(b.getChip(4, 4) == MachinePlayer.WHITE);
    b.placeChip(MachinePlayer.BLACK, 4, 2);
    b.placeChip(MachinePlayer.WHITE, 6, 4);
    b.placeChip(MachinePlayer.BLACK, 6, 6);
    b.placeChip(MachinePlayer.WHITE, 4, 6);
    b.placeChip(MachinePlayer.BLACK, 2, 6);
    b.placeChip(MachinePlayer.WHITE, 2, 2);
    Position[] connections = b.getConnections(b.grid[4][4]);
    boardAssert(connections[0] == b.grid[4][2]);
    boardAssert(connections[1] == null);
    boardAssert(connections[2] == b.grid[6][4]);
    boardAssert(connections[3] == b.grid[6][6]);
    boardAssert(connections[4] == b.grid[4][6]);
    boardAssert(connections[5] == b.grid[2][6]);
    boardAssert(connections[6] == null);
    boardAssert(connections[7] == b.grid[2][2]);
  }

  private static int lookFor(Board b, int color) {
    int counter = 0;
    for (int y = 0; y < DIMENSION; y++) {
      for (int x = 0; x < DIMENSION; x++) {
        if (b.grid[x][y].color == color) {
          counter++;
        }
      }
    }
    return counter;
  }

  private static int randomPiece() {
    double randNum = Math.random();
    if (randNum < 0.34) {
      return MachinePlayer.WHITE;
    } else if (randNum < 0.67) {
      return MachinePlayer.BLACK;
    } else {
      return MachinePlayer.EMPTY;
    }
  }

  private static void randomAction(Board b, int x, int y) {
    double randNum = Math.random();
    if (randNum < 0.50) {
      b.placeChip(randomPiece(), x, y);
    } else {
      b.removeChip(x, y);
    }
  }

  private static void boardTestDetailed(Board b) {
    //System.out.println("RUNNING boardTestDetailed...");

    b.makeEmpty();
    checkInvariants(b);

    //System.out.println("Inserting pieces at illegal positions...");

    //Off of board
    //System.out.println("Trying to insert off of the board...");
    b.placeChip(MachinePlayer.WHITE, -1, -2);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, -1, -2);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 2, 100);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 2, 100);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 8, 2);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 8, 2);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 9, 8);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 9, 8);
    checkInvariants(b);
    boardAssert(b.isEmpty());

    //In corner
    //System.out.println("Trying to insert into the corner...");
    b.placeChip(MachinePlayer.WHITE, 0, 0);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 0, 0);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 0, 7);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 0, 7);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 7, 7);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 7, 7);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 7, 0);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 7, 0);
    checkInvariants(b);
    boardAssert(b.isEmpty()); 

    //In oppposite goal
    //System.out.println("Trying to insert into the opposite goal...");
    b.placeChip(MachinePlayer.WHITE, 3, 0);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 3, 7);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 0, 4);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 7, 4);
    checkInvariants(b);
    boardAssert(b.isEmpty());

    //System.out.println("Inserting two MachinePlayer.BLACK pieces in top goal area...");
    b.placeChip(MachinePlayer.BLACK, 3, 0);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 5, 0);
    checkInvariants(b);
    boardAssert(b.numBlack == 2);
    //System.out.println(b);

    //System.out.println("Trying to insert into an occupied space...");
    b.placeChip(MachinePlayer.WHITE, 3, 0);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 5, 0);
    checkInvariants(b);
    boardAssert(b.numBlack == 2);
    //System.out.println("Trying to create an illegal black cluster...");
    b.placeChip(MachinePlayer.BLACK, 4, 0);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 4, 1);
    checkInvariants(b);
    boardAssert(b.numBlack == 2);

    //System.out.println("Inserting two MachinePlayer.WHITE pieces in left and right goal areas...");
    b.placeChip(MachinePlayer.WHITE, 0, 3);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 7, 6);
    checkInvariants(b);
    boardAssert(b.numBlack == 2);
    boardAssert(b.numWhite == 2);
    boardAssert(b.countChips() == 4);
    //System.out.println(b);

    //System.out.println("Setting up tricky false alarm cluster #1...");
    b.placeChip(MachinePlayer.BLACK, 4, 3);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 4, 4);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 4, 5);
    checkInvariants(b);
    boardAssert(b.countChips() == 7);
    //System.out.println(b);
    
    //System.out.println("Trying tricky insert...");
    b.placeChip(MachinePlayer.BLACK, 5, 3);
    checkInvariants(b);
    boardAssert(b.countChips() == 8);
    //System.out.println(b);

    //System.out.println("Taking down tricky false alarm cluster #1 and setting up tricky false alarm cluster #2...");
    b.removeChip(5, 3);
    checkInvariants(b);
    boardAssert(b.countChips() == 7);
    //System.out.println(b);
    
    //System.out.println("Trying tricky insert...");
    b.placeChip(MachinePlayer.BLACK, 5, 2);
    checkInvariants(b);
    boardAssert(b.countChips() == 8);
    //System.out.println(b);

    //System.out.println("Taking down tricky false alarm cluster #2 and setting up new cluster situation...");
    b.removeChip(5, 2);
    checkInvariants(b);
    b.removeChip(4, 3);
    checkInvariants(b);
    b.removeChip(4, 4);
    checkInvariants(b);
    b.removeChip(4, 5);
    checkInvariants(b);
    
    b.placeChip(MachinePlayer.WHITE, 2, 6);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 2, 5);
    checkInvariants(b);
    boardAssert(b.countChips() == 6);
    //System.out.println(b);

    //System.out.println("Trying tricky insert...");
    b.placeChip(MachinePlayer.WHITE, 3, 4);
    checkInvariants(b);
    boardAssert(b.countChips() == 6);
    //System.out.println(b);

    //System.out.println("Taking down cluster situation chips...");
    b.removeChip(2, 6);
    checkInvariants(b);
    b.removeChip(2, 5);
    checkInvariants(b);
    boardAssert(b.numBlack == 2);
    boardAssert(b.numWhite == 2);
    boardAssert(b.countChips() == 4);
    //System.out.println(b);

    //System.out.println("Removing the two MachinePlayer.BLACK chips on the board...");
    b.removeChip(3, 0);
    checkInvariants(b);
    b.removeChip(5, 0);
    checkInvariants(b);
    boardAssert(b.numBlack == 0);
    boardAssert(b.numWhite == 2);
    boardAssert(b.countChips() == 2);
    
    //System.out.println("Removing the two MachinePlayer.WHITE chips on the board...");
    b.removeChip(0, 3);
    checkInvariants(b);
    b.removeChip(7, 6);
    checkInvariants(b);
    
    boardAssert(b.isEmpty());
    boardAssert(b.numBlack == 0);
    boardAssert(b.numWhite == 0);
    boardAssert(b.countChips() == 0);
    //System.out.println(b);

    b.placeChip(MachinePlayer.WHITE, 4, 4);
    b.placeChip(MachinePlayer.WHITE, 4, 5);
    checkInvariants(b);
    b.makeEmpty();
    checkInvariants(b);

    //System.out.println("END boardTestDetailed.");
    //System.out.println(" ");
  }

  private static void boardTestRandom(Board b, int numLoops) {
    //System.out.println("RUNNING boardTestRandom...");
    //System.out.println("Implementing random actions over " + numLoops + " passes of the board...");

    for (int i = 0; i < numLoops; i++) {
      for (int y = 0; y < DIMENSION; y++) {
        for (int x = 0; x < DIMENSION; x++) {
          randomAction(b, x, y);
          checkInvariants(b);
        }
      }
    }
    //System.out.println("The final board: \n" + b);
    //System.out.println("END boardTestRandom.");
  }

  private static void boardTest2() {
    //System.out.println("Running boardTest2...");
    Board b = new Board();
    b.placeChip(MachinePlayer.WHITE, 1, 5);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 2, 6);
    checkInvariants(b);
    b.placeChip(MachinePlayer.WHITE, 0, 4);
    checkInvariants(b);
    b.placeChip(MachinePlayer.BLACK, 3, 3);
    checkInvariants(b);
    //System.out.println(b);
    boardAssert(!b.canPlace(MachinePlayer.WHITE, 1, 3));
    b.placeChip(MachinePlayer.WHITE, 1, 3);
    checkInvariants(b);
    //System.out.println(b);
    //System.out.println(" ");
  }

  private static void testHasNetwork() {
    Board b = new Board();
    b.placeChip(MachinePlayer.WHITE, 0, 3);
    b.placeChip(MachinePlayer.BLACK, 6, 0);
    boardAssert(!b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
    b.placeChip(MachinePlayer.WHITE, 2, 3);
    b.placeChip(MachinePlayer.BLACK, 4, 0);
    boardAssert(!b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
    b.placeChip(MachinePlayer.WHITE, 2, 5);
    b.placeChip(MachinePlayer.BLACK, 2, 0);
    boardAssert(!b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
    b.placeChip(MachinePlayer.WHITE, 3, 6);
    b.placeChip(MachinePlayer.BLACK, 6, 7);
    boardAssert(!b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
    b.placeChip(MachinePlayer.WHITE, 5, 6);
    b.placeChip(MachinePlayer.BLACK, 4, 7);
    boardAssert(!b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
    b.placeChip(MachinePlayer.WHITE, 7, 4);
    b.placeChip(MachinePlayer.BLACK, 2, 7);
    boardAssert(b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
    b.removeChip(5, 6);
    b.placeChip(MachinePlayer.WHITE, 7, 6);
    //System.out.println(b);
    boardAssert(!b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
    b.placeChip(MachinePlayer.WHITE, 0, 5);
    b.placeChip(MachinePlayer.BLACK, 4, 4);
    boardAssert(!b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
    b.placeChip(MachinePlayer.WHITE, 0, 1);
    b.placeChip(MachinePlayer.BLACK, 4, 3);
    boardAssert(!b.hasNetwork(MachinePlayer.WHITE));
    boardAssert(!b.hasNetwork(MachinePlayer.BLACK));
  }

   */ // <-------------------------------[comment/uncomment to access test code]
  
  /* ************************************************************************ *
   * *                          END TESTING MODULE                          * *
   * ************************************************************************ */


}
