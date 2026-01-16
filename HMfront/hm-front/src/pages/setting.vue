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

const unifiInfo = reactive({
  baseUri: null,
  apiKey: null,
})

const onUnifiSubmit = async () => {
  try {
    await http.post("/unifi/setting", {"baseUri": unifiInfo.baseUri, "apiKey": unifiInfo.apiKey});
    userNotificationStore().showSuccess("设置保存成功")
  } catch (err) {

  }
}
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
            <v-btn color="primary" @click="onUnifiSubmit" variant="flat">提交</v-btn>
          </v-card>
        </v-sheet>
      </v-tabs-window-item>
    </v-tabs-window>
  </v-sheet>
</template>

<style scoped lang="sass">

</style>
