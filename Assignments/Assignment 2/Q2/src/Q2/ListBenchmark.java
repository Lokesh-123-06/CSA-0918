package Q2;
import java.util.*;
public class ListBenchmark {
 static long run(String name,Runnable r){long t=System.nanoTime();r.run();long d=System.nanoTime()-t;System.out.println(name+" ns="+d);return d;}
 public static void main(String[]x){for(int n:new int[]{1000,10000,50000}){System.out.println("n="+n);List<Integer>a=new ArrayList<>(),l=new LinkedList<>();run("ArrayList append",()->{for(int i=0;i<n;i++)a.add(i);});run("LinkedList append",()->{for(int i=0;i<n;i++)l.add(i);});run("ArrayList indexed",()->{long s=0;for(int i=0;i<n;i++)s+=a.get(i);});run("LinkedList iterate",()->{long s=0;for(int v:l)s+=v;});run("ArrayDeque ends",()->{Deque<Integer>d=new ArrayDeque<>();for(int i=0;i<n;i++){d.addFirst(i);d.addLast(i);}while(!d.isEmpty())d.removeFirst();});}}
}
