// Utilities
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
  }),
  actions: {
    login(token) {
      this.token = token;
      localStorage.setItem('HMToken', token);
    },
    logout() {
      this.token = null;
      localStorage.removeItem('HMToken');
    },
    checkAuth() {
      const token = localStorage.getItem('HMToken');
      if (token) {
        this.token = token;
        return true;
      }else {
        localStorage.removeItem('HMToken');
        return false;
      }
    },
  },
});
