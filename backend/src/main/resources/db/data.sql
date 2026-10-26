-- 初始化数据：平台/品类插件/字典/租户/演示业务数据
SET NAMES utf8mb4;
USE `meiye`;

INSERT INTO `sys_tenant` (`id`,`code`,`name`,`contact`,`phone`,`store_limit`,`customer_limit`,`technician_limit`,`status`,`open_at`,`expire_at`,`remark`) VALUES
(1,'5MY','五月美业连锁','李经理','13800000001',5,5000,100,'NORMAL',DATE_SUB(NOW(), INTERVAL 120 DAY),DATE_ADD(NOW(), INTERVAL 245 DAY),'演示租户-美业'),
(2,'2HL','华丽口腔门诊','王院长','13800000002',2,2000,40,'TRIAL',DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_ADD(NOW(), INTERVAL 20 DAY),'演示租户-医疗类目')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `sys_platform_user` (`id`,`username`,`password`,`name`,`role`) VALUES
(1,'admin','$2y$10$duFwUvh25HtT/fHSQTA4COT03ekW0E1lHZyEoJ7JrdNAaM7RlIWC6','平台超级管理员','SYSTEM'),
(2,'operator','$2y$10$duFwUvh25HtT/fHSQTA4COT03ekW0E1lHZyEoJ7JrdNAaM7RlIWC6','平台运营','OPERATOR')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `biz_type_plugin` (`id`,`code`,`name`,`icon`,`default_enabled`,`medical`,`sub_modules`,`field_template`,`default_commission`,`sort`) VALUES
(1,'BEAUTY','美容','💆',1,0,'["inventory"]','{"positions":["美容师","美容顾问"],"workstationTypes":["BED","ROOM"],"itemCategories":["面部护理","身体护理"],"slotMinutes":30}','{"baseRate":10,"levels":[{"min":0,"rate":8},{"min":5000,"rate":10},{"min":10000,"rate":12}]}',10),
(2,'HAIR','美发','✂️',1,0,'["inventory"]','{"positions":["发型师","洗发师"],"workstationTypes":["SEAT"],"itemCategories":["剪发","烫染","护理"],"slotMinutes":30}','{"baseRate":15,"levels":[{"min":0,"rate":12},{"min":8000,"rate":15}]}',20),
(3,'NAIL','美甲美睫','💅',0,0,'["inventory"]','{"positions":["美甲师","美睫师"],"workstationTypes":["SEAT"],"itemCategories":["美甲","美睫"],"slotMinutes":30}','{"baseRate":20}',30),
(4,'SKIN','皮肤管理','✨',1,0,'["inventory"]','{"positions":["皮肤管理师"],"workstationTypes":["BED","ROOM"],"itemCategories":["清洁","抗衰","补水"],"slotMinutes":45}','{"baseRate":12}',40),
(5,'SPA','SPA','🌿',1,0,'["inventory"]','{"positions":["SPA理疗师"],"workstationTypes":["ROOM"],"itemCategories":["精油SPA","身体舒缓"],"slotMinutes":60}','{"baseRate":15}',50),
(6,'FOOT','足浴推拿','🦶',0,0,'["inventory"]','{"positions":["足疗师","推拿师"],"workstationTypes":["BED","ROOM"],"itemCategories":["足疗","推拿"],"slotMinutes":60}','{"baseRate":18}',60),
(7,'TCM','中医养生','🌱',0,0,'["inventory"]','{"positions":["中医理疗师"],"workstationTypes":["BED"],"itemCategories":["艾灸","拔罐","经络"],"slotMinutes":60}','{"baseRate":15}',70),
(8,'DENTAL','口腔','🦷',0,1,'["inventory"]','{"positions":["口腔医生","牙科护士"],"workstationTypes":["ROOM"],"itemCategories":["洁牙","修复","种植"],"slotMinutes":30}','{"baseRate":10}',80),
(9,'CHECKUP','体检','🩺',0,1,'[]','{"positions":["体检医师"],"workstationTypes":["ROOM"],"itemCategories":["常规体检","专项体检"],"slotMinutes":30}','{"baseRate":5}',90),
(10,'MEDBEAUTY','医美轻护理','💉',0,1,'["inventory"]','{"positions":["医美医师","护理师"],"workstationTypes":["ROOM"],"itemCategories":["光电","注射","皮肤修复"],"slotMinutes":30}','{"baseRate":8}',100),
(11,'YOGA','瑜伽健身','🧘',0,0,'[]','{"positions":["瑜伽教练"],"workstationTypes":["ROOM"],"itemCategories":["团课","私教"],"slotMinutes":60}','{"baseRate":25}',110)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `sys_global_dict` (`id`,`dict_type`,`label`,`value`,`sort`) VALUES
(1,'item_category','面部护理','FACE',1),(2,'item_category','身体护理','BODY',2),(3,'item_category','剪发','HAIRCUT',3),(4,'item_category','烫染','PERM',4),(5,'item_category','美甲','NAIL',5),(6,'item_category','精油SPA','SPA',6),(7,'item_category','足疗','FOOT',7),
(8,'technician_position','美容师','BEAUTICIAN',1),(9,'technician_position','发型师','HAIRDRESSER',2),(10,'technician_position','美甲师','NAILIST',3),(11,'technician_position','皮肤管理师','SKIN_MANAGER',4),(12,'technician_position','SPA理疗师','SPA_THERAPIST',5),(13,'technician_position','口腔医生','DENTIST',6),
(14,'workstation_type','床位','BED',1),(15,'workstation_type','房间','ROOM',2),(16,'workstation_type','座位','SEAT',3),
(17,'member_level','普通会员','NORMAL',1),(18,'member_level','银卡会员','SILVER',2),(19,'member_level','金卡会员','GOLD',3),(20,'member_level','黑金会员','BLACK',4),
(21,'order_status','已预约','PENDING',1),(22,'order_status','待核销','PAID',2),(23,'order_status','已核销','VERIFIED',3),(24,'order_status','已完成','DONE',4),(25,'order_status','售后','AFTER_SALE',5),
(26,'settlement_status','草稿','DRAFT',1),(27,'settlement_status','已确认','CONFIRMED',2),(28,'settlement_status','已打款','PAID',3),
(29,'review_status','已发布','PUBLISHED',1),(30,'review_status','已隐藏','HIDDEN',2)
ON DUPLICATE KEY UPDATE label=VALUES(label);

INSERT INTO `platform_param` (`id`,`param_key`,`param_value`,`name`,`remark`) VALUES
(1,'cert_warn_days','30','证书到期预警天数','提前N天预警'),
(2,'card_warn_days','15','会员卡到期预警天数','提前N天预警'),
(3,'appointment_timeout_minutes','30','预约超时释放分钟数','超时未到店自动释放'),
(4,'platform_commission_rate','5','平台默认抽成比例(%)','结算分账使用'),
(5,'withdrawal_fee_rate','1','提现手续费率(%)','技师提现'),
(6,'tenant_expire_strategy','AUTO_DISABLE','租户到期策略','AUTO_DISABLE/MANUAL'),
(7,'slot_minutes','30','默认预约时段粒度(分钟)','品类未配置时使用'),
(8,'points_per_yuan','1','消费1元获得积分','会员积分')
ON DUPLICATE KEY UPDATE param_value=VALUES(param_value);

INSERT INTO `sys_language` (`id`,`code`,`name`,`enabled`,`is_default`,`sort`) VALUES
(1,'zh-CN','简体中文',1,1,1),(2,'en-US','English',1,0,2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `sys_role` (`id`,`tenant_id`,`code`,`name`,`permissions`,`platform`) VALUES
(1,0,'SYSTEM','系统超级管理员','["*"]',1),
(2,0,'OPERATOR','平台运营','["platform:*"]',1),
(3,0,'TENANT_ADMIN','租户管理员','["tenant:*"]',0),
(4,0,'STORE_MANAGER','门店店长','["store","customer","technician","service","appointment","order","card","report"]',0),
(5,0,'CASHIER','前台收银','["customer","appointment","order","card"]',0),
(6,0,'TECHNICIAN','技师','["worker"]',0),
(7,0,'MARKETING','市场运营','["marketing","customer","report"]',0),
(8,0,'FINANCE','财务','["settlement","order","report"]',0)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `tenant_biz_type` (`tenant_id`,`biz_type`,`enabled`,`enabled_at`,`inventory_enabled`) VALUES
(1,'BEAUTY',1,DATE_SUB(NOW(), INTERVAL 100 DAY),1),
(1,'HAIR',1,DATE_SUB(NOW(), INTERVAL 100 DAY),1),
(1,'SKIN',1,DATE_SUB(NOW(), INTERVAL 60 DAY),1),
(1,'SPA',1,DATE_SUB(NOW(), INTERVAL 60 DAY),0),
(2,'DENTAL',1,DATE_SUB(NOW(), INTERVAL 10 DAY),1),
(2,'MEDBEAUTY',1,DATE_SUB(NOW(), INTERVAL 5 DAY),0)
ON DUPLICATE KEY UPDATE enabled=VALUES(enabled);

INSERT INTO `tenant_config` (`tenant_id`,`name`,`logo`,`service_phone`,`config_json`) VALUES
(1,'五月美业连锁','/static/logo-5my.png','400-800-1234','{"wechatPay":true,"notifySms":false,"commissionRate":5,"crossStore":true,"slogan":"让每一次护理都值得期待"}'),
(2,'华丽口腔门诊','/static/logo-2hl.png','400-800-5678','{"wechatPay":true,"notifySms":true,"commissionRate":5,"crossStore":false,"slogan":"专业口腔健康管理"}')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `tenant_dict` (`tenant_id`,`dict_type`,`label`,`value`,`sort`) VALUES
(1,'customer_tag','高净值','VIP',1),(1,'customer_tag','价格敏感','PRICE',2),(1,'customer_tag','沉睡客户','SLEEP',3),
(1,'item_category','头皮管理','SCALP',10)
ON DUPLICATE KEY UPDATE label=VALUES(label);

INSERT INTO `sys_tenant_user` (`id`,`tenant_id`,`username`,`password`,`name`,`phone`,`role_code`,`store_id`) VALUES
(1,1,'admin','$2y$10$duFwUvh25HtT/fHSQTA4COT03ekW0E1lHZyEoJ7JrdNAaM7RlIWC6','五月管理员','13800000001','TENANT_ADMIN',1),
(2,1,'manager','$2y$10$duFwUvh25HtT/fHSQTA4COT03ekW0E1lHZyEoJ7JrdNAaM7RlIWC6','张店长','13800000011','STORE_MANAGER',1),
(3,1,'cashier','$2y$10$duFwUvh25HtT/fHSQTA4COT03ekW0E1lHZyEoJ7JrdNAaM7RlIWC6','王收银','13800000012','CASHIER',1),
(4,2,'admin','$2y$10$duFwUvh25HtT/fHSQTA4COT03ekW0E1lHZyEoJ7JrdNAaM7RlIWC6','口腔管理员','13800000002','TENANT_ADMIN',4)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `t_store` (`id`,`tenant_id`,`code`,`name`,`phone`,`address`,`service_scope`,`manager`,`biz_types`,`allow_cross_store`,`allow_share_profit`,`description`) VALUES
(1,1,'S001','五月美业·国贸店','010-88880001','北京市朝阳区建国路88号','面部护理,美发,SPA','张店长','BEAUTY,HAIR,SKIN,SPA',1,0,'旗舰店'),
(2,1,'S002','五月美业·望京店','010-88880002','北京市朝阳区望京SOHO T2','皮肤管理,美甲美睫','刘店长','BEAUTY,SKIN',1,0,'社区店'),
(3,1,'S003','五月美业·三里屯店','010-88880003','北京市朝阳区三里屯太古里','美发,SPA','赵店长','HAIR,SPA',1,1,'潮流店'),
(4,2,'S101','华丽口腔·总院','010-99990001','北京市海淀区中关村大街1号','口腔,医美轻护理','王院长','DENTAL,MEDBEAUTY',0,0,'口腔专科')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `t_workstation` (`id`,`tenant_id`,`store_id`,`name`,`biz_type`,`type`,`status`) VALUES
(1,1,1,'A01美容床','BEAUTY','BED','FREE'),(2,1,1,'A02美容床','BEAUTY','BED','FREE'),(3,1,1,'VIP1护理房','SKIN','ROOM','FREE'),
(4,1,1,'美发1号位','HAIR','SEAT','FREE'),(5,1,1,'SPA1号房','SPA','ROOM','FREE'),
(6,1,2,'B01美容床','BEAUTY','BED','FREE'),(7,1,3,'C01美发位','HAIR','SEAT','FREE'),
(8,2,4,'口腔1诊室','DENTAL','ROOM','FREE')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `t_technician` (`id`,`tenant_id`,`tech_no`,`name`,`phone`,`id_card`,`password`,`store_id`,`position`,`skills`,`cert_status`,`rotation_seq`,`rating`,`service_count`,`total_revenue`,`avatar`,`entry_at`,`bio`) VALUES
(1,1,'5MY-TECH-2026-0001','林小雅','13900000001','110101199203011234','$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',1,'美容师','BEAUTY,SKIN','OK',1,4.90,128,38600,'/static/tech-1.png','2023-03-01','资深美容师，擅长面部抗衰'),
(2,1,'5MY-TECH-2026-0002','陈美琪','13900000002','110101199505052345','$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',1,'皮肤管理师','SKIN,SPA','WARN',2,4.80,96,29800,'/static/tech-2.png','2023-06-15','皮肤管理专家'),
(3,1,'5MY-TECH-2026-0003','周托尼','13900000003','110101199001013456','$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',3,'发型师','HAIR','OK',1,4.70,210,56200,'/static/tech-3.png','2022-08-01','擅长日系剪裁'),
(4,2,'2HL-TECH-2026-0001','赵医生','13900000004','110101198802024567','$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',4,'口腔医生','DENTAL,MEDBEAUTY','OK',1,4.95,88,128000,'/static/tech-4.png','2021-01-10','口腔医学硕士')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `t_technician_cert` (`tenant_id`,`technician_id`,`cert_type`,`cert_no`,`issuer`,`expire_at`,`status`) VALUES
(1,1,'健康证','JK2026001','北京市疾控中心',DATE_ADD(CURDATE(), INTERVAL 120 DAY),'VALID'),
(1,2,'健康证','JK2026002','北京市疾控中心',DATE_ADD(CURDATE(), INTERVAL 12 DAY),'VALID'),
(1,3,'美容师资格证','MR2023003','人社部',DATE_ADD(CURDATE(), INTERVAL 400 DAY),'VALID'),
(1,2,'美容师资格证','MR2022008','人社部',DATE_SUB(CURDATE(), INTERVAL 10 DAY),'EXPIRED'),
(2,4,'医师执业证','YL2020004','卫健委',DATE_ADD(CURDATE(), INTERVAL 800 DAY),'VALID');

INSERT INTO `t_customer` (`id`,`tenant_id`,`customer_no`,`name`,`phone`,`id_card`,`password`,`store_id`,`level`,`growth`,`points`,`tags`,`follower_id`,`debt`,`total_consume`,`total_recharge`,`gender`,`remark`,`last_visit_at`) VALUES
(1,1,'5MY-CUS-2026-0001','苏婉晴','13700000001','110101199408081234','$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',1,'金卡会员',3200,860,'高净值,注重服务',2,0,16800,20000,'女','敏感肌，偏好温和护理',DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2,1,'5MY-CUS-2026-0002','顾清欢','13700000002','110101199610101234','$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',1,'银卡会员',1200,320,'价格敏感',3,680,7600,8000,'女','偏好周末下午',DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3,1,'5MY-CUS-2026-0003','陆时衍','13700000003','110101199201011234','$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',3,'黑金会员',8600,2100,'高净值,商务',2,0,49800,60000,'男','企业客户',DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4,1,'5MY-CUS-2026-0004','夏知微','13700000004','110101199801011234','$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',2,'普通会员',180,20,'沉睡客户',3,0,980,1000,'女','待回访',DATE_SUB(NOW(), INTERVAL 60 DAY)),
(5,2,'2HL-CUS-2026-0001','沈亦白','13700000005',NULL,'$2y$10$EDeAWwHfy6yBv6e8l5VFX.mXy49WvdZEspDwKxoin2L36bFbEmdv6',4,'普通会员',100,10,NULL,NULL,0,1200,0,'男','洁牙客户',DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `t_service_item` (`id`,`tenant_id`,`biz_type`,`name`,`category`,`duration`,`price`,`member_price`,`commission_rate`,`store_ids`,`image`,`description`,`ext_json`) VALUES
(1,1,'BEAUTY','深层补水面部护理','FACE',60,398,328,10,'1,2','/static/item-1.jpg','清洁+补水+提亮，适合干燥缺水肌','{"skin":"干燥"}'),
(2,1,'SKIN','小气泡深层清洁','CLEAN',45,298,238,12,'1,2','/static/item-2.jpg','深层清洁毛孔，改善黑头','{"skin":"油性"}'),
(3,1,'BEAUTY','肩颈舒缓理疗','BODY',60,268,218,10,'1,3','/static/item-3.jpg','专业手法放松肩颈','{}'),
(4,1,'HAIR','日系剪发造型','HAIRCUT',60,268,228,15,'1,3','/static/item-4.jpg','含洗吹造型','{"length":"中长发"}'),
(5,1,'SPA','精油全身SPA','SPA',90,598,498,15,'1,3','/static/item-5.jpg','进口精油，深度放松','{}'),
(6,1,'SKIN','抗衰紧致护理','ANTI_AGE',75,698,598,12,'1','/static/item-6.jpg','射频+精华导入','{}'),
(7,2,'DENTAL','超声波洁牙','CLEANING',45,299,259,8,'4','/static/item-7.jpg','全口洁牙+抛光','{}'),
(8,2,'MEDBEAUTY','光子嫩肤','PHOTON',30,1200,999,6,'4','/static/item-8.jpg','改善肤色不均','{}')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `t_service_package` (`tenant_id`,`biz_type`,`name`,`type`,`price`,`face_value`,`times`,`valid_days`,`items_json`,`description`) VALUES
(1,'BEAUTY','补水季卡·6次','COUNT',1980,2388,6,180,'[{"itemId":1,"times":6}]','春季补水优选'),
(1,'SKIN','小气泡年卡','COUNT',2980,3576,12,365,'[{"itemId":2,"times":12}]','全年深层清洁'),
(1,'BEAUTY','闺蜜组合礼包','COMBO',3680,4176,10,365,'[{"itemId":1,"times":5},{"itemId":3,"times":5}]','双人共享'),
(2,'DENTAL','家庭口腔护理卡','GENERAL',999,1299,3,365,'[{"itemId":7,"times":3}]','一家三口洁牙');

INSERT INTO `t_service_flow_template` (`tenant_id`,`biz_type`,`item_id`,`name`,`steps_json`) VALUES
(1,'BEAUTY',1,'面部护理标准流程','[{"step":1,"title":"皮肤检测","desc":"使用皮肤检测仪记录状态","minutes":5},{"step":2,"title":"深层清洁","desc":"卸妆洁面，热喷打开毛孔","minutes":10},{"step":3,"title":"精华导入","desc":"根据肤质选择精华","minutes":20},{"step":4,"title":"面膜舒缓","desc":"敷面膜并头部放松","minutes":20},{"step":5,"title":"护肤收尾","desc":"涂抹水乳防晒","minutes":5}]'),
(1,'HAIR',4,'剪发服务流程','[{"step":1,"title":"沟通需求","minutes":5},{"step":2,"title":"洗发","minutes":15},{"step":3,"title":"剪裁","minutes":30},{"step":4,"title":"吹风造型","minutes":10}]');

INSERT INTO `t_schedule` (`tenant_id`,`store_id`,`technician_id`,`work_date`,`start_time`,`end_time`,`status`,`biz_type`) VALUES
(1,1,1,CURDATE(),'09:00:00','18:00:00','WORK','BEAUTY'),
(1,1,2,CURDATE(),'10:00:00','19:00:00','WORK','SKIN'),
(1,3,3,CURDATE(),'10:00:00','20:00:00','WORK','HAIR'),
(1,1,1,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','18:00:00','WORK','BEAUTY'),
(1,1,2,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'10:00:00','19:00:00','WORK','SKIN'),
(1,3,3,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'10:00:00','20:00:00','WORK','HAIR'),
(1,1,1,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','18:00:00','WORK','BEAUTY'),
(1,1,2,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'10:00:00','19:00:00','LEAVE','SKIN');

INSERT INTO `t_tech_rotation` (`tenant_id`,`store_id`,`technician_id`,`biz_type`,`rotation_date`,`seq`,`status`) VALUES
(1,1,1,'BEAUTY',CURDATE(),1,'WAITING'),
(1,1,2,'SKIN',CURDATE(),2,'WAITING'),
(1,3,3,'HAIR',CURDATE(),1,'WAITING');

INSERT INTO `t_appointment` (`id`,`tenant_id`,`appointment_no`,`biz_type`,`customer_id`,`store_id`,`technician_id`,`item_id`,`workstation_id`,`start_time`,`end_time`,`status`,`source`,`amount`,`remark`,`created_by`) VALUES
(1,1,'5MY-APT-2026-0001','BEAUTY',1,1,1,1,1,DATE_ADD(CURDATE(), INTERVAL 10 HOUR),DATE_ADD(CURDATE(), INTERVAL 11 HOUR),'CONFIRMED','MINI',328,'希望手法轻柔',3),
(2,1,'5MY-APT-2026-0002','SKIN',2,1,2,2,3,DATE_ADD(CURDATE(), INTERVAL 14 HOUR),DATE_ADD(CURDATE(), INTERVAL 14.75 HOUR),'PENDING','STAFF',238,'',2),
(3,1,'5MY-APT-2026-0003','HAIR',3,3,3,4,7,DATE_ADD(CURDATE(), INTERVAL 16 HOUR),DATE_ADD(CURDATE(), INTERVAL 17 HOUR),'ARRIVED','H5',228,'',3),
(4,1,'5MY-APT-2026-0004','SPA',1,3,3,5,5,DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 10 HOUR,DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 11.5 HOUR,'PENDING','WECHAT',498,'',3),
(5,2,'2HL-APT-2026-0001','DENTAL',5,4,4,7,8,DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 9 HOUR,DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 9.75 HOUR,'CONFIRMED','MINI',259,'',4)
ON DUPLICATE KEY UPDATE remark=VALUES(remark);

INSERT INTO `t_card_type` (`id`,`tenant_id`,`biz_type`,`name`,`type`,`price`,`face_value`,`times`,`valid_days`,`bonus`,`discount`,`commission_rate`,`description`) VALUES
(1,1,'BEAUTY','储值卡·充5000送800','STORED',5000,5800,0,365,800,90,5,'储值消费享9折'),
(2,1,'BEAUTY','补水护理10次卡','COUNT',2980,3980,10,365,0,95,10,'单次298'),
(3,1,'SKIN','小气泡季卡','TIME',1280,1780,0,90,0,90,12,'90天内不限次'),
(4,1,'HAIR','剪发年卡','COUNT',1980,2680,12,365,0,100,15,'每月一次'),
(5,1,'BEAUTY','亲子附属卡','SUB',999,999,5,180,0,95,5,'共享主卡'),
(6,2,'DENTAL','口腔护理储值卡','STORED',3000,3300,0,365,300,95,5,'洁牙9折')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `t_member_card` (`id`,`tenant_id`,`card_no`,`customer_id`,`card_type_id`,`type`,`name`,`biz_type`,`store_id`,`balance`,`times`,`total_times`,`expire_at`,`status`,`discount`) VALUES
(1,1,'5MY-CARD-2026-0001',1,1,'STORED','储值卡·充5000送800','BEAUTY',1,4200,0,0,DATE_ADD(NOW(), INTERVAL 300 DAY),'ACTIVE',90),
(2,1,'5MY-CARD-2026-0002',1,2,'COUNT','补水护理10次卡','BEAUTY',1,0,6,10,DATE_ADD(NOW(), INTERVAL 200 DAY),'ACTIVE',95),
(3,1,'5MY-CARD-2026-0003',2,3,'TIME','小气泡季卡','SKIN',1,0,0,0,DATE_ADD(NOW(), INTERVAL 10 DAY),'ACTIVE',90),
(4,1,'5MY-CARD-2026-0004',3,1,'STORED','储值卡·充5000送800','BEAUTY',3,9800,0,0,DATE_ADD(NOW(), INTERVAL 330 DAY),'ACTIVE',90),
(5,2,'2HL-CARD-2026-0001',5,6,'STORED','口腔护理储值卡','DENTAL',4,2800,0,0,DATE_ADD(NOW(), INTERVAL 350 DAY),'ACTIVE',95)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO `t_card_flow` (`tenant_id`,`card_id`,`customer_id`,`type`,`amount`,`times`,`balance_after`,`times_after`,`remark`,`operator`) VALUES
(1,1,1,'OPEN',5000,0,5000,0,'开卡充值','王收银'),
(1,1,1,'CONSUME',398,0,4602,0,'深层补水面部护理',NULL),
(1,1,1,'CONSUME',398,0,4204,0,'深层补水面部护理',NULL),
(1,2,1,'OPEN',2980,10,0,10,'开卡','王收银'),
(1,2,1,'CONSUME',0,-2,0,8,'护理核销',NULL),
(1,2,1,'CONSUME',0,-2,0,6,'护理核销',NULL);

INSERT INTO `t_order` (`id`,`tenant_id`,`order_no`,`biz_type`,`customer_id`,`store_id`,`technician_id`,`appointment_id`,`total_amount`,`discount_amount`,`paid_amount`,`pay_type`,`status`,`cashier_id`,`paid_at`,`remark`) VALUES
(1,1,'5MY-ORD-2026-0001','BEAUTY',1,1,1,1,398,70,328,'BALANCE','DONE',3,DATE_SUB(NOW(), INTERVAL 3 DAY),'会员价'),
(2,1,'5MY-ORD-2026-0002','SKIN',2,1,2,2,298,60,238,'WECHAT','PAID',3,DATE_SUB(NOW(), INTERVAL 1 DAY),''),
(3,1,'5MY-ORD-2026-0003','HAIR',3,3,3,3,268,40,228,'ALIPAY','VERIFIED',3,NOW(),''),
(4,1,'5MY-ORD-2026-0004','SPA',1,3,3,NULL,598,100,498,'CASH','DONE',3,DATE_SUB(NOW(), INTERVAL 5 DAY),''),
(5,2,'2HL-ORD-2026-0001','DENTAL',5,4,4,NULL,299,40,259,'WECHAT','DONE',4,DATE_SUB(NOW(), INTERVAL 5 DAY),'')
ON DUPLICATE KEY UPDATE paid_amount=VALUES(paid_amount);

INSERT INTO `t_order_item` (`tenant_id`,`order_id`,`item_id`,`name`,`price`,`quantity`,`amount`,`technician_id`,`commission_rate`,`commission_amount`) VALUES
(1,1,1,'深层补水面部护理',398,1,398,1,10,39.80),
(1,2,2,'小气泡深层清洁',298,1,298,2,12,35.76),
(1,3,4,'日系剪发造型',268,1,268,3,15,40.20),
(1,4,5,'精油全身SPA',598,1,598,3,15,89.70),
(2,5,7,'超声波洁牙',299,1,299,4,8,23.92);

INSERT INTO `t_payment` (`tenant_id`,`order_id`,`pay_no`,`channel`,`amount`,`status`,`trade_no`,`paid_at`) VALUES
(1,1,'PAY20260001','BALANCE',328,'SUCCESS','BAL20260001',DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1,2,'PAY20260002','WECHAT',238,'SUCCESS','WX20260002',DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1,3,'PAY20260003','ALIPAY',228,'SUCCESS','ALI20260003',NOW()),
(1,4,'PAY20260004','CASH',498,'SUCCESS',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2,5,'PAY20260005','WECHAT',259,'SUCCESS','WX20260005',DATE_SUB(NOW(), INTERVAL 5 DAY));

INSERT INTO `t_cashier_shift` (`tenant_id`,`store_id`,`cashier_id`,`cashier_name`,`start_at`,`end_at`,`opening_cash`,`closing_cash`,`system_amount`,`diff_amount`,`status`) VALUES
(1,1,3,'王收银',DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 9 HOUR,DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 18 HOUR,500,2686,2186,0,'CLOSED'),
(1,1,3,'王收银',CURDATE() + INTERVAL 9 HOUR,NULL,500,NULL,228,NULL,'OPEN');

INSERT INTO `t_coupon` (`tenant_id`,`customer_id`,`name`,`type`,`amount`,`discount`,`min_amount`,`start_at`,`expire_at`,`status`,`source`) VALUES
(1,1,'新客立减50','CASH',50,100,200,DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_ADD(NOW(), INTERVAL 20 DAY),'UNUSED','SYSTEM'),
(1,2,'会员9折券','DISCOUNT',0,90,100,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_ADD(NOW(), INTERVAL 25 DAY),'UNUSED','MARKETING'),
(1,3,'SPA体验券','CASH',100,100,300,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_ADD(NOW(), INTERVAL 30 DAY),'USED','GROUP');

INSERT INTO `t_debt` (`tenant_id`,`customer_id`,`order_id`,`amount`,`paid_amount`,`status`,`remark`) VALUES
(1,2,NULL,680,0,'UNPAID','上次消费挂账');

INSERT INTO `t_commission_rule` (`tenant_id`,`biz_type`,`name`,`technician_level`,`min_amount`,`max_amount`,`rate`) VALUES
(1,'BEAUTY','美容师基础提成',NULL,0,5000,8),
(1,'BEAUTY','美容师进阶提成',NULL,5000,10000,10),
(1,'BEAUTY','美容师高阶提成',NULL,10000,NULL,12),
(1,'HAIR','发型师提成',NULL,0,NULL,15),
(1,'SKIN','皮肤管理师提成',NULL,0,8000,12),
(1,'SPA','SPA理疗师提成',NULL,0,NULL,15);

INSERT INTO `t_settlement` (`id`,`tenant_id`,`settlement_no`,`period_type`,`start_date`,`end_date`,`store_id`,`revenue`,`tech_commission`,`platform_share`,`store_share`,`dividend`,`status`) VALUES
(1,1,'5MY-SET-2026-0001','MONTH',DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH),'%Y-%m-01'),LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)),1,89600,10752,4480,74368,0,'CONFIRMED')
ON DUPLICATE KEY UPDATE revenue=VALUES(revenue);

INSERT INTO `t_settlement_item` (`tenant_id`,`settlement_id`,`technician_id`,`order_id`,`revenue`,`rate`,`commission_amount`) VALUES
(1,1,1,1,12800,10,1280),
(1,1,2,2,9600,12,1152),
(1,1,3,3,16800,15,2520);

INSERT INTO `t_withdrawal` (`tenant_id`,`tech_id`,`amount`,`fee`,`actual_amount`,`status`,`remark`) VALUES
(1,1,1200,12,1188,'PAID','4月提现'),
(1,2,800,8,792,'APPLIED','5月提现申请');

INSERT INTO `t_review` (`tenant_id`,`customer_id`,`order_id`,`technician_id`,`score`,`tags`,`content`,`reply`,`status`) VALUES
(1,1,1,1,5,'手法专业,服务贴心','护理后皮肤很水润，林老师很专业','感谢您的认可，期待再次为您服务','PUBLISHED'),
(1,2,2,2,4,'效果不错,环境舒适','小气泡清洁很彻底，就是等待时间有点长',NULL,'PUBLISHED'),
(1,3,3,3,5,'发型好看','托尼老师审美在线',NULL,'PUBLISHED');

INSERT INTO `t_follow_up` (`tenant_id`,`customer_id`,`order_id`,`type`,`due_at`,`status`,`content`,`handler_id`) VALUES
(1,1,1,'CONSUME',DATE_ADD(NOW(), INTERVAL 1 DAY),'PENDING','护理后回访，了解皮肤状态',2),
(1,4,NULL,'SLEEP',DATE_ADD(NOW(), INTERVAL 2 DAY),'PENDING','沉睡客户唤醒',3),
(1,3,3,'CONSUME',DATE_SUB(NOW(), INTERVAL 1 DAY),'DONE','回访满意度',2);

INSERT INTO `t_after_sale` (`tenant_id`,`order_id`,`customer_id`,`type`,`reason`,`status`,`result`,`compensation`) VALUES
(1,2,2,'COMPLAINT','等待时间过长','PROCESSING','赠送小气泡一次',0),
(1,4,1,'RERUN','SPA后肩颈不适','DONE','安排复检并调整手法',0);

INSERT INTO `t_tech_service_log` (`tenant_id`,`technician_id`,`appointment_id`,`order_id`,`customer_id`,`content`,`duration`) VALUES
(1,1,1,1,1,'皮肤检测：中性偏干；使用补水精华，无过敏反应',60),
(1,3,3,3,3,'客户偏好日系层次，已建议下次修剪时间',60);

INSERT INTO `t_marketing_activity` (`tenant_id`,`biz_type`,`type`,`name`,`price`,`origin_price`,`start_at`,`end_at`,`config_json`) VALUES
(1,'BEAUTY','GROUP','3人拼团·深层补水护理',298,398,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_ADD(NOW(), INTERVAL 25 DAY),'{"groupSize":3}'),
(1,'SKIN','BARGAIN','小气泡砍价0元购',0,298,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_ADD(NOW(), INTERVAL 15 DAY),'{"minPrice":98}'),
(1,'HAIR','FLASH','周三剪发秒杀',168,268,DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_ADD(NOW(), INTERVAL 7 DAY),'{}');

INSERT INTO `t_inventory_item` (`tenant_id`,`store_id`,`biz_type`,`name`,`sku`,`unit`,`stock`,`cost_price`,`warn_stock`) VALUES
(1,1,'BEAUTY','补水精华原液','SKU-B001','瓶',12,120,5),
(1,1,'SKIN','小气泡溶液','SKU-S002','瓶',3,80,5),
(1,1,'HAIR','染发膏-栗棕','SKU-H003','支',8,65,3),
(1,3,'SPA','薰衣草精油','SKU-P004','瓶',2,180,3);

INSERT INTO `t_stock_flow` (`tenant_id`,`item_id`,`type`,`quantity`,`cost`,`remark`,`operator`) VALUES
(1,1,'IN',20,2400,'采购入库','张店长'),
(1,1,'USE',8,960,'护理领用','林小雅'),
(1,2,'IN',10,800,'采购入库','张店长'),
(1,2,'USE',7,560,'清洁项目领用','陈美琪');

INSERT INTO `t_point_record` (`tenant_id`,`customer_id`,`points`,`type`,`order_id`,`remark`) VALUES
(1,1,328,'EARN',1,'消费获得积分'),
(1,1,-100,'USE',NULL,'积分兑换面膜'),
(1,2,238,'EARN',2,'消费获得积分');

INSERT INTO `t_distribution` (`tenant_id`,`customer_id`,`parent_customer_id`,`order_id`,`level`,`amount`,`rate`,`commission`,`status`) VALUES
(1,2,1,2,1,238,5,11.90,'PENDING'),
(1,3,1,3,1,228,5,11.40,'SETTLED');

INSERT INTO `storage_config` (`tenant_id`,`driver`,`config_json`,`status`) VALUES
(0,'LOCAL','{"localDir":"./data/uploads","publicBase":"/api/files"}',1),
(1,'LOCAL','{"localDir":"./data/uploads","publicBase":"/api/files"}',1),
(2,'LOCAL','{"localDir":"./data/uploads","publicBase":"/api/files"}',1);

INSERT INTO `wechat_config` (`tenant_id`,`mini_app_id`,`mini_secret`,`mp_app_id`,`mp_secret`,`mp_token`,`mp_aes_key`,`mp_url`,`union_app_id`,`app_app_id`,`app_secret`,`subscribe_ids`,`push_channel`) VALUES
(1,'wx_demo_mini_5my','ENC:demo-secret','wx_demo_mp_5my','ENC:demo-secret','meiye5my','','https://api.xiegongzhu.com/api/wechat/event','wx_union_demo','wx_app_demo','ENC:demo-app-secret','["APPOINTMENT_CONFIRM","APPOINTMENT_REMIND","VERIFY","STORED","EXPIRY","FOLLOWUP"]','["WECHAT_MP","IN_APP","SMS"]'),
(2,'wx_demo_mini_2hl','ENC:demo-secret','wx_demo_mp_2hl','ENC:demo-secret','meiye2hl','','https://api.xiegongzhu.com/api/wechat/event','wx_union_demo','wx_app_demo','ENC:demo-app-secret','["APPOINTMENT_CONFIRM","VERIFY"]','["WECHAT_MP","IN_APP"]');

INSERT INTO `message_template` (`tenant_id`,`code`,`name`,`template_id`,`channel`,`fields_json`) VALUES
(1,'APPOINTMENT_CONFIRM','预约确认','TPL-APPT-CONFIRM','WECHAT_MP','["项目","门店","时间","技师"]'),
(1,'APPOINTMENT_REMIND','预约提醒','TPL-APPT-REMIND','WECHAT_MP','["时间","技师","工位"]'),
(1,'VERIFY','核销确认','TPL-VERIFY','WECHAT_MP','["订单","完成状态"]'),
(1,'STORED','储值到账','TPL-STORED','WECHAT_MP','["金额","余额"]'),
(1,'EXPIRY','卡项到期','TPL-EXPIRY','WECHAT_MP','["卡种","剩余次数","余额"]'),
(1,'FOLLOWUP','回访提醒','TPL-FOLLOWUP','WECHAT_MP','["客户","上次服务"]'),
(2,'APPOINTMENT_CONFIRM','预约确认','TPL-APPT-CONFIRM','WECHAT_MP','["项目","门店","时间","技师"]'),
(2,'VERIFY','核销确认','TPL-VERIFY','WECHAT_MP','["订单","完成状态"]');

INSERT INTO `sys_menu_config` (`tenant_id`,`target`,`content`) VALUES
(0,'ADMIN','[{"path":"/dashboard","title":"平台看板","icon":"DataBoard"},{"path":"/tenant","title":"租户管理","icon":"OfficeBuilding"},{"path":"/biz-plugin","title":"品类插件","icon":"Grid"},{"path":"/platform-report","title":"平台报表","icon":"TrendCharts"}]'),
(1,'MINIAPP','[{"pagePath":"/pages/index/index","text":"首页","iconPath":"static/tab-home.png"},{"pagePath":"/pages/appointment/index","text":"预约","iconPath":"static/tab-book.png"},{"pagePath":"/pages/order/index","text":"订单","iconPath":"static/tab-order.png"},{"pagePath":"/pages/mine/index","text":"我的","iconPath":"static/tab-mine.png"}]'),
(1,'H5','[{"title":"首页","url":"/"},{"title":"项目","url":"/items"},{"title":"门店","url":"/stores"},{"title":"资讯","url":"/news"},{"title":"我的","url":"/account"}]'),
(1,'WECHAT','[{"name":"预约服务","type":"view","url":"https://www.xiegongzhu.com/appointment"},{"name":"我的卡包","type":"view","url":"https://www.xiegongzhu.com/account/cards"},{"name":"门店导航","type":"view","url":"https://www.xiegongzhu.com/stores"}]'),
(2,'MINIAPP','[{"pagePath":"/pages/index/index","text":"首页","iconPath":"static/tab-home.png"},{"pagePath":"/pages/appointment/index","text":"预约","iconPath":"static/tab-book.png"},{"pagePath":"/pages/mine/index","text":"我的","iconPath":"static/tab-mine.png"}]');

INSERT INTO `audit_log` (`tenant_id`,`operator_id`,`operator_name`,`user_type`,`module`,`action`,`target`,`detail`,`ip`) VALUES
(0,1,'平台超级管理员','PLATFORM','租户管理','开通租户','华丽口腔门诊','配额：2门店/2000客户/40技师','127.0.0.1'),
(1,1,'五月管理员','TENANT','客户档案','新增客户','苏婉晴','客户编号 5MY-CUS-2026-0001','127.0.0.1'),
(1,3,'王收银','TENANT','收银','核销订单','5MY-ORD-2026-0003','支付宝 228.00','127.0.0.1');

INSERT INTO `cms_article` (`tenant_id`,`type`,`title`,`title_en`,`summary`,`content`,`content_en`,`cover`,`seo_keywords`,`seo_description`,`status`,`published_at`) VALUES
(1,'NEWS','秋季补水指南：让肌肤喝饱水','Autumn Hydration Guide','换季干燥？三步科学补水方案','<h2>为什么秋季更容易缺水</h2><p>气温下降、皮脂分泌减少，肌肤屏障更容易受损。建议每周进行1-2次深层补水护理，并配合居家保湿。</p><p>五月美业秋季补水季，深层补水面部护理会员价 328 元。</p>','<h2>Why skin gets dry in autumn</h2><p>Book a hydration facial at May Beauty.</p>','/static/news-1.jpg','美容,补水,秋季护肤,北京美容院','秋季补水指南与深层护理预约',1,'2026-09-20 10:00:00'),
(1,'NOTICE','国庆营业时间调整通知',NULL,'10月1日-3日营业时间 10:00-18:00','<p>尊敬的会员：国庆期间门店营业时间调整为 10:00-18:00，预约请提前一天。</p>',NULL,'/static/news-2.jpg','营业时间,国庆,美业门店',NULL,1,'2026-09-25 09:00:00'),
(1,'PAGE','关于五月美业',NULL,'让每一次护理都值得期待','<p>五月美业成立于2018年，专注面部护理、皮肤管理与SPA，在北京拥有三家直营门店。</p>',NULL,'/static/about.jpg','五月美业,品牌介绍',NULL,1,'2026-01-01 00:00:00'),
(2,'NEWS','口腔健康从洁牙开始',NULL,'每年两次洁牙，守护牙龈健康','<p>超声波洁牙可以去除牙结石与菌斑，建议每半年进行一次。</p>',NULL,'/static/news-3.jpg','口腔,洁牙,口腔门诊',NULL,1,'2026-09-18 10:00:00');

INSERT INTO `wechat_material` (`tenant_id`,`type`,`title`,`content`,`url`) VALUES
(1,'NEWS','秋季补水季｜深层护理限时328元','<p>换季补水正当时，点击预约到店体验。</p>','/static/mp-news-1.jpg'),
(1,'IMAGE','门店环境实拍','','/static/mp-image-1.jpg'),
(2,'NEWS','口腔健康科普｜洁牙的那些事','<p>专业口腔医生为您解答。</p>','/static/mp-news-2.jpg');

INSERT INTO `wechat_tag` (`tenant_id`,`name`,`fan_count`) VALUES
(1,'高活跃会员',128),(1,'沉睡客户',56),(1,'新关注',210),(2,'洁牙客户',32);

INSERT INTO `wechat_fan` (`tenant_id`,`openid`,`union_id`,`nickname`,`tags`,`subscribed`,`subscribed_at`) VALUES
(1,'openid-demo-001','union-demo-001','晴天','高活跃会员',1,NOW()),
(1,'openid-demo-002','union-demo-002','小满','新关注',1,NOW()),
(1,'openid-demo-003','union-demo-003','Lily','沉睡客户',0,DATE_SUB(NOW(), INTERVAL 60 DAY)),
(2,'openid-demo-101','union-demo-101','牙牙','洁牙客户',1,NOW());
