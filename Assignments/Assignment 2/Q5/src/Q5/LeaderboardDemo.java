package Q5;
import java.util.*;
record Team(String name,int points,int goalDiff) {public String toString(){return name+"("+points+","+goalDiff+")";}}
class Leaderboard<T>{private final List<T>items=new ArrayList<>();private final Comparator<? super T>order;Leaderboard(Comparator<? super T>o){order=o;}void add(T x){items.add(x);}List<T>ranking(){List<T>copy=new ArrayList<>(items);copy.sort(order);return copy;}}
public class LeaderboardDemo{public static void main(String[]x){Comparator<Team>byRank=Comparator.comparingInt(Team::points).reversed().thenComparing(Comparator.comparingInt(Team::goalDiff).reversed()).thenComparing(Team::name);Leaderboard<Team>b=new Leaderboard<>(byRank);b.add(new Team("Panthers",9,4));b.add(new Team("Eagles",9,6));b.add(new Team("Cobras",6,1));b.add(new Team("Bisons",9,4));System.out.println(b.ranking());}}
