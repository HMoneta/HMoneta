// Utilities
import {defineStore} from 'pinia'
import {http} from "@/common/request.js"

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    isValidToken: false,
  }),
  actions: {
    login(token) {
      this.token = token;
      sessionStorage.setItem('HMToken', token);
    },
    logout() {
      this.token = null;
      sessionStorage.removeItem('HMToken');
    },
    async checkAuth() {
      const token = sessionStorage.getItem('HMToken');
      if (token) {
        const valid = await http.get('/user/valid', {HMToken: token})
        if (valid) {
          this.token = token;
          this.isValidToken = valid;
          return valid;
        } else {
          sessionStorage.removeItem('HMToken');
          this.isValidToken = false;
          return false;
        }
      } else {
        sessionStorage.removeItem('HMToken');
        this.isValidToken = false;
        return false;
      }
    },
  },
});

export const userNotificationStore = defineStore('userNotification', {
  state: () => ({
    show: false,
    message: '',
    color: 'error'
  }),
  actions: {
    showError(msg) {
      this.message = msg;
      this.color = 'error';
      this.show = true;
    },
    showSuccess(msg) {
      this.message = msg;
      this.color = 'success';
      this.show = true;
    }
  }
});
