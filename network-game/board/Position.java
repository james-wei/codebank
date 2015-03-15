/* Position.java */

package board;

/**
 *  An implementation of a pair of (x, y) coordinates on a Board and the color
 *  corresponding to the (x, y) pair.
 */
public class Position {

  private int x, y;        // The coordinates of the Position
  int color;               // The color of the Position (only used by Board)
  boolean visited;         // If the Position was visited (only used by Path)
  Position[] connections;  // Connected Positions (only used by Board)

  /**
   *  Position constructor. The coordinates of the Position are set to the
   *  specified parameters, the color is set to MachinePlayer.EMPTY, the
   *  Position is marked as unvisited, and no connections are defined.
   *  @param x the x-coordinate of the Position.
   *  @param y the y-coordinate of the Position.
   */
  Position(int x, int y) {
    this.x = x;
    this.y = y;
    this.color = player.MachinePlayer.EMPTY;
    visited = false;
    connections = new Position[8];
  }

  /**
   *  getX() returns the x-coordinate of this Position.
   *  @return the x-coordinate.
   *  Performance: runs in O(1) time.
   */
  int getX() {
    return x;
  }

  /**
   *  getY() returns the y-coordinate of this Position.
   *  @return the y-coordinate.
   *  Performance: runs in O(1) time.
   */
  int getY() {
    return y;
  }

  /**
   *  getGoal() returns which goal (Board.LEFT, Board.RIGHT, Board.TOP, or
   *  Board.BOTTOM) this Position is in, depending on its coordinates.
   *  @return the goal of this Position.
   *  Performance: runs in O(1) time.
   */
  int getGoal() {
    if (x == 0) {
      return Board.LEFT;
    }
    if (x == Board.DIMENSION - 1) {
      return Board.RIGHT;
    }
    if (y == 0) {
      return Board.TOP;
    }
    if (y == Board.DIMENSION - 1) {
      return Board.BOTTOM;
    }
    return Board.NONE;
  }
}
