<script setup lang="ts">
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
</script>

<template>
  <div class="min-h-screen bg-[var(--app-bg)] text-[var(--app-text)] transition-colors duration-300">
    <Toast position="top-right" />
    <ConfirmDialog />
    <slot />
  </div>
</template>
