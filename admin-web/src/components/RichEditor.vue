<template>
  <div class="rich-editor">
    <Toolbar class="re-toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <Editor
      class="re-body"
      :style="{ height: height + 'px' }"
      :default-config="editorConfig"
      :model-value="modelValue"
      mode="default"
      @on-created="handleCreated"
      @on-change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, shallowRef } from 'vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'

const props = withDefaults(defineProps<{ modelValue?: string; height?: number; placeholder?: string }>(), {
  modelValue: '',
  height: 340,
  placeholder: '请输入内容，支持加粗、标题、列表、图片、链接等'
})

const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const editorRef = shallowRef<IDomEditor>()

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['group-video', 'fullScreen']
}

function authHeaders(): Record<string, string> {
  const headers: Record<string, string> = {}
  const token = localStorage.getItem('meiye_token')
  if (token) headers.Authorization = 'Bearer ' + token
  const drill = localStorage.getItem('meiye_drill_tenant')
  if (drill) headers['X-Tenant-Id'] = drill
  return headers
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      server: '/api/files/upload',
      fieldName: 'file',
      maxFileSize: 10 * 1024 * 1024,
      allowedFileTypes: ['image/*'],
      headers: authHeaders(),
      meta: { dir: 'cms' },
      customInsert(res: any, insertFn: (url: string, alt: string, href: string) => void) {
        const url = res?.data?.url
        if (url) insertFn(url, res?.data?.name || 'image', url)
      }
    }
  }
}

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

function handleChange(editor: IDomEditor) {
  emit('update:modelValue', editor.getHtml())
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<style scoped>
.rich-editor {
  width: 100%;
  border: 1px solid #e4e7f0;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  z-index: 10;
}
.re-toolbar { border-bottom: 1px solid #eef0f6; }
.re-body { overflow-y: auto; }
:deep(.w-e-text-container) { background: #fff; }
:deep(.w-e-text-placeholder) { font-style: normal; color: #b6bac9; }
</style>
