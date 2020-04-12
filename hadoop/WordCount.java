import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Job;
import java.util.*;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class WordCount {
    static class WordCountMapper extends Mapper<LongWritable, Text, Text, Text> {
        Text word = new Text();
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            // Only alphanumeric characters (no more commas or quotes)
            line = line.replaceAll("[^a-zA-Z0-9+]", " ");
            StringTokenizer tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                word.set(tokenizer.nextToken().toLowerCase());
                context.write(word, new Text(((FileSplit) context.getInputSplit()).getPath().toString()));
            }
        }
    }

    static class WordCountReducer extends Reducer<Text, Text, Text, IntWritable> {
        public void reduce(Text key, Iterable<Text> docPaths, Context context) throws IOException, InterruptedException {
            //This map will locally (per mapper data) calculate all the frequencies for a given term \t docPath
            HashMap<Text, Integer> map = new HashMap<Text, Integer>();
            for (Text docPath : docPaths) {
                String[] tokens = docPath.toString().split("/");
                String path = tokens[tokens.length - 1];
                if (map.containsKey(new Text(key.toString() + "\t" + path))) {
                    Integer frequency = map.get(new Text(key.toString() + "\t" + path));
                    map.put(new Text(key.toString() + "\t" + path), frequency + 1);
                } else {
                    map.put(new Text(key.toString() + "\t" + path), 1);
                }
            }

            // Iterate through and write the results to the context (docId, wordcount)
            map.forEach((Text k, Integer v) -> {
                try {
                    context.write(k, new IntWritable(v));
                } catch (Exception e) {
                    System.out.println("Error: Reducer write failed");
                }
            });
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        if (args.length != 2) {
            System.err.println("Usage: java WordCount <input path> <output path>");
            System.exit(-1);
        }
        Job job = new Job();
        job.setJarByClass(WordCount.class);
        job.setJobName("Inverted Index");
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.setMapperClass(WordCountMapper.class);
        job.setReducerClass(WordCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.waitForCompletion(true);
    }
}

