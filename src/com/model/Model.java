package com.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by calc on 22.07.14.
 *
 */
abstract public class Model {
    //protected String table;
    protected long id;

    abstract protected String getInsertSql();
    public void insert() throws SQLException {
        setID(execute(getInsertSql()));
    }

    private void setID(long id){
        this.id = id;
    }

    abstract protected String getSaveSql();
    public void save() throws SQLException {
        execute(getSaveSql());
    }

    private synchronized long execute(String sql) throws SQLException {
        long lID = 0;
        Statement statement = Database.getConnection().createStatement();
        statement.executeUpdate(sql);
        if(statement.getGeneratedKeys().next()){
            lID = statement.getGeneratedKeys().getLong(1);
        }
        statement.close();

        return lID;
    }

    public synchronized void delete() throws SQLException {
        String sql = "DELETE FROM " + getTableName() + " where ID = " + id + ";";
        Statement statement = Database.getConnection().createStatement();
        statement.executeUpdate(sql);
    }

    public long getId() {
        return id;
    }

    protected synchronized void fromResult(ResultSet rs) throws SQLException {
        id = rs.getInt("ID");
        childFromResult(rs);
    }

    protected abstract void childFromResult(ResultSet rs) throws SQLException;

    abstract protected String getTableName();

    public synchronized static <T extends Model> T findByID(T model, int id) throws SQLException {
        String sql = "SELECT * FROM " + model.getTableName() + " where ID = " + id + ";";

        Statement statement = Database.getConnection().createStatement();
        ResultSet rs = statement.executeQuery(sql);

        rs.next();
        model.fromResult(rs);

        return model;
    }

    protected abstract Model create();

    public static <T extends Model> List<T> selectAll(T model) throws SQLException {
        return select(model, "");
    }

    public synchronized static <T extends Model> List<T> select(T model, String where) throws SQLException {
        String sql = "SELECT * FROM " + model.getTableName();
        if(!where.equals("")) sql += " where " + where;
        sql += ";";

        Statement statement = Database.getConnection().createStatement();
        ResultSet rs = statement.executeQuery(sql);

        ArrayList<T> list = new ArrayList<T>();
        while(rs.next()){
            T t = (T) model.create();
            t.fromResult(rs);
            list.add(t);
        }

        return list;
    }
}
