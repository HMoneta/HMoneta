<script setup>
import {http} from "@/common/request.js";

// Unifi相关代码
const toolbarTitle = ref();
const unifiStatus = ref(false);
const checkUnifiAccessible = async () => {
  return await http.get("/unifi/status")
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
onMounted(() => {
  unifiStatus.value = checkUnifiAccessible()
  if (unifiStatus.value) {
    toolbarTitle.value = '已连接Unifi'
    querySites()
  }

})
</script>

<template>

  <v-card class="mt-6" v-for="site in siteLists" :key="site.id">
    <v-toolbar density="compact" :elevation="8" :title="toolbarTitle"></v-toolbar>
    <v-data-table
      :headers="clientsHeaders"
      :items="siteClientsMap[site.id] || []"
      item-value="name"
      :items-per-page="10"
    ></v-data-table>

  </v-card>
</template>

<style scoped lang="sass">

</style>
