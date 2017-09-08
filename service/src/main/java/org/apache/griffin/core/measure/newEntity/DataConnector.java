package org.apache.griffin.core.measure.newEntity;

import org.apache.griffin.core.measure.entity.AuditableEntity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Created by xiangrchen on 9/6/17.
 */
@Entity
@Table(name = "newDataCollector")
public class DataConnector extends AuditableEntity{
    private static final long serialVersionUID = -4748881017029815594L;

    public enum ConnectorType {
        avro,
        hive
    }

    @Enumerated(EnumType.STRING)
    private ConnectorType type;

    private String version;

//    private Map<String,String> config;
    private String config;

    public ConnectorType getType() {
        return type;
    }

    public void setType(ConnectorType type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public DataConnector() {
    }

    public DataConnector(ConnectorType type, String version, String config) {
        this.type = type;
        this.version = version;
        this.config = config;
    }
}
