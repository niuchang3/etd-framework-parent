create table drools_rule
(
   ID                   varchar(32) not null comment '主键ID',
   KIE_BASE_NAME        varchar(50) comment 'KIE_BASE_NAME',
   KIE_PACKAGE_NAME     varchar(200) comment 'KIE_PACKAGE_NAME',
   RULE_CONTENT         text comment 'RULE_CONTENT',
   DESCRIPTION          varchar(500) comment 'DESCRIPTION',
   CREATED_TIME         datetime comment 'CREATED_TIME',
   primary key (ID)
);

alter table drools_rule comment '规则表';


create table drools_rule_names
(
   ID                   varchar(32) not null comment 'ID',
   DROOLS_RULE_ID       varchar(32) comment 'DROOLS_RULE_ID',
   RULE_NAME            varchar(200) comment 'RULE_NAME',
   primary key (ID)
);

alter table drools_rule_names comment '规则名称表';
