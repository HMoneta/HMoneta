<template>
  <div class="login-container">
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>

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
                  size="80"
                  class="mb-4 logo-avatar"
                  color="primary"
                >
                  <v-icon size="40" color="white">mdi-lock</v-icon>
                </v-avatar>

                <h2 class="text-h4 font-weight-bold text-white mb-2">
                  {{ isLogin ? 'Welcome Back' : 'Create Account' }}
                </h2>

                <p class="text-subtitle-1 text-grey-lighten-1">
                  {{ isLogin ? 'Login to continue' : 'Sign up to get started' }}
                </p>
              </div>

              <!-- Form -->
              <v-form ref="loginForm" @submit.prevent="handleSubmit">
                <!-- Email (for signup) -->
                <v-expand-transition>
                  <div v-if="!isLogin" class="mb-4">
                    <v-text-field
                      v-model="email"
                      label="Email"
                      prepend-inner-icon="mdi-email"
                      type="email"
                      variant="outlined"
                      density="comfortable"
                      class="glass-input"
                      :rules="emailRules"
                    ></v-text-field>
                  </div>
                </v-expand-transition>

                <!-- Username -->
                <v-text-field
                  v-model="username"
                  label="Username"
                  prepend-inner-icon="mdi-account"
                  variant="outlined"
                  density="comfortable"
                  class="glass-input mb-4"
                  :rules="usernameRules"
                ></v-text-field>

                <!-- Password -->
                <v-text-field
                  v-model="password"
                  label="Password"
                  prepend-inner-icon="mdi-lock"
                  :type="showPassword ? 'text' : 'password'"
                  :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                  @click:append-inner="showPassword = !showPassword"
                  variant="outlined"
                  density="comfortable"
                  class="glass-input mb-4"
                  :rules="passwordRules"
                ></v-text-field>

                <!-- Remember Me / Forgot Password -->
                <v-expand-transition>
                  <div v-if="isLogin" class="d-flex justify-space-between align-center mb-6">
                    <v-checkbox
                      v-model="rememberMe"
                      label="Remember me"
                      density="compact"
                      hide-details
                      color="primary"
                      class="text-grey-lighten-1"
                    ></v-checkbox>

                    <v-btn
                      variant="text"
                      size="small"
                      color="primary"
                      class="text-none"
                    >
                      Forgot password?
                    </v-btn>
                  </div>
                </v-expand-transition>

                <!-- Submit Button -->
                <v-btn
                  type="submit"
                  block
                  size="large"
                  class="submit-btn mb-6"
                  :loading="loading"
                >
                  {{ isLogin ? 'Sign In' : 'Sign Up' }}
                </v-btn>
              </v-form>

              <!-- Toggle Login/Signup -->
              <div class="text-center mb-6">
                <span class="text-grey-lighten-1">
                  {{ isLogin ? "Don't have an account?" : "Already have an account?" }}
                </span>
                <v-btn
                  variant="text"
                  color="primary"
                  class="text-none font-weight-bold"
                  @click="toggleMode"
                >
                  {{ isLogin ? 'Sign Up' : 'Sign In' }}
                </v-btn>
              </div>

              <!-- Divider -->
              <v-divider class="my-6">
                <span class="text-grey px-4">Or continue with</span>
              </v-divider>

              <!-- Social Login -->
              <v-row dense>
                <v-col cols="4">
                  <v-btn
                    block
                    variant="outlined"
                    class="social-btn"
                  >
                    <v-icon>mdi-google</v-icon>
                  </v-btn>
                </v-col>
                <v-col cols="4">
                  <v-btn
                    block
                    variant="outlined"
                    class="social-btn"
                  >
                    <v-icon>mdi-github</v-icon>
                  </v-btn>
                </v-col>
                <v-col cols="4">
                  <v-btn
                    block
                    variant="outlined"
                    class="social-btn"
                  >
                    <v-icon>mdi-twitter</v-icon>
                  </v-btn>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup>
import {onMounted, onUnmounted, ref} from 'vue'

// Form state
const isLogin = ref(true)
const username = ref('')
const password = ref('')
const email = ref('')
const showPassword = ref(false)
const rememberMe = ref(false)
const loading = ref(false)
const loginForm = ref(null)

// Particle canvas
const particleCanvas = ref(null)
let animationId = null

// Validation rules
const usernameRules = [
  v => !!v || 'Username is required',
  v => v.length >= 3 || 'Username must be at least 3 characters'
]

const passwordRules = [
  v => !!v || 'Password is required',
  v => v.length >= 6 || 'Password must be at least 6 characters'
]

const emailRules = [
  v => !!v || 'Email is required',
  v => /.+@.+\..+/.test(v) || 'Email must be valid'
]

// Methods
const toggleMode = () => {
  isLogin.value = !isLogin.value
  if (loginForm.value) {
    loginForm.value.reset()
  }
}

const handleSubmit = async () => {
  const {valid} = await loginForm.value.validate()

  if (valid) {
    loading.value = true

    // Simulate API call
    setTimeout(() => {
      console.log('Form submitted:', {
        username: username.value,
        password: password.value,
        email: email.value,
        isLogin: isLogin.value
      })
      loading.value = false
    }, 1500)
  }
}

// Particle animation
const initParticles = () => {
  const canvas = particleCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles = []
  const particleCount = 100
  const connectionDistance = 150

  class Particle {
    constructor() {
      this.x = Math.random() * canvas.width
      this.y = Math.random() * canvas.height
      this.vx = (Math.random() - 0.5) * 0.5
      this.vy = (Math.random() - 0.5) * 0.5
      this.radius = Math.random() * 2 + 1
    }

    update() {
      this.x += this.vx
      this.y += this.vy

      if (this.x < 0 || this.x > canvas.width) this.vx *= -1
      if (this.y < 0 || this.y > canvas.height) this.vy *= -1
    }

    draw() {
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(139, 92, 246, 0.5)'
      ctx.fill()
    }
  }

  for (let i = 0; i < particleCount; i++) {
    particles.push(new Particle())
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    particles.forEach(particle => {
      particle.update()
      particle.draw()
    })

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const distance = Math.sqrt(dx * dx + dy * dy)

        if (distance < connectionDistance) {
          ctx.beginPath()
          ctx.strokeStyle = `rgba(139, 92, 246, ${0.2 * (1 - distance / connectionDistance)})`
          ctx.lineWidth = 0.5
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.stroke()
        }
      }
    }

    animationId = requestAnimationFrame(animate)
  }

  animate()

  const handleResize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }

  window.addEventListener('resize', handleResize)

  return () => {
    window.removeEventListener('resize', handleResize)
  }
}

onMounted(() => {
  const cleanup = initParticles()

  onUnmounted(() => {
    if (animationId) {
      cancelAnimationFrame(animationId)
    }
    if (cleanup) {
      cleanup()
    }
  })
})
</script>

<style scoped lang="scss">
.login-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #0f172a 0%, #581c87 50%, #0f172a 100%);
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.login-content {
  position: relative;
  z-index: 1;
  height: 100vh;
}

.login-card {
  backdrop-filter: blur(20px);
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px !important;
  transition: all 0.3s ease;

  &:hover {
    transform: scale(1.02);
    box-shadow: 0 20px 60px rgba(139, 92, 246, 0.3) !important;
  }
}

.logo-avatar {
  background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, #a855f7 100%) !important;
  box-shadow: 0 8px 32px rgba(139, 92, 246, 0.4);
}

:deep(.glass-input) {
  .v-field {
    background: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border-radius: 12px;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.08) !important;
      border-color: rgba(139, 92, 246, 0.3);
      box-shadow: 0 0 20px rgba(139, 92, 246, 0.1);
    }
  }

  .v-field--focused {
    background: rgba(255, 255, 255, 0.08) !important;
    border-color: rgb(var(--v-theme-primary)) !important;
    box-shadow: 0 0 20px rgba(139, 92, 246, 0.2);
  }

  .v-field__input,
  .v-label,
  .v-icon {
    color: rgba(255, 255, 255, 0.9) !important;
  }
}

.submit-btn {
  background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, #a855f7 100%) !important;
  color: white !important;
  font-weight: 600;
  text-transform: none;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(139, 92, 246, 0.4);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 32px rgba(139, 92, 246, 0.6);
    background: linear-gradient(135deg, #a855f7 0%, rgb(var(--v-theme-primary)) 100%) !important;
  }
}

.social-btn {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.9) !important;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1) !important;
    border-color: rgba(139, 92, 246, 0.5) !important;
    transform: translateY(-2px);
  }
}

:deep(.v-checkbox) {
  .v-label {
    color: rgba(255, 255, 255, 0.7) !important;
  }
}

:deep(.v-divider) {
  border-color: rgba(255, 255, 255, 0.1);
}
</style>
