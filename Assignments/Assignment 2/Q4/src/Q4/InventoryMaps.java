package Q4;
import java.util.*;
public class InventoryMaps {public static void main(String[]x){Map<String,Integer>h=new HashMap<>(),t=new TreeMap<>(),l=new LinkedHashMap<>();String[]codes={"M30","M10","M20"};for(int i=0;i<codes.length;i++){h.put(codes[i],10+i);t.put(codes[i],10+i);l.put(codes[i],10+i);}for(int n:new int[]{100,10000,100000}){long a=System.nanoTime();for(int i=0;i<n;i++)h.get(codes[i%3]);System.out.println("HashMap lookup "+n+" ns="+(System.nanoTime()-a));}System.out.println("Sorted report "+t.keySet());System.out.println("Insertion report "+l.keySet());}}
