package org.etd.upms.organization.controller.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SystemOrganizationVO {

    private Long id;

    private Date createTime;

    private Date updateTime;

    private Long parentId;

    private String parentIdPath;

    private String orgCode;

    private String orgName;

    private String orgType;

    private Long leaderUserId;

    private Integer sort;

    private Boolean enabled;

    private List<SystemOrganizationVO> children = new ArrayList<>();
}
