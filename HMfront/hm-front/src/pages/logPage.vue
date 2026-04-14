<script setup>
import {ref, watch, onMounted} from 'vue';
import {useWebSocket} from '@vueuse/core';
import PageHeader from "@/components/PageHeader.vue";
import StatusBadge from "@/components/StatusBadge.vue";

const wsUrl = import.meta.env.VITE_WS_BASE_URL;
const logs = ref([]);
const virtualScrollerRef = ref(null);
const autoScroll = ref(true);

const {status, open, close, send, data} = useWebSocket(wsUrl, {
  autoReconnect: {
    retries: 3,
    delay: 1000
  },
  heartbeat: {
    message: JSON.stringify({action: 'ping'}),
    interval: 30000
  },
  immediate: false,
});

const statusMap = {
  'OPEN': 'connected',
  'CLOSED': 'disconnected',
  'CONNECTING': 'pending',
  'ERROR': 'error'
}

const connectionStatus = computed(() => {
  return statusMap[status.value] || 'unknown';
})

const scrollToBottom = async () => {
  if (autoScroll.value && logs.value.length > 0) {
    await nextTick();
    if (virtualScrollerRef.value) {
      virtualScrollerRef.value.scrollToIndex(logs.value.length - 1);
    }
  }
};

watch(status, (newStatus) => {
  if (newStatus === 'OPEN') {
    send(JSON.stringify({
      action: 'subscribeAll'
    }));
  }
});

watch(data, (newData) => {
  if (newData) {
    try {
      const logMessage = JSON.parse(newData);
      if (logMessage.type !== 'pong') {
        let level = 'info';
        let levelColor = '#10b981';
        let content = logMessage.message || '';

        // System messages (connected, subscribed, etc.)
        if (logMessage.type === 'connected' || logMessage.type === 'subscribed') {
          level = 'system';
          levelColor = '#10b981';
          content = logMessage.message || logMessage.type;
        }
        // Error messages
        else if (logMessage.level === 'ERROR' || logMessage.level === 'WARN') {
          level = logMessage.level.toLowerCase();
          levelColor = logMessage.level === 'ERROR' ? '#ef4444' : '#f59e0b';
        }
        // Log level from backend
        else if (logMessage.level) {
          level = logMessage.level.toLowerCase();
          if (level === 'error') levelColor = '#ef4444';
          else if (level === 'warn') levelColor = '#f59e0b';
          else if (level === 'debug') levelColor = '#8b5cf6';
        }

        logs.value.push({
          time: new Date(logMessage.timestamp || Date.now()).toLocaleTimeString(),
          level: level.toUpperCase(),
          levelColor,
          service: logMessage.service || '',
          content: content,
          thread: logMessage.thread || '',
          raw: logMessage
        });
      }
      if (logs.value.length > 500) {
        logs.value.shift();
      }
      scrollToBottom();
    } catch (error) {
      // Fallback for non-JSON messages
      logs.value.push({
        time: new Date().toLocaleTimeString(),
        level: 'INFO',
        levelColor: '#10b981',
        service: '',
        content: newData,
        thread: '',
        raw: null
      });
      scrollToBottom();
    }
  }
});

const clearLogs = () => {
  logs.value = [];
};

const toggleAutoScroll = () => {
  autoScroll.value = !autoScroll.value;
};

onMounted(() => {
  open();
});
</script>

<template>
  <PageHeader
    icon="mdi-file-document-outline"
    title="实时日志"
    subtitle="查看系统运行日志"
  >
    <template #actions>
      <StatusBadge :status="connectionStatus" class="mr-2" />
      <v-btn
        :icon="autoScroll ? 'mdi-arrow-down-bold' : 'mdi-arrow-up-bold'"
        variant="text"
        size="small"
        @click="toggleAutoScroll"
        :color="autoScroll ? 'primary' : undefined"
      />
      <v-btn
        icon="mdi-delete-outline"
        variant="text"
        size="small"
        @click="clearLogs"
      />
    </template>
  </PageHeader>

  <v-card class="log-card">
    <div class="log-toolbar pa-2 d-flex align-center">
      <v-icon size="18" class="mr-2">mdi-console</v-icon>
      <span class="text-body-2">日志输出</span>
      <v-spacer />
      <span class="text-caption text-medium-emphasis">{{ logs.length }} 条记录</span>
    </div>
    <v-virtual-scroll
      ref="virtualScrollerRef"
      :height="500"
      :items="logs"
      class="log-scroller"
    >
      <template v-slot:default="{ item }">
        <div class="log-entry">
          <span class="log-time">{{ item.time }}</span>
          <span v-if="item.service" class="log-service" :style="{ color: item.levelColor }">{{ item.service }}</span>
          <span class="log-level" :style="{ color: item.levelColor }">[{{ item.level }}]</span>
          <span class="log-content">{{ item.content }}</span>
          <span v-if="item.thread" class="log-thread">{{ item.thread }}</span>
        </div>
      </template>
    </v-virtual-scroll>
  </v-card>
</template>

<style scoped>
.log-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.log-toolbar {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  background: rgb(var(--v-theme-surface)) !important;
}

.log-scroller {
  background: #0a0a0a !important;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.log-entry {
  padding: 4px 16px;
  font-size: 12px;
  line-height: 1.6;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.log-entry:hover {
  background: rgba(16, 185, 129, 0.04);
}

.log-time {
  color: #6b7280;
  flex-shrink: 0;
  font-size: 11px;
  min-width: 70px;
}

.log-service {
  font-weight: 600;
  flex-shrink: 0;
  min-width: 80px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-level {
  font-weight: 700;
  flex-shrink: 0;
  min-width: 55px;
}

.log-content {
  color: #e5e5e5;
  flex: 1;
  word-break: break-all;
  white-space: pre-wrap;
}

.log-thread {
  color: #6b7280;
  flex-shrink: 0;
  font-size: 10px;
  padding: 0 6px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
