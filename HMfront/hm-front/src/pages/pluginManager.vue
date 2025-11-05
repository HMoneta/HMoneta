<script setup>
import {http} from "@/common/request.js";

const plugin = ref();
const onUpload = async () => {
  console.log();
  const formData = new FormData();
  formData.append('plugin', plugin.value);
  await http.post("/plugin/upload", formData)
}
</script>

<template>
  <v-file-upload v-model="plugin" clearable density="compact" variant="compact"/>
  <v-btn color="blue darken-1" text @click="onUpload">上传</v-btn>
</template>

<style scoped lang="sass">

</style>
