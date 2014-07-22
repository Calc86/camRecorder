package com.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by calc on 22.07.14.
 *
 */
public class SQLite {

    public static void main(String[] args) {
        Connection c = null;
        Statement statement = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");
            System.out.println("success");

            statement = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS CAMS " +
                    "(ID INT PRIMARY KEY       NOT NULL, " +
                    " URL            CHAR(250) NOT NULL) ";

            statement.executeUpdate(sql);
            statement.close();
            c.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
