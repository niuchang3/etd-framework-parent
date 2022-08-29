package org.etd.framework.demo.entity;

import java.io.Serializable;

/**
 * (ClasssDTO)实体类
 *
 * @author 牛昌
 * @since 2022-07-28 17:10:41
 */
public class Classs implements Serializable {
    private static final long serialVersionUID = 529647567868299459L;
    
    private Integer id;
    
    private String name;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

