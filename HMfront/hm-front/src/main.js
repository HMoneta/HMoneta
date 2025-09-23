/**
 * main.js
 *
 * Bootstraps Vuetify and other plugins then mounts the App`
 */

// Plugins
import { registerPlugins } from '@/plugins'

// Components
import App from './App.vue'

// Composables
import { createApp } from 'vue'

// Styles
import 'unfonts.css'
import router from "@/router/index.js";

const app = createApp(App)

registerPlugins(app)

// 确保路由准备好后再挂载应用
router.isReady().then(() => {
  app.mount('#app')
})
