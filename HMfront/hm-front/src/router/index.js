/**
 * router/index.ts
 *
 * Automatic routes for `./src/pages/*.vue`
 */

// Composables
import { createRouter, createWebHistory } from 'vue-router'
import { setupLayouts } from 'virtual:generated-layouts'
import { routes } from 'vue-router/auto-routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: setupLayouts(routes),
})

// 检查用户是否已登录的函数
const isAuthenticated = () => {
  // 这里可以检查 localStorage、sessionStorage、cookie 或状态管理中的登录状态
  // 示例：检查 localStorage 中的 token
  const token = localStorage.getItem('authToken')
  return !!token

  // 或者检查其他登录标识
  // const isLoggedIn = localStorage.getItem('isLoggedIn')
  // return isLoggedIn === 'true'
}

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
  if (!isAuthenticated()) {
    // 未登录，重定向到登录页
    next('/login')
  } else {
    // 已登录，允许访问
    next()
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
