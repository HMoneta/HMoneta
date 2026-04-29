<script setup>
import {ref, watch, onMounted, onUnmounted, computed} from 'vue';
import PageHeader from "@/components/PageHeader.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import {useLogStore} from "@/stores/log.js";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL.replace('/hm', '');
const wsUrl = `${apiBaseUrl}/hm/logs/stream`;

const logStore = useLogStore();

const activeTab = ref('realtime');
const realtimeLogs = ref([]);
const autoScroll = ref(true);
const virtualScrollerRef = ref(null);

const connectionStatus = ref('disconnected');
let eventSource = null;

const logLevels = ['INFO', 'WARN', 'ERROR', 'DEBUG'];

const statusMap = {
  'CONNECTING': 'pending',
  'OPEN': 'connected',
  'CLOSED': 'disconnected',
  'ERROR': 'error'
};

const connectSse = () => {
  if (eventSource) {
    eventSource.close();
  }

  connectionStatus.value = 'pending';
  eventSource = new EventSource(wsUrl);

  eventSource.onopen = () => {
    connectionStatus.value = 'connected';
  };

  eventSource.onerror = () => {
    connectionStatus.value = 'error';
    setTimeout(() => {
      if (activeTab.value === 'realtime') {
        connectSse();
      }
    }, 3000);
  };

  eventSource.onmessage = (event) => {
    try {
      const logMessage = JSON.parse(event.data);
      if (logMessage.type !== 'connected') {
        let level = logMessage.level ? logMessage.level.toLowerCase() : 'info';
        let levelColor = '#10b981';

        if (level === 'error') levelColor = '#ef4444';
        else if (level === 'warn') levelColor = '#f59e0b';
        else if (level === 'debug') levelColor = '#8b5cf6';

        realtimeLogs.value.push({
          time: new Date(logMessage.timestamp || Date.now()).toLocaleTimeString(),
          level: (logMessage.level || 'INFO').toUpperCase(),
          levelColor,
          service: logMessage.service || '',
          content: logMessage.message || '',
          thread: logMessage.thread || '',
          raw: logMessage
        });

        if (realtimeLogs.value.length > 500) {
          realtimeLogs.value.shift();
        }
        scrollToBottom();
      }
    } catch (e) {
      // ignore parse errors
    }
  };
};

const disconnectSse = () => {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
    connectionStatus.value = 'disconnected';
  }
};

const scrollToBottom = async () => {
  if (autoScroll.value && realtimeLogs.value.length > 0) {
    await new Promise(resolve => setTimeout(resolve, 0));
    if (virtualScrollerRef.value) {
      virtualScrollerRef.value.scrollToIndex(realtimeLogs.value.length - 1);
    }
  }
};

const clearLogs = () => {
  realtimeLogs.value = [];
};

const toggleAutoScroll = () => {
  autoScroll.value = !autoScroll.value;
};

const loadHistoricalLogs = async () => {
  await logStore.queryLogs();
};

const handleFilterChange = () => {
  loadHistoricalLogs();
};

const handlePageChange = (page) => {
  logStore.setPage(page - 1);
  loadHistoricalLogs();
};

const applyDateFilter = (type) => {
  const now = Date.now();
  let startTime = null;

  if (type === '1h') {
    startTime = now - 60 * 60 * 1000;
  } else if (type === '6h') {
    startTime = now - 6 * 60 * 60 * 1000;
  } else if (type === '24h') {
    startTime = now - 24 * 60 * 60 * 1000;
  } else if (type === '7d') {
    startTime = now - 7 * 24 * 60 * 60 * 1000;
  }

  if (startTime) {
    logStore.setFilters({startTime, endTime: now});
  } else {
    logStore.setFilters({startTime: null, endTime: null});
  }
  loadHistoricalLogs();
};

watch(activeTab, (newTab) => {
  if (newTab === 'realtime') {
    connectSse();
    disconnectHistorical();
  } else {
    disconnectSse();
    loadHistoricalLogs();
    logStore.fetchServices();
  }
});

onMounted(() => {
  if (activeTab.value === 'realtime') {
    connectSse();
  } else {
    loadHistoricalLogs();
    logStore.fetchServices();
  }
});

onUnmounted(() => {
  disconnectSse();
});

const disconnectHistorical = () => {
  // no-op for historical
};
</script>

<template>
  <PageHeader
    icon="mdi-file-document-outline"
    title="日志"
    subtitle="查看系统运行日志"
  >
    <template #actions>
      <StatusBadge :status="connectionStatus" class="mr-2"/>
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
        @click="activeTab === 'realtime' ? clearLogs() : () => {}"
      />
    </template>
  </PageHeader>

  <v-tabs v-model="activeTab" grow class="mb-4">
    <v-tab value="realtime">实时日志</v-tab>
    <v-tab value="historical">历史日志</v-tab>
  </v-tabs>

  <v-window v-model="activeTab">
    <!-- 实时日志 Tab -->
    <v-window-item value="realtime">
      <v-card class="log-card">
        <div class="log-toolbar pa-2 d-flex align-center">
          <v-icon size="18" class="mr-2">mdi-console</v-icon>
          <span class="text-body-2">日志输出</span>
          <v-spacer/>
          <span class="text-caption text-medium-emphasis">{{ realtimeLogs.length }} 条记录</span>
        </div>
        <v-virtual-scroll
          ref="virtualScrollerRef"
          :height="500"
          :items="realtimeLogs"
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
    </v-window-item>

    <!-- 历史日志 Tab -->
    <v-window-item value="historical">
      <v-card class="log-card">
        <div class="log-toolbar pa-3 d-flex align-center flex-wrap ga-2">
          <span class="text-caption">时间范围:</span>
          <v-btn size="small" variant="tonal" @click="applyDateFilter('1h')">1小时</v-btn>
          <v-btn size="small" variant="tonal" @click="applyDateFilter('6h')">6小时</v-btn>
          <v-btn size="small" variant="tonal" @click="applyDateFilter('24h')">24小时</v-btn>
          <v-btn size="small" variant="tonal" @click="applyDateFilter('7d')">7天</v-btn>
          <v-btn size="small" variant="tonal" @click="applyDateFilter('all')">全部</v-btn>

          <v-divider vertical class="mx-2"/>

          <v-select
            v-model="logStore.filters.level"
            :items="logLevels"
            label="级别"
            density="compact"
            variant="outlined"
            hide-details
            clearable
            style="max-width: 120px"
            @update:model-value="handleFilterChange"
          />

          <v-select
            v-model="logStore.filters.service"
            :items="logStore.services"
            label="服务"
            density="compact"
            variant="outlined"
            hide-details
            clearable
            style="max-width: 150px"
            @update:model-value="handleFilterChange"
          />

          <v-btn color="primary" @click="loadHistoricalLogs">查询</v-btn>
        </div>

        <v-virtual-scroll
          :height="400"
          :items="logStore.historicalLogs"
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

        <div class="pa-3 d-flex justify-center">
          <v-pagination
            v-model="logStore.pagination.page"
            :length="logStore.pagination.totalPages"
            :total-visible="5"
            @update:model-value="handlePageChange"
          />
        </div>
      </v-card>
    </v-window-item>
  </v-window>
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
