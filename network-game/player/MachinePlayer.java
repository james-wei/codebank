/* MachinePlayer.java */

package player;
import board.Board;

/**
 *  An implementation of an automatic Network player.  Keeps track of moves
 *  made by both players.  Can select a move for itself.
 */
public class MachinePlayer extends Player {

  /* ***Public Static Final Constants*** */

  public static final int BLACK = 0;
  public static final int WHITE = 1;
  public static final int EMPTY = 2;


  /* ***Private Static Final Constants*** */

  private static final double WORST_ALPHA = -100;
  private static final double WORST_BETA = 100;
  private static final double BEST_ALPHA = 100;
  private static final double BEST_BETA = -100;

  private static final int DEFAULT_SEARCH_DEPTH = 3;
  private static final int MIN_SEARCH_DEPTH = 1;
  private static final int START_DEPTH = 0;


  /* ***Private MachinePlayer Fields*** */
  
  private int playerColor;
  private int opponentColor;
  private int searchDepth;
  private int currentPlayer = WHITE;
  private int moveKind = Move.ADD;
  private Board board;


  /* ***MachinePlayer Constructors*** */

  /**
   *  MachinePlayer constructor. Creates a machine player with the given color.
   *  Color is either BLACK (0) or WHITE (1). White has the first move. 
   *  The default search depth is used.
   */
  public MachinePlayer(int color) {
    this(color, DEFAULT_SEARCH_DEPTH);
  }

  /**
   *  MachinePlayer constructor. Creates a machine player with the given color
   *  and search depth. Color is either BLACK (0) or WHITE (1). White has the
   *  first move. The search depth must be at least MIN_SEARCH_DEPTH.
   */
  public MachinePlayer(int color, int searchDepth) {

    // Set the colors of "this" player and its opponent
    if (color == WHITE) {
      playerColor = WHITE;
      opponentColor = BLACK;
    } else {
      playerColor = BLACK;
      opponentColor = WHITE;
    }

    // Set the search depth
    if (searchDepth < MIN_SEARCH_DEPTH) {
      this.searchDepth = MIN_SEARCH_DEPTH;
    } else {
      this.searchDepth = searchDepth;
    }

    // Create a new (empty) internal board
    board = new Board();
  }


  /* ***MachinePlayer Methods*** */

  /**
   *  chooseMove() returns a new move by "this" player and internally records
   *  the move as a move by "this" player. The first move is hard-coded. 
   *  Otherwise, use the minimax algorithm to determine the best move.
   *  @return a Move object representative of the move that "this" player made.
   */
  public Move chooseMove() {
    Move bestMove;
    int numChips = board.countChips();

    if (playerColor == WHITE && numChips == 0) { 
      // Hard-coded first move for WHITE:
      bestMove = new Move((Board.DIMENSION/2) - 1, (Board.DIMENSION/2) - 1);
    } else if (playerColor == BLACK && numChips == 1) { 
      // Hard-coded first move for BLACK:
      bestMove = blackFirstMove();
    } else {
      // Use the minimax algorithm to determine the best move:
      MoveScore bestMS = minimax(playerColor, opponentColor, START_DEPTH, 
                                 WORST_ALPHA, WORST_BETA);
      bestMove = bestMS.move;
    }

    // Record the best move as a move by "this" player and return it:
    forceMove(bestMove);
    return bestMove;
  }

  /**
   *  blackFirstMove() returns the hard-coded first move for a BLACK player. If
   *  the position (Board.DIMENSION/2, Board.DIMENSION/2) is already occupied,
   *  then return an alternate first move for the BLACK player.
   *  @return a Move object containing a BLACK MachinePlayer's first move.
   */
  private Move blackFirstMove() {
    Move firstMove;
    if (board.canPlace(playerColor, Board.DIMENSION/2, Board.DIMENSION/2)){
      firstMove = new Move(Board.DIMENSION/2, Board.DIMENSION/2);
    } else {
      firstMove = new Move((Board.DIMENSION/2) - 1, (Board.DIMENSION/2) - 1);
    }
    return firstMove;
  }

  /* Minimax Module */

  /**
   *  minimax() recursively runs the minimax algorithm to determine the best 
   *  move for the current player.
   *  @param an integer representing the current player.
   *  @param an integer representing the current player's opponent.
   *  @param an integer representing the current depth of the tree search.
   *  @param a double containing an alpha value (used for alpha-beta pruning).
   *  @param a double containing a beta value (used for alpha-beta pruning).
   *  @return a MoveScore object containing the best possible Move for the
   *          current player and the board score that results from the Move.
   */
  private MoveScore minimax(int currPlayer, int currOpponent, int currDepth, 
                            double alpha, double beta) {
    
    // [Setup]: Create a MoveScore object with the most pessimistic boardScore
    MoveScore myMoveScore = new MoveScore();
    if (currPlayer == BLACK) {
      myMoveScore.boardScore = WORST_ALPHA;
    } else {
      myMoveScore.boardScore = WORST_BETA;
    }

    // [Case #1]: The current board already has a network
    if (board.hasNetwork(currPlayer)) {
      myMoveScore.boardScore = winScore(currDepth, currPlayer);
      return myMoveScore;
    } else if (board.hasNetwork(currOpponent)) {
      myMoveScore.boardScore = winScore(currDepth, currOpponent);
      return myMoveScore;
    }

    // [Case #2]: The minimax algorithm has reached the deepest search level
    if (searchDepth - currDepth < 1) {
      myMoveScore.boardScore = board.boardEvaluate();
      return myMoveScore;
    }

    // [Case #3]: The minimax algorithm continues to analyze possible moves
    if (board.countColor(currPlayer) < 10) {
      myMoveScore = minimaxAddMoves(myMoveScore, currPlayer, currOpponent, 
                                    currDepth, alpha, beta);
    } else {
      myMoveScore = minimaxStepMoves(myMoveScore, currPlayer, currOpponent, 
                                     currDepth, alpha, beta);
    }
    return myMoveScore;
  }

  /**
   *  winScore() computes the minimax board score of a board with a complete 
   *  network. The magnitude of the minimax board score of a board with a 
   *  network on it depends on the current depth of the minimax recursive call:
   *  greater depth results in a smaller score. In other words, a board with a 
   *  network found in one move will have a score whose magnitude is larger 
   *  than that of a board with a network found in two, three, or four moves.
   *  However, the magnitude of the score of a board containing a network will 
   *  never fall below 1.01. That means the magnitude of the score of a board 
   *  with a network is always larger than the magnitude of the score of a 
   *  board without a network.
   *  @param an integer representing the depth of the recursive minimax call.
   *  @param an integer representing the player with the winning network.
   *  @return a double representing the minimax score of a board with a
   *          complete network.
   */
  private double winScore(int currentDepth, int winnerColor) {
    if (winnerColor == WHITE) {
      if (currentDepth < BEST_ALPHA) {
        return BEST_BETA + (currentDepth - 1);
      } else {
        return -1 * ((1/(currentDepth - 98.988)) + 1.01);
      }
    } else {
      if (currentDepth < BEST_ALPHA) {
        return BEST_ALPHA - (currentDepth - 1);
      } else {
        return (1/(currentDepth - 98.988)) + 1.01;
      }
    }
  }

  /**
   *  minimaxAddMoves() performs all possible ADD moves for the minimax 
   *  algorithm and recursively calls minimax() to continue enumerating 
   *  possible board configurations.
   *  @param an MoveScore object from the current minimax() call.
   *  @param an integer representing the current player
   *  @param an integer representing the current player's opponent.
   *  @param an integer representing the current depth of the tree search.
   *  @param a double containing an alpha value (used for alpha-beta pruning).
   *  @param a double containing a beta value (used for alpha-beta pruning).
   *  @return a MoveScore object containing the best possible Move for the
   *          current player and the board score that results from the Move.
   */
  private MoveScore minimaxAddMoves(MoveScore ms, int currPlayer, 
                                    int opponent, int currentDepth,
                                    double alpha, double beta) {
    MoveScore reply;
    for (int y = 0; y < Board.DIMENSION; y++) {
      for (int x = 0; x < Board.DIMENSION; x++) {
        if (board.canPlace(currPlayer, x, y)) {
          board.placeChip(currPlayer, x, y);
          reply = minimax(opponent, currPlayer, currentDepth + 1, 
                          alpha, beta);
          board.removeChip(x, y);

          // Choose new optimal move
          if (currPlayer == BLACK && reply.boardScore > ms.boardScore) {
            ms.boardScore = reply.boardScore;
            ms.move = new Move(x, y);
            alpha = reply.boardScore;
          } else if (currPlayer == WHITE && reply.boardScore < ms.boardScore) {
            ms.boardScore = reply.boardScore;
            ms.move = new Move(x, y);
            beta = reply.boardScore;
          }

          // Pruning
          if (alpha >= beta) {              
            return ms;
          }
        }
      }
    }
    return ms;
  }

  /**
   *  minimaxStepMoves() performs all possible STEP moves for the minimax 
   *  algorithm and recursively calls minimax() to continue enumerating 
   *  possible board configurations.
   *  @param an MoveScore object from the current minimax() call.
   *  @param an integer representing the current player
   *  @param an integer representing the current player's opponent.
   *  @param an integer representing the current depth of the tree search.
   *  @param a double containing an alpha value (used for alpha-beta pruning).
   *  @param a double containing a beta value (used for alpha-beta pruning).
   *  @return a MoveScore object containing the best possible Move for the
   *          current player and the board score that results from the Move.
   */
  private MoveScore minimaxStepMoves(MoveScore ms, int currPlayer, 
                                     int opponent, int currentDepth,
                                     double alpha, double beta) {
    MoveScore reply;
    for (int yi = 0; yi < Board.DIMENSION; yi++) {
      for (int xi = 0; xi < Board.DIMENSION; xi++) {
        
        // Check if chip color matches player color
        if (board.getChip(xi, yi) != currPlayer) {
          continue;
        }
        
        for (int yf = 0; yf < Board.DIMENSION; yf++) {
          for (int xf = 0; xf < Board.DIMENSION; xf++) {
            if (board.canMove(currPlayer, xi, yi, xf, yf)) {
              board.removeChip(xi, yi);
              board.placeChip(currPlayer, xf, yf);
              reply = minimax(opponent, currPlayer, currentDepth + 1, alpha, beta);
              board.removeChip(xf, yf);
              board.placeChip(currPlayer, xi, yi);

              // Choose new optimal move
              if (currPlayer == BLACK && reply.boardScore > ms.boardScore) {
                ms.boardScore = reply.boardScore;
                ms.move = new Move(xf, yf, xi, yi);
                alpha = reply.boardScore;
              } else if (currPlayer == WHITE && reply.boardScore < ms.boardScore) {
                ms.boardScore = reply.boardScore;
                ms.move = new Move(xf, yf, xi, yi);
                beta = reply.boardScore;
              }
              
              // Pruning
              if (alpha >= beta) {
                return ms;
              }
            }
          }
        }
      }
    }
    return ms;
  }

  /* End Minimax Module */


  /**
   *  opponentMove() checks if a move made by the opponent is legal. If it is,
   *  this method records the move as a move by the opponent and returns true.
   *  If the move is illegal, this method returns false without recording the 
   *  move. This method allows opponents to inform "this" MachinePlayer of 
   *  their moves.
   *  @param a Move object containing the opponent's move.
   *  @return a boolean of whether the opponent's move was legal.
   */
  public boolean opponentMove(Move m) {
    return makeMove(opponentColor, m);
  }

  /**
   *  forceMove() checks if a move made by "this" MachinePlayer is legal. If it
   *  is, this method records the move as a move by "this" player and returns 
   *  true. If the move is illegal, this method returns false without recording
   *  the move. This method is used to set up "Network problems" for "this" 
   *  MachinePlayer to solve.
   *  @param a Move object containing "this" player's move.
   *  @return a boolean of whether "this" player's move was legal.
   */
  public boolean forceMove(Move m) {
    return makeMove(playerColor, m);
  }

  /**
   *  makeMove() updates "this" MachinePlayer's internal game board with a Move 
   *  made by a player of a specified color.
   *  @param an integer representing the color of the player who made a move.
   *  @param a Move object containing the move that the player made.
   *  @return a boolean of whether the internal game board updated successfully.
   */
  private boolean makeMove(int color, Move m) {
    if (m == null || !isLegalMove(color, m)) {
      return false;
    }
    
    // If m is a step move, first remove the chip at (m.x2, m.y2)
    if (m.moveKind == Move.STEP) {
      board.removeChip(m.x2, m.y2);
    }
    board.placeChip(color, m.x1, m.y1);
    
    // Update currentPlayer and moveKind as necessary
    if (currentPlayer == BLACK) {
      currentPlayer = WHITE;
    } else {
      currentPlayer = BLACK;
    }

    if (board.countChips() == 20) {
      moveKind = Move.STEP;
    }

    return true;
  }

  /**
   *  isLegalMove() checks if a player of the specified color is legally 
   *  allowed to make a certain move.
   *  @param an integer representing the color of the player who made a move.
   *  @param a Move object containing the move that the player made.
   *  @return a boolean of whether the move is legal.
   */
  private boolean isLegalMove(int color, Move m) {
    
    // m.moveKind must be moveKind (unless it's a QUIT move), and m must be 
    // performed by currentPlayer
    if (m.moveKind != moveKind || color != currentPlayer) {
      return (m.moveKind == Move.QUIT) && (color == currentPlayer);
    }

    if (m.moveKind == Move.ADD) {
      return board.canPlace(color, m.x1, m.y1);
    }
    return board.canMove(color, m.x2, m.y2, m.x1, m.y1);
  }


  /* ************************************************************************ *
   * *                            TESTING MODULE                            * *
   * ************************************************************************ */

  /* // <-------------------------------[comment/uncomment to access test code]

  public static void main(String[] args) {
    test1();
    testMinimax1();
    testMinimax2();
    testMinimax3(100);
    testMinimax4();
    testMinimax5();
    testMinimax6();
    testMinimax7();
  }

  private static void mpAssert(boolean flag) {
    if (!flag) {
      System.out.println("******INVARIANT FAILED******");
      Thread.dumpStack();
      System.exit(0);
    }
  }

  private static void test1() {
    MachinePlayer mp = new MachinePlayer(1);
    mpAssert(!mp.forceMove(new Move(6, 0)));
    mpAssert(!mp.opponentMove(new Move(6, 0)));
    mpAssert(mp.forceMove(new Move(7, 6)));
    mpAssert(!mp.forceMove(new Move(4, 4)));
    mpAssert(mp.opponentMove(new Move(6, 7)));
    mpAssert(!mp.opponentMove(new Move(4, 4)));
    mpAssert(mp.forceMove(new Move(4, 3)));
    mpAssert(mp.opponentMove(new Move(5, 4)));
    mpAssert(!mp.forceMove(new Move(6, 5, 4, 3)));
    mpAssert(mp.forceMove(new Move(4, 4)));
    mpAssert(mp.opponentMove(new Move(5, 5)));
    mpAssert(!mp.forceMove(new Move(5, 3)));
    for (int i = 1; i <= 2; i++) {
      for (int j = 1; j < 7; j += 2) {
        mpAssert(mp.forceMove(new Move(i, j)));
        mpAssert(mp.opponentMove(new Move(i, j + 1)));
      }
    }
    mpAssert(mp.forceMove(new Move(7, 1)));
    mpAssert(mp.opponentMove(new Move(6, 0)));
    //System.out.println("After add moves:");
    //System.out.println(mp.board);
    mpAssert(!mp.forceMove(new Move(7, 4)));
    mpAssert(mp.forceMove(new Move(7, 4, 7, 1)));
    mpAssert(!mp.opponentMove(new Move(4, 0)));
    mpAssert(!mp.opponentMove(new Move(4, 0, 7, 1)));
    mpAssert(!mp.opponentMove(new Move(6, 1, 1, 1)));
    mpAssert(mp.opponentMove(new Move(6, 1, 6, 0)));
    mpAssert(!mp.forceMove(new Move(4, 1, 3, 1)));
    mpAssert(mp.forceMove(new Move(4, 1, 1, 1)));
    mpAssert(!mp.opponentMove(new Move(1, 1, 6, 1)));
    mpAssert(mp.opponentMove(new Move(1, 1, 1, 2)));
    mpAssert(mp.forceMove(new Move(0, 5, 1, 5)));
    mpAssert(mp.opponentMove(new Move(1, 7, 1, 6)));
    //System.out.println("After all moves:");
    //System.out.println(mp.board);
    //System.out.println(" ");
  }

  private static void testMinimax1() {
    // ***Test for Step Move.***

    //System.out.println("Minimax Test 1:");
    MachinePlayer mp = new MachinePlayer(BLACK);
    mpAssert(mp.opponentMove(new Move(2, 1)));
    mpAssert(mp.forceMove(new Move(1, 0)));
    mpAssert(mp.opponentMove(new Move(5, 3)));
    mpAssert(mp.forceMove(new Move(1, 2)));
    mpAssert(mp.opponentMove(new Move(3, 4)));
    mpAssert(mp.forceMove(new Move(2, 2)));
    mpAssert(mp.opponentMove(new Move(5, 4)));
    mpAssert(mp.forceMove(new Move(1, 4)));
    mpAssert(mp.opponentMove(new Move(0, 5)));
    mpAssert(mp.forceMove(new Move(2, 4)));
    mpAssert(mp.opponentMove(new Move(3, 5)));
    mpAssert(!mp.board.hasNetwork(BLACK));
    mpAssert(!mp.board.hasNetwork(WHITE));
    //System.out.println("After all moves:");
    //System.out.println(mp.board);
    Move chosenMove = mp.chooseMove();
    mpAssert(chosenMove.moveKind == Move.ADD);
    mpAssert(chosenMove.x1 == 1 && chosenMove.y1 == 7);
    //System.out.println("It is now BLACK's turn. Performing move...");
    //System.out.println(mp.board);
    mpAssert(mp.board.hasNetwork(BLACK));
    mpAssert(!mp.board.hasNetwork(WHITE));
    //System.out.println("Black has a network!");
    //System.out.println(" ");
  }

  private static void testMinimax2() {
    // ***Test for Step Move.***
    
    //System.out.println("Minimax Test 2:");
    MachinePlayer mp = new MachinePlayer(BLACK);
    mpAssert(mp.opponentMove(new Move(0, 1)));
    mpAssert(mp.forceMove(new Move(1, 0)));
    mpAssert(mp.opponentMove(new Move(3, 1)));
    mpAssert(mp.forceMove(new Move(2, 0)));
    mpAssert(mp.opponentMove(new Move(5, 1)));
    mpAssert(mp.forceMove(new Move(5, 0)));
    mpAssert(mp.opponentMove(new Move(7, 1)));
    mpAssert(mp.forceMove(new Move(6, 0)));
    mpAssert(mp.opponentMove(new Move(7, 2)));
    mpAssert(mp.forceMove(new Move(1, 2)));
    mpAssert(mp.opponentMove(new Move(0, 3)));
    mpAssert(mp.forceMove(new Move(2, 3)));
    mpAssert(mp.opponentMove(new Move(7, 4)));
    mpAssert(mp.forceMove(new Move(4, 3)));
    mpAssert(mp.opponentMove(new Move(0, 5)));
    mpAssert(mp.forceMove(new Move(5, 4)));
    mpAssert(mp.opponentMove(new Move(7, 5)));
    mpAssert(mp.forceMove(new Move(1, 7)));
    mpAssert(mp.opponentMove(new Move(0, 6)));
    mpAssert(mp.forceMove(new Move(3, 7)));
    mpAssert(mp.opponentMove(new Move(7, 6, 7, 5)));
    mpAssert(!mp.board.hasNetwork(BLACK));
    mpAssert(!mp.board.hasNetwork(WHITE));
    //System.out.println("After all moves:");
    //System.out.println(mp.board);
    Move chosenMove = mp.chooseMove();
    mpAssert(chosenMove.moveKind == Move.STEP);
    mpAssert(chosenMove.x1 == 3 && chosenMove.y1 == 6);
    mpAssert(chosenMove.x2 == 1 && chosenMove.y2 == 0);
    //System.out.println("It is now BLACK's turn. Performing move...");
    mpAssert(mp.board.hasNetwork(BLACK));
    mpAssert(!mp.board.hasNetwork(WHITE));
    //System.out.println(mp.board);
    //System.out.println("Black has a network!");
    //System.out.println(" ");
  }

  private static void testMinimax3(int numMoves) {
    //System.out.println("Minimax Test 3:");
    MachinePlayer whitePlayer = new MachinePlayer(WHITE);
    MachinePlayer blackPlayer = new MachinePlayer(BLACK);

    Move whiteMove, blackMove;

    for (int i = 1; i <= numMoves; i++) {

      if (blackPlayer.board.hasNetwork(WHITE)){
        //System.out.println("WHITE has a network.");
        //System.out.println("The game has been won after " + i + " rounds.");
        return;
      } else if (whitePlayer.board.hasNetwork(BLACK)){
        //System.out.println("BLACK has a network.");
        //System.out.println("The game has been won after " + i + " rounds.");
        return;
      }
      whiteMove = whitePlayer.chooseMove();
      //System.out.println("WHITE chooses to " + whiteMove);
      mpAssert(blackPlayer.opponentMove(whiteMove));

      //System.out.println("After WHITE has moved:");
      //System.out.println(blackPlayer.board);
      //System.out.println(" ");
      whiteMove = null;

      if (whitePlayer.board.hasNetwork(BLACK)){
        //System.out.println("BLACK has a network.");
        //System.out.println("The game has been won after " + i + " rounds.");
        return;
      } else if (blackPlayer.board.hasNetwork(WHITE)){
        //System.out.println("WHITE has a network.");
        //System.out.println("The game has been won after " + i + " rounds.");
        return;
      }
      blackMove = blackPlayer.chooseMove();
      //System.out.println("BLACK chooses to " + blackMove);
      mpAssert(whitePlayer.opponentMove(blackMove));
      //System.out.println("After BLACK has moved:");
      //System.out.println(whitePlayer.board);
      //System.out.println(" ");
      blackMove = null;
    }
  }

  private static void testMinimax4() {
    MachinePlayer whitePlayer = new MachinePlayer(WHITE);
    MachinePlayer blackPlayer = new MachinePlayer(BLACK);

    Move whiteMove1 = new Move(3, 5);
    Move whiteMove2 = new Move(4, 6);
    Move whiteMove3 = new Move(4, 5);
    Move blackMove1 = new Move(6, 3);
    Move blackMove2 = new Move(5, 0);

    mpAssert(whitePlayer.forceMove(whiteMove1));
    mpAssert(blackPlayer.opponentMove(whiteMove1));  
    mpAssert(blackPlayer.forceMove(blackMove1));
    mpAssert(whitePlayer.opponentMove(blackMove1));
    mpAssert(whitePlayer.forceMove(whiteMove2));
    mpAssert(blackPlayer.opponentMove(whiteMove2));
    mpAssert(blackPlayer.forceMove(blackMove2));
    mpAssert(whitePlayer.opponentMove(blackMove2));

    mpAssert(!whitePlayer.forceMove(whiteMove3));
    mpAssert(!blackPlayer.opponentMove(whiteMove3));
  }

  private static void testMinimax5() {
    MachinePlayer whitePlayer = new MachinePlayer(WHITE);
    MachinePlayer blackPlayer = new MachinePlayer(BLACK);

    Move whiteMove1 = new Move(5, 2);
    Move whiteMove2 = new Move(2, 2);
    Move whiteMove3 = new Move(4, 1);
    Move whiteMove4 = new Move(5, 3); // This should error

    Move blackMove1 = new Move(1, 1);
    Move blackMove2 = new Move(6, 1);
    Move blackMove3 = new Move(5, 6);

    mpAssert(whitePlayer.forceMove(whiteMove1));
    mpAssert(blackPlayer.opponentMove(whiteMove1));  
    mpAssert(blackPlayer.forceMove(blackMove1));
    mpAssert(whitePlayer.opponentMove(blackMove1));
    mpAssert(whitePlayer.forceMove(whiteMove2));
    mpAssert(blackPlayer.opponentMove(whiteMove2));
    mpAssert(blackPlayer.forceMove(blackMove2));
    mpAssert(whitePlayer.opponentMove(blackMove2));
    mpAssert(whitePlayer.forceMove(whiteMove3));
    mpAssert(blackPlayer.opponentMove(whiteMove3));
    mpAssert(blackPlayer.forceMove(blackMove3));
    mpAssert(whitePlayer.opponentMove(blackMove3));

    mpAssert(!whitePlayer.forceMove(whiteMove4));
    mpAssert(!blackPlayer.opponentMove(whiteMove4));
  }

  private static void testMinimax6() {
    MachinePlayer whitePlayer = new MachinePlayer(WHITE); // HUMAN
    MachinePlayer blackPlayer = new MachinePlayer(BLACK); // MACHINE

    Move whiteMove1 = new Move(0, 4);
    Move whiteMove2 = new Move(7, 3);
    Move whiteMove3 = new Move(3, 1);
    Move whiteMove4 = new Move(3, 2);
    Move whiteMove5 = new Move(5, 2);
    Move whiteMove6 = new Move(5, 1);

    mpAssert(whitePlayer.forceMove(whiteMove1));
    mpAssert(blackPlayer.opponentMove(whiteMove1));  
    
    Move blackMove1 = blackPlayer.chooseMove();
    mpAssert(whitePlayer.opponentMove(blackMove1));
    
    mpAssert(whitePlayer.forceMove(whiteMove2));
    mpAssert(blackPlayer.opponentMove(whiteMove2));
    
    //System.out.println(whitePlayer.board);

    Move blackMove2 = blackPlayer.chooseMove();
    mpAssert(whitePlayer.opponentMove(blackMove2));
    
    mpAssert(whitePlayer.forceMove(whiteMove3));
    mpAssert(blackPlayer.opponentMove(whiteMove3));
    
    Move blackMove3 = blackPlayer.chooseMove();
    mpAssert(whitePlayer.opponentMove(blackMove3));

    mpAssert(whitePlayer.forceMove(whiteMove4));
    mpAssert(blackPlayer.opponentMove(whiteMove4));  
    
    Move blackMove4 = blackPlayer.chooseMove();
    mpAssert(whitePlayer.opponentMove(blackMove4));
    
    mpAssert(whitePlayer.forceMove(whiteMove5));
    mpAssert(blackPlayer.opponentMove(whiteMove5));
    
    Move blackMove5 = blackPlayer.chooseMove();
    mpAssert(whitePlayer.opponentMove(blackMove5));
    
    mpAssert(whitePlayer.forceMove(whiteMove6));
    mpAssert(blackPlayer.opponentMove(whiteMove6));

    //System.out.println(blackPlayer.board);
  }

  private static void testMinimax7() {
    MachinePlayer whitePlayer = new MachinePlayer(WHITE); // Machine
    MachinePlayer blackPlayer = new MachinePlayer(BLACK); // Human

    Move whiteMove1 = new Move(0, 2);
    Move whiteMove2 = new Move(1, 2);
    Move whiteMove3 = new Move(1, 5);
    Move whiteMove4 = new Move(4, 5);
    Move whiteMove5 = new Move(4, 2);

    Move blackMove1 = new Move(1, 1);
    Move blackMove2 = new Move(2, 1);
    Move blackMove3 = new Move(4, 1);
    Move blackMove4 = new Move(5, 1);
    Move blackMove5 = new Move(1, 6);
    
    mpAssert(whitePlayer.forceMove(whiteMove1));
    mpAssert(blackPlayer.opponentMove(whiteMove1));  
    
    mpAssert(blackPlayer.forceMove(blackMove1));
    mpAssert(whitePlayer.opponentMove(blackMove1));
    
    mpAssert(whitePlayer.forceMove(whiteMove2));
    mpAssert(blackPlayer.opponentMove(whiteMove2));
    
    mpAssert(blackPlayer.forceMove(blackMove2));
    mpAssert(whitePlayer.opponentMove(blackMove2));
    
    mpAssert(whitePlayer.forceMove(whiteMove3));
    mpAssert(blackPlayer.opponentMove(whiteMove3));
    
    mpAssert(blackPlayer.forceMove(blackMove3));
    mpAssert(whitePlayer.opponentMove(blackMove3));

    mpAssert(whitePlayer.forceMove(whiteMove4));
    mpAssert(blackPlayer.opponentMove(whiteMove4));  
    
    mpAssert(blackPlayer.forceMove(blackMove4));
    mpAssert(whitePlayer.opponentMove(blackMove4));
    
    mpAssert(whitePlayer.forceMove(whiteMove5));
    mpAssert(blackPlayer.opponentMove(whiteMove5));
    
    mpAssert(blackPlayer.forceMove(blackMove5));
    mpAssert(whitePlayer.opponentMove(blackMove5));

    //System.out.println("After all moves:");
    //System.out.println(blackPlayer.board);

    Move whiteChoice = whitePlayer.chooseMove();

    mpAssert(whiteChoice.x1 == 7);
    mpAssert(whiteChoice.y1 == 2);
    
    mpAssert(blackPlayer.opponentMove(whiteChoice));

    //System.out.println("WHITE chose to " + whiteChoice);
    //System.out.println(blackPlayer.board);
  }

  */ // <-------------------------------[comment/uncomment to access test code]

  /* ************************************************************************ *
   * *                          END TESTING MODULE                          * *
   * ************************************************************************ */

}