package B1;
import java.io.*;import java.nio.file.*;
public class BufferedLineCounter{public static long count(Path p)throws IOException{long n=0;try(BufferedReader r=Files.newBufferedReader(p)){while(r.readLine()!=null)n++;}return n;}public static void main(String[]x)throws Exception{Path p=Files.createTempFile("attendance",".txt");Files.writeString(p,"A\\nB\\nC\\n");System.out.println("Lines: "+count(p));Files.delete(p);}}
