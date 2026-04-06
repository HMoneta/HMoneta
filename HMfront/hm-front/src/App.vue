<template>
  <v-app>
    <router-view/>
    <!-- 全局通知 -->
    <v-snackbar
      v-model="notificationStore.show"
      :color="notificationStore.color"
      :timeout="3000"
      location="bottom right"
      rounded="lg"
      elevation="8"
      class="notification-snackbar"
    >
      <div class="d-flex align-center">
        <v-icon
          :icon="notificationIcon"
          :color="notificationStore.color === 'error' ? 'white' : 'white'"
          class="mr-3"
        />
        {{ notificationStore.message }}
      </div>
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
import {computed} from 'vue';
import {userNotificationStore} from '@/stores/app.js';

const notificationStore = userNotificationStore();

const notificationIcon = computed(() => {
  switch (notificationStore.color) {
    case 'success':
      return 'mdi-check-circle';
    case 'error':
      return 'mdi-alert-circle';
    case 'warning':
      return 'mdi-alert';
    case 'info':
      return 'mdi-information';
    default:
      return 'mdi-information';
  }
});
</script>

<style scoped>
.notification-snackbar {
  font-weight: 500;
}
</style>
