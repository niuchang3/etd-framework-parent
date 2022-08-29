package org.etd.framework.demo.dto;

import java.io.Serializable;

/**
 * (StudentDTO)实体类
 *
 * @author 牛昌
 * @since 2022-07-28 17:10:41
 */
public class StudentDTO implements Serializable {
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


    public static void main(String[] args) {
        String sys_user_role = ToHumpUtil.toHump("sys_user_role-r_niu", false);
        System.out.println(sys_user_role);
        String s = ToHumpUtil.upperCaseFirstOne(sys_user_role);
        System.out.println(s);
    }


}

