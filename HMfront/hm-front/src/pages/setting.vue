<script setup>
import {ref} from 'vue'
import {http} from "@/common/request.js";
import {userNotificationStore} from "@/stores/app.js";

const tab = ref(null)
// acme设置
const acmeInfo = reactive({
  id: null,
  userEmail: null,
})
const onSubmit = async () => {
  await http.post("/acme/modify", acmeInfo);
}
// unifi
const unifiInfo = reactive({
  baseUri: null,
  apiKey: null,
  localBaseUri: null,
  localApiKey: null,
})

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
  try {
    await http.post("/unifi/setting", unifiInfo);
    userNotificationStore().showSuccess("设置保存成功")
  } catch (err) {

  }
}

// LeiChi
const leichi = reactive({
  baseUrl: '',
  token: ''
})
const queryLeiChiToken = async () => {
  try {
    const resp = await http.get("/waf/lc/info");
    leichi.token = resp.token
    leichi.baseUrl = resp.baseUrl
    console.log(leichi.token)
  } catch (err) {
  }
}
const onLeiChiSubmit = async () => {
  try {
    await http.post("/waf/lc/info", leichi);
    userNotificationStore().showSuccess("设置保存成功")
  } catch (err) {

  }
}
onMounted(async () => {
  await queryUnifiInfo();
  await queryLeiChiToken();
})
</script>

<template>
  <v-sheet elevation="4">
    <v-tabs v-model="tab" color="cyan" bg-color="lime-lighten-2">
      <v-tab value="network">网络配置</v-tab>
      <v-tab value="api">外部Api</v-tab>
    </v-tabs>

    <v-divider></v-divider>
    <v-tabs-window v-model="tab">
      <v-tabs-window-item value="network">
        <v-sheet class="pa-5">
          <v-card class="pa-5" title="ACME配置">
            <v-row align="center" class="ma-0">
              <v-col cols="11" class="pa-0">
                <v-text-field clearable label="Let`s Encrypt 账号" variant="solo-filled"
                              v-model="acmeInfo.userEmail" class="mt-3"></v-text-field>
              </v-col>
              <v-col cols="1" class="pa-0 d-flex align-center justify-center">
                <v-btn color="primary" @click="onSubmit" variant="flat">提交</v-btn>
              </v-col>
            </v-row>
          </v-card>
        </v-sheet>
      </v-tabs-window-item>
      <v-tabs-window-item value="api">
        <v-sheet class="pa-5">
          <v-card class="pa-5" title="UnifiApi设置">
            <v-text-field clearable label="UnifiApiBaseUrl" variant="solo-filled"
                          v-model="unifiInfo.baseUri" class="mt-3"></v-text-field>
            <v-text-field clearable label="UnifiApiKey" variant="solo-filled"
                          v-model="unifiInfo.apiKey" class="mt-3"></v-text-field>
            <v-text-field clearable label="UnifiApiBaseUrl-Local" variant="solo-filled"
                          v-model="unifiInfo.localBaseUri" class="mt-3"></v-text-field>
            <v-text-field clearable label="UnifiApiKey-Local" variant="solo-filled"
                          v-model="unifiInfo.localApiKey" class="mt-3"></v-text-field>
            <v-btn color="primary" @click="onUnifiSubmit" variant="flat">提交</v-btn>
          </v-card>

          <v-card class="pa-5" title="LeiChi设置">
            <v-text-field clearable label="BaseUri" variant="solo-filled"
                          v-model="leichi.baseUrl" class="mt-3"></v-text-field>
            <v-text-field clearable label="ApiToken" variant="solo-filled"
                          v-model="leichi.token" class="mt-3"></v-text-field>
            <v-btn color="primary" @click="onLeiChiSubmit" variant="flat">保存</v-btn>
          </v-card>
        </v-sheet>
      </v-tabs-window-item>
    </v-tabs-window>
  </v-sheet>
</template>

<style scoped lang="sass">

</style>
