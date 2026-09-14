/*==============================================================*/
/* table: evt_event_type                                        */
/*==============================================================*/
create table evt_event_type
(
    id                 bigint not null,
    create_time        timestamp(6) with time zone not null default current_timestamp,
    create_by          bigint,
    update_time        timestamp(6) with time zone not null default current_timestamp,
    update_by          bigint,
    version            integer not null default 0,
    data_status        integer not null default 1,
    del_flag           smallint not null default 0,
    event_type         varchar(150) not null,
    event_name         varchar(150) not null,
    source_application varchar(100) not null,
    latest_version     integer not null default 1,
    description        varchar(500),
    primary key (id)
);

comment on table evt_event_type is '事件类型定义表';
comment on column evt_event_type.id is '主键ID';
comment on column evt_event_type.create_time is '创建时间';
comment on column evt_event_type.create_by is '创建人';
comment on column evt_event_type.update_time is '修改时间';
comment on column evt_event_type.update_by is '修改人';
comment on column evt_event_type.version is '数据库记录乐观锁版本';
comment on column evt_event_type.data_status is '数据状态：0禁用，1启用';
comment on column evt_event_type.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column evt_event_type.event_type is '全局唯一事件类型，例如 upms.user.created';
comment on column evt_event_type.event_name is '事件类型显示名称';
comment on column evt_event_type.source_application is '事件来源应用';
comment on column evt_event_type.latest_version is '当前最新事件协议版本';
comment on column evt_event_type.description is '事件类型说明';

create unique index uk_evt_event_type
    on evt_event_type (event_type);

create index idx_evt_event_type_source_status
    on evt_event_type (source_application, data_status, update_time desc, id desc)
    where del_flag = 0;

/*==============================================================*/
/* tables: evt_event_message_0 ... evt_event_message_1           */
/*==============================================================*/
do $block$
declare
    shard_index integer;
    shard_suffix text;
begin
    for shard_index in 0..1 loop
        shard_suffix := shard_index::text;
        execute format($ddl$
            create table evt_event_message_%1$s
            (
                id                 bigint not null,
                create_time        timestamp(6) with time zone not null default current_timestamp,
                update_time        timestamp(6) with time zone not null default current_timestamp,
                version            integer not null default 0,
                del_flag           smallint not null default 0,
                event_id           varchar(100) not null,
                event_type_id      bigint not null references evt_event_type (id),
                event_version      integer not null,
                occurred_at        timestamp(6) with time zone not null,
                source_application varchar(100) not null,
                partition_key      varchar(250),
                event_context      jsonb not null default '{}'::jsonb,
                event_payload      jsonb not null,
                primary key (id)
            );
            comment on table evt_event_message_%1$s is '事件中心原始消息物理分表 %1$s';
            create unique index uk_evt_event_message_%1$s_event_id
                on evt_event_message_%1$s (event_id);
            create index idx_evt_event_message_%1$s_type_time
                on evt_event_message_%1$s (event_type_id, create_time desc, id desc)
                where del_flag = 0;
            create index idx_evt_event_message_%1$s_source_time
                on evt_event_message_%1$s (source_application, create_time desc, id desc)
                where del_flag = 0;
        $ddl$, shard_suffix);
    end loop;
end
$block$;

/*==============================================================*/
/* table: evt_event_subscription                                */
/*==============================================================*/
create table evt_event_subscription
(
    id                     bigint not null,
    create_time            timestamp(6) with time zone not null default current_timestamp,
    create_by              bigint,
    update_time            timestamp(6) with time zone not null default current_timestamp,
    update_by              bigint,
    version                integer not null default 0,
    data_status            integer not null default 1,
    del_flag               smallint not null default 0,
    subscription_code      varchar(100) not null,
    subscription_name      varchar(150) not null,
    event_type_id          bigint not null references evt_event_type (id),
    subscriber_application varchar(100) not null,
    target_topic           varchar(250) not null,
    consumer_group         varchar(150) not null,
    description            varchar(500),
    primary key (id)
);

comment on table evt_event_subscription is '事件业务订阅配置表';
comment on column evt_event_subscription.id is '主键ID';
comment on column evt_event_subscription.create_time is '创建时间';
comment on column evt_event_subscription.create_by is '创建人';
comment on column evt_event_subscription.update_time is '修改时间';
comment on column evt_event_subscription.update_by is '修改人';
comment on column evt_event_subscription.version is '数据库记录乐观锁版本';
comment on column evt_event_subscription.data_status is '订阅状态：0禁用，1启用';
comment on column evt_event_subscription.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column evt_event_subscription.subscription_code is '全局唯一且稳定的订阅编码';
comment on column evt_event_subscription.subscription_name is '订阅显示名称';
comment on column evt_event_subscription.event_type_id is '订阅的事件类型主键';
comment on column evt_event_subscription.subscriber_application is '订阅事件的业务应用';
comment on column evt_event_subscription.target_topic is '业务应用独立的 Kafka 接收 Topic';
comment on column evt_event_subscription.consumer_group is '业务客户端约定使用的 Kafka 消费组';
comment on column evt_event_subscription.description is '订阅用途说明';

create unique index uk_evt_event_subscription_code
    on evt_event_subscription (subscription_code);

create unique index uk_evt_event_subscription_application_type
    on evt_event_subscription (subscriber_application, event_type_id)
    where del_flag = 0;

create index idx_evt_event_subscription_match
    on evt_event_subscription (event_type_id, data_status, update_time desc, id desc)
    where del_flag = 0;

create index idx_evt_event_subscription_application
    on evt_event_subscription (subscriber_application, data_status, update_time desc, id desc)
    where del_flag = 0;

/*==============================================================*/
/* tables: evt_event_delivery_0 ... evt_event_delivery_1         */
/*==============================================================*/
do $block$
declare
    shard_index integer;
    shard_suffix text;
begin
    for shard_index in 0..1 loop
        shard_suffix := shard_index::text;
        execute format($ddl$
            create table evt_event_delivery_%1$s
            (
                id               bigint not null,
                create_time      timestamp(6) with time zone not null default current_timestamp,
                update_time      timestamp(6) with time zone not null default current_timestamp,
                version          integer not null default 0,
                del_flag         smallint not null default 0,
                event_id         varchar(100) not null,
                event_message_id bigint not null,
                subscription_id  bigint not null references evt_event_subscription (id),
                target_topic     varchar(250) not null,
                delivery_status  smallint not null default 0,
                attempt_count    integer not null default 0,
                next_retry_at    timestamp(6) with time zone not null default current_timestamp,
                last_error       varchar(2000),
                kafka_partition  integer,
                kafka_offset     bigint,
                published_at     timestamp(6) with time zone,
                primary key (id)
            );
            comment on table evt_event_delivery_%1$s is '事件订阅投递任务物理分表 %1$s';
            create unique index uk_evt_event_delivery_%1$s_message_subscription
                on evt_event_delivery_%1$s (event_id, event_message_id, subscription_id);
            create index idx_evt_event_delivery_%1$s_pending
                on evt_event_delivery_%1$s (next_retry_at, id)
                where del_flag = 0 and delivery_status in (0, 3);
            create index idx_evt_event_delivery_%1$s_processing
                on evt_event_delivery_%1$s (update_time, id)
                where del_flag = 0 and delivery_status = 1;
            create index idx_evt_event_delivery_%1$s_message
                on evt_event_delivery_%1$s (event_message_id, id)
                where del_flag = 0;
            create index idx_evt_event_delivery_%1$s_event
                on evt_event_delivery_%1$s (event_id, id)
                where del_flag = 0;
            create index idx_evt_event_delivery_%1$s_status_time
                on evt_event_delivery_%1$s (delivery_status, create_time desc, id desc)
                where del_flag = 0;
            create index idx_evt_event_delivery_%1$s_subscription_time
                on evt_event_delivery_%1$s (subscription_id, create_time desc, id desc)
                where del_flag = 0;
        $ddl$, shard_suffix);
    end loop;
end
$block$;
