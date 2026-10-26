#!/usr/bin/env python3
"""Generate MyBatis-Plus entities/mappers from schema.sql (project bootstrap helper)."""
import re, os, json

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCHEMA = os.path.join(ROOT, "backend/src/main/resources/db/schema.sql")
OUT_ENTITY = os.path.join(ROOT, "backend/src/main/java/com/meiye/domain/entity")
OUT_MAPPER = os.path.join(ROOT, "backend/src/main/java/com/meiye/domain/mapper")

OVERRIDES = {
    "sys_tenant": "Tenant",
    "sys_platform_user": "PlatformUser",
    "sys_global_dict": "GlobalDict",
    "sys_language": "Language",
    "sys_role": "Role",
    "sys_tenant_user": "TenantUser",
    "sys_user_role": "UserRole",
    "sys_attachment": "Attachment",
    "sys_menu_config": "MenuConfig",
    "t_technician": "Technician",
    "t_technician_cert": "TechnicianCert",
    "t_workstation": "Workstation",
    "t_customer": "Customer",
    "t_store": "Store",
    "t_service_item": "ServiceItem",
    "t_service_package": "ServicePackage",
    "t_service_flow_template": "ServiceFlowTemplate",
    "t_appointment": "Appointment",
    "t_appointment_log": "AppointmentLog",
    "t_schedule": "Schedule",
    "t_tech_rotation": "TechRotation",
    "t_order": "Order",
    "t_order_item": "OrderItem",
    "t_payment": "Payment",
    "t_cashier_shift": "CashierShift",
    "t_card_type": "CardType",
    "t_member_card": "MemberCard",
    "t_card_flow": "CardFlow",
    "t_coupon": "Coupon",
    "t_debt": "Debt",
    "t_commission_rule": "CommissionRule",
    "t_settlement": "Settlement",
    "t_settlement_item": "SettlementItem",
    "t_withdrawal": "Withdrawal",
    "t_distribution": "Distribution",
    "t_point_record": "PointRecord",
    "t_marketing_activity": "MarketingActivity",
    "t_review": "Review",
    "t_follow_up": "FollowUp",
    "t_after_sale": "AfterSale",
    "t_tech_service_log": "TechServiceLog",
    "t_inventory_item": "InventoryItem",
    "t_stock_flow": "StockFlow",
    "biz_type_plugin": "BizTypePlugin",
    "platform_param": "PlatformParam",
    "tenant_biz_type": "TenantBizType",
    "tenant_config": "TenantConfig",
    "tenant_dict": "TenantDict",
    "audit_log": "AuditLog",
    "storage_config": "StorageConfig",
    "wechat_config": "WechatConfig",
    "message_template": "MessageTemplate",
    "message_log": "MessageLog",
    "wechat_material": "WechatMaterial",
    "wechat_tag": "WechatTag",
    "wechat_fan": "WechatFan",
    "cms_article": "CmsArticle",
}

COMMON = {"id", "tenant_id", "created_at", "updated_at", "deleted"}
SPECIAL_FIELDS = {
    "password": '@JsonIgnore\n    private String password;',
    "phone": '@TableField(typeHandler = AesTypeHandler.class)\n    private String phone;',
    "id_card": '@TableField(typeHandler = AesTypeHandler.class)\n    private String idCard;',
    "mini_secret": '@TableField(typeHandler = AesTypeHandler.class)\n    private String miniSecret;',
    "mp_secret": '@TableField(typeHandler = AesTypeHandler.class)\n    private String mpSecret;',
    "mp_aes_key": '@TableField(typeHandler = AesTypeHandler.class)\n    private String mpAesKey;',
    "app_secret": '@TableField(typeHandler = AesTypeHandler.class)\n    private String appSecret;',
}

TYPE_MAP = [
    (r"bigint", "Long"),
    (r"int\b", "Integer"),
    (r"tinyint", "Integer"),
    (r"decimal", "BigDecimal"),
    (r"datetime", "LocalDateTime"),
    (r"timestamp", "LocalDateTime"),
    (r"date\b", "LocalDate"),
    (r"time\b", "LocalTime"),
    (r"longtext|text", "String"),
    (r"varchar|char", "String"),
]

def camel(s):
    parts = s.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])

def java_type(sql_type):
    t = sql_type.lower()
    for pat, jt in TYPE_MAP:
        if re.search(pat, t):
            return jt
    return "String"

def parse():
    sql = open(SCHEMA, encoding="utf-8").read()
    pattern = re.compile(r"CREATE TABLE IF NOT EXISTS `(\w+)` \((.*?)\n\) ENGINE", re.S)
    tables = []
    for m in pattern.finditer(sql):
        name, body = m.group(1), m.group(2)
        cols = []
        for line in body.split("\n"):
            line = line.strip().rstrip(",")
            cm = re.match(r"`(\w+)` ([a-z0-9]+(?:\([^)]*\))?)", line, re.I)
            if not cm:
                continue
            col, typ = cm.group(1), cm.group(2)
            cols.append((col, typ))
        tables.append((name, cols))
    return tables

def main():
    os.makedirs(OUT_ENTITY, exist_ok=True)
    os.makedirs(OUT_MAPPER, exist_ok=True)
    tables = parse()
    index = []
    for table, cols in tables:
        base_name = table
        for prefix in ("sys_", "t_"):
            if base_name.startswith(prefix):
                base_name = base_name[len(prefix):]
                break
        cls = OVERRIDES.get(table) or "".join(p.capitalize() for p in base_name.split("_"))
        has_tenant = any(c == "tenant_id" for c, _ in cols)
        base = "TenantEntity" if has_tenant else "BaseEntity"
        fields = []
        imports = set()
        for col, typ in cols:
            if col in COMMON:
                continue
            jt = java_type(typ)
            if jt == "BigDecimal":
                imports.add("java.math.BigDecimal")
            elif jt == "LocalDateTime":
                imports.add("java.time.LocalDateTime")
            elif jt == "LocalDate":
                imports.add("java.time.LocalDate")
            elif jt == "LocalTime":
                imports.add("java.time.LocalTime")
            fname = camel(col)
            if col == "order":
                fname = "order"
            if col in ("desc", "status", "type", "level", "value", "operator", "action", "target", "size"):
                pass
            if col in SPECIAL_FIELDS:
                block = SPECIAL_FIELDS[col].replace("private String password;", f"private String {fname};").replace("private String phone;", f"private String {fname};").replace("private String idCard;", f"private String {fname};").replace("private String miniSecret;", f"private String {fname};").replace("private String mpSecret;", f"private String {fname};").replace("private String mpAesKey;", f"private String {fname};").replace("private String appSecret;", f"private String {fname};")
            else:
                block = f"private {jt} {fname};"
            fields.append("    " + block)
        header = f"""package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
{chr(10).join('import ' + i + ';' for i in sorted(imports))}

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "{table}", autoResultMap = true)
public class {cls} extends {base} {{
{chr(10).join(fields)}
}}
"""
        open(os.path.join(OUT_ENTITY, cls + ".java"), "w", encoding="utf-8").write(header)
        mapper = f"""package com.meiye.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.domain.entity.{cls};
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {cls}Mapper extends BaseMapper<{cls}> {{
}}
"""
        open(os.path.join(OUT_MAPPER, cls + "Mapper.java"), "w", encoding="utf-8").write(mapper)
        index.append((table, cls))
    print(json.dumps(index, ensure_ascii=False, indent=1))

if __name__ == "__main__":
    main()
