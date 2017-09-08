package org.apache.griffin.core.measure.newEntity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.griffin.core.measure.entity.AuditableEntity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Created by xiangrchen on 9/6/17.
 */
@Entity
public class Rule extends AuditableEntity{

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public enum DSLType{
        griffinDsl("griffin-dsl");

        private final String text;

        DSLType(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public enum DQType{
        accuracy
    }

    @Enumerated(EnumType.STRING)
    private DSLType dslType;

    @Enumerated(EnumType.STRING)
    private DQType dqType;

    private String rule;

    @JsonProperty("dsl.type")
    public DSLType getDslType() {
        return dslType;
    }

    @JsonProperty("dsl.type")
    public void setDslType(DSLType dslType) {
        this.dslType = dslType;
    }

    @JsonProperty("dq.type")
    public DQType getDqType() {
        return dqType;
    }

    @JsonProperty("dq.type")
    public void setDqType(DQType dqType) {
        this.dqType = dqType;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Rule() {
    }

    public Rule(DSLType dslType, DQType dqType, String rule) {
        this.dslType = dslType;
        this.dqType = dqType;
        this.rule = rule;
    }
}
