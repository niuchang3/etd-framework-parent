package org.etd.framework.demo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * (ClasssDTO)实体类
 *
 * @author 牛昌
 * @since 2022-07-28 17:10:41
 */
@Data
public class ClasssDTO implements Serializable {
    private static final long serialVersionUID = 529647567868299459L;

    private Integer id;

    private String name;

    private List<StudentDTO> studentDTOList;
}

