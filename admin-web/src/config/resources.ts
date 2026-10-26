export interface Option { label: string; value: any }

export interface Field {
  key: string
  label: string
  type?: 'input' | 'number' | 'select' | 'date' | 'datetime' | 'textarea' | 'switch' | 'password' | 'money' | 'remote'
  options?: Option[]
  remote?: string
  required?: boolean
  filter?: boolean
  placeholder?: string
  span?: number
  min?: number
  max?: number
  precision?: number
  hiddenOnEdit?: boolean
  tip?: string
}

export interface Column {
  key: string
  label: string
  width?: number
  minWidth?: number
  type?: 'money' | 'tag' | 'date' | 'datetime' | 'switch' | 'image' | 'boolean'
  map?: Record<string, string>
  tagType?: Record<string, string>
  options?: Option[]
}

export interface Resource {
  title: string
  subtitle?: string
  endpoint: string
  columns: Column[]
  fields: Field[]
  permissionHint?: string
  pageSize?: number
}

const STATUS_ENABLE: Option[] = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

export const resources: Record<string, Resource> = {
  stores: {
    title: '门店管理',
    subtitle: '门店档案、服务范围、跨店通兑与总部财务分账开关',
    endpoint: '/tenant/stores',
    columns: [
      { key: 'code', label: '门店编码', width: 130 },
      { key: 'name', label: '门店名称', minWidth: 160 },
      { key: 'phone', label: '联系电话', width: 140 },
      { key: 'address', label: '地址', minWidth: 220 },
      { key: 'manager', label: '店长', width: 100 },
      { key: 'allowCrossStore', label: '跨店通兑', type: 'switch', width: 110 },
      { key: 'allowShareProfit', label: '总部财务分账', type: 'switch', width: 130 },
      { key: 'status', label: '状态', type: 'tag', width: 90, options: STATUS_ENABLE }
    ],
    fields: [
      { key: 'code', label: '门店编码', required: true, filter: true },
      { key: 'name', label: '门店名称', required: true, filter: true },
      { key: 'phone', label: '联系电话' },
      { key: 'manager', label: '店长' },
      { key: 'address', label: '门店地址', span: 24 },
      { key: 'serviceScope', label: '服务范围', type: 'textarea', span: 24 },
      { key: 'bizTypes', label: '经营品类', type: 'remote', remote: 'bizTypes', placeholder: '逗号分隔，如 BEAUTY,SPA' },
      { key: 'allowCrossStore', label: '跨店通兑', type: 'switch' },
      { key: 'allowShareProfit', label: '总部财务分账', type: 'switch' },
      { key: 'status', label: '状态', type: 'switch', options: STATUS_ENABLE },
      { key: 'description', label: '门店介绍', type: 'textarea', span: 24 }
    ]
  },
  workstations: {
    title: '工位 / 房间 / 床位',
    subtitle: '按品类维护门店工位、房间、床位状态',
    endpoint: '/tenant/workstations',
    columns: [
      { key: 'name', label: '名称', minWidth: 150 },
      { key: 'storeId', label: '门店', type: 'remote' as any, width: 160 },
      { key: 'bizType', label: '业务品类', width: 130 },
      { key: 'type', label: '类型', width: 120, map: { ROOM: '房间', BED: '床位', CHAIR: '工位', DESK: '操作台' } },
      { key: 'status', label: '状态', width: 100, map: { FREE: '空闲', BUSY: '使用中', DISABLED: '停用' } },
      { key: 'remark', label: '备注', minWidth: 180 }
    ],
    fields: [
      { key: 'name', label: '名称', required: true, filter: true },
      { key: 'storeId', label: '门店', type: 'remote', remote: 'stores', required: true, filter: true },
      { key: 'bizType', label: '业务品类', type: 'remote', remote: 'bizTypes', required: true, filter: true },
      { key: 'type', label: '类型', type: 'select', options: [
        { label: '房间', value: 'ROOM' }, { label: '床位', value: 'BED' }, { label: '工位', value: 'CHAIR' }, { label: '操作台', value: 'DESK' }
      ] },
      { key: 'status', label: '状态', type: 'select', options: [
        { label: '空闲', value: 'FREE' }, { label: '使用中', value: 'BUSY' }, { label: '停用', value: 'DISABLED' }
      ] },
      { key: 'remark', label: '备注', type: 'textarea', span: 24 }
    ]
  },
  serviceItems: {
    title: '服务项目',
    subtitle: '按品类维护项目、价格、时长、提成与适用门店',
    endpoint: '/tenant/service-items',
    columns: [
      { key: 'name', label: '项目名称', minWidth: 180 },
      { key: 'bizType', label: '品类', width: 120 },
      { key: 'category', label: '分类', width: 120 },
      { key: 'duration', label: '时长(分钟)', width: 110 },
      { key: 'price', label: '单价', type: 'money', width: 110 },
      { key: 'memberPrice', label: '会员价', type: 'money', width: 110 },
      { key: 'commissionRate', label: '提成%', width: 90 },
      { key: 'status', label: '状态', type: 'tag', width: 90, options: STATUS_ENABLE }
    ],
    fields: [
      { key: 'name', label: '项目名称', required: true, filter: true },
      { key: 'bizType', label: '业务品类', type: 'remote', remote: 'bizTypes', required: true, filter: true },
      { key: 'category', label: '项目分类', filter: true },
      { key: 'duration', label: '服务时长(分钟)', type: 'number', min: 15 },
      { key: 'price', label: '单次价格', type: 'money' },
      { key: 'memberPrice', label: '会员价', type: 'money' },
      { key: 'commissionRate', label: '提成比例(%)', type: 'number', precision: 2 },
      { key: 'storeIds', label: '适用门店', type: 'remote', remote: 'stores', placeholder: '门店ID逗号分隔，留空为全部门店' },
      { key: 'image', label: '项目主图', placeholder: '上传后填写图片地址' },
      { key: 'status', label: '状态', type: 'switch' },
      { key: 'description', label: '项目介绍', type: 'textarea', span: 24 }
    ]
  },
  servicePackages: {
    title: '套餐管理',
    subtitle: '次卡、自由卡、通用卡、限期套餐、组合礼包',
    endpoint: '/tenant/service-packages',
    columns: [
      { key: 'name', label: '套餐名称', minWidth: 180 },
      { key: 'type', label: '类型', width: 120, map: { COUNT: '次卡', FREE: '自由卡', GENERAL: '通用卡', TIME: '限期套餐', COMBO: '组合礼包' } },
      { key: 'price', label: '售价', type: 'money', width: 110 },
      { key: 'faceValue', label: '面值', type: 'money', width: 110 },
      { key: 'times', label: '次数', width: 90 },
      { key: 'validDays', label: '有效期(天)', width: 110 },
      { key: 'status', label: '状态', type: 'tag', width: 90, options: STATUS_ENABLE }
    ],
    fields: [
      { key: 'name', label: '套餐名称', required: true, filter: true },
      { key: 'bizType', label: '业务品类', type: 'remote', remote: 'bizTypes', required: true, filter: true },
      { key: 'type', label: '套餐类型', type: 'select', options: [
        { label: '次卡', value: 'COUNT' }, { label: '自由卡', value: 'FREE' }, { label: '通用卡', value: 'GENERAL' },
        { label: '限期套餐', value: 'TIME' }, { label: '组合礼包', value: 'COMBO' }
      ] },
      { key: 'price', label: '售价', type: 'money' },
      { key: 'faceValue', label: '面值', type: 'money' },
      { key: 'times', label: '包含次数', type: 'number' },
      { key: 'validDays', label: '有效期(天)', type: 'number' },
      { key: 'status', label: '状态', type: 'switch' },
      { key: 'itemsJson', label: '包含项目 JSON', type: 'textarea', span: 24, tip: '示例：[{"itemId":1,"times":5}]' },
      { key: 'description', label: '套餐说明', type: 'textarea', span: 24 }
    ]
  },
  serviceFlows: {
    title: '标准服务流程',
    subtitle: '图文步骤模板，服务过程标准化',
    endpoint: '/tenant/service-flows',
    columns: [
      { key: 'name', label: '流程名称', minWidth: 180 },
      { key: 'bizType', label: '品类', width: 120 },
      { key: 'itemId', label: '关联项目', width: 120 },
      { key: 'status', label: '状态', type: 'tag', width: 90, options: STATUS_ENABLE },
      { key: 'stepsJson', label: '步骤', minWidth: 300 }
    ],
    fields: [
      { key: 'name', label: '流程名称', required: true, filter: true },
      { key: 'bizType', label: '业务品类', type: 'remote', remote: 'bizTypes', required: true },
      { key: 'itemId', label: '关联项目', type: 'remote', remote: 'items' },
      { key: 'status', label: '状态', type: 'switch' },
      { key: 'stepsJson', label: '步骤 JSON', type: 'textarea', span: 24, tip: '示例：[{"title":"清洁","content":"温和清洁"}...]' }
    ]
  },
  cardTypes: {
    title: '卡种管理',
    subtitle: '计次卡/时间卡/套餐卡/组合礼包/储值卡/附属卡',
    endpoint: '/tenant/card-types',
    columns: [
      { key: 'name', label: '卡种名称', minWidth: 160 },
      { key: 'type', label: '类型', width: 110, map: { COUNT: '计次卡', TIME: '时间卡', PACKAGE: '套餐卡', COMBO: '组合礼包', STORED: '储值卡', SUB: '附属卡' } },
      { key: 'price', label: '售价', type: 'money', width: 110 },
      { key: 'faceValue', label: '面值/卡金', type: 'money', width: 120 },
      { key: 'times', label: '次数', width: 90 },
      { key: 'validDays', label: '有效期(天)', width: 110 },
      { key: 'discount', label: '折扣', width: 90 },
      { key: 'status', label: '状态', type: 'tag', width: 90, options: STATUS_ENABLE }
    ],
    fields: [
      { key: 'name', label: '卡种名称', required: true, filter: true },
      { key: 'bizType', label: '业务品类', type: 'remote', remote: 'bizTypes', required: true },
      { key: 'type', label: '卡类型', type: 'select', options: [
        { label: '计次卡', value: 'COUNT' }, { label: '时间卡', value: 'TIME' }, { label: '套餐卡', value: 'PACKAGE' },
        { label: '组合礼包', value: 'COMBO' }, { label: '储值卡', value: 'STORED' }, { label: '附属卡', value: 'SUB' }
      ] },
      { key: 'price', label: '售价', type: 'money' },
      { key: 'faceValue', label: '面值/卡金', type: 'money' },
      { key: 'bonus', label: '赠送金额', type: 'money' },
      { key: 'times', label: '次数', type: 'number' },
      { key: 'validDays', label: '有效期(天)', type: 'number' },
      { key: 'discount', label: '折扣(如 90 表示九折)', type: 'number', precision: 2 },
      { key: 'commissionRate', label: '开卡提成(%)', type: 'number', precision: 2 },
      { key: 'status', label: '状态', type: 'switch' },
      { key: 'description', label: '卡种说明', type: 'textarea', span: 24 }
    ]
  },
  tenantUsers: {
    title: '员工账号',
    subtitle: '租户管理员 / 门店店长 / 前台收银 / 技师 / 市场 / 财务',
    endpoint: '/tenant/users',
    columns: [
      { key: 'username', label: '账号', width: 130 },
      { key: 'name', label: '姓名', width: 110 },
      { key: 'phone', label: '手机号', width: 140 },
      { key: 'roleCode', label: '角色', width: 150, map: {
        TENANT_ADMIN: '租户管理员', STORE_MANAGER: '门店店长', CASHIER: '前台收银',
        TECHNICIAN: '技师', MARKETING: '市场', FINANCE: '财务'
      } },
      { key: 'storeId', label: '所属门店', width: 130 },
      { key: 'status', label: '状态', type: 'tag', width: 90, options: STATUS_ENABLE },
      { key: 'lastLoginAt', label: '最后登录', type: 'datetime', width: 170 }
    ],
    fields: [
      { key: 'username', label: '登录账号', required: true, filter: true },
      { key: 'name', label: '姓名', required: true, filter: true },
      { key: 'phone', label: '手机号' },
      { key: 'password', label: '密码', type: 'password', tip: '新增必填；编辑留空表示不修改', hiddenOnEdit: false },
      { key: 'roleCode', label: '角色', type: 'select', options: [
        { label: '租户管理员', value: 'TENANT_ADMIN' }, { label: '门店店长', value: 'STORE_MANAGER' },
        { label: '前台收银', value: 'CASHIER' }, { label: '技师', value: 'TECHNICIAN' },
        { label: '市场', value: 'MARKETING' }, { label: '财务', value: 'FINANCE' }
      ] },
      { key: 'storeId', label: '所属门店', type: 'remote', remote: 'stores' },
      { key: 'status', label: '状态', type: 'switch' }
    ]
  },
  tenantDicts: {
    title: '自定义字典',
    subtitle: '租户级字典隔离，用于项目分类、岗位、房间类型等',
    endpoint: '/tenant/dicts',
    columns: [
      { key: 'dictType', label: '字典类型', width: 160 },
      { key: 'label', label: '显示名称', minWidth: 160 },
      { key: 'value', label: '字典值', width: 160 },
      { key: 'sort', label: '排序', width: 90 },
      { key: 'status', label: '状态', type: 'tag', width: 90, options: STATUS_ENABLE }
    ],
    fields: [
      { key: 'dictType', label: '字典类型', required: true, filter: true },
      { key: 'label', label: '显示名称', required: true, filter: true },
      { key: 'value', label: '字典值', required: true },
      { key: 'sort', label: '排序', type: 'number' },
      { key: 'status', label: '状态', type: 'switch' }
    ]
  },
  coupons: {
    title: '优惠券',
    subtitle: '发放、核销、过期状态管理',
    endpoint: '/tenant/coupons',
    columns: [
      { key: 'name', label: '优惠券名称', minWidth: 160 },
      { key: 'customerId', label: '会员', width: 120 },
      { key: 'type', label: '类型', width: 100, map: { CASH: '代金券', DISCOUNT: '折扣券', GIFT: '礼品券' } },
      { key: 'amount', label: '金额', type: 'money', width: 100 },
      { key: 'discount', label: '折扣', width: 90 },
      { key: 'minAmount', label: '门槛', type: 'money', width: 100 },
      { key: 'status', label: '状态', type: 'tag', width: 100, map: { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' } },
      { key: 'expireAt', label: '到期时间', type: 'datetime', width: 170 }
    ],
    fields: [
      { key: 'name', label: '优惠券名称', required: true, filter: true },
      { key: 'customerId', label: '会员', type: 'remote', remote: 'customers', required: true, filter: true },
      { key: 'type', label: '券类型', type: 'select', options: [
        { label: '代金券', value: 'CASH' }, { label: '折扣券', value: 'DISCOUNT' }, { label: '礼品券', value: 'GIFT' }
      ] },
      { key: 'amount', label: '面额', type: 'money' },
      { key: 'discount', label: '折扣(如 85)', type: 'number', precision: 2 },
      { key: 'minAmount', label: '使用门槛', type: 'money' },
      { key: 'status', label: '状态', type: 'select', options: [
        { label: '未使用', value: 'UNUSED' }, { label: '已使用', value: 'USED' }, { label: '已过期', value: 'EXPIRED' }
      ] },
      { key: 'expireAt', label: '到期时间', type: 'datetime' }
    ]
  },
  debts: {
    title: '欠款管理',
    subtitle: '客户挂账、还款与欠款余额',
    endpoint: '/tenant/debts',
    columns: [
      { key: 'customerId', label: '会员', width: 140 },
      { key: 'orderId', label: '关联订单', width: 110 },
      { key: 'amount', label: '欠款金额', type: 'money', width: 120 },
      { key: 'paidAmount', label: '已还金额', type: 'money', width: 120 },
      { key: 'status', label: '状态', type: 'tag', width: 100, map: { UNPAID: '未结清', PARTIAL: '部分还款', PAID: '已结清' } },
      { key: 'remark', label: '备注', minWidth: 180 },
      { key: 'createdAt', label: '创建时间', type: 'datetime', width: 170 }
    ],
    fields: [
      { key: 'customerId', label: '会员', type: 'remote', remote: 'customers', required: true, filter: true },
      { key: 'orderId', label: '关联订单ID', type: 'number' },
      { key: 'amount', label: '欠款金额', type: 'money' },
      { key: 'paidAmount', label: '已还金额', type: 'money' },
      { key: 'status', label: '状态', type: 'select', options: [
        { label: '未结清', value: 'UNPAID' }, { label: '部分还款', value: 'PARTIAL' }, { label: '已结清', value: 'PAID' }
      ] },
      { key: 'remark', label: '备注', type: 'textarea', span: 24 }
    ]
  },
  marketing: {
    title: '营销活动',
    subtitle: '拼团、砍价、秒杀、分销活动',
    endpoint: '/tenant/marketing/activities',
    columns: [
      { key: 'name', label: '活动名称', minWidth: 180 },
      { key: 'type', label: '类型', width: 110, map: { GROUP: '拼团', BARGAIN: '砍价', FLASH: '秒杀', DISTRIBUTION: '分销' } },
      { key: 'price', label: '活动价', type: 'money', width: 110 },
      { key: 'originPrice', label: '原价', type: 'money', width: 110 },
      { key: 'startAt', label: '开始时间', type: 'datetime', width: 170 },
      { key: 'endAt', label: '结束时间', type: 'datetime', width: 170 },
      { key: 'status', label: '状态', type: 'tag', width: 90, map: { 1: '进行中', 0: '已停止' } }
    ],
    fields: [
      { key: 'name', label: '活动名称', required: true, filter: true },
      { key: 'type', label: '活动类型', type: 'select', options: [
        { label: '拼团', value: 'GROUP' }, { label: '砍价', value: 'BARGAIN' },
        { label: '秒杀', value: 'FLASH' }, { label: '分销', value: 'DISTRIBUTION' }
      ] },
      { key: 'bizType', label: '业务品类', type: 'remote', remote: 'bizTypes' },
      { key: 'price', label: '活动价', type: 'money' },
      { key: 'originPrice', label: '原价', type: 'money' },
      { key: 'startAt', label: '开始时间', type: 'datetime' },
      { key: 'endAt', label: '结束时间', type: 'datetime' },
      { key: 'status', label: '状态', type: 'switch' },
      { key: 'configJson', label: '活动配置 JSON', type: 'textarea', span: 24, tip: '拼团人数、砍价人数、分销比例等' }
    ]
  },
  commissionRules: {
    title: '提成规则',
    subtitle: '阶梯提成：按订单金额区间设置提成比例',
    endpoint: '/tenant/commission-rules',
    columns: [
      { key: 'name', label: '规则名称', minWidth: 160 },
      { key: 'bizType', label: '品类', width: 120 },
      { key: 'technicianLevel', label: '技师等级', width: 120 },
      { key: 'minAmount', label: '区间下限', type: 'money', width: 120 },
      { key: 'maxAmount', label: '区间上限', type: 'money', width: 120 },
      { key: 'rate', label: '提成比例(%)', width: 120 },
      { key: 'status', label: '状态', type: 'tag', width: 90, options: STATUS_ENABLE }
    ],
    fields: [
      { key: 'name', label: '规则名称', required: true, filter: true },
      { key: 'bizType', label: '业务品类', type: 'remote', remote: 'bizTypes' },
      { key: 'technicianLevel', label: '技师等级' },
      { key: 'minAmount', label: '区间下限', type: 'money' },
      { key: 'maxAmount', label: '区间上限', type: 'money' },
      { key: 'rate', label: '提成比例(%)', type: 'number', precision: 2 },
      { key: 'status', label: '状态', type: 'switch' }
    ]
  }
}
