<script setup>
import {ref} from 'vue'

const tab = ref(null)
</script>

<template>
  <v-sheet elevation="4">
    <v-tabs v-model="tab" color="cyan" bg-color="lime-lighten-2">
      <v-tab value="network">网络配置</v-tab>
    </v-tabs>

    <v-divider></v-divider>
    <v-tabs-window v-model="tab">
      <v-tabs-window-item value="network">
        <v-sheet class="pa-5">
          <v-card class="pa-5" title="ACME配置">
            <v-text-field clearable label="Let`s Encrypt 账号" variant="solo-filled"></v-text-field>
          </v-card>


        </v-sheet>
      </v-tabs-window-item>
    </v-tabs-window>
  </v-sheet>
</template>

<style scoped lang="sass">

</style>
