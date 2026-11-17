package B2;
import java.sql.*;
public class LoginSafety{public static boolean unsafe(Connection c,String id,String pwd)throws SQLException{String sql="SELECT COUNT(*) FROM users WHERE id='"+id+"' AND pwd='"+pwd+"'";try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getInt(1)>0;}}public static boolean safe(Connection c,String id,String pwd)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users WHERE id=? AND pwd=?")){p.setString(1,id);p.setString(2,pwd);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1)>0;}}}}
