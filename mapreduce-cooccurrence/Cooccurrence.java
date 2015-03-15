import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Math;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class Cooccurrence {

    /*
     * Inputs is a set of (docID, document contents) pairs.
     */
    public static class Map1 extends Mapper<WritableComparable, Text, Text, DoublePair> {
        /** Regex pattern to find words (alphanumeric + _). */
        final static Pattern WORD_PATTERN = Pattern.compile("\\w+");

        private String targetGram = null;
        private int funcNum = 0;

        /* Text object to store a word to write to output. */
        private Text word = new Text();

        /*
         * Setup gets called exactly once for each mapper, before map() gets called the first time.
         * It's a good place to do configuration or setup that can be shared across many calls to map
         */
        @Override
        public void setup(Context context) {
            targetGram = context.getConfiguration().get("targetWord").toLowerCase();
            try {
                funcNum = Integer.parseInt(context.getConfiguration().get("funcNum"));
            } catch (NumberFormatException e) {
                /* Do nothing. */
            }
        }

        @Override
        public void map(WritableComparable docID, Text docContents, Context context)
                throws IOException, InterruptedException {
            Matcher matcher = WORD_PATTERN.matcher(docContents.toString());
            Func func = funcFromNum(funcNum);

            String currWord = null;     // Current word being parsed
            
            int wordIndex = 0,          // Index of the current word
                currClosestIndex = 0,   // Index of the closest targetGram
                distBtwnCurr = 0,       // Distance between current word 
                                        //     and current targetGram
                distBtwnNext = 0;       // Distance between current word
                                        //     and next targetGram
            
            // Distance between current word and closest targetGram:
            double dist = Double.POSITIVE_INFINITY;

            // Stores the positions of the targetGram in the document:
            ArrayList<Integer> targetPositions = new ArrayList<Integer>(0);

            // Find all of the positions of the target word and store it
            // inside of the ArrayList targetPositions:
            while(matcher.find()) {
                if (matcher.group().toLowerCase().equals(targetGram)) {
                    targetPositions.add(wordIndex);
                }
                wordIndex++;
            }

            // Reset the matcher so that it begins parsing from the
            // beginning of the document again. Also, reset the
            // word index counter:
            matcher.reset();
            wordIndex = 0;

            while(matcher.find()) {
                currWord = matcher.group().toLowerCase();
                word.set(currWord);

                // Case I: The targetGram was not found in the document.
                //         The distance between each word and the target
                //         is positive infinity.
                if (targetPositions.isEmpty()) {
                    // The key is a Text object containing the current
                    // word being parsed.
                    // The value is a DoublePair containing f(d) as its 
                    // first double and the number of occurrences of the 
                    // current word (1) as its second double.
                    context.write(word, new DoublePair(func.f(dist), 1));
                } 

                // Case II: The targetGram was found in the document and
                //          the current word is not equal to the targetGram.
                else if (!currWord.equals(targetGram)) {
                    // Subcase A: The current targetGram is the last 
                    //            targetGram in the document.
                    if (currClosestIndex == targetPositions.size() - 1) {
                        dist = Math.abs(wordIndex - targetPositions.get(currClosestIndex));
                        context.write(word, new DoublePair(func.f(dist), 1));
                    } 
                    // Subcase B: The current word is potentially between
                    //            two targetGrams. Find the distances to
                    //            both and use the shortest distance.
                    else {
                        distBtwnCurr = Math.abs(wordIndex - targetPositions.get(currClosestIndex));
                        distBtwnNext = Math.abs(wordIndex - targetPositions.get(currClosestIndex + 1));
                        if (distBtwnCurr > distBtwnNext) {
                            dist = (double) distBtwnNext;
                            // Update the current targetGram:
                            currClosestIndex++;
                        } else {
                            dist = (double) distBtwnCurr;
                        }
                        // The key is a Text object containing the current
                        // word being parsed.
                        // The value is a DoublePair containing f(d) as its
                        // first double and the number of occurrences of the
                        // current word (1) as its second double.
                        context.write(word, new DoublePair(func.f(dist), 1));
                    }
                }
                wordIndex++;
            }
        }

        /** Returns the Func corresponding to FUNCNUM*/
        private Func funcFromNum(int funcNum) {
            Func func = null;
            switch (funcNum) {
                case 0:	
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0;
                        }			
                    };	
                    break;
                case 1:
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0 + 1.0 / d;
                        }			
                    };
                    break;
                case 2:
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0 + Math.sqrt(d);
                        }			
                    };
                    break;
            }
            return func;
        }
    }

    /** Here's where you'll be implementing your combiner. It must be non-trivial for you to receive credit. */
    public static class Combine1 extends Reducer<Text, DoublePair, Text, DoublePair> {
        @Override
        public void reduce(Text key, Iterable<DoublePair> values,
                Context context) throws IOException, InterruptedException { 
            double sumScore = 0,
                   numOccur = 0;
            
            // Sum all of the values of f(d) into sumScore.
            // Sum all of the occurrences of the word in the key into numOccur.
            for (DoublePair dp : values) {
                sumScore += dp.getDouble1();
                numOccur += dp.getDouble2();
            }
            // Emit a key-value pair where the key is the word and the value
            // is a new DoublePair object containing sumScore and numOccur.
            context.write(key, new DoublePair(sumScore, numOccur));
        }
    }


    public static class Reduce1 extends Reducer<Text, DoublePair, DoubleWritable, Text> {
        @Override
        public void reduce(Text key, Iterable<DoublePair> values,
                Context context) throws IOException, InterruptedException {
            double sumScore = 0,    // sumScore is S_w
                   numOccur = 0,    // numOccur is A_w
                   coScore = 0;     // coScore is the co-occurrence 
            
            // Sum all of the values of f(d) into sumScore.
            // Sum all of the occurrences of the word in the key into numOccur.
            for (DoublePair dp : values) {
                sumScore += dp.getDouble1();
                numOccur += dp.getDouble2();
            }
            if (sumScore > 0) {
                // Calculate co-occurrence if the sum of f(d) is greater than zero
                coScore = (sumScore*Math.pow(Math.log(sumScore), 3.0))/(numOccur);
            }
            context.write(new DoubleWritable(coScore), key);
        }
    }

    public static class Map2 extends Mapper<DoubleWritable, Text, DoubleWritable, Text> {
        @Override
        public void map(DoubleWritable score, Text word, Context context)
                throws IOException, InterruptedException {
            // Emit a key-value pair that is very similar to the input k.v.
            // pair, except that the output DoubleWritable key should contain
            // the negative of the input DoubleWritable key.
            // Multiplying by -1 is done in order to force Hadoop to sort
            // the key-value pairs such that the words with larger scores
            // are passed to Reduce2 before words with smaller scores.
            context.write(new DoubleWritable(-1*score.get()), word);
        }
    }

    public static class Reduce2 extends Reducer<DoubleWritable, Text, DoubleWritable, Text> {
        int n = 0;
        static int N_TO_OUTPUT = 100;

        /*
         * Setup gets called exactly once for each reducer, before reduce() gets called the first time.
         * It's a good place to do configuration or setup that can be shared across many calls to reduce
         */
        @Override
        protected void setup(Context c) {
            n = 0;
        }

        /*
         * Your output should be a in the form of (DoubleWritable score, Text word)
         * where score is the co-occurrence value for the word. Your output should be
         * sorted from largest co-occurrence to smallest co-occurrence.
         */
        @Override
        public void reduce(DoubleWritable key, Iterable<Text> values,
                Context context) throws IOException, InterruptedException {
            for (Text word : values) {
                if (n < N_TO_OUTPUT) {
                    // Multiply the double value in key by -1 to obtain the
                    // original co-occurrence.
                    context.write(new DoubleWritable(-1*key.get()), word);
                    n++;
                } else {
                    break;
                }
            }
        }
    }

    public static void main(String[] rawArgs) throws Exception {
        GenericOptionsParser parser = new GenericOptionsParser(rawArgs);
        Configuration conf = parser.getConfiguration();
        String[] args = parser.getRemainingArgs();

        boolean runJob2 = conf.getBoolean("runJob2", true);
        boolean combiner = conf.getBoolean("combiner", false);

        System.out.println("Target word: " + conf.get("targetWord"));
        System.out.println("Function num: " + conf.get("funcNum"));

        if(runJob2)
            System.out.println("running both jobs");
        else
            System.out.println("for debugging, only running job 1");

        if(combiner)
            System.out.println("using combiner");
        else
            System.out.println("NOT using combiner");

        Path inputPath = new Path(args[0]);
        Path middleOut = new Path(args[1]);
        Path finalOut = new Path(args[2]);
        FileSystem hdfs = middleOut.getFileSystem(conf);
        int reduceCount = conf.getInt("reduces", 32);

        if(hdfs.exists(middleOut)) {
            System.err.println("can't run: " + middleOut.toUri().toString() + " already exists");
            System.exit(1);
        }
        if(finalOut.getFileSystem(conf).exists(finalOut) ) {
            System.err.println("can't run: " + finalOut.toUri().toString() + " already exists");
            System.exit(1);
        }

        {
            Job firstJob = new Job(conf, "job1");

            firstJob.setJarByClass(Map1.class);

            /* You may need to change things here */
            firstJob.setMapOutputKeyClass(Text.class);
            firstJob.setMapOutputValueClass(DoublePair.class);
            firstJob.setOutputKeyClass(DoubleWritable.class);
            firstJob.setOutputValueClass(Text.class);
            /* End region where we expect you to perhaps need to change things. */

            firstJob.setMapperClass(Map1.class);
            firstJob.setReducerClass(Reduce1.class);
            firstJob.setNumReduceTasks(reduceCount);


            if(combiner)
                firstJob.setCombinerClass(Combine1.class);

            firstJob.setInputFormatClass(SequenceFileInputFormat.class);
            if(runJob2)
                firstJob.setOutputFormatClass(SequenceFileOutputFormat.class);

            FileInputFormat.addInputPath(firstJob, inputPath);
            FileOutputFormat.setOutputPath(firstJob, middleOut);

            firstJob.waitForCompletion(true);
        }

        if(runJob2) {
            Job secondJob = new Job(conf, "job2");

            secondJob.setJarByClass(Map1.class);
            /* You may need to change things here */
            secondJob.setMapOutputKeyClass(DoubleWritable.class);
            secondJob.setMapOutputValueClass(Text.class);
            secondJob.setOutputKeyClass(DoubleWritable.class);
            secondJob.setOutputValueClass(Text.class);
            /* End region where we expect you to perhaps need to change things. */

            secondJob.setMapperClass(Map2.class);
            secondJob.setReducerClass(Reduce2.class);

            secondJob.setInputFormatClass(SequenceFileInputFormat.class);
            secondJob.setOutputFormatClass(TextOutputFormat.class);
            secondJob.setNumReduceTasks(1);


            FileInputFormat.addInputPath(secondJob, middleOut);
            FileOutputFormat.setOutputPath(secondJob, finalOut);

            secondJob.waitForCompletion(true);
        }
    }

}
