package Q1;
public class StringBenchmark {
 static String plus(String[] a){String s=""; for(String x:a)s+=x; return s;}
 static String builder(String[] a){StringBuilder b=new StringBuilder();for(String x:a)b.append(x);return b.toString();}
 static String buffer(String[] a){StringBuffer b=new StringBuffer();for(String x:a)b.append(x);return b.toString();}
 static long time(java.util.function.Supplier<String> f,int reps){long t=System.nanoTime();int z=0;for(int i=0;i<reps;i++)z+=f.get().length();if(z==0)System.out.print("");return System.nanoTime()-t;}
 public static void main(String[] args){int[][] workloads={{6,1},{50,10000},{20,100000}};for(int[] w:workloads){String[] a=new String[w[0]];for(int i=0;i<a.length;i++)a[i]="FIELD"+i+"-VALUE;";int reps=w[1];System.out.println("fields="+w[0]+", tickets="+reps);time(()->builder(a),1000);System.out.println("plus ns="+time(()->plus(a),reps));System.out.println("builder ns="+time(()->builder(a),reps));System.out.println("buffer ns="+time(()->buffer(a),reps));}}
}
