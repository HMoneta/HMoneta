<template>
  <v-chip
    :color="statusConfig.color"
    :size="size"
    :variant="variant"
  >
    <v-icon
      v-if="statusConfig.icon"
      :icon="statusConfig.icon"
      start
      :size="iconSize"
    />
    {{ statusConfig.text || label }}
  </v-chip>
</template>

<script setup>
import {computed} from 'vue'

const props = defineProps({
  status: {
    type: String,
    default: 'unknown'
  },
  label: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'small'
  },
  variant: {
    type: String,
    default: 'tonal'
  }
})

const statusConfigs = {
  success: { color: 'success', icon: 'mdi-check-circle', text: '正常' },
  error: { color: 'error', icon: 'mdi-alert-circle', text: '错误' },
  warning: { color: 'warning', icon: 'mdi-alert', text: '警告' },
  info: { color: 'info', icon: 'mdi-information', text: '信息' },
  pending: { color: 'grey', icon: 'mdi-clock-outline', text: '等待' },
  connected: { color: 'success', icon: 'mdi-link', text: '已连接' },
  disconnected: { color: 'error', icon: 'mdi-link-off', text: '未连接' },
  active: { color: 'success', icon: 'mdi-check', text: '活跃' },
  inactive: { color: 'grey', icon: 'mdi-minus', text: '未激活' },
  enabled: { color: 'success', icon: 'mdi-check', text: '已启用' },
  disabled: { color: 'grey', icon: 'mdi-close', text: '已禁用' },
  unknown: { color: 'grey', icon: 'mdi-help-circle', text: '未知' }
}

const statusConfig = computed(() => {
  return statusConfigs[props.status] || statusConfigs.unknown
})

const iconSize = computed(() => {
  return props.size === 'small' ? 12 : props.size === 'default' ? 16 : 20
})
</script>
