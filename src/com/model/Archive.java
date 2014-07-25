package com.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by calc on 24.07.14.
 *
 */
public class Archive extends Model {
    private long cid;
    private long start;
    private long stop;

    @Override
    protected String getTableName() {
        return "ARCHIVE";
    }

    @Override
    protected Model create() {
        return new Archive();
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO " + getTableName() + "(CID, START, STOP) " +
                " VALUES (" + cid + ", " + start +", " + stop + ");";
    }

    @Override
    protected String getSaveSql() {
        return null;
    }

    @Override
    protected void childFromResult(ResultSet rs) throws SQLException {
        cid = rs.getInt("CID");
        start = rs.getInt("START");
        stop = rs.getInt("STOP");
    }

    public long getCid() {
        return cid;
    }

    public void setCid(long cid) {
        this.cid = cid;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }
}
