package org.etd.framework.starter.job.repository.exdent;

import com.google.common.collect.Maps;
import org.etd.framework.starter.job.bean.DefaultJob;
import org.etd.framework.starter.job.repository.JobRepository;

import java.util.Map;


public class MemoryJobRepository implements JobRepository<DefaultJob> {

    private Map<String, Object> jobs = Maps.newHashMap();




    @Override
    public void save(DefaultJob defaultJob) {
        jobs.put(defaultJob.getJobId(), jobs);
    }

    @Override
    public void delete(DefaultJob defaultJob) {
        jobs.remove(defaultJob.getJobId());
    }

    @Override
    public void update(DefaultJob defaultJob) {
        save(defaultJob);
    }

    @Override
    public void select(DefaultJob defaultJob) {
        jobs.get(defaultJob.getJobId());
    }
}
