<script setup>

import {http} from "@/common/request.js";

const loading = ref(false);
const step = ref(1)
const dnsProviderDic = ref({})
const options = ref([])
const selected = ref()
const providerDesc = ref([])

const providerAuth = reactive({id: null, key: null})

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

const handleSubmit = () => {
  
}
// 监听DNS服务商选择变化
watch(selected, (newVal) => {
  if (newVal) {
    let _provider = dnsProviderDic.value[newVal]
    providerDesc.value.push({text: "服务商ID：" + _provider.id, icon: "mdi-account"})
    providerDesc.value.push({text: "服务商插件更新时间：" + _provider.updatedAt, icon: "mdi-clock"})
    console.log(_provider)
  }

})

const dialog = ref(false)
const addDns = () => {
  dialog.value = true
}

onMounted(() => {
  queryDnsProvider()
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
    <v-stepper
      width="1200px"
      v-model="step"
      :items="['选择DNS服务商', '服务商账号', '需解析的域名']">
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
        <v-card title="授权信息" flat>
          <v-text-field v-model="providerAuth.id" clearable label="授权ID" variant="outlined"></v-text-field>
          <v-text-field v-model="providerAuth.key" clearable label="授权Key" variant="outlined"></v-text-field>
        </v-card>
      </template>

      <template v-slot:item.3>
        <v-card title="Step Three" flat>...</v-card>
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
  </v-dialog>
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
