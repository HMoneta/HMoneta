import {defineStore} from 'pinia'
import {http} from "@/common/request.js"

export const useLogStore = defineStore('log', {
  state: () => ({
    historicalLogs: [],
    pagination: {page: 0, size: 50, totalPages: 0, total: 0},
    filters: {startTime: null, endTime: null, level: null, service: null},
    services: []
  }),
  actions: {
    async queryLogs() {
      const params = {}
      if (this.filters.startTime) params.startTime = this.filters.startTime
      if (this.filters.endTime) params.endTime = this.filters.endTime
      if (this.filters.level) params.level = this.filters.level
      if (this.filters.service) params.service = this.filters.service
      params.page = this.pagination.page
      params.size = this.pagination.size
      const resp = await http.get('/logs', params)
      if (resp && resp.logs) {
        this.historicalLogs = resp.logs.map(log => ({
          time: new Date(log.timestamp).toLocaleTimeString(),
          level: log.level,
          levelColor: log.level === 'ERROR' ? '#ef4444' : log.level === 'WARN' ? '#f59e0b' : log.level === 'DEBUG' ? '#8b5cf6' : '#10b981',
          service: log.service || '',
          content: log.message || '',
          thread: log.thread || '',
          raw: log
        }))
        this.pagination.total = resp.total
        this.pagination.totalPages = resp.totalPages
      }
      return resp
    },
    async fetchServices() {
      const services = await http.get('/logs/services')
      if (services) {
        this.services = services
      }
    },
    setFilters(filters) {
      this.filters = {...this.filters, ...filters}
      this.pagination.page = 0
    },
    setPage(page) {
      this.pagination.page = page
    }
  }
})
