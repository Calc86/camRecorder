package ru.xsrv.recorder.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by calc on 22.07.14.
 *
 */
public class Database {
    private static Database instance = new Database();

    private static Database getInstance(){
        return instance;
    }

    public static Connection getConnection(){
        return getInstance().getC();
    }

    Connection c = null;

    public Database() {
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");

            String sql;
            Statement statement;

            statement = c.createStatement();
            sql = "CREATE TABLE IF NOT EXISTS CAMS " +
                    "(ID INTEGER PRIMARY KEY       AUTOINCREMENT, " +
                    " URL            CHAR(250) NOT NULL) ";

            statement.executeUpdate(sql);
            statement.close();

            sql = "CREATE TABLE IF NOT EXISTS ARCHIVE " +
                    "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "CID INTEGER NOT NULL, " +
                    "START INTEGER NOT NULL, "+
                    "STOP INTEGER NOT NULL) ";

            statement = c.createStatement();
            statement.executeUpdate(sql);
            statement.close();


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getC() {
        return c;
    }
}
