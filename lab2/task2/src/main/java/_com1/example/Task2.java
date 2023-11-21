package com1.example;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class Task2 {

/**
* @param args
* 对A,B两个文件进行合并，并剔除其中重复的内容，得到一个新的输出文件C
*/
//重载map函数，直接将输入中的value复制到输出数据的key上
public static class Map extends Mapper<Object, Text, Text, IntWritable>{
    private final static IntWritable one = new IntWritable(1);
    public void map(Object key, Text value, Context context) throws IOException,InterruptedException{
        Counter lineCounter = context.getCounter("Custom Counters", "Line Count");
        long lineNumber = lineCounter.getValue();
        // 将每一行的CSV记录分割成字段
        String[] fields = value.toString().split(",");
        if (fields.length > 0 && lineNumber > 1) { // 跳过标题行
        // 获取target列的值
            String target = fields[25].trim();
        // 输出键值对
            context.write(new Text(target), one);
        }
        lineCounter.increment(1);
    }
    protected void setup(Context context) throws IOException, InterruptedException {
        Counter lineCounter = context.getCounter("Custom Counters", "Line Count");
        lineCounter.setValue(1);
    }
}

//重载reduce函数，直接将输入中的key复制到输出数据的key上
public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable>{
    private TreeMap<Integer, Text> countMap = new TreeMap<>();

    public void reduce(Text key, Iterable<IntWritable> values, Context context)throws IOException, InterruptedException {
        int sum = 0;
        for (IntWritable value : values) {
            sum += value.get();
        }
        countMap.put(sum, new Text(key));
    }

    protected void cleanup(Context context) throws IOException, InterruptedException {
        for (Entry<Integer, Text> entry : countMap.descendingMap().entrySet()) {
            int count = entry.getKey();
            Text weekday = entry.getValue();
            context.write(weekday, new IntWritable(count));
        }
    }
}


public static void main(String[] args) throws Exception{

// TODO Auto-generated method stub
Configuration conf = new Configuration();
conf.set("fs.default.name","hdfs://localhost:9000");
String[] otherArgs = new String[]{"input","output"}; /* 直接设置输入参数 */
if (otherArgs.length != 2) {
System.err.println("Usage: wordcount <in> <out>");
System.exit(2);
}
Job job = Job.getInstance(conf,"target count");
job.setJarByClass(Task2.class);
job.setMapperClass(Map.class);
job.setCombinerClass(Reduce.class);
job.setReducerClass(Reduce.class);
job.setOutputKeyClass(Text.class);
job.setOutputValueClass(IntWritable.class);
FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
System.exit(job.waitForCompletion(true) ? 0 : 1);
}
}