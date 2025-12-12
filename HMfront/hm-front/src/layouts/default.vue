<template>
  <v-app class="app-container">
    <template v-if="!isLoginPage">


      <v-app-bar app title="HMoneta"></v-app-bar>
      <v-navigation-drawer
        v-if="isLoggedIn"
        app
        expand-on-hover
        permanent
        rail
      >
        <v-list nav>
          <!-- 添加菜单项 -->
          <v-list-item v-for="(item, index) in menuItems" :key="index" :to="item.route" link>
            <v-list-item-title>{{ item.title }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-navigation-drawer>

      <v-main class="main-content">
        <v-container>
          <v-sheet>
            <router-view></router-view>
          </v-sheet>
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

const router = useRouter()
const route = useRoute()

const isLoginPage = computed(() => {
  return route.path === '/login' || route.path === '/register' || route.path === '/forgot-password'
})

// 定义菜单项
const menuItems = ref([
  {title: 'DNS', route: '/dns'},
  {title: '插件管理', route: '/pluginManager'},
  {title: '日志', route: '/logPage'},
  {title: '设置', route: '/setting'},

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
<style>
html, body {
  height: 100%;
  overflow: hidden; /* 防止整体页面滚动 */
}

.app-container {
  height: 100vh;
  overflow: hidden;
}

.main-content {
  height: 100%;
  overflow-y: auto;
  display: flex;
  justify-content: center;
}

</style>
