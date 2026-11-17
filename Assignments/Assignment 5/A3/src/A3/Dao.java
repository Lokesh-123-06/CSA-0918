package A3;
import java.sql.*;import java.util.*;
public interface Dao<T,K>{void insert(T value)throws SQLException;Optional<T>findById(K id)throws SQLException;List<T>findAll()throws SQLException;void update(T value)throws SQLException;void delete(K id)throws SQLException;}
