/**
 * router/index.ts
 *
 * Automatic routes for `./src/pages/*.vue`
 */

// Composables
import { createRouter, createWebHistory } from 'vue-router'
import { setupLayouts } from 'virtual:generated-layouts'
import { routes } from 'vue-router/auto-routes'
import {useUserStore} from "@/stores/app.js";
import {el} from "vuetify/locale";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: setupLayouts(routes),
})


// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 不需要认证的路由列表
  const publicRoutes = ['/login', '/register', '/forgot-password']

  // 如果目标路由不需要认证，直接通过
  if (publicRoutes.includes(to.path)) {
    next()
    return
  }

  // 检查用户是否已登录
  const userStore = useUserStore();
  if (!userStore.checkAuth()) {
    // 未登录，重定向到登录页
    next('/login')
  } else {
    // 强制推送至About界面
    if(to.path === '/'){
      next('about')
    }else{
      next()
    }

  }
})


// Workaround for https://github.com/vitejs/vite/issues/11804
router.onError((err, to) => {
  if (err?.message?.includes?.('Failed to fetch dynamically imported module')) {
    if (localStorage.getItem('vuetify:dynamic-reload')) {
      console.error('Dynamic import error, reloading page did not fix it', err)
    } else {
      console.log('Reloading page to fix dynamic import error')
      localStorage.setItem('vuetify:dynamic-reload', 'true')
      location.assign(to.fullPath)
    }
  } else {
    console.error(err)
  }
})

router.isReady().then(() => {
  localStorage.removeItem('vuetify:dynamic-reload')
})

export default router
