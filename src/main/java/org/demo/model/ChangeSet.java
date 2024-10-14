package org.demo.model;

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import org.demo.util.HashGenerator;

import java.util.Objects;
import java.util.StringJoiner;

@XmlRootElement(name = "changeset")
public class ChangeSet {

    private String id;
    private String author;
    private Integer order;
    private String statement;
    private DefaultConsistencyLevel consistencyLevel;
    private String md5Sum;


    @XmlTransient
    public String getMd5Sum() {
        if (Objects.isNull(md5Sum)) {
            String row = new StringJoiner("#")
                    .add(id)
                    .add(author)
                    .add(String.valueOf(order))
                    .add(this.getStatement())
                    .toString();
            return HashGenerator.generateHash(row, HashGenerator.ALGType.MD5);
        }
        return md5Sum;
    }

    public void setMd5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }

    public Integer getOrder() {
        return order;
    }

    @XmlAttribute
    public void setOrder(Integer order) {
        this.order = order;
    }

    public DefaultConsistencyLevel getConsistencyLevel() {
        return consistencyLevel;
    }

    @XmlAttribute
    public void setConsistencyLevel(DefaultConsistencyLevel consistencyLevel) {
        this.consistencyLevel = consistencyLevel;
    }

    public String getAuthor() {
        return author;
    }

    @XmlAttribute
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getStatement() {
        if (Objects.nonNull(statement)) {
            return statement.replaceAll("[\\n\\r]+", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }
        return null;
    }

    @XmlElement
    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getId() {
        return id;
    }

    @XmlAttribute
    public void setId(String id) {
        this.id = id;
    }

    public void validate() {
        if (Objects.isNull(id)) throw new RuntimeException("id cannot be null");
        if (Objects.isNull(author)) throw new RuntimeException("author cannot be null");
        if (Objects.isNull(statement)) throw new RuntimeException("statement cannot be null");
        if (Objects.isNull(consistencyLevel)) throw new RuntimeException("consistencyLevel cannot be null");
        if (Objects.isNull(order)) throw new RuntimeException("order cannot be null");
    }
}
