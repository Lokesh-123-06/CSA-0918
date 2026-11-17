package Q3;
import java.util.*;
final class Student {final String regNo,name;Student(String r,String n){regNo=Objects.requireNonNull(r);name=n;}public boolean equals(Object o){return o instanceof Student s&&regNo.equals(s.regNo);}public int hashCode(){return regNo.hashCode();}public String toString(){return regNo+" "+name;}}
public class Attendance {public static void main(String[]x){Set<Student>s=new HashSet<>();s.add(new Student("192311001","Arun"));s.add(new Student("192311002","Divya"));s.add(new Student("192311001","Arun"));System.out.println("Unique students: "+s.size());Set<Student> mutableDemo=new HashSet<>();Student a=new Student("A1","A");mutableDemo.add(a);System.out.println("Duplicate scan ignored: "+!mutableDemo.add(new Student("A1","A2")));}}
