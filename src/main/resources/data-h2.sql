create table "store"
(
    "key"   varchar(255) not null,
    "value" varchar(255),
    primary key ("key")
);

create table "task"
(
    "id"              binary              not null,
    "create_date"     timestamp           not null,
    "update_date"     timestamp           not null,
    "async"           boolean             not null,
    "code"            varchar(2147483647) not null,
    "cron_expression" varchar(255),
    "description"     varchar(255)        not null,
    "enabled"         boolean             not null,
    "interval"        bigint,
    "name"            varchar(255)        not null,
    primary key ("id")
);

create table "task_log"
(
    "id"          binary              not null,
    "create_date" timestamp           not null,
    "update_date" timestamp           not null,
    "level"       integer             not null,
    "task_id"     binary,
    "text"        varchar(2147483647) not null,
    primary key ("id")
);