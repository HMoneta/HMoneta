<template>
  <v-app class="app-container">
    <template v-if="!isLoginPage">

      <v-app-bar app elevation="0" class="app-bar-custom">
        <template v-slot:prepend>
          <v-icon class="ml-2" size="28" color="primary">mdi-shield-check</v-icon>
        </template>
        <v-app-bar-title class="app-title">
          HMoneta
        </v-app-bar-title>
        <template v-slot:append>
          <v-btn
            :icon="theme.global.current.value.dark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
            variant="text"
            @click="toggleTheme"
          />
        </template>
      </v-app-bar>

      <v-navigation-drawer
        v-if="isLoggedIn"
        app
        expand-on-hover
        permanent
        rail
        class="nav-drawer-custom"
      >
        <v-list nav density="compact" class="nav-list-custom">
          <v-list-item
            v-for="(item, index) in menuItems"
            :key="index"
            :to="item.route"
            link
            :prepend-icon="item.icon"
            :title="item.title"
            class="nav-item-custom"
          />
        </v-list>
      </v-navigation-drawer>

      <v-main class="main-content">
        <v-container fluid class="pa-6">
          <transition name="fade-slide" mode="out-in">
            <router-view></router-view>
          </transition>
        </v-container>
      </v-main>
    </template>
    <template v-else>
      <router-view/>
    </template>

    <AppFooter/>
  </v-app>
</template>

<script setup>
import {ref} from 'vue';
import AppFooter from "@/components/AppFooter.vue";
import {useUserStore} from "@/stores/app.js";
import {useRoute, useRouter} from "vue-router";
import {useTheme} from "vuetify";

const router = useRouter()
const route = useRoute()
const theme = useTheme()

const isLoginPage = computed(() => {
  return route.path === '/login' || route.path === '/register' || route.path === '/forgot-password'
})

const toggleTheme = () => {
  theme.global.name.value = theme.global.current.value.dark ? 'hmonetaLight' : 'hmonetaDark'
}

// 定义菜单项
const menuItems = ref([
  { title: 'DNS', route: '/dns', icon: 'mdi-dns' },
  { title: '网络', route: '/network', icon: 'mdi-network' },
  { title: '插件管理', route: '/pluginManager', icon: 'mdi-puzzle-outline' },
  { title: '日志', route: '/logPage', icon: 'mdi-file-document-outline' },
  { title: '设置', route: '/setting', icon: 'mdi-cog-outline' },
]);

const isLoggedIn = ref(false);
const userStore = useUserStore()
watch(() => userStore.token, () => {
  isLoggedIn.value = userStore.checkAuth();
});

onMounted(() => {
  isLoggedIn.value = userStore.checkAuth();
})
</script>

<style scoped>
html, body {
  height: 100%;
  overflow: hidden;
}

.app-container {
  height: 100vh;
  overflow: hidden;
}

.app-bar-custom {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.app-title {
  font-weight: 600;
  letter-spacing: 0.5px;
}

.nav-drawer-custom {
  border-right: 1px solid rgba(255, 255, 255, 0.08) !important;
}

.nav-list-custom {
  padding-top: 8px;
}

.nav-item-custom {
  margin: 4px 8px;
  border-radius: 8px;
  transition: background-color 150ms ease;
}

.nav-item-custom:hover {
  background-color: rgba(16, 185, 129, 0.08);
}

.nav-item-custom.v-list-item--active {
  background-color: rgba(16, 185, 129, 0.12);
}

.nav-item-custom.v-list-item--active .v-list-item__prepend-icon {
  color: #10b981;
}

.main-content {
  height: 100%;
  overflow-y: auto;
  display: flex;
  justify-content: center;
}

/* Page transitions */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
