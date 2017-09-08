package org.apache.griffin.core.measure.newEntity;


import org.apache.griffin.core.measure.entity.AuditableEntity;

import javax.persistence.*;
import java.util.List;

/**
 * Created by xiangrchen on 9/6/17.
 */
@Entity
public class DataSource extends AuditableEntity{
    private static final long serialVersionUID = -4748881017079815794L;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
//    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name="dataSource_id")
    private List<DataConnector> connectors;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DataConnector> getConnectors() {
        return connectors;
    }

    public void setConnectors(List<DataConnector> connectors) {
        this.connectors = connectors;
    }

    public DataSource() {
    }

    public DataSource(String name, List<DataConnector> connectors) {
        this.name = name;
        this.connectors = connectors;
    }
}
