<template>
  <div class="loading-state d-flex flex-column align-center justify-center">
    <v-progress-circular
      :color="color"
      :size="size"
      :width="width"
      indeterminate
    />
    <p v-if="message" class="mt-4 text-body-2 text-medium-emphasis">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: ''
  },
  size: {
    type: [Number, String],
    default: 48
  },
  width: {
    type: [Number, String],
    default: 3
  },
  color: {
    type: String,
    default: 'primary'
  }
})
</script>

<style scoped>
.loading-state {
  padding: 32px;
}
</style>
