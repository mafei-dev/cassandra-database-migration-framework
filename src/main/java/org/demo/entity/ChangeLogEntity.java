package org.demo.entity;


import java.time.Instant;


public class ChangeLogEntity {

    private Integer majorVersion;
    private String md5Sum;
    private String description;
    private Instant initDatetime;


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getInitDatetime() {
        return initDatetime;
    }

    public void setInitDatetime(Instant initDatetime) {
        this.initDatetime = initDatetime;
    }

    public Integer getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(Integer majorVersion) {
        this.majorVersion = majorVersion;
    }

    public String getMd5Sum() {
        return md5Sum;
    }

    public void setMd5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }

    public interface Keys {
        String TABLE_NAME = "changeset_log";
        String MAJOR_VERSION_BUKET_ID = "major_version_buket_id";
        String MD5_SUM = "md5_sum";
        String DESCRIPTION = "description";
        String INIT_DATETIME = "init_datetime";
    }
}
