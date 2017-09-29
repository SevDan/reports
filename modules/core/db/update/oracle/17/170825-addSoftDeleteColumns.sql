alter table REPORT_REPORT add DELETE_TS timestamp^
alter table REPORT_REPORT add DELETED_BY varchar2(50)^

alter table REPORT_GROUP add DELETE_TS timestamp^
alter table REPORT_GROUP add DELETED_BY varchar2(50)^

alter table REPORT_TEMPLATE add DELETE_TS timestamp^
alter table REPORT_TEMPLATE add DELETED_BY varchar2(50)^

drop index IDX_REPORT_REPORT_UNIQ_NAME^
create unique index IDX_REPORT_REPORT_UNIQ_NAME on REPORT_REPORT (NAME, DELETE_TS)^

drop index IDX_REPORT_GROUP_UNIQ_TITLE^
create unique index IDX_REPORT_GROUP_UNIQ_TITLE on REPORT_GROUP (TITLE, DELETE_TS)^
