package com.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by calc on 22.07.14.
 *
 */
public class Cam extends Model {
    private URI url;

    public Cam() {
        super("CAMS");
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO " + table + "(URL) " +
                "VALUES ('" + url.toString() + "');";
    }

    @Override
    protected String getSaveSql() {
        return null;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public URI getUrl() {
        return url;
    }

    public List<Cam> selectAll() throws SQLException {
        String sql = "SELECT * FROM " + table + ";";

        Statement statement = Database.getConnection().createStatement();
        ResultSet rs = statement.executeQuery(sql);

        List<Cam> list = new ArrayList<Cam>();
        while(rs.next()){
            Cam cam = new Cam();
            cam.fromResult(rs);
            list.add(cam);
        }

        return list;
    }

    protected void fromResult(ResultSet rs) throws SQLException {
        try {
            id = rs.getInt("ID");
            url = new URI(rs.getString("URL"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void findByPK(int id) throws SQLException {
        String sql = "SELECT * FROM " + table + " where ID = " + id + ";";

        Statement statement = Database.getConnection().createStatement();
        ResultSet rs = statement.executeQuery(sql);

        rs.next();
        fromResult(rs);
    }

    @Override
    public String toString() {
        return id + ":" + url;
    }

    public static void main(String[] args) throws URISyntaxException {
        Cam cam = new Cam();

        /*cam.setUrl(new URI("http://127.0.0.1:8080/asd.mp4"));

        try {
            cam.insert();
            System.out.println("success");
        } catch (SQLException e) {
            e.printStackTrace();
        }*/

        List<Cam> list = null;
        try {
            list = cam.selectAll();
            System.out.println(list.size());

            for(Cam c : list){
                System.out.println(c.getUrl());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
}
