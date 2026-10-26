<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">菜单可视化</h2>
        <div class="page-subtitle">小程序 tabBar、H5 导航、公众号菜单可视化编辑，保存后各端动态生效，无需发版</div>
      </div>
      <div class="flex gap-8">
        <el-select v-model="target" style="width: 190px" @change="load">
          <el-option label="小程序底部导航" value="MINIAPP" />
          <el-option label="H5 门户菜单" value="H5" />
          <el-option label="公众号菜单" value="WECHAT" />
          <el-option label="管理端菜单" value="ADMIN" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存并生效</el-button>
      </div>
    </div>

    <el-alert v-if="target === 'WECHAT'" type="info" :closable="false" show-icon class="mb-16"
              title="公众号菜单保存后，前往「微信配置」点击“同步公众号菜单”推送到微信服务器；一级菜单最多 3 个，每个最多 5 个子菜单。" />

    <el-row :gutter="18">
      <el-col :span="7">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">菜单树</span>
            <div class="flex gap-4">
              <el-button size="small" :icon="Plus" @click="addNode()">一级</el-button>
              <el-button size="small" :disabled="!isWechat || !selected || selected.__level > 0" @click="addNode(selected)">子项</el-button>
            </div>
          </div>
          <el-tree
            ref="treeRef"
            :data="nodes"
            node-key="__key"
            :props="{ label: labelOf, children: 'children' }"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="onSelect"
          >
            <template #default="{ data }">
              <div class="tree-node">
                <span class="tree-label">{{ labelOf(data) || '(未命名)' }}</span>
                <span class="tree-path">{{ pathOf(data) || '-' }}</span>
              </div>
            </template>
          </el-tree>
          <el-empty v-if="!nodes.length" description="暂无菜单，请新增" :image-size="70" />

          <div v-if="selected" class="tree-actions">
            <el-button size="small" :icon="ArrowUp" :disabled="isFirst" @click="move(-1)">上移</el-button>
            <el-button size="small" :icon="ArrowDown" :disabled="isLast" @click="move(1)">下移</el-button>
            <el-button size="small" type="danger" plain :icon="Delete" @click="removeNode">删除</el-button>
          </div>
        </div>
      </el-col>

      <el-col :span="10">
        <div class="panel">
          <div class="panel-head"><span class="panel-title">属性面板</span></div>
          <el-empty v-if="!selected" description="请在左侧选择一个菜单项" :image-size="70" />
          <el-form v-else :model="selected" label-width="96px" class="prop-form">
            <template v-if="target === 'MINIAPP'">
              <el-form-item label="菜单文字"><el-input v-model="selected.text" maxlength="6" show-word-limit /></el-form-item>
              <el-form-item label="页面路径"><el-input v-model="selected.pagePath" placeholder="/pages/index/index" /></el-form-item>
              <el-form-item label="图标路径">
                <el-input v-model="selected.iconPath" placeholder="static/tab-home.png" />
                <div class="tip">图标需随小程序包发布，建议 81×81 PNG</div>
              </el-form-item>
              <el-form-item label="选中图标">
                <el-input v-model="selected.selectedIconPath" placeholder="static/tab-home-active.png" />
              </el-form-item>
            </template>

            <template v-else-if="target === 'H5'">
              <el-form-item label="菜单名称"><el-input v-model="selected.title" maxlength="12" show-word-limit /></el-form-item>
              <el-form-item label="跳转地址"><el-input v-model="selected.url" placeholder="/items" /></el-form-item>
              <el-form-item label="新窗口"><el-switch v-model="selected.blank" /></el-form-item>
              <el-form-item label="仅登录可见"><el-switch v-model="selected.auth" /></el-form-item>
            </template>

            <template v-else-if="target === 'WECHAT'">
              <el-form-item label="菜单名称"><el-input v-model="selected.name" maxlength="8" show-word-limit /></el-form-item>
              <el-form-item label="类型">
                <el-select v-model="selected.type" style="width: 100%">
                  <el-option label="跳转网页 (view)" value="view" />
                  <el-option label="跳转小程序 (miniprogram)" value="miniprogram" />
                  <el-option label="点击事件 (click)" value="click" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="selected.type === 'view'" label="网页地址">
                <el-input v-model="selected.url" placeholder="https://m.example.com/booking" />
              </el-form-item>
              <el-form-item v-if="selected.type === 'miniprogram'" label="小程序 AppID">
                <el-input v-model="selected.appid" placeholder="wx..." />
              </el-form-item>
              <el-form-item v-if="selected.type === 'miniprogram'" label="小程序路径">
                <el-input v-model="selected.pagepath" placeholder="pages/index/index" />
              </el-form-item>
              <el-form-item v-if="selected.type === 'click'" label="事件 Key">
                <el-input v-model="selected.key" placeholder="BOOKING" />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="菜单标题"><el-input v-model="selected.title" /></el-form-item>
              <el-form-item label="路由路径"><el-input v-model="selected.path" placeholder="/dashboard" /></el-form-item>
              <el-form-item label="图标"><el-input v-model="selected.icon" placeholder="DataLine" /></el-form-item>
            </template>

            <el-form-item label="排序值">
              <el-input-number v-model="selected.sort" :min="0" :max="999" style="width: 140px" />
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <el-col :span="7">
        <div class="panel preview-panel">
          <div class="panel-head"><span class="panel-title">实时预览</span></div>
          <div class="phone">
            <div class="phone-notch" />
            <div class="phone-body">
              <div class="phone-brand">
                <div class="brand-logo">美</div>
                <div>
                  <div class="brand-name">{{ target === 'MINIAPP' ? '会员中心' : target === 'WECHAT' ? '公众号' : '品牌门户' }}</div>
                  <div class="brand-sub">{{ target === 'MINIAPP' ? '预约 · 卡包 · 积分' : '服务与资讯' }}</div>
                </div>
              </div>
              <div class="phone-content">
                <div class="ph-card" v-for="n in 3" :key="n" />
                <div class="ph-line" />
                <div class="ph-line short" />
              </div>
              <div class="phone-tabbar" :class="{ wechat: target === 'WECHAT' }">
                <div v-for="(it, idx) in previewItems" :key="idx" class="tab-item" :class="{ active: idx === 0 }">
                  <div class="tab-icon" />
                  <div class="tab-text">{{ labelOf(it) || '菜单' }}</div>
                </div>
                <div v-if="!previewItems.length" class="tab-empty">暂无菜单项</div>
              </div>
            </div>
          </div>
          <div class="preview-tip">
            {{ target === 'WECHAT' ? '公众号底部菜单（最多 3 个一级菜单）' : target === 'ADMIN' ? '管理端左侧菜单预览' : '移动端底部导航预览' }}
          </div>
        </div>
      </el-col>
    </el-row>

    <el-collapse class="mt-16">
      <el-collapse-item title="高级：JSON 源码编辑（适用于批量导入/复杂子菜单）" name="json">
        <el-input v-model="content" type="textarea" :rows="12" class="json-editor" spellcheck="false" />
        <div class="flex gap-8 mt-12">
          <el-button @click="formatJson">格式化</el-button>
          <el-button @click="validate">校验 JSON</el-button>
          <el-button type="primary" plain @click="applyJson">应用到可视化编辑器</el-button>
        </div>
        <el-alert v-if="parseError" type="error" :closable="false" show-icon :title="parseError" class="mt-12" />
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus, Delete, ArrowUp, ArrowDown, Check } from '@element-plus/icons-vue'
import request from '@/api/request'

const target = ref('MINIAPP')
const nodes = ref<any[]>([])
const content = ref('[]')
const saving = ref(false)
const parseError = ref('')
const selected = ref<any>(null)
const treeRef = ref()
let keySeed = 1

const isWechat = computed(() => target.value === 'WECHAT')
const previewItems = computed(() => (target.value === 'WECHAT' ? nodes.value.slice(0, 3) : nodes.value.slice(0, 5)))

const parentOf = computed<any>(() => {
  if (!selected.value) return null
  return nodes.value.find(n => n === selected.value || (n.children || []).includes(selected.value)) || null
})
const siblings = computed<any[]>(() => (parentOf.value && parentOf.value !== selected.value ? parentOf.value.children : nodes.value))
const currentIndex = computed(() => siblings.value.indexOf(selected.value))
const isFirst = computed(() => currentIndex.value <= 0)
const isLast = computed(() => currentIndex.value < 0 || currentIndex.value >= siblings.value.length - 1)

function labelOf(n: any) { return n?.text || n?.title || n?.name || '' }
function pathOf(n: any) { return n?.pagePath || n?.url || n?.path || '' }

function decorate(list: any[], level = 0): any[] {
  return (Array.isArray(list) ? list : []).map((raw) => {
    const node = { ...raw, __key: 'k' + keySeed++, __level: level }
    if (Array.isArray(raw.sub_button)) node.children = decorate(raw.sub_button, level + 1)
    else if (Array.isArray(raw.children)) node.children = decorate(raw.children, level + 1)
    return node
  })
}

function clean(list: any[]): any[] {
  return list.map((n) => {
    const out: any = {}
    Object.keys(n).forEach((k) => {
      if (!k.startsWith('__')) out[k] = n[k]
    })
    if (n.children && n.children.length) {
      if (target.value === 'WECHAT') out.sub_button = clean(n.children)
      else out.children = clean(n.children)
    } else {
      delete out.children
      delete out.sub_button
    }
    return out
  })
}

function findFirst(list: any[]): any {
  for (const n of list) {
    if (n.children && n.children.length) {
      const c = findFirst(n.children)
      if (c) return c
    }
  }
  return list[0] || null
}

async function load() {
  const res: any = await request.get('/menus', { params: { target: target.value } })
  const data = res.data
  content.value = data?.content || '[]'
  let arr: any[] = []
  try {
    arr = JSON.parse(content.value)
    if (!Array.isArray(arr)) arr = []
  } catch (e) { arr = [] }
  nodes.value = decorate(arr)
  selected.value = findFirst(nodes.value)
  await nextTick()
  if (selected.value) treeRef.value?.setCurrentKey(selected.value.__key)
}

function onSelect(data: any) {
  selected.value = data
}

function addNode(parent?: any) {
  const node: any = isWechat.value
    ? { name: '新菜单', type: 'view', url: '', __key: 'k' + keySeed++, __level: parent ? 1 : 0 }
    : target.value === 'MINIAPP'
      ? { text: '新菜单', pagePath: '', iconPath: '', __key: 'k' + keySeed++, __level: parent ? 1 : 0 }
      : target.value === 'H5'
        ? { title: '新菜单', url: '/', __key: 'k' + keySeed++, __level: 0 }
        : { title: '新菜单', path: '/', icon: 'Menu', __key: 'k' + keySeed++, __level: 0 }
  if (parent) {
    parent.children = parent.children || []
    parent.children.push(node)
    parent.__expanded = true
  } else {
    nodes.value.push(node)
  }
  selected.value = node
  syncContent()
}

async function removeNode() {
  if (!selected.value) return
  await ElMessageBox.confirm(`确认删除菜单「${labelOf(selected.value) || '未命名'}」？`, '删除菜单', { type: 'warning' })
  const list = siblings.value
  const idx = list.indexOf(selected.value)
  if (idx >= 0) list.splice(idx, 1)
  selected.value = findFirst(nodes.value)
  syncContent()
}

function move(delta: number) {
  const list = siblings.value
  const idx = list.indexOf(selected.value)
  const to = idx + delta
  if (idx < 0 || to < 0 || to >= list.length) return
  ;[list[idx], list[to]] = [list[to], list[idx]]
  syncContent()
}

function syncContent() {
  content.value = JSON.stringify(clean(nodes.value), null, 2)
  parseError.value = ''
}

function formatJson() {
  try {
    content.value = JSON.stringify(JSON.parse(content.value || '[]'), null, 2)
    ElMessage.success('已格式化')
  } catch (e: any) {
    ElMessage.error('JSON 格式错误：' + e.message)
  }
}

function validate() {
  try {
    const arr = JSON.parse(content.value || '[]')
    if (!Array.isArray(arr)) throw new Error('根节点必须是数组')
    ElMessage.success(`校验通过，共 ${arr.length} 个一级菜单`)
  } catch (e: any) {
    ElMessage.error('校验失败：' + e.message)
  }
}

async function applyJson() {
  try {
    const arr = JSON.parse(content.value || '[]')
    if (!Array.isArray(arr)) throw new Error('根节点必须是数组')
    nodes.value = decorate(arr)
    selected.value = findFirst(nodes.value)
    ElMessage.success('已应用到可视化编辑器')
  } catch (e: any) {
    ElMessage.error('JSON 格式错误：' + e.message)
  }
}

async function save() {
  syncContent()
  try {
    const arr = JSON.parse(content.value)
    if (!Array.isArray(arr)) throw new Error('根节点必须是数组')
  } catch (e: any) {
    ElMessage.error('JSON 格式错误：' + e.message)
    return
  }
  saving.value = true
  try {
    await request.put('/menus', { target: target.value, content: content.value })
    ElMessage.success('菜单配置已保存，各端下次进入即时生效')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.panel { border: 1px solid #eef0f6; border-radius: 14px; background: #fff; padding: 14px 16px; min-height: 520px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.panel-title { font-weight: 600; font-size: 14px; padding-left: 8px; border-left: 3px solid var(--my-primary); }
.tree-node { display: flex; flex-direction: column; line-height: 1.35; padding: 2px 0; }
.tree-label { font-size: 13px; }
.tree-path { font-size: 11px; color: #9aa0b2; }
.tree-actions { display: flex; gap: 6px; margin-top: 14px; padding-top: 12px; border-top: 1px dashed #eef0f6; }
.prop-form { padding-right: 10px; }
.tip { font-size: 11px; color: #9aa0b2; margin-top: 4px; line-height: 1.5; }
.preview-panel { background: #fbfbfe; }
.phone { width: 250px; margin: 0 auto; border-radius: 28px; background: #191c2e; padding: 10px; box-shadow: 0 18px 40px rgba(30, 26, 60, .18); }
.phone-notch { width: 80px; height: 14px; background: #191c2e; border-radius: 0 0 10px 10px; margin: 0 auto 6px; }
.phone-body { background: #fff; border-radius: 20px; overflow: hidden; min-height: 420px; display: flex; flex-direction: column; }
.phone-brand { display: flex; gap: 8px; align-items: center; padding: 14px 12px 10px; background: linear-gradient(135deg, #f3efff, #fff); }
.brand-logo { width: 30px; height: 30px; border-radius: 9px; background: linear-gradient(135deg, #7b5cff, #c9a5ff); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 14px; }
.brand-name { font-size: 12px; font-weight: 600; }
.brand-sub { font-size: 10px; color: #9aa0b2; margin-top: 2px; }
.phone-content { flex: 1; padding: 10px 12px; }
.ph-card { height: 34px; border-radius: 8px; background: linear-gradient(90deg, #f4f2ff, #faf9ff); margin-bottom: 8px; }
.ph-line { height: 8px; border-radius: 4px; background: #f1f2f7; margin-bottom: 6px; }
.ph-line.short { width: 60%; }
.phone-tabbar { display: flex; border-top: 1px solid #eef0f6; padding: 6px 4px 10px; background: #fff; }
.phone-tabbar.wechat { justify-content: space-around; }
.tab-item { flex: 1; text-align: center; color: #8b90a3; }
.tab-item.active { color: var(--my-primary, #6b4eff); }
.tab-icon { width: 18px; height: 18px; border-radius: 6px; background: currentColor; opacity: .25; margin: 0 auto 3px; }
.tab-item.active .tab-icon { opacity: .9; }
.tab-text { font-size: 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; padding: 0 2px; }
.tab-empty { flex: 1; text-align: center; font-size: 10px; color: #b6bac9; padding: 6px 0; }
.preview-tip { text-align: center; font-size: 11px; color: #9aa0b2; margin-top: 12px; }
.json-editor :deep(textarea) { font-family: Menlo, Consolas, monospace; font-size: 12px; line-height: 1.6; }
</style>
