/* Path.java */

package board;

import list.DList;
import list.DListNode;

/**
 *  An implementation of a stack of Positions in a Network game. A Path must
 *  start at a goal, but it need not be a complete network between two goals. A
 *  Path does not allow a Position to be appended if that Position guarantees
 *  that the Path will never be a valid network. The last Position in a Path
 *  can also be popped, if necessary. Throughout the class, it is assumed that
 *  if p1.x == p2.x and p1.y == p2.y, then p1 == p2.
 */
public class Path {

  // Direction flags
  private static final int NO_DIRECTION = 0;
  private static final int VERTICAL = 1;
  private static final int HORIZONTAL = 2;
  private static final int FWD_DIAG = 3;  // Bottom left to top right
  private static final int BKWD_DIAG = 4; // Top left to bottom right
  // The minimum length for a network
  private static final int MIN_NETWORK_LENGTH = 6;

  // The list of previously visited Positions
  private DList positions; // The list of previously visited Positions
  // The start and end goals (opposite from each other), or both Board.NONE
  private int startGoal;
  private int endGoal;
  private boolean network; // whether the Path is currently a naetwork

  /**
   *  Path constructor. The first Position appended to the Path will be in the
   *  specified start goal, and the end Position will be in the goal opposite
   *  start.
   *  @param start the goal in which the Path will start (must be Board.LEFT,
   *         Board.RIGHT, Board.TOP, or Board.BOTTOM).
   */
  Path(int start) {
    positions = new DList();
    startGoal = start;
    if (start == Board.LEFT) {
      endGoal = Board.RIGHT;
    } else if (start == Board.RIGHT) {
      endGoal = Board.LEFT;
    } else if (start == Board.TOP) {
      endGoal = Board.BOTTOM;
    } else if (start == Board.BOTTOM) {
      endGoal = Board.TOP;
    } else {
      startGoal = Board.NONE;
      endGoal = Board.NONE;
    }
    network = false;
  }

  /**
   *  append() adds Position p to the end of this Path if the Path can later be
   *  extended to have more Positions. If p is null, this is already a network,
   *  p is already visited, the parameter passed to the constructor is invalid,
   *  this is empty and p is not in the start goal, p is in the wrong goal, or
   *  the last two positions and p lie on the same line, p cannot be appended.
   *  @param p the Position to add to the end of this Path.
   *  @return true if p can be added to the end of this Path, false otherwise.
   *  Performance: runs in O(1) time.
   */
  boolean append(Position p) {
    if (!canAppend(p)) {
      return false;
    }
    // Insert p to the front of positions, mark p as visited, and set network
    // to true (if necessary)
    positions.insertFront(p);
    p.visited = true;
    if (p.getGoal() == endGoal) {
      network = true;
    }
    return true;
  }

  /**
   *  canAppend() returns whether a Position can be added to the end of this
   *  Path, as specified in the documentation for append().
   *  @param p the Position to add to the end of this Path.
   *  @return true if p can be added to the end of this Path, false otherwise.
   *  Performance: runs in O(1) time.
   */
  private boolean canAppend(Position p) {
    // If p is null, return false to avoid a null-pointer exception
    // If this is already a network, return false
    // If p is already visited, return false
    // If endGoal was set to Board.NONE during construction, return false
    if (p == null || network || p.visited || endGoal == Board.NONE) {
      return false;
    }
    int currentGoal = p.getGoal();
    int length = positions.length();
    // If this is empty and p is not in startGoal, return false
    if (length == 0 && currentGoal != startGoal) {
      return false;
    }
    // If this is nonempty and p is in a goal other than endGoal, return false
    if (length > 0 && (currentGoal != Board.NONE && currentGoal != endGoal)) {
      return false;
    }
    // If this has one Position p1 and p1 does not direct to p, return false
    if (length == 1) {
      Position p1 = (Position)positions.front().item;
      if (getDirection(p, p1) == NO_DIRECTION) {
        return false;
      }
    }
    // If this has at least two Positions and the last position does not direct
    // to p, or the second-last position and last position and p all lie on the
    // same line, return false
    if (length > 1) {
      Position p1 = (Position)positions.front().item;
      Position p2 = (Position)positions.next(positions.front()).item;
      int d1 = getDirection(p, p1);
      int d2 = getDirection(p1, p2);
      if (d2 == NO_DIRECTION || d1 == d2) {
        return false;
      }
    }
    // If the end goal is reached too soon, return false
    if (length < MIN_NETWORK_LENGTH - 1 && p.getGoal() == endGoal) {
      return false;
    }
    return true;
  }

  /**
   *  getDirection() takes two Positions and returns VERTICAL if they lie on
   *  the same vertical line, HORIZONTAL if they lie on the same horizontal
   *  line, and so on. If none of the four direction flags apply, the function
   *  returns NO_DIRECTION. It is assumed that p1 != p2.
   *  @param p1 the first Position.
   *  @param p2 the second Position.
   *  @return the appropriate direction flag for p1 and p2.
   *  Performance: runs in O(1) time.
   */
  private int getDirection(Position p1, Position p2) {
    int dx = p1.getX() - p2.getX();
    int dy = p1.getY() - p2.getY();
    if (dx == 0) {
      return VERTICAL;
    } else if (dy == 0) {
      return HORIZONTAL;
    } else if (dx == -dy) {
      return FWD_DIAG;
    } else if (dx == dy) {
      return BKWD_DIAG;
    } else {
      return NO_DIRECTION;
    }
  }

  /**
   *  pop() removes the Position at the end of this Path, or does nothing if
   *  this Path is already empty.
   *  Performance: runs in O(1) time.
   */
  void pop() {
    DListNode node = positions.front();
    if (node != null) {
      ((Position)node.item).visited = false;
      positions.remove(node);
      network = false;
    }
  }

  /**
   *  end() returns the last (most recently appended) Position in this Path.
   *  @return the end Position, or null if no Positions have been appended yet.
   *  Performance: runs in O(1) time.
   */
  Position end() {
    DListNode node = positions.front();
    if (node == null) {
      return null;
    } else {
      return (Position)node.item;
    }
  }

  /**
   *  isNetwork() returns true if this Path is a network between two opposite
   *  goals, or false otherwise. Once this Path is a network, trying to append
   *  another Position will always return false.
   *  @return whether this Path is a network.
   *  Performance: runs in O(1) time.
   */
  boolean isNetwork() {
    return network;
  }

  /**
   *  length() returns the number of Positions in this Path.
   *  @return an integer that represents the length of the Path.
   *  Performance: runs in O(1) time.
   */
  int length() {
    return positions.length();
  }

  /* ************************************************************************ *
   * *                            TESTING MODULE                            * *
   * ************************************************************************ */
   /* // <-------------------------------[comment/uncomment to access test code]

  public static void main(String[] args) {
    test1();
    test2();
    test3();
    test4();
    test5();
    test6();
    test7();
    test8();
    test9();
  }

  private static void test1() {
    Path p = new Path(Board.BOTTOM);
    pathAssert(!p.append(new Position(0, 2)));
    pathAssert(!p.append(new Position(7, 2)));
    pathAssert(!p.append(new Position(2, 0)));
    pathAssert(p.append(new Position(2, 7)));
  }

  private static void test2() {
    Position p01 = new Position(0, 1);
    Path p = new Path(Board.LEFT);
    pathAssert(p.append(p01));
    pathAssert((Position)p.positions.back().item == p01);
    pathAssert(p.end() == p01);
    pathAssert(!p.isNetwork());
    Position p02 = new Position(0, 2);
    pathAssert(!p.append(p02));
    pathAssert(!p.isNetwork());
  }

  private static void test3() {
    Path p = new Path(Board.TOP);
    pathAssert(p.append(new Position(1, 0)));
    pathAssert(!p.append(new Position(0, 1)));
    pathAssert(!p.isNetwork());
  }

  private static void test4() {
    Path p = new Path(Board.RIGHT);
    pathAssert(p.append(new Position(7, 1)));
    pathAssert(!p.append(new Position(0, 1)));
    pathAssert(!p.isNetwork());
    pathAssert(!p.append(new Position(1, 2)));
  }

  private static void test5() {
    Path p = new Path(Board.TOP);
    pathAssert(p.append(new Position(1, 0)));
    Position p11 = new Position(1, 1);
    pathAssert(p.append(p11));
    pathAssert(p.append(new Position(2, 2)));
    pathAssert(p.append(new Position(2, 1)));
    pathAssert(!p.append(p11));
  }

  private static void test6() {
    Path p = new Path(Board.LEFT);
    pathAssert(p.append(new Position(0, 6)));
    pathAssert(p.append(new Position(1, 6)));
    pathAssert(!p.append(new Position(2, 6)));
    pathAssert(p.append(new Position(2, 5)));
    pathAssert(p.append(new Position(3, 6)));
    pathAssert(!p.append(new Position(7, 6)));
    pathAssert(!p.isNetwork());
  }

  private static void test7() {
    Path p = new Path(Board.LEFT);
    pathAssert(p.append(new Position(0, 6)));
    pathAssert(p.append(new Position(1, 5)));
    pathAssert(p.append(new Position(2, 6)));
    pathAssert(p.append(new Position(3, 5)));
    pathAssert(p.append(new Position(4, 6)));
    pathAssert(p.append(new Position(7, 6)));
    pathAssert(p.isNetwork());
    pathAssert(!p.append(new Position(6, 5)));
    pathAssert(p.isNetwork());
  }

  private static void test8() {
    Path p1 = new Path(Board.LEFT);
    pathAssert(p1.append(new Position(0, 2)));
    pathAssert(p1.append(new Position(2, 2)));
  }

  private static void test9() {
    Path p = new Path(Board.LEFT);
    Position p02 = new Position(0, 2);
    pathAssert(p.append(p02));
    pathAssert(p.append(new Position(2, 2)));
    p.pop();
    pathAssert((Position)p.positions.back().item == p02);
    pathAssert(p.end() == p02);
    p.pop();
    pathAssert(p.positions.back() == null);
    pathAssert(p.end() == null);
    Position p03 = new Position(0, 3);
    pathAssert(!p.append(new Position(6, 0)));
    pathAssert(p.append(p03));
    pathAssert((Position)p.positions.back().item == p03);
    pathAssert(p.end() == p03);
    p.pop();
    pathAssert(p.positions.back() == null);
    pathAssert(p.end() == null);
  }

  private static void pathAssert(boolean flag) {
    if (!flag) {
      System.out.println("******INVARIANT FAILED******");
      Thread.dumpStack();
      System.exit(0);
    }
  }

  */ // <-------------------------------[comment/uncomment to access test code]
  /* ************************************************************************ *
   * *                          END TESTING MODULE                          * *
   * ************************************************************************ */

}
