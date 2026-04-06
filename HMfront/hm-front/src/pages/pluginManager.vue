<script setup>
import {ref} from 'vue'
import {http} from "@/common/request.js";
import {userNotificationStore} from "@/stores/app.js";
import PageHeader from "@/components/PageHeader.vue";
import LoadingState from "@/components/LoadingState.vue";

let notificationStore = userNotificationStore();

const plugin = ref();
const uploadLoading = ref(false);
const onUpload = async () => {
  if (!plugin.value) {
    notificationStore.showError("请选择要上传的插件文件");
    return;
  }
  uploadLoading.value = true;
  try {
    const formData = new FormData();
    formData.append('plugin', plugin.value);
    await http.post("/plugin/upload", formData)
    notificationStore.showSuccess("上传成功")
    plugin.value = null;
  }catch (e) {
    notificationStore.showError("上传失败");
  } finally {
    uploadLoading.value = false;
  }
}
</script>

<template>
  <PageHeader
    icon="mdi-puzzle-outline"
    title="插件管理"
    subtitle="上传和管理 DNS 服务商插件"
  />

  <v-card class="plugin-upload-card">
    <div class="upload-zone pa-8 text-center">
      <v-icon size="64" color="primary" class="mb-4">mdi-cloud-upload</v-icon>
      <h3 class="text-h6 mb-2">上传插件</h3>
      <p class="text-body-2 text-medium-emphasis mb-4">
        支持 .jar 格式的插件文件
      </p>

      <v-file-upload
        v-model="plugin"
        clearable
        density="compact"
        variant="outlined"
        accept=".jar"
        show-size
        class="mb-4"
      />

      <v-btn
        color="primary"
        size="large"
        @click="onUpload"
        :loading="uploadLoading"
        :disabled="!plugin"
        variant="elevated"
      >
        <v-icon start>mdi-upload</v-icon>
        上传插件
      </v-btn>
    </div>
  </v-card>

  <v-card class="plugin-info-card mt-4">
    <v-card-title class="d-flex align-center">
      <v-icon start color="info">mdi-information</v-icon>
      插件说明
    </v-card-title>
    <v-card-text>
      <ul class="info-list">
        <li>插件文件格式: <code>.jar</code></li>
        <li>插件需要实现 <code>fan.summer.HmDnsProviderPlugin</code> 接口</li>
        <li>上传后系统会自动加载插件</li>
        <li>插件存放目录: <code>plugins/</code></li>
      </ul>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.plugin-upload-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.upload-zone {
  background: rgba(16, 185, 129, 0.02);
  border: 2px dashed rgba(16, 185, 129, 0.3);
  border-radius: 12px;
  transition: border-color var(--transition-normal), background-color var(--transition-normal);
}

.upload-zone:hover {
  border-color: rgba(16, 185, 129, 0.5);
  background: rgba(16, 185, 129, 0.04);
}

.plugin-info-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(59, 130, 246, 0.02) !important;
}

.plugin-info-card :deep(.v-card-title) {
  background: rgba(59, 130, 246, 0.08);
}

.info-list {
  margin: 0;
  padding-left: 20px;
}

.info-list li {
  margin-bottom: 8px;
  color: rgb(var(--v-theme-on-surface-variant));
}

.info-list code {
  background: rgba(255, 255, 255, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.875em;
  color: #10b981;
}
</style>
