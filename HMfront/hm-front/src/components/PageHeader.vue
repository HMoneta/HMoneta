<template>
  <div class="page-header mb-6">
    <div class="d-flex align-center">
      <v-icon v-if="icon" :icon="icon" :size="iconSize" class="mr-3" color="primary" />
      <div>
        <h1 class="text-h5 font-weight-medium">{{ title }}</h1>
        <p v-if="subtitle" class="text-body-2 text-medium-emphasis mt-1">{{ subtitle }}</p>
      </div>
      <v-spacer />
      <slot name="actions" />
    </div>
    <v-divider v-if="divider !== false" class="mt-4" />
  </div>
</template>

<script setup>
defineProps({
  icon: {
    type: String,
    default: ''
  },
  iconSize: {
    type: [Number, String],
    default: 32
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  divider: {
    type: Boolean,
    default: true
  }
})
</script>

<style scoped>
.page-header {
  padding-bottom: 8px;
}
</style>
