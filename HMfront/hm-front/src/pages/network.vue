<script setup>
import {ref, onMounted, computed} from 'vue';
import {http} from "@/common/request.js";
import {userNotificationStore} from '@/stores/app.js';
import PageHeader from "@/components/PageHeader.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import LoadingState from "@/components/LoadingState.vue";
import EmptyState from "@/components/EmptyState.vue";

let notificationStore = userNotificationStore();

const toolbarTitle = ref();
const leiChiStatus = ref(false);
const loading = ref(true);
const devices = ref([]);

const checkLeiChiAccessible = async () => {
  try {
    // 尝试调用接口，如果能返回数据则认为已配置
    // 注意：后端直接返回数组，不是 {data: [...]} 格式
    const resp = await http.get("/network/devices")
    leiChiStatus.value = true
    devices.value = resp || []
  } catch (e) {
    leiChiStatus.value = false
  }
}

const clientsHeaders = ref([
  {title: '类型', key: 'type', align: 'center'},
  {title: '设备名称', key: 'name', align: 'center'},
  {title: 'Ip地址', key: 'ipAddress', align: 'center'},
  {title: 'WAF站点', key: 'title', align: 'center'},
  {title: '服务状态', key: 'exposed', align: 'center'},
])

const connectionStatus = computed(() => {
  if (loading.value) return 'pending';
  return leiChiStatus.value ? 'connected' : 'disconnected';
})

const exposedCount = computed(() => {
  // 对外服务：leiChiId 不为空且站点已启用
  return devices.value.filter(d => d.leiChiId != null && d.isEnabled).length
})

const notExposedCount = computed(() => {
  // 非对外服务：没有匹配站点或站点未启用
  return devices.value.filter(d => d.leiChiId == null || !d.isEnabled).length
})

const getExposureBadge = (item) => {
  if (item.leiChiId != null && item.isEnabled) {
    return {color: 'success', text: '对外服务', icon: 'mdi-web'}
  }
  return {color: 'warning', text: '非对外服务', icon: 'mdi-web-off'}
}

onMounted(async () => {
  loading.value = true;
  try {
    await checkLeiChiAccessible()
    if (leiChiStatus.value) {
      toolbarTitle.value = `已连接雷池 (${exposedCount.value}/${devices.value.length} 对外服务)`
    } else {
      notificationStore.showError("未配置雷池API信息，无法获取网络设备")
    }
  } finally {
    loading.value = false;
  }
})
</script>

<template>
  <PageHeader
    icon="mdi-network"
    title="网络设备"
    subtitle="查看 UniFi 网络中的已连接客户端及 WAF 保护状态"
  >
    <template #actions>
      <StatusBadge :status="connectionStatus" />
    </template>
  </PageHeader>

  <LoadingState v-if="loading" message="正在获取网络设备..." />

  <EmptyState
    v-else-if="!leiChiStatus"
    icon="mdi-network-off"
    title="未连接到雷池"
    description="请在设置中配置雷池 API 信息"
  />

  <template v-else>
    <!-- 统计卡片 -->
    <v-row class="mb-4">
      <v-col cols="6" md="3">
        <v-card class="stat-card">
          <v-card-text class="d-flex align-center">
            <v-icon icon="mdi-devices" color="primary" size="32" class="mr-3" />
            <div>
              <div class="text-h5 font-weight-bold">{{ devices.length }}</div>
              <div class="text-caption text-medium-emphasis">设备总数</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="6" md="3">
        <v-card class="stat-card">
          <v-card-text class="d-flex align-center">
            <v-icon icon="mdi-shield-check" color="success" size="32" class="mr-3" />
            <div>
              <div class="text-h5 font-weight-bold text-success">{{ exposedCount }}</div>
              <div class="text-caption text-medium-emphasis">对外服务数</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="6" md="3">
        <v-card class="stat-card">
          <v-card-text class="d-flex align-center">
            <v-icon icon="mdi-shield-alert" color="error" size="32" class="mr-3" />
            <div>
              <div class="text-h5 font-weight-bold text-warning">{{ notExposedCount }}</div>
              <div class="text-caption text-medium-emphasis">非对外服务</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="6" md="3">
        <v-card class="stat-card">
          <v-card-text class="d-flex align-center">
            <v-icon icon="mdi-shield" color="info" size="32" class="mr-3" />
            <div>
              <div class="text-h5 font-weight-bold text-info">{{ devices.length > 0 ? Math.round(exposedCount / devices.length * 100) : 0 }}%</div>
              <div class="text-caption text-medium-emphasis">对外服务率</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- 设备列表 -->
    <v-card class="site-card">
      <v-toolbar class="site-toolbar" density="compact">
        <template v-slot:prepend>
          <v-icon start>mdi-devices</v-icon>
        </template>
        <v-toolbar-title class="font-weight-medium">网络设备列表</v-toolbar-title>
      </v-toolbar>
      <v-data-table
        :headers="clientsHeaders"
        :items="devices"
        item-value="id"
        :items-per-page="10"
        class="clients-table"
      >
        <template v-slot:item.type="{ item }">
          <v-icon :icon="item.type === 'wired' ? 'mdi-ethernet' : 'mdi-wifi'" size="20" />
        </template>
        <template v-slot:item.title="{ item }">
          <span v-if="item.title">{{ item.title }}</span>
          <span v-else class="text-medium-emphasis">-</span>
        </template>
        <template v-slot:item.exposed="{ item }">
          <v-chip
            :color="getExposureBadge(item).color"
            variant="tonal"
            size="small"
          >
            <v-icon start size="14" :icon="getExposureBadge(item).icon" />
            {{ getExposureBadge(item).text }}
          </v-chip>
        </template>
      </v-data-table>
    </v-card>
  </template>
</template>

<style scoped>
.stat-card {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: box-shadow var(--transition-normal), border-color var(--transition-normal);
}

.stat-card:hover {
  border-color: rgba(16, 185, 129, 0.2);
  box-shadow: var(--shadow-glow);
}

.site-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: box-shadow var(--transition-normal), border-color var(--transition-normal);
}

.site-card:hover {
  border-color: rgba(16, 185, 129, 0.2);
  box-shadow: var(--shadow-glow);
}

.site-toolbar {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  background: rgb(var(--v-theme-surface)) !important;
}

.clients-table {
  background: transparent !important;
}

.clients-table :deep(tr:hover) {
  background-color: rgba(16, 185, 129, 0.04) !important;
}
</style>
