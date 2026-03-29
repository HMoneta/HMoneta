<script setup>
import {ref} from 'vue'
import {http} from "@/common/request.js";
import {userNotificationStore} from "@/stores/app.js";
import PageHeader from "@/components/PageHeader.vue";

const tab = ref(null)

// acme设置
const acmeInfo = reactive({
  id: null,
  userEmail: null,
})
const acmeLoading = ref(false);
const onSubmit = async () => {
  acmeLoading.value = true;
  try {
    await http.post("/acme/modify", acmeInfo);
    userNotificationStore().showSuccess("设置保存成功")
  } finally {
    acmeLoading.value = false;
  }
}

// unifi
const unifiInfo = reactive({
  baseUri: null,
  apiKey: null,
  localBaseUri: null,
  localApiKey: null,
})

const unifiLoading = ref(false);

const queryUnifiInfo = async () => {
  try {
    const json = await http.get("/unifi/info")
    if (json != null) {
      unifiInfo.baseUri = json.baseUri;
      unifiInfo.apiKey = json.apiKey;
      unifiInfo.localApiKey = json.localApiKey
      unifiInfo.localBaseUri = json.localBaseUri
    }
  } catch (_) {

  }
}

const onUnifiSubmit = async () => {
  unifiLoading.value = true;
  try {
    await http.post("/unifi/setting", unifiInfo);
    userNotificationStore().showSuccess("设置保存成功")
  } catch (err) {

  } finally {
    unifiLoading.value = false;
  }
}

// LeiChi
const leichi = reactive({
  baseUrl: '',
  token: ''
})
const leichiLoading = ref(false);
const queryLeiChiToken = async () => {
  try {
    const resp = await http.get("/waf/lc/info");
    leichi.token = resp.token
    leichi.baseUrl = resp.baseUrl
  } catch (err) {
  }
}
const onLeiChiSubmit = async () => {
  leichiLoading.value = true;
  try {
    await http.post("/waf/lc/info", leichi);
    userNotificationStore().showSuccess("设置保存成功")
  } catch (err) {

  } finally {
    leichiLoading.value = false;
  }
}
onMounted(async () => {
  await queryUnifiInfo();
  await queryLeiChiToken();
})
</script>

<template>
  <PageHeader
    icon="mdi-cog-outline"
    title="系统设置"
    subtitle="配置 ACME、Unifi API 和 LeiChi WAF"
  />

  <v-card class="settings-card">
    <v-tabs v-model="tab" color="primary" align-tabs="start">
      <v-tab value="network" class="settings-tab">
        <v-icon start size="18">mdi-router-network</v-icon>
        网络配置
      </v-tab>
      <v-tab value="api" class="settings-tab">
        <v-icon start size="18">mdi-api</v-icon>
        外部Api
      </v-tab>
    </v-tabs>

    <v-divider />

    <v-tabs-window v-model="tab">
      <v-tabs-window-item value="network">
        <v-sheet class="pa-6">
          <v-card class="setting-section-card mb-4" flat>
            <div class="section-header d-flex align-center mb-4">
              <v-icon color="primary" class="mr-2">mdi-shield-account</v-icon>
              <h3 class="text-h6">ACME 配置</h3>
            </div>
            <p class="text-body-2 text-medium-emphasis mb-4">
              配置 Let's Encrypt 账号邮箱，用于自动申请 SSL 证书
            </p>
            <v-row align="center">
              <v-col cols="12" sm="9">
                <v-text-field
                  clearable
                  label="Let's Encrypt 账号邮箱"
                  variant="outlined"
                  v-model="acmeInfo.userEmail"
                  prepend-inner-icon="mdi-email-outline"
                />
              </v-col>
              <v-col cols="12" sm="3" class="d-flex justify-start">
                <v-btn
                  color="primary"
                  @click="onSubmit"
                  :loading="acmeLoading"
                  variant="elevated"
                >
                  <v-icon start>mdi-content-save</v-icon>
                  保存
                </v-btn>
              </v-col>
            </v-row>
          </v-card>
        </v-sheet>
      </v-tabs-window-item>

      <v-tabs-window-item value="api">
        <v-sheet class="pa-6">
          <v-card class="setting-section-card mb-4" flat>
            <div class="section-header d-flex align-center mb-4">
              <v-icon color="primary" class="mr-2">mdi-router-wireless</v-icon>
              <h3 class="text-h6">Unifi API 设置</h3>
            </div>
            <p class="text-body-2 text-medium-emphasis mb-4">
              配置 Unifi Controller API 以连接网络设备
            </p>

            <v-row>
              <v-col cols="12" sm="6">
                <v-text-field
                  clearable
                  label="Unifi API BaseUrl (云端)"
                  variant="outlined"
                  v-model="unifiInfo.baseUri"
                  placeholder="https://unifi.ui.com"
                  prepend-inner-icon="mdi-cloud"
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-text-field
                  clearable
                  label="Unifi API Key (云端)"
                  variant="outlined"
                  v-model="unifiInfo.apiKey"
                  prepend-inner-icon="mdi-key"
                />
              </v-col>
            </v-row>

            <v-row>
              <v-col cols="12" sm="6">
                <v-text-field
                  clearable
                  label="Unifi API BaseUrl (本地)"
                  variant="outlined"
                  v-model="unifiInfo.localBaseUri"
                  placeholder="https://192.168.1.1:8443"
                  prepend-inner-icon="mdi-server-network"
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-text-field
                  clearable
                  label="Unifi API Key (本地)"
                  variant="outlined"
                  v-model="unifiInfo.localApiKey"
                  prepend-inner-icon="mdi-key-variant"
                />
              </v-col>
            </v-row>

            <v-btn
              color="primary"
              @click="onUnifiSubmit"
              :loading="unifiLoading"
              variant="elevated"
            >
              <v-icon start>mdi-content-save</v-icon>
              保存
            </v-btn>
          </v-card>

          <v-card class="setting-section-card" flat>
            <div class="section-header d-flex align-center mb-4">
              <v-icon color="primary" class="mr-2">mdi-fire</v-icon>
              <h3 class="text-h6">LeiChi WAF 设置</h3>
            </div>
            <p class="text-body-2 text-medium-emphasis mb-4">
              配置 LeiChi Web 应用防火墙连接信息
            </p>

            <v-row align="center">
              <v-col cols="12" sm="6">
                <v-text-field
                  clearable
                  label="BaseUri"
                  variant="outlined"
                  v-model="leichi.baseUrl"
                  prepend-inner-icon="mdi-link"
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-text-field
                  clearable
                  label="ApiToken"
                  variant="outlined"
                  v-model="leichi.token"
                  prepend-inner-icon="mdi-token"
                />
              </v-col>
            </v-row>

            <v-btn
              color="primary"
              @click="onLeiChiSubmit"
              :loading="leichiLoading"
              variant="elevated"
            >
              <v-icon start>mdi-content-save</v-icon>
              保存
            </v-btn>
          </v-card>
        </v-sheet>
      </v-tabs-window-item>
    </v-tabs-window>
  </v-card>
</template>

<style scoped>
.settings-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.settings-tab {
  min-width: 120px;
}

.setting-section-card {
  background-color: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 24px;
  transition: border-color var(--transition-normal);
}

.setting-section-card:hover {
  border-color: rgba(16, 185, 129, 0.2);
}

.section-header h3 {
  font-weight: 600;
}
</style>
