package org.etd.framework.starter.job.repository.exdent;

import com.google.common.collect.Maps;
import org.etd.framework.starter.job.bean.Job;
import org.etd.framework.starter.job.repository.JobRepository;

import java.util.Map;


public class MemoryJobRepository implements JobRepository<Job> {

    private Map<String, Object> jobs = Maps.newHashMap();




    @Override
    public void save(Job job) {
        jobs.put(job.getJobId(), jobs);
    }

    @Override
    public void delete(Job job) {
        jobs.remove(job.getJobId());
    }

    @Override
    public void update(Job job) {
        save(job);
    }

    @Override
    public void select(Job job) {
        jobs.get(job.getJobId());
    }
}
