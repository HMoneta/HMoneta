<template>
  <v-app>
    <v-app-bar app title="HMoneta"></v-app-bar>
    <v-navigation-drawer
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

    <v-main class="d-flex justify-center">
      <v-container>
        <v-sheet>
          <router-view></router-view>
        </v-sheet>
      </v-container>
    </v-main>

    <AppFooter />
  </v-app>
</template>

<script setup>
import { ref } from 'vue';
import AppFooter from "@/components/AppFooter.vue";

// 定义菜单项
const menuItems = ref([
  { title: '关于', route: '/about' },
  { title: '测试', route: '/HelloWorld' },
]);
</script>
