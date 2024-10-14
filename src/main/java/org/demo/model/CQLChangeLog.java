package org.demo.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "cqlChangeLog")
public class CQLChangeLog {

    private List<ChangeSet> changeSets;
    private List<Include> includeList;

    private Integer majorVersion;

    @XmlElement(name = "majorVersion")
    public Integer getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(Integer majorVersion) {
        this.majorVersion = majorVersion;
    }

    @XmlElement(name = "include")
    public List<Include> getIncludeList() {
        return includeList;
    }

    public void setIncludeList(List<Include> includeList) {
        this.includeList = includeList;
    }

    public List<ChangeSet> getChangeSets() {
        return changeSets;
    }

    @XmlElement(name = "changeset")
    public void setChangeSets(List<ChangeSet> changeSets) {
        this.changeSets = changeSets;
    }
}
