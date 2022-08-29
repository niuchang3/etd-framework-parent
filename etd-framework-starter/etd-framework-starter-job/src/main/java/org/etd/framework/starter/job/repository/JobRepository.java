package org.etd.framework.starter.job.repository;


/**
 * Job增删改查
 */
public interface JobRepository<T> {

    void save(T job);

    void delete(T job);

    void update(T job);

    void select(T job);
}
