<template>
  <div class="login-container">
    <v-container class="login-content">
      <v-row justify="center" align="center" class="fill-height">
        <v-col cols="12" sm="10" md="6" lg="5" xl="4">
          <div class="card-container">
            <div class="card-content">
              <!-- 顶部装饰区域 - Vanta.js背景 -->
              <div class="header-section">
                <div id="vanta-canvas" ref="vantaRef"></div>
                <div class="header-content">
                  <span class="secure-badge">SECURE ACCESS</span>
                  <h2 class="main-title">HMoneta</h2>
                  <div class="title-underline"></div>
                </div>
              </div>

              <!-- 表单区域 -->
              <div class="form-section">
                <div>
                  <span class="auth-badge">AUTHENTICATION</span>
                  <h3 class="section-title">Account Login</h3>

                  <v-form ref="loginForm" @submit.prevent="handleLogin">
                    <!-- 用户名 -->
                    <div class="input-group">
                      <label for="username" class="input-label">USERNAME</label>
                      <input
                        v-model="loginInfo.username"
                        type="text"
                        id="username"
                        class="tech-input"
                        placeholder="Enter your username"
                        @keyup.enter="handleLogin"
                      />
                    </div>

                    <!-- 密码 -->
                    <div class="input-group">
                      <div class="label-row">
                        <label for="password" class="input-label">PASSWORD</label>
                        <!--                        <a href="#" class="forgot-link">Forgot?</a>-->
                      </div>
                      <div class="password-wrapper">
                        <input
                          v-model="loginInfo.password"
                          :type="showPassword ? 'text' : 'password'"
                          id="password"
                          class="tech-input"
                          placeholder="••••••••"
                          @keyup.enter="handleLogin"
                        />
                        <button
                          type="button"
                          class="toggle-password"
                          @click="showPassword = !showPassword"
                        >
                          <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" class="icon" fill="none"
                               viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                          </svg>
                          <svg v-else xmlns="http://www.w3.org/2000/svg" class="icon" fill="none" viewBox="0 0 24 24"
                               stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                  d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                          </svg>
                        </button>
                      </div>
                    </div>

                    <!-- 按钮组 -->
                    <div class="button-group">
                      <button type="submit" class="tech-btn primary-btn" :disabled="loading">
                        <svg v-if="!loading" xmlns="http://www.w3.org/2000/svg" class="btn-icon" fill="none"
                             viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"/>
                        </svg>
                        <span v-if="loading" class="loading-spinner"></span>
                        {{ loading ? 'Logging in...' : 'Login' }}
                      </button>
                    </div>
                  </v-form>
                </div>

                <!-- 底部信息 -->
                <div class="footer-section">
                  <div class="divider-gradient"></div>
                  <div class="status-indicator">
    <span
      class="status-dot"
      :style="{ backgroundColor: statusConfig.color }"
    ></span>
                    <span class="status-text">System Status: {{ statusConfig.text }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </v-col>
      </v-row>
    </v-container>

    <!-- 错误提示 -->
    <transition name="slide">
      <div v-if="errorMessage" class="error-toast">
        {{ errorMessage }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import {onBeforeUnmount, onMounted, reactive, ref} from 'vue';
import {useUserStore} from "@/stores/app.js";
import {http} from "@/common/request.js";
import router from "@/router/index.js";

// 状态
const showPassword = ref(false);
const rememberMe = ref(false);
const loading = ref(false);
const loginForm = ref(null);
const errorMessage = ref('');
const vantaRef = ref(null);
let vantaEffect = null;

// 检查后端服务器状态
const statusConfig = ref(
  {color: '#6b7280', text: 'Offline'}
)
const fetchServerStatus = async () => {
  try {
    const response = await http.get('/user/login/status')
    console.log(response)
    if (response) {
      statusConfig.value.color = '#10b981'
      statusConfig.value.text = 'Operational'
    }
  } catch (error) {
    statusConfig.value.color = '#ef4444'
    statusConfig.value.text = 'Offline'
  }

}


// 登录信息
const loginInfo = reactive({
  username: "",
  password: "",
});

// 登录处理
const handleLogin = async () => {
  if (!loginInfo.username || !loginInfo.password) {
    showError('请输入用户名和密码');
    return;
  }

  if (loginInfo.username.length < 3) {
    showError('用户名至少需要3个字符');
    return;
  }

  if (loginInfo.password.length < 6) {
    showError('密码至少需要6个字符');
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const json = await http.post("/user/login", loginInfo);
    const userStore = useUserStore();
    userStore.login(json);
    await router.push('/');

  } catch (error) {
    showError(error.toString());
  } finally {
    loading.value = false;
  }
};

const showError = (message) => {
  errorMessage.value = message;
  setTimeout(() => {
    errorMessage.value = '';
  }, 3000);
};

// 加载外部脚本
const loadScript = (src) => {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = src;
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });
};

// 初始化 Vanta.js 效果
const initVanta = () => {
  if (window.VANTA && window.VANTA.NET && vantaRef.value) {
    vantaEffect = window.VANTA.NET({
      el: vantaRef.value,
      mouseControls: true,
      touchControls: true,
      gyroControls: false,
      minHeight: 150,
      minWidth: 200,
      scale: 1.00,
      scaleMobile: 1.00,
      color: 0xd1d5db,
      backgroundColor: 0x171717,
      points: 8,
      maxDistance: 20.00,
      spacing: 18.00,
      showDots: true
    });
  }
};

onMounted(async () => {
  try {
    // 检查脚本是否已加载
    if (!window.THREE) {
      await loadScript('https://cdnjs.cloudflare.com/ajax/libs/three.js/r134/three.min.js');
    }
    if (!window.VANTA) {
      await loadScript('https://cdn.jsdelivr.net/npm/vanta@latest/dist/vanta.net.min.js');
    }

    // 等待DOM更新后初始化
    setTimeout(() => {
      initVanta();
    }, 100);
    fetchServerStatus()
  } catch (error) {
    console.error('加载Vanta.js失败:', error);
  }
});

onBeforeUnmount(() => {
  if (vantaEffect) {
    vantaEffect.destroy();
  }
});
</script>

<style scoped lang="scss">
/* 容器 */
.login-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  background: #0a0a0a;
}

.login-content {
  position: relative;
  z-index: 10;
  height: 100vh;
}

/* 卡片容器 */
.card-container {
  position: relative;
  z-index: 0;

  &::before {
    content: "";
    position: absolute;
    inset: -1px;
    background: linear-gradient(to bottom right, #525252, transparent, #262626);
    border-radius: 12px;
    z-index: -1;
  }
}

.card-content {
  border-radius: 12px;
  overflow: hidden;
  background: #171717;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

/* 头部区域 */
.header-section {
  height: 150px;
  position: relative;
  overflow: hidden;
}

#vanta-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.header-content {
  position: relative;
  z-index: 10;
  padding: 16px;
}

.secure-badge {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(38, 38, 38, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 9999px;
  font-size: 0.75rem;
  color: #a3a3a3;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.main-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  margin: 8px 0;
}

.title-underline {
  height: 4px;
  width: 48px;
  background: #a3a3a3;
  border-radius: 9999px;
  margin-top: 8px;
}

/* 表单区域 */
.form-section {
  padding: 24px;
  background: #171717;
}

.auth-badge {
  display: inline-block;
  padding: 4px 8px;
  background: #262626;
  border-radius: 9999px;
  font-size: 0.75rem;
  color: #a3a3a3;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #e5e5e5;
  margin-bottom: 16px;
}

/* 输入组 */
.input-group {
  margin-bottom: 16px;
}

.input-label {
  display: block;
  color: #d4d4d4;
  font-size: 0.75rem;
  font-weight: 500;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.forgot-link {
  color: #a3a3a3;
  font-size: 0.75rem;
  text-decoration: none;
  transition: color 0.2s;

  &:hover {
    color: #d4d4d4;
  }
}

.password-wrapper {
  position: relative;
}

.tech-input {
  width: 100%;
  background: #262626;
  border: 1px solid #404040;
  border-radius: 8px;
  padding: 10px 16px;
  color: #e5e5e5;
  font-size: 0.875rem;
  transition: all 0.2s;
  font-family: inherit;

  &:focus {
    outline: none;
    border-color: #525252;
    box-shadow: 0 0 0 2px rgba(82, 82, 82, 0.2);
  }

  &::placeholder {
    color: #737373;
  }
}

.password-wrapper .tech-input {
  padding-right: 40px;
}

.toggle-password {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #737373;
  cursor: pointer;
  padding: 4px;
  transition: color 0.2s;
  display: flex;
  align-items: center;

  &:hover {
    color: #a3a3a3;
  }

  .icon {
    width: 20px;
    height: 20px;
  }
}

/* 记住我 */
.remember-group {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.tech-checkbox {
  width: 16px;
  height: 16px;
  background: #262626;
  border: 1px solid #404040;
  border-radius: 4px;
  margin-right: 8px;
  cursor: pointer;
  accent-color: #525252;
}

.remember-label {
  color: #a3a3a3;
  font-size: 0.75rem;
  cursor: pointer;
  user-select: none;
}

/* 按钮组 */
.button-group {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.tech-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
  border: none;
  cursor: pointer;
  font-family: inherit;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-icon {
  width: 16px;
  height: 16px;
  margin-right: 8px;
}

.primary-btn {
  background: #262626;
  color: #e5e5e5;

  &:hover:not(:disabled) {
    background: #404040;
  }
}

.secondary-btn {
  background: #0a0a0a;
  color: #d4d4d4;

  &:hover {
    background: #262626;
  }
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #404040;
  border-top-color: #e5e5e5;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 底部区域 */
.footer-section {
  padding-top: 16px;
  text-align: center;
}

.divider-gradient {
  height: 1px;
  background: linear-gradient(to right, transparent, #525252, transparent);
  margin-bottom: 16px;
}

.footer-text {
  color: #a3a3a3;
  font-size: 0.75rem;
  margin-bottom: 16px;
}

.footer-link {
  color: #d4d4d4;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.status-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.status-text {
  color: #a3a3a3;
  font-size: 0.75rem;
}

/* 错误提示 */
.error-toast {
  position: fixed;
  top: 24px;
  right: 24px;
  background: #7f1d1d;
  color: #fecaca;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 0.875rem;
  z-index: 1000;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease-out;
}

.slide-enter-from {
  transform: translateX(400px);
  opacity: 0;
}

.slide-leave-to {
  transform: translateX(400px);
  opacity: 0;
}

/* 响应式 */
@media (max-width: 640px) {
  .button-group {
    flex-direction: column;
  }

  .header-section {
    height: 120px;
  }

  .main-title {
    font-size: 1.25rem;
  }
}
</style>
