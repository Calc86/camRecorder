package com.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by calc on 22.07.14.
 *
 */
abstract public class Model {
    protected String table;
    protected int id;

    protected Model(String table) {
        this.table = table;
    }

    abstract protected String getInsertSql();
    public void insert() throws SQLException {
        execute(getInsertSql());
    }

    abstract protected String getSaveSql();
    public void save() throws SQLException {
        execute(getSaveSql());
    }

    private void execute(String sql) throws SQLException {
        Statement statement = Database.getConnection().createStatement();
        statement.executeUpdate(sql);
        statement.close();
    }

    public void delete() throws SQLException {
        String sql = "DELETE FROM " + table + " where ID = " + id + ";";
        Statement statement = Database.getConnection().createStatement();
        statement.executeUpdate(sql);
    }

    public int getId() {
        return id;
    }
}
