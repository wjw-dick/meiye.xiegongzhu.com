<template>
  <div class="image-upload">
    <div
      class="drop-zone"
      :class="{ dragging, disabled: reachedLimit }"
      @click="trigger"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
    >
      <el-icon class="up-icon"><UploadFilled /></el-icon>
      <div class="up-title">点击或拖拽图片到此处上传</div>
      <div class="up-tip">{{ tip || `支持 JPG / PNG / WEBP，单张不超过 ${maxSize}MB${multiple ? '，最多 ' + limit + ' 张' : ''}` }}</div>
      <input ref="inputRef" type="file" :accept="accept" :multiple="multiple" hidden @change="onPick" />
    </div>

    <div v-if="items.length" class="thumb-list">
      <div v-for="(it, idx) in items" :key="it.uid" class="thumb-card">
        <div class="thumb-box" @click="preview(it)">
          <img v-if="it.url && !it.fileType?.includes('pdf')" :src="it.url" :alt="it.name" />
          <div v-else class="file-placeholder">
            <el-icon><Document /></el-icon>
            <span>{{ it.name }}</span>
          </div>
          <div v-if="it.status === 'uploading'" class="mask">
            <el-progress type="circle" :percentage="it.percent" :width="52" :stroke-width="6" color="#6b4eff" />
          </div>
          <div v-else class="mask hover-mask">
            <el-icon><ZoomIn /></el-icon>
            <span>预览</span>
          </div>
        </div>
        <div class="thumb-meta" :title="it.name">{{ it.name }}</div>
        <div class="thumb-actions">
          <el-button v-if="multiple" link :disabled="idx === 0" @click="move(idx, -1)"><el-icon><ArrowUp /></el-icon></el-button>
          <el-button v-if="multiple" link :disabled="idx === items.length - 1" @click="move(idx, 1)"><el-icon><ArrowDown /></el-icon></el-button>
          <el-button link type="danger" @click="remove(idx)"><el-icon><Delete /></el-icon></el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="previewVisible" :title="previewItem?.name || '预览'" width="720px" append-to-body>
      <div class="preview-body">
        <img v-if="previewItem && !previewItem.fileType?.includes('pdf')" :src="previewItem.url" />
        <iframe v-else-if="previewItem" :src="previewItem.url" style="width: 100%; height: 60vh; border: 0" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Delete, ZoomIn, ArrowUp, ArrowDown, Document } from '@element-plus/icons-vue'
import request from '@/api/request'

interface Item {
  uid: number
  name: string
  size: number
  url: string
  percent: number
  status: 'uploading' | 'done' | 'error'
  fileType?: string
}

const props = withDefaults(defineProps<{
  modelValue?: string | string[]
  multiple?: boolean
  limit?: number
  maxSize?: number
  accept?: string
  dir?: string
  tip?: string
}>(), {
  modelValue: '',
  multiple: false,
  limit: 9,
  maxSize: 10,
  accept: 'image/*',
  dir: 'common',
  tip: ''
})

const emit = defineEmits<{ (e: 'update:modelValue', v: string | string[]): void }>()

const inputRef = ref<HTMLInputElement>()
const items = ref<Item[]>([])
const dragging = ref(false)
const previewVisible = ref(false)
const previewItem = ref<Item | null>(null)
let uidSeed = 1

const reachedLimit = computed(() => !props.multiple || items.value.filter(i => i.status !== 'error').length >= props.limit)

function syncFromModel() {
  const val = props.modelValue
  const list = Array.isArray(val) ? val : (val ? [val] : [])
  const current = items.value.map(i => i.url).filter(Boolean)
  if (list.length === current.length && list.every((u, i) => u === current[i])) return
  items.value = list.map((url) => ({
    uid: uidSeed++,
    name: decodeURIComponent(url.split('/').pop() || 'image'),
    size: 0,
    url,
    percent: 100,
    status: 'done' as const
  }))
}

watch(() => props.modelValue, syncFromModel, { immediate: true, deep: true })

function emitValue() {
  const urls = items.value.filter(i => i.status === 'done' && i.url).map(i => i.url)
  emit('update:modelValue', props.multiple ? urls : (urls[0] || ''))
}

function trigger() {
  if (reachedLimit.value) {
    ElMessage.warning(props.multiple ? `最多上传 ${props.limit} 张` : '请先移除当前图片')
    return
  }
  inputRef.value?.click()
}

function onPick(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (files) uploadFiles(Array.from(files))
  ;(e.target as HTMLInputElement).value = ''
}

function onDrop(e: DragEvent) {
  dragging.value = false
  if (reachedLimit.value) return
  const files = e.dataTransfer?.files
  if (files) uploadFiles(Array.from(files))
}

async function uploadFiles(files: File[]) {
  for (const file of files) {
    if (props.multiple && items.value.filter(i => i.status !== 'error').length >= props.limit) {
      ElMessage.warning(`最多上传 ${props.limit} 张`)
      break
    }
    if (file.size > props.maxSize * 1024 * 1024) {
      ElMessage.error(`${file.name} 超过 ${props.maxSize}MB`)
      continue
    }
    const item: Item = {
      uid: uidSeed++,
      name: file.name,
      size: file.size,
      url: '',
      percent: 0,
      status: 'uploading',
      fileType: file.type
    }
    items.value.push(item)
    const fd = new FormData()
    fd.append('file', file)
    fd.append('dir', props.dir)
    try {
      const res: any = await request.post('/files/upload', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e: any) => {
          item.percent = e.total ? Math.round((e.loaded / e.total) * 100) : 0
        }
      })
      item.url = res.data?.url || ''
      item.percent = 100
      item.status = 'done'
      emitValue()
    } catch (err) {
      item.status = 'error'
      ElMessage.error(`${file.name} 上传失败`)
    }
  }
  if (!props.multiple) {
    // 单选只保留最新一张
    const done = items.value.filter(i => i.status === 'done')
    if (done.length > 1) {
      items.value = [done[done.length - 1]]
      emitValue()
    }
  }
}

function remove(idx: number) {
  items.value.splice(idx, 1)
  emitValue()
}

function move(idx: number, delta: number) {
  const target = idx + delta
  if (target < 0 || target >= items.value.length) return
  const list = items.value
  ;[list[idx], list[target]] = [list[target], list[idx]]
  emitValue()
}

function preview(it: Item) {
  if (!it.url) return
  previewItem.value = it
  previewVisible.value = true
}
</script>

<style scoped>
.image-upload { width: 100%; }
.drop-zone {
  border: 1.5px dashed #d6d9e6; border-radius: 12px; background: #fafbff;
  padding: 18px 16px; text-align: center; cursor: pointer; transition: all .18s ease;
}
.drop-zone:hover, .drop-zone.dragging { border-color: var(--my-primary, #6b4eff); background: #f4f1ff; }
.drop-zone.disabled { cursor: not-allowed; opacity: .6; }
.up-icon { font-size: 30px; color: var(--my-primary, #6b4eff); }
.up-title { font-size: 13px; color: #4b5163; margin-top: 6px; font-weight: 500; }
.up-tip { font-size: 11px; color: #9aa0b2; margin-top: 4px; }
.thumb-list { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 14px; }
.thumb-card { width: 132px; }
.thumb-box {
  position: relative; width: 132px; height: 132px; border-radius: 10px; overflow: hidden;
  background: #f5f6fa; border: 1px solid #e8eaf2; cursor: pointer;
}
.thumb-box img { width: 100%; height: 100%; object-fit: cover; display: block; }
.file-placeholder {
  height: 100%; display: flex; flex-direction: column; gap: 6px; align-items: center;
  justify-content: center; color: #8b90a3; font-size: 11px; padding: 8px; text-align: center;
}
.file-placeholder .el-icon { font-size: 30px; }
.mask {
  position: absolute; inset: 0; background: rgba(17, 20, 34, .55); color: #fff;
  display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 12px;
}
.hover-mask { opacity: 0; transition: opacity .16s ease; }
.thumb-box:hover .hover-mask { opacity: 1; }
.thumb-meta {
  font-size: 11px; color: #7d8398; margin-top: 6px; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis;
}
.thumb-actions { display: flex; justify-content: center; gap: 2px; }
.thumb-actions .el-button { padding: 2px 4px; }
.preview-body { text-align: center; }
.preview-body img { max-width: 100%; max-height: 65vh; border-radius: 8px; }
</style>
