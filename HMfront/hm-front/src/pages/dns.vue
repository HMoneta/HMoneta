<script setup>

import {http} from "@/common/request.js";
import {userNotificationStore} from '@/stores/app.js';

let notificationStore = userNotificationStore();
const loading = ref(false);
const step = ref(1)
const dnsProviderDic = ref({})
const options = ref([])
const selected = ref()
const providerDesc = ref([])
const urls = ref()
const dialog = ref(false)

const dnsGroup = reactive({
  providerId: null,
  groupName: null,
  authId: null,
  authKey: null,
  urls: null
})

const dnsGroups = ref()

// Table
const headers = ref([
  {title: '网址', key: 'url', align: 'center'},
  {title: '解析状态', key: 'resolveStatus', align: 'center'},
  {title: 'Ip地址', key: 'ipAddress', align: 'center'},
  {title: '创建时间', key: 'createTime', align: 'center'},
  {title: '更新时间', key: 'updateTime', align: 'center'},
  {title: '操作', key: 'action', align: 'center'},
])


const addDns = () => {
  dialog.value = true
}
const queryDnsProvider = () => {
  http.get('/dns/query_all').then(resp => {
    if (resp) {
      dnsProviderDic.value = {}
      for (const element of resp) {
        dnsProviderDic.value[element.providerName] = element
        options.value.push(element.providerName)
      }
    }
  })
}

const handleSubmit = async () => {
  try {
    await http.post('/dns/insert_group', dnsGroup)
    notificationStore.showSuccess("分组添加成功")
  } catch (err) {
    console.log(err)
  }
}

const queryAllDnsResolveInfo = async () => {
  const resp = await http.get('/dns/resolve_info')
  dnsGroups.value = resp
}

const deleteItem = async (item) => {
  try {
    await http.post('/dns/delete', item.id)
    notificationStore.showSuccess("删除" + item.url + "DNS解析成功")
    queryAllDnsResolveInfo()
  } catch (err) {
    console.log(err)
  }
}

// 监听DNS服务商选择变化
watch(selected, (newVal) => {
  if (newVal) {
    let _provider = dnsProviderDic.value[newVal]
    providerDesc.value.push({text: "服务商ID：" + _provider.id, icon: "mdi-account"})
    dnsGroup.providerId = _provider.id
    providerDesc.value.push({text: "服务商插件更新时间：" + _provider.updatedAt, icon: "mdi-clock"})
  }

})

watch(urls, (newVal) => {
  if (newVal) {
    dnsGroup.urls = urls.value.split('\n')
  }
})


onMounted(() => {
  queryDnsProvider()
  queryAllDnsResolveInfo()
})
</script>

<template>
  <v-btn @click="addDns">
    增加DNS解析组
  </v-btn>
  <v-dialog
    v-model="dialog"
    width="auto"
    persistent
  >
    <v-card>
      <v-toolbar>
        <v-btn
          icon="mdi-close"
          @click="dialog = false"
        ></v-btn>
        <v-toolbar-title>创建解析组</v-toolbar-title>
      </v-toolbar>
      <v-stepper
        width="1200px"
        v-model="step"
        :items="['选择DNS服务商', '分组及服务商基本信息', '需解析的域名']">
        <template v-slot:item.1>
          <v-card>
            <v-container>
              <v-row no-gutters>
                <v-col
                  class="d-flex align-center"
                  cols="12"
                  sm="5"
                >
                  <v-sheet class="ma-2 pa-2 w-100">
                    <v-select
                      v-model="selected"
                      label="选择DNS服务商"
                      :items="options"
                      variant="solo-inverted"
                    />
                  </v-sheet>
                </v-col>
                <v-col
                  class="d-flex align-center"
                  cols="12"
                  sm="7"
                >
                  <v-sheet
                    class="ma-2 pa-2 w-100"
                  >
                    <v-list density="compact">
                      <v-list-subheader>DNS服务商简介</v-list-subheader>

                      <v-list-item
                        v-for="(item, i) in providerDesc"
                        :key="i"
                        :value="item"
                        color="primary"
                      >
                        <template v-slot:prepend>
                          <v-icon :icon="item.icon"></v-icon>
                        </template>

                        <v-list-item-title v-text="item.text"></v-list-item-title>
                      </v-list-item>
                    </v-list>
                  </v-sheet>
                </v-col>
              </v-row>
            </v-container>
          </v-card>

        </template>

        <template v-slot:item.2>
          <v-card title="分组及服务商基本信息" flat>
            <v-text-field v-model="dnsGroup.groupName" clearable label="分组名称" variant="outlined"/>
            <v-text-field v-model="dnsGroup.authId" clearable label="服务商授权ID" variant="outlined"></v-text-field>
            <v-text-field v-model="dnsGroup.authKey" clearable label="服务商授权Key" variant="outlined"></v-text-field>
          </v-card>
        </template>

        <template v-slot:item.3>
          <v-card title="需解析网址" flat>
            <v-textarea v-model="urls" label="输入需解析的网址，每行一个网址" variant="outlined"></v-textarea>
          </v-card>
        </template>
        <template v-slot:actions>
          <div class="stepper-actions">
            <v-btn
              v-if="step >= 1"
              variant="plain"
              size="large"
              :ripple="true"
              class="stepper-btn stepper-btn-prev"
              :disabled="step === 1"
              @click="step--"
            >
              上一步
            </v-btn>

            <v-spacer></v-spacer>

            <v-btn
              v-if="step < 3"
              variant="elevated"
              :ripple="true"
              size="large"
              class="stepper-btn stepper-btn-next"
              @click="step++"
            >
              下一步
            </v-btn>

            <v-btn
              v-else
              variant="elevated"
              :ripple="true"
              color="success"
              size="large"
              class="stepper-btn stepper-btn-submit"
              :loading="loading"
              @click="handleSubmit"
            >
              创建解析组
            </v-btn>
          </div>
        </template>

      </v-stepper>
    </v-card>

  </v-dialog>
  <v-card class="mt-5" v-for="group in dnsGroups" :key="group.id" :title="group.groupName">
    <v-data-table-server
      :headers="headers"
      :items="group.urls"
      item-value="name"
    >
      <template v-slot:item.resolveStatus="{ item }">
        <span v-if="item.resolveStatus === 0"><v-badge inline location="top right" color="error"
                                                       content="解析失败"/></span>
        <span v-else><v-badge inline location="top right" color="success" content="解析成功"/></span>
      </template>
      <template v-slot:item.action="{ item }">
        <v-btn
          icon="mdi-pencil"
          size="small"
          variant="text"
          @click="editItem(item)"
        ></v-btn>
        <v-btn
          icon="mdi-delete"
          size="small"
          variant="text"
          color="error"
          @click="deleteItem(item)"
        ></v-btn>
      </template>

    </v-data-table-server>

  </v-card>
</template>

<style scoped>
.stepper-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 20px;
  background-color: #2a2a2a;
}

.stepper-btn {
  min-width: 100px;
  height: 40px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  border-radius: 8px;
}

.stepper-btn-prev {
  background-color: #2a2a2a !important;
  color: white !important;
  border: 1px solid #4a4a4a;
}

.stepper-btn-prev:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.stepper-btn-prev:hover:not(:disabled) {
  background-color: #3a3a3a !important;
}

.stepper-btn-next,
.stepper-btn-submit {
  margin-left: auto; /* 确保右侧按钮靠右 */
}

</style>
