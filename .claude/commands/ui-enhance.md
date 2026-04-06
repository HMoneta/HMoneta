<!--
name: ui-enhance
description: 提升 Vue 3 + Vuetify 3 前端界面 UI 质感
-->

当你需要提升 HMoneta 项目前端的 UI/UX 质感时，按照以下方案执行：

## 核心原则

1. **不修改登录页面**（login.vue、loginNew.vue）
2. **建立统一的设计系统**，而非分散的样式
3. **增强视觉层次感**，添加适当的阴影、边框、动画

## 实现方案

### Phase 1: 设计系统基础

**1. 配置 Vuetify 主题** (`src/plugins/vuetify.js`)：

```javascript
theme: {
  defaultTheme: 'hmonetaDark',
  themes: {
    hmonetaDark: {
      dark: true,
      colors: {
        background: '#0a0a0a',
        surface: '#171717',
        'surface-bright': '#262626',
        'surface-variant': '#404040',
        'on-surface-variant': '#a3a3a3',
        primary: '#10b981',      // 绿色强调色
        secondary: '#6366f1',     // 靛蓝辅助色
        error: '#ef4444',
        warning: '#f59e0b',
        success: '#10b981',
        'on-background': '#e5e5e5',
        'on-surface': '#e5e5e5',
      },
    },
    hmonetaLight: {
      dark: false,
      colors: {
        background: '#fafafa',
        surface: '#ffffff',
        primary: '#10b981',
        secondary: '#6366f1',
        // ...
      },
    },
  },
}
```

**2. 设计令牌** (`src/styles/settings.scss`)：

```scss
$space-unit: 4px;
$space-xs: $space-unit; $space-sm: 8px; $space-md: 16px; $space-lg: 24px;
$radius-sm: 4px; $radius-md: 8px; $radius-lg: 12px;
$shadow-sm: 0 2px 4px rgba(0,0,0,0.3);
$shadow-md: 0 4px 12px rgba(0,0,0,0.4);
$shadow-glow: 0 0 20px rgba(16,185,129,0.15);
```

**3. 全局样式** (`src/styles/global.scss`)：
- CSS 自定义属性
- 滚动条样式（webkit-scrollbar）
- 页面过渡动画（fade-slide）
- 工具类（card-glow、hover-lift）

### Phase 2: 布局增强

**`src/layouts/default.vue`**：
- App Bar 添加图标和主题切换按钮
- 导航抽屉添加 MDI 图标，激活状态高亮
- 页面过渡动画

```javascript
const menuItems = ref([
  { title: 'DNS', route: '/dns', icon: 'mdi-dns' },
  { title: '网络', route: '/network', icon: 'mdi-network' },
  { title: '插件管理', route: '/pluginManager', icon: 'mdi-puzzle-outline' },
  { title: '日志', route: '/logPage', icon: 'mdi-file-document-outline' },
  { title: '设置', route: '/setting', icon: 'mdi-cog-outline' },
]);

const toggleTheme = () => {
  theme.global.name.value = theme.global.current.value.dark ? 'hmonetaLight' : 'hmonetaDark'
}
```

### Phase 3: 共享 UI 组件

创建以下可复用组件：

| 组件 | 用途 |
|------|------|
| `PageHeader.vue` | 页面标题区（图标+标题+描述+分隔线） |
| `LoadingState.vue` | 加载状态指示器 |
| `EmptyState.vue` | 空状态提示 |
| `StatusBadge.vue` | 状态标签（v-chip 封装） |
| `ConfirmDialog.vue` | 确认对话框 |

### Phase 4: 各页面改进

- **dns.vue**: PageHeader + StatusBadge + EmptyState，卡片 glow 效果，表格 hover
- **network.vue**: PageHeader + StatusBadge + LoadingState
- **setting.vue**: 卡片式布局，outlined 表单字段，分区图标和帮助文本
- **logPage.vue**: 连接状态指示器，清空按钮，优化日志样式
- **pluginManager.vue**: PageHeader，上传区域样式

### Phase 5: 全局增强

- **App.vue**: Snackbar 添加图标、圆角、elevation

## 关键文件路径

```
HMfront/hm-front/src/
├── plugins/vuetify.js          # 主题配置
├── styles/
│   ├── settings.scss           # 设计令牌
│   └── global.scss             # 全局样式
├── layouts/default.vue         # 主布局
├── components/
│   ├── PageHeader.vue
│   ├── LoadingState.vue
│   ├── EmptyState.vue
│   ├── StatusBadge.vue
│   └── ConfirmDialog.vue
├── pages/
│   ├── dns.vue
│   ├── network.vue
│   ├── setting.vue
│   ├── logPage.vue
│   └── pluginManager.vue
└── App.vue                     # 根组件
```

## 验证方式

```bash
cd HMfront/hm-front && yarn dev
```

启动后检查：
1. 深色主题是否正确应用
2. 主题切换按钮是否正常
3. 页面过渡动画是否流畅
4. 各页面的视觉效果是否统一
