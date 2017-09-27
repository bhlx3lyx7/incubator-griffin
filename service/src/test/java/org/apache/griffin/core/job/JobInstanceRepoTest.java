//package org.apache.griffin.core.job;
//
//import org.apache.griffin.core.job.entity.JobInstance;
//import org.apache.griffin.core.job.entity.LivySessionStates;
//import org.apache.griffin.core.job.repo.JobInstanceRepo;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.context.annotation.PropertySource;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.Assert.assertEquals;
//
//@RunWith(SpringRunner.class)
//@PropertySource("classpath:application.properties")
//@DataJpaTest
//public class JobInstanceRepoTest {
//
//    @Autowired
//    private TestEntityManager entityManager;
//
//    @Autowired
//    private JobInstanceRepo jobInstanceRepo;
//
//    @Before
//    public void setUp() {
//        entityManager.clear();
//        entityManager.flush();
//        setEntityManager();
//    }
//
//    @Test
//    public void testFindByGroupNameAndJobNameWithPageable() {
//        Pageable pageRequest = new PageRequest(0, 10, Sort.Direction.DESC, "timestamp");
//        List<JobInstance> instances = jobInstanceRepo.findByGroupNameAndJobName("BA", "job3", pageRequest);
//        assertThat(instances.size()).isEqualTo(1);
//        assertEquals(instances.get(0).getAppId(), "appId3");
//    }
//
//    @Test
//    public void testFindByGroupNameAndJobName() {
//        List<JobInstance> instances = jobInstanceRepo.findByGroupNameAndJobName("BA", "job1");
//        assertThat(instances.size()).isEqualTo(1);
//        assertEquals(instances.get(0).getAppId(), "appId1");
//    }
//
//    @Test
//    public void testFindGroupWithJobName() {
//        List<Object> list = jobInstanceRepo.findGroupWithJobName();
//        assertThat(list.size()).isEqualTo(3);
//    }
//
//    @Test
//    public void testDeleteByGroupAndJobName() {
//        jobInstanceRepo.deleteByGroupAndJobName("BA", "job1");
//        assertThat(jobInstanceRepo.count()).isEqualTo(2);
//    }
//
////    @Test
////    public void testUpdate() {
////        List<JobInstance> instances0 = jobInstanceRepo.findGroupWithJobName();
////        jobInstanceRepo.update(1L, LivySessionStates.State.dead, "appIdChanged", "appUriChanged");
////        List<JobInstance> instances = jobInstanceRepo.findByGroupNameAndJobName("BA", "job1");
////        assertEquals(jobInstanceRepo.findOne(1L).getState(), LivySessionStates.State.dead);
////    }
//
//    private void setEntityManager() {
//        JobInstance instance1 = new JobInstance("BA", "job1", 0, LivySessionStates.State.success,
//                "appId1", "http://domain.com/uri1", System.currentTimeMillis());
//        JobInstance instance2 = new JobInstance("BA", "job2", 1, LivySessionStates.State.error,
//                "appId2", "http://domain.com/uri2", System.currentTimeMillis());
//        JobInstance instance3 = new JobInstance("BA", "job3", 2, LivySessionStates.State.starting,
//                "appId3", "http://domain.com/uri3", System.currentTimeMillis());
//        entityManager.persistAndFlush(instance1);
//        entityManager.persistAndFlush(instance2);
//        entityManager.persistAndFlush(instance3);
//    }
//}
