package org.apache.griffin.core.measure.newEntity;

import org.apache.griffin.core.measure.entity.AuditableEntity;

import javax.persistence.*;
import java.util.List;

/**
 * Created by xiangrchen on 9/6/17.
 */
@Entity
@Table(name = "newEvaluteRule")
public class EvaluateRule extends AuditableEntity{
    @OneToMany(cascade = CascadeType.ALL)
//    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name="evaluateRule_id")
    List<Rule> rules;

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public EvaluateRule() {
    }

    public EvaluateRule(List<Rule> rules) {
        this.rules = rules;
    }
}

