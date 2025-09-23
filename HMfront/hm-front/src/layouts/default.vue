<template>
  <v-layout class="rounded rounded-md border">
    <v-app-bar title="HMoneta"></v-app-bar>
    <v-navigation-drawer
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

    <v-main class="d-flex align-center justify-center" height="300">
      <v-container>
        <v-sheet style="padding-top: 210px"
        >
          <router-view></router-view>
        </v-sheet>
      </v-container>
    </v-main>

    <AppFooter />
  </v-layout>
</template>

<script setup>
import { ref } from 'vue';
import AppFooter from "@/components/AppFooter.vue";

// 定义菜单项
const menuItems = ref([
  { title: '关于', route: '/helloWorld' }
]);
</script>
