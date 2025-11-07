// Plugins
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import Fonts from 'unplugin-fonts/vite'
import Layouts from 'vite-plugin-vue-layouts-next'
import Vue from '@vitejs/plugin-vue'
import VueRouter from 'unplugin-vue-router/vite'
import {VueRouterAutoImports} from 'unplugin-vue-router'
import Vuetify, {transformAssetUrls} from 'vite-plugin-vuetify'

// Utilities
import {defineConfig} from 'vite'
import {fileURLToPath, URL} from 'node:url'
import dotenv from 'dotenv'
import {dirname, resolve} from 'node:path' // 改这里：使用 node:path
import {existsSync} from 'node:fs' // 改这里：使用 node:fs

// 获取当前文件目录
const __dirname = dirname(fileURLToPath(import.meta.url))

// https://vitejs.dev/config/
export default defineConfig(({mode}) => {
    // 加载环境变量
    const envFile = resolve(process.cwd(), `.env.${mode}`)

    if (existsSync(envFile)) {
        const result = dotenv.config({path: envFile})
        console.log('✅ 成功加载环境文件:', envFile)
        console.log('📋 环境变量:', result.parsed)
    } else {
        console.log('❌ 环境文件不存在:', envFile)
    }

    // 收集所有 VITE_ 开头的环境变量
    const envDefines = {}
    Object.keys(process.env).forEach(key => {
        if (key.startsWith('VITE_')) {
            envDefines[`import.meta.env.${key}`] = JSON.stringify(process.env[key])
        }
    })

    console.log('🔧 注入的环境变量:', envDefines)

    return {
        plugins: [
            VueRouter(),
            Layouts(),
            Vue({
                template: {transformAssetUrls},
            }),
            // https://github.com/vuetifyjs/vuetify-loader/tree/master/packages/vite-plugin#readme
            Vuetify({
                autoImport: true,
                styles: {
                    configFile: 'src/styles/settings.scss',
                },
            }),
            Components(),
            Fonts({
                google: {
                    families: [{
                        name: 'Roboto',
                        styles: 'wght@100;300;400;500;700;900',
                    }],
                },
            }),
            AutoImport({
                imports: [
                    'vue',
                    VueRouterAutoImports,
                    {
                        pinia: ['defineStore', 'storeToRefs'],
                    },
                ],
                eslintrc: {
                    enabled: true,
                },
                vueTemplate: true,
            }),
        ],
        optimizeDeps: {
            exclude: [
                'vuetify',
                'vue-router',
                'unplugin-vue-router/runtime',
                'unplugin-vue-router/data-loaders',
                'unplugin-vue-router/data-loaders/basic',
            ],
        },
        // 修改这里：保留 process.env 但不覆盖 import.meta.env
        define: {
            'process.env': {},
            // 如果需要，可以添加自定义全局变量
            // __APP_VERSION__: JSON.stringify(env.VITE_APP_VERSION),
        },
        resolve: {
            alias: {
                '@': fileURLToPath(new URL('src', import.meta.url)),
            },
            extensions: [
                '.js',
                '.json',
                '.jsx',
                '.mjs',
                '.ts',
                '.tsx',
                '.vue',
            ],
        },
        server: {
            port: 3000,
        },
    }
})
