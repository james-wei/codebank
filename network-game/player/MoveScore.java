/* MoveScore.java */

package player;

/**
 *  A data storage class that stores a Move object and an associated minimax 
 *  score.
 */
public class MoveScore {

  /* ***Protected MoveScore Fields*** */
  
  protected Move move;
  protected double boardScore;


  /* ***MoveScore Constructor*** */

  /**
   *  MoveScore() is a constructor for MoveScore objects. The default 
   *  boardScore is initially set to 0.
   */
  public MoveScore() {
    boardScore = 0;
  }

}