<template>
  <div class="login-container">
    <!-- 优雅的现代背景 -->
    <div class="elegant-background">
      <!-- 动态渐变背景 -->
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>

      <!-- 柔和网格 -->
      <div class="soft-grid"></div>

      <!-- 浮动光点 -->
      <div class="floating-lights">
        <div class="light-dot" v-for="n in 20" :key="n"
             :style="getLightDotStyle(n)"></div>
      </div>

      <!-- 优雅的几何形状 -->
      <div class="geometric-elements">
        <div class="floating-circle circle-1"></div>
        <div class="floating-circle circle-2"></div>
        <div class="floating-rect rect-1"></div>
        <div class="floating-rect rect-2"></div>
      </div>

      <!-- 柔光效果 -->
      <div class="ambient-glow"></div>
    </div>

    <v-container class="login-content">
      <v-row justify="center" align="center" class="fill-height">
        <v-col cols="12" sm="10" md="6" lg="5" xl="4">
          <v-card
            class="login-card"
            elevation="24"
          >
            <v-card-text class="pa-8">
              <!-- Logo and Title -->
              <div class="text-center mb-8">
                <v-avatar
                  size="120"
                  class="mb-4 logo-avatar pulse-animation"
                  color="primary"
                >
                  <v-icon size="60" color="white">mdi-home-automation</v-icon>
                </v-avatar>

                <h2 class="text-h4 font-weight-bold text-white mb-2 gradient-text">
                  HMoneta
                </h2>
              </div>

              <!-- Form -->
              <v-form ref="loginForm" @submit.prevent="handleLogin">
                <!-- Username -->
                <v-text-field
                  v-model="loginInfo.username"
                  label="用户名"
                  prepend-inner-icon="mdi-account"
                  variant="outlined"
                  density="comfortable"
                  class="glass-input mb-4"
                  :rules="usernameRules"
                  @keyup.enter="handleLogin"
                ></v-text-field>

                <!-- Password -->
                <v-text-field
                  v-model="loginInfo.password"
                  label="密码"
                  prepend-inner-icon="mdi-lock"
                  :type="showPassword ? 'text' : 'password'"
                  :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                  @click:append-inner="showPassword = !showPassword"
                  variant="outlined"
                  density="comfortable"
                  class="glass-input mb-4"
                  :rules="passwordRules"
                  @keyup.enter="handleLogin"
                ></v-text-field>


                <!-- Submit Button -->
                <v-btn
                  type="submit"
                  block
                  size="large"
                  class="submit-btn mb-6"
                  :loading="loading"
                >
                  <v-icon start>mdi-lock-open</v-icon>
                  登录
                </v-btn>
              </v-form>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>

    <!-- 优雅的扫描线 -->
    <div class="elegant-scanline"></div>
  </div>
</template>

<script setup>
import {http} from "@/common/request.js";
import {useUserStore} from "@/stores/app.js";
import router from "@/router/index.js"; // Form state

// Form state
const showPassword = ref(false)
const rememberMe = ref(false)
const loading = ref(false)
const loginForm = ref(null)

// Login info
const loginInfo = reactive({
  username: "",
  password: "",
})

// Validation rules
const usernameRules = [
  v => !!v || '用户名是必需的',
  v => v.length >= 3 || '用户名至少需要3个字符'
]

const passwordRules = [
  v => !!v || '密码是必需的',
  v => v.length >= 6 || '密码至少需要6个字符'
]

// Methods
const handleLogin = async () => {
  if (!loginInfo.username || !loginInfo.password) {
    showSnackbar('请输入用户名和密码');
    return;
  }

  const {valid} = await loginForm.value.validate()
  if (valid) {
    loading.value = true

    try {
      const json = await http.post("/user/login", loginInfo);
      const userStore = useUserStore();
      userStore.login(json);
      await router.push('/');
    } catch (error) {
      showSnackbar(error.toString());
    } finally {
      loading.value = false;
    }
  }
}


const showSnackbar = (message) => {

}

// 光点样式计算
const getLightDotStyle = (index) => {
  const positions = [
    {top: '20%', left: '15%'},
    {top: '30%', left: '80%'},
    {top: '50%', left: '25%'},
    {top: '70%', left: '75%'},
    {top: '80%', left: '40%'},
    {top: '15%', left: '60%'},
    {top: '45%', left: '85%'},
    {top: '65%', left: '20%'},
    {top: '25%', left: '45%'},
    {top: '75%', left: '65%'},
    {top: '35%', left: '70%'},
    {top: '55%', left: '35%'},
    {top: '85%', left: '25%'},
    {top: '40%', left: '90%'},
    {top: '60%', left: '15%'},
    {top: '18%', left: '75%'},
    {top: '42%', left: '50%'},
    {top: '78%', left: '85%'},
    {top: '32%', left: '30%'},
    {top: '68%', left: '55%'}
  ];

  const pos = positions[index - 1] || {top: '50%', left: '50%'};
  const size = 3 + Math.random() * 4;
  const delay = Math.random() * 8;
  const duration = 6 + Math.random() * 4;
  const opacity = 0.2 + Math.random() * 0.4;

  return {
    ...pos,
    width: size + 'px',
    height: size + 'px',
    animationDelay: delay + 's',
    animationDuration: duration + 's',
    opacity: opacity
  };
};

</script>

<style scoped lang="scss">
/* 容器样式 */
.login-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #0a0a23 0%, #1a1a3a 30%, #2d1b69 70%, #0a0a23 100%);
}

/* 优雅背景容器 */
.elegant-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

/* 动态渐变光球 */
.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.6;
  animation: float-gentle 20s ease-in-out infinite;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(103, 58, 183, 0.4) 0%, transparent 70%);
  top: -200px;
  left: -200px;
  animation-delay: 0s;
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(33, 150, 243, 0.3) 0%, transparent 70%);
  top: 50%;
  right: -250px;
  animation-delay: -7s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(156, 39, 176, 0.2) 0%, transparent 70%);
  bottom: -150px;
  left: 30%;
  animation-delay: -14s;
}

@keyframes float-gentle {
  0%, 100% {
    transform: translateY(0px) translateX(0px) scale(1);
  }
  25% {
    transform: translateY(-30px) translateX(20px) scale(1.05);
  }
  50% {
    transform: translateY(-60px) translateX(-10px) scale(0.95);
  }
  75% {
    transform: translateY(-30px) translateX(-30px) scale(1.02);
  }
}

/* 柔和网格 */
.soft-grid {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
  linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  animation: grid-move 30s linear infinite;
}

@keyframes grid-move {
  0% {
    transform: translate(0, 0);
  }
  100% {
    transform: translate(60px, 60px);
  }
}

/* 浮动光点 */
.floating-lights {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.light-dot {
  position: absolute;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.8) 0%, rgba(103, 58, 183, 0.4) 50%, transparent 100%);
  border-radius: 50%;
  animation: light-float 10s ease-in-out infinite;
  box-shadow: 0 0 20px rgba(103, 58, 183, 0.3);
}

@keyframes light-float {
  0%, 100% {
    transform: translateY(0px) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translateY(-40px) scale(1.2);
    opacity: 1;
  }
}

/* 几何元素 */
.geometric-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.floating-circle {
  position: absolute;
  border: 1px solid rgba(103, 58, 183, 0.2);
  border-radius: 50%;
  animation: circle-drift 25s ease-in-out infinite;
}

.circle-1 {
  width: 120px;
  height: 120px;
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 80px;
  height: 80px;
  bottom: 30%;
  right: 15%;
  animation-delay: -12s;
}

.floating-rect {
  position: absolute;
  border: 1px solid rgba(33, 150, 243, 0.15);
  border-radius: 8px;
  animation: rect-rotate 30s linear infinite;
}

.rect-1 {
  width: 100px;
  height: 60px;
  top: 60%;
  left: 80%;
  animation-delay: -5s;
}

.rect-2 {
  width: 70px;
  height: 100px;
  top: 15%;
  right: 25%;
  animation-delay: -18s;
}

@keyframes circle-drift {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
    opacity: 0.4;
  }
  50% {
    transform: translateY(-50px) rotate(180deg);
    opacity: 0.8;
  }
}

@keyframes rect-rotate {
  0% {
    transform: rotate(0deg) scale(1);
    opacity: 0.3;
  }
  50% {
    transform: rotate(180deg) scale(1.1);
    opacity: 0.6;
  }
  100% {
    transform: rotate(360deg) scale(1);
    opacity: 0.3;
  }
}

/* 柔光效果 */
.ambient-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 30% 30%, rgba(103, 58, 183, 0.1) 0%, transparent 50%),
  radial-gradient(circle at 70% 70%, rgba(33, 150, 243, 0.08) 0%, transparent 50%),
  radial-gradient(circle at 50% 50%, rgba(156, 39, 176, 0.05) 0%, transparent 70%);
  animation: ambient-pulse 15s ease-in-out infinite;
}

@keyframes ambient-pulse {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 0.8;
  }
}

/* 优雅扫描线 */
.elegant-scanline {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(255, 255, 255, 0.02) 50%,
      transparent 100%
  );
  background-size: 200% 100%;
  pointer-events: none;
  z-index: 3;
  animation: elegant-scan 8s linear infinite;
}

@keyframes elegant-scan {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.login-content {
  position: relative;
  z-index: 10;
  height: 100vh;
}

.login-card {
  backdrop-filter: blur(30px);
  background: rgba(26, 26, 58, 0.85) !important;
  border: 1px solid rgba(103, 58, 183, 0.2);
  border-radius: 20px !important;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4),
  0 0 0 1px rgba(103, 58, 183, 0.1),
  inset 0 1px 0 rgba(255, 255, 255, 0.05);

  &:hover {
    transform: translateY(-6px) scale(1.01);
    box-shadow: 0 20px 60px rgba(103, 58, 183, 0.2),
    0 0 0 1px rgba(103, 58, 183, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
    border-color: rgba(103, 58, 183, 0.4);
  }
}

.logo-avatar {
  background: linear-gradient(135deg, #673ab7 0%, #2196f3 50%, #9c27b0 100%) !important;
  box-shadow: 0 8px 32px rgba(103, 58, 183, 0.3),
  0 0 0 1px rgba(103, 58, 183, 0.2);
  padding: 15px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(45deg, transparent, rgba(255, 255, 255, 0.1), transparent);
    animation: logo-shimmer 4s linear infinite;
  }
}

@keyframes logo-shimmer {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.pulse-animation {
  animation: elegant-pulse 3s ease-in-out infinite;
}

@keyframes elegant-pulse {
  0%, 100% {
    box-shadow: 0 8px 32px rgba(103, 58, 183, 0.3),
    0 0 0 0 rgba(33, 150, 243, 0.3);
  }
  50% {
    box-shadow: 0 8px 32px rgba(103, 58, 183, 0.5),
    0 0 0 15px rgba(33, 150, 243, 0);
  }
}

.gradient-text {
  background: linear-gradient(90deg, #673ab7, #2196f3, #9c27b0, #673ab7);
  background-size: 300% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: text-gradient 4s ease-in-out infinite;
}

@keyframes text-gradient {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

:deep(.glass-input) {
  .v-field {
    background: rgba(26, 26, 58, 0.8) !important;
    border: 1px solid rgba(103, 58, 183, 0.25);
    backdrop-filter: blur(20px);
    border-radius: 14px;
    transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(103, 58, 183, 0.1), transparent);
      transition: 0.8s;
    }

    &:hover::before {
      left: 100%;
    }

    &:hover {
      background: rgba(26, 26, 58, 0.9) !important;
      border-color: rgba(103, 58, 183, 0.5);
      box-shadow: 0 0 25px rgba(103, 58, 183, 0.2),
      0 8px 32px rgba(0, 0, 0, 0.3);
      transform: translateY(-1px);
    }
  }

  .v-field--focused {
    background: rgba(26, 26, 58, 0.95) !important;
    border-color: #673ab7 !important;
    box-shadow: 0 0 30px rgba(103, 58, 183, 0.4),
    0 8px 32px rgba(0, 0, 0, 0.4);
    transform: translateY(-1px);
  }

  .v-field__input,
  .v-label,
  .v-icon {
    color: rgba(255, 255, 255, 0.95) !important;
  }

  .v-label.v-field-label--floating {
    color: #673ab7 !important;
  }
}

.submit-btn {
  background: linear-gradient(135deg, #673ab7 0%, #2196f3 50%, #9c27b0 100%) !important;
  background-size: 200% 100%;
  color: white !important;
  font-weight: 600;
  text-transform: none;
  border-radius: 14px;
  box-shadow: 0 8px 25px rgba(103, 58, 183, 0.3),
  0 0 0 1px rgba(103, 58, 183, 0.2);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  overflow: hidden;
  position: relative;
  animation: button-shimmer 4s ease-in-out infinite;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(
        90deg,
        transparent,
        rgba(255, 255, 255, 0.2),
        transparent
    );
    transition: 0.6s;
  }

  &:hover::before {
    left: 100%;
  }

  &:hover {
    transform: translateY(-3px) scale(1.02);
    box-shadow: 0 12px 35px rgba(103, 58, 183, 0.4),
    0 0 0 1px rgba(103, 58, 183, 0.3);
    background-position: 100% 0;
  }

  &:active {
    transform: translateY(-1px) scale(0.98);
  }
}

@keyframes button-shimmer {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.social-btn {
  background: rgba(26, 26, 58, 0.8) !important;
  border: 1px solid rgba(103, 58, 183, 0.3) !important;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.9) !important;
  transition: all 0.3s ease;
  backdrop-filter: blur(15px);

  &:hover {
    background: rgba(26, 26, 58, 0.95) !important;
    border-color: rgba(103, 58, 183, 0.5) !important;
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(103, 58, 183, 0.2);
  }
}

:deep(.v-checkbox) {
  .v-label {
    color: rgba(255, 255, 255, 0.8) !important;
  }

  .v-selection-control__input {
    color: #673ab7 !important;
  }
}

:deep(.v-divider) {
  border-color: rgba(103, 58, 183, 0.3);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, #673ab7, transparent);
    animation: divider-shimmer 3s ease-in-out infinite;
  }
}

@keyframes divider-shimmer {
  0%, 100% {
    opacity: 0.4;
  }
  50% {
    opacity: 0.8;
  }
}

:deep(.v-chip) {
  background-color: rgba(103, 58, 183, 0.15) !important;
  border-color: rgba(103, 58, 183, 0.4) !important;
  color: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(15px);
  animation: chip-glow 4s ease-in-out infinite;
}

@keyframes chip-glow {
  0%, 100% {
    box-shadow: 0 0 8px rgba(103, 58, 183, 0.2);
  }
  50% {
    box-shadow: 0 0 15px rgba(103, 58, 183, 0.4);
  }
}
</style>
