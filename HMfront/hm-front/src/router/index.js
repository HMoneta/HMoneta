/**
 * router/index.ts
 *
 * Automatic routes for `./src/pages/*.vue`
 */

// Composables
import {createRouter, createWebHistory} from 'vue-router'
import {setupLayouts} from 'virtual:generated-layouts'
import {routes} from 'vue-router/auto-routes'
import {useUserStore} from "@/stores/app.js";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: setupLayouts(routes),
})
let userStore = null
// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  // 不需要认证的路由列表
  const publicRoutes = ['/login', '/register', '/forgot-password']
  // 检查用户是否已登录
  if (userStore == null) {
    userStore = useUserStore();
  }

  // 如果目标路由不需要认证，直接通过
  if (publicRoutes.includes(to.path)) {
    next()
    return
  }
  let isAuthenticated
  if (!userStore.isValidToken) {
    isAuthenticated = await userStore.checkAuth()
  } else {
    isAuthenticated = true
  }
  if (!isAuthenticated) {
    // 未登录，重定向到登录页
    next('/login')
  } else {
    // 强制推送至About界面
    if (to.path === '/') {
      next('dns')
    } else {
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
