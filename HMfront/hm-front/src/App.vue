<template>
  <v-app>
    <router-view/>
    <!-- 全局通知 -->
    <v-snackbar
      v-model="notificationStore.show"
      :color="notificationStore.color"
      :timeout="3000"
      location="bottom"
    >
      {{ notificationStore.message }}
      <template v-slot:actions>
        <v-btn
          variant="text"
          @click="notificationStore.show = false"
        >
          关闭
        </v-btn>
      </template>
    </v-snackbar>
  </v-app>

</template>

<script setup>
import {userNotificationStore} from '@/stores/app.js';

const notificationStore = userNotificationStore();
</script>
