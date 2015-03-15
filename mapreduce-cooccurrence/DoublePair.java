/*
 * DoublePair.java is a class which stores two doubles and 
 * implements the Writable interface. It can be used as a 
 * custom value for Hadoop. To use this as a key, you can
 * choose to implement the WritableComparable interface,
 * although that is not necessary for credit.
 */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class DoublePair implements Writable {

    private double double1;
    private double double2;

    /**
     * Constructs a DoublePair with both doubles set to zero.
     */
    public DoublePair() {
        this.double1 = 0;
        this.double2 = 0;
    }

    /**
     * Constructs a DoublePair containing double1 and double2.
     */ 
    public DoublePair(double double1, double double2) {
        this.double1 = double1;
        this.double2 = double2;
    }

    /**
     * Returns the value of the first double.
     */
    public double getDouble1() {
        return this.double1;
    }

    /**
     * Returns the value of the second double.
     */
    public double getDouble2() {
        return this.double2;
    }

    /**
     * Sets the first double to val.
     */
    public void setDouble1(double val) {
        this.double1 = val;
    }

    /**
     * Sets the second double to val.
     */
    public void setDouble2(double val) {
        this.double2 = val;
    }

    /**
     * write() is required for implementing Writable.
     */
    public void write(DataOutput out) throws IOException {
        out.writeDouble(this.double1);
        out.writeDouble(this.double2);
    }

    /**
     * readFields() is required for implementing Writable.
     */
    public void readFields(DataInput in) throws IOException {
        this.double1 = in.readDouble();
        this.double2 = in.readDouble();
    }

    /**
     * pairAssert() is a helper method for the tests in main.
     */
    private static void pairAssert(boolean flag) {
        if (!flag) {
            System.out.println("Error!");
            Thread.dumpStack();
            System.exit(0);
        }
    }

    /**
     * Contains tests.
     */
    public static void main(String[] args) {
        DoublePair myDoublePair1 = new DoublePair();
        pairAssert(myDoublePair1.getDouble1() == 0);
        pairAssert(myDoublePair1.getDouble2() == 0);
        DoublePair myDoublePair2 = new DoublePair(123.456, 789.012);
        pairAssert(myDoublePair2.getDouble1() == 123.456);
        pairAssert(myDoublePair2.getDouble2() == 789.012);
        myDoublePair2.setDouble1(789.012);
        myDoublePair2.setDouble2(123.456);
        pairAssert(myDoublePair2.getDouble1() == 789.012);
        pairAssert(myDoublePair2.getDouble2() == 123.456);
        System.out.println("All tests completed successfully.");
    }
}
