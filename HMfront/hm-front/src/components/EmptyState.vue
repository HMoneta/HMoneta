<template>
  <div class="empty-state d-flex flex-column align-center justify-center text-center pa-8">
    <v-icon
      :icon="icon"
      :size="iconSize"
      class="text-disabled mb-4"
    />
    <h3 class="text-h6 mb-2">{{ title }}</h3>
    <p v-if="description" class="text-body-2 text-medium-emphasis mb-4 max-width-300">
      {{ description }}
    </p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  icon: {
    type: String,
    default: 'mdi-inbox-outline'
  },
  iconSize: {
    type: [Number, String],
    default: 64
  },
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.empty-state {
  min-height: 200px;
}

.max-width-300 {
  max-width: 300px;
}
</style>
