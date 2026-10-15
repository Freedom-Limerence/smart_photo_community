<template>
  <a-config-provider :locale="zhCN" :theme="antdTheme">
    <router-view v-slot="{ Component, route }">
      <transition name="page" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { antdTheme } from '@/config/antdTheme'

dayjs.locale('zh-cn')
</script>

<style>
@import '@/styles/tokens.css';
@import '@/styles/app.css';
</style>
