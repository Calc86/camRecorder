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

    @Override
    protected String getTableName() {
        return "CAMS";
    }

    @Override
    protected Model create() {
        return new Cam();
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO " + getTableName() + "(URL) " +
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

   /* public List<Cam> selectAll() throws SQLException {
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
    }*/

    @Override
    protected void childFromResult(ResultSet rs) throws SQLException {
        try {
            url = new URI(rs.getString("URL"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /*public void findByPK(int id) throws SQLException {
        String sql = "SELECT * FROM " + table + " where ID = " + id + ";";

        Statement statement = Database.getConnection().createStatement();
        ResultSet rs = statement.executeQuery(sql);

        rs.next();
        fromResult(rs);
    }*/

    @Override
    public String toString() {
        return id + ":" + url;
    }
}
