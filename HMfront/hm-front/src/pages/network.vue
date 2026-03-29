<script setup>
import {ref, onMounted} from 'vue';
import {http} from "@/common/request.js";
import {userNotificationStore} from '@/stores/app.js';
import PageHeader from "@/components/PageHeader.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import LoadingState from "@/components/LoadingState.vue";
import EmptyState from "@/components/EmptyState.vue";

let notificationStore = userNotificationStore();

const toolbarTitle = ref();
const unifiStatus = ref(false);
const loading = ref(true);
const checkUnifiAccessible = async () => {
  unifiStatus.value = await http.get("/unifi/status")
}
const siteLists = ref();
const siteClientsMap = ref({})
const siteConnectedClientsInfo = ref();


const querySites = async () => {
  const resp = await http.get("/unifi/sites/info")
  siteLists.value = resp.data


  const clientsMap = {}
  for (const site of resp.data) {
    clientsMap[site.id] = await queryClientsBySiteId(site.id)
  }

  siteClientsMap.value = clientsMap

}

const queryClientsBySiteId = async (siteId) => {
  const resp = await http.get("/unifi/sites/clients/info", {'siteId': siteId})
  return resp.data;
}

const clientsHeaders = ref([
  {title: '类型', key: 'type', align: 'center'},
  {title: '设备名称', key: 'name', align: 'center'},
  {title: '连接时间', key: 'connectedAt', align: 'center'},
  {title: 'Ip地址', key: 'ipAddress', align: 'center'},
])

const connectionStatus = computed(() => {
  if (loading.value) return 'pending';
  return unifiStatus.value ? 'connected' : 'disconnected';
})

onMounted(async () => {
  loading.value = true;
  try {
    await checkUnifiAccessible()
    if (unifiStatus.value) {
      toolbarTitle.value = '已连接Unifi'
      await querySites()
    } else {
      notificationStore.showError("未配置UnifiApi相关信息，无法连接至Unifi")
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
    subtitle="查看 Unifi 网络中的已连接客户端"
  >
    <template #actions>
      <StatusBadge :status="connectionStatus" />
    </template>
  </PageHeader>

  <LoadingState v-if="loading" message="正在连接 Unifi..." />

  <EmptyState
    v-else-if="!unifiStatus"
    icon="mdi-network-off"
    title="未连接到 Unifi"
    description="请在设置中配置 Unifi API 信息"
  />

  <template v-else>
    <v-card
      v-for="site in siteLists"
      :key="site.id"
      class="site-card mb-4"
    >
      <v-toolbar class="site-toolbar" density="compact">
        <template v-slot:prepend>
          <v-icon start>mdi-router-network</v-icon>
        </template>
        <v-toolbar-title class="font-weight-medium">{{ site.name || site.desc }}</v-toolbar-title>
        <v-chip size="small" variant="tonal" color="success">
          {{ siteClientsMap[site.id]?.length || 0 }} 台设备
        </v-chip>
      </v-toolbar>
      <v-data-table
        :headers="clientsHeaders"
        :items="siteClientsMap[site.id] || []"
        item-value="name"
        :items-per-page="10"
        class="clients-table"
      >
        <template v-slot:item.type="{ item }">
          <v-icon :icon="item.type === 'wired' ? 'mdi-ethernet' : 'mdi-wifi'" size="20" />
        </template>
      </v-data-table>
    </v-card>
  </template>
</template>

<style scoped>
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
