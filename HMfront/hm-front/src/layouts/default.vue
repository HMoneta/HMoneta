<template>
  <v-layout class="rounded rounded-md border">
    <v-navigation-drawer>
      <v-list nav>
        <!-- 添加菜单项 -->
        <v-list-item v-for="(item, index) in menuItems" :key="index" :to="item.route" link>
          <v-list-item-title>{{ item.title }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>

    <v-app-bar title="HMoneta"></v-app-bar>

    <v-main class="d-flex align-center justify-center" height="300">
      <v-container style="padding-top: 12px">
        <router-view></router-view>
      </v-container>
    </v-main>
  </v-layout>
</template>

<script setup>
import { ref } from 'vue';

// 定义菜单项
const menuItems = ref([
  { title: '关于', route: '/about' },
  { title: '关于', route: '/helloWorld' }
]);
</script>
