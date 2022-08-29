package org.etd.framework.demo.entity;

import java.io.Serializable;

/**
 * (StudentDTO)实体类
 *
 * @author 牛昌
 * @since 2022-07-28 17:10:41
 */
public class Student implements Serializable {
    private static final long serialVersionUID = -70185151864571000L;
    
    private Long id;
    
    private String name;
    
    private Long classId;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

}

