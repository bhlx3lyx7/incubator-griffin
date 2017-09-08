package org.apache.griffin.core.measure.newEntity.repo;

import org.apache.griffin.core.measure.newEntity.NewMeasure;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by xiangrchen on 9/8/17.
 */
public interface NewMeasureRepo extends CrudRepository<NewMeasure,Long> {
}
