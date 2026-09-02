package org.etd.framework.starter.job.repository.exdent;

import com.google.common.collect.Maps;
import org.etd.framework.starter.job.bean.DefaultJob;
import org.etd.framework.starter.job.repository.JobRepository;

import java.util.Map;


/**
 * 内存模式分布式任务仓储实现类。
 */
public class MemoryJobRepository implements JobRepository<DefaultJob> {

    private Map<String, Object> jobs = Maps.newHashMap();




    /**
     * 保存
     *
     * @param defaultJob 参数 defaultJob
     */
    @Override
    public void save(DefaultJob defaultJob) {
        jobs.put(defaultJob.getJobId(), jobs);
    }

    /**
     * 删除
     *
     * @param defaultJob 参数 defaultJob
     */
    @Override
    public void delete(DefaultJob defaultJob) {
        jobs.remove(defaultJob.getJobId());
    }

    /**
     * 更新修改
     *
     * @param defaultJob 参数 defaultJob
     */
    @Override
    public void update(DefaultJob defaultJob) {
        save(defaultJob);
    }

    /**
     * 查询
     *
     * @param defaultJob 参数 defaultJob
     */
    @Override
    public void select(DefaultJob defaultJob) {
        jobs.get(defaultJob.getJobId());
    }
}
