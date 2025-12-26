<script setup>
import {http} from "@/common/request.js";
import {userNotificationStore} from "@/stores/app.js";

let notificationStore = userNotificationStore();

const plugin = ref();
const onUpload = async () => {
  try {
    const formData = new FormData();
    formData.append('plugin', plugin.value);
    await http.post("/plugin/upload", formData)
    notificationStore.showSuccess("上传成功")
  }catch (e){

  }
}
</script>

<template>
  <v-file-upload v-model="plugin" clearable density="compact" variant="compact"/>
  <v-btn color="blue darken-1" text @click="onUpload">上传</v-btn>
</template>

<style scoped lang="sass">

</style>
