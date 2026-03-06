import { defineStore } from 'pinia'
import { getHouses, getHouseDetail, searchHouses as searchHousesApi } from '../api/house.js'

export const useHouseStore = defineStore('house', {
  state: () => ({
    houses: [],
    currentHouse: null,
    total: 0,
    page: 1,
    pageSize: 12,
    filters: {
      city: '',
      district: '',
      minPrice: null,
      maxPrice: null,
      rooms: '',
      ownerType: '',
      decoration: '',
      keyword: ''
    }
  }),
  actions: {
    async fetchHouses(params = {}) {
      const query = {
        page: this.page,
        pageSize: this.pageSize,
        ...this.filters,
        ...params
      }
      const res = await getHouses(query)
      if (res && res.list !== undefined) {
        this.houses = res.list
        this.total = res.total
      } else if (Array.isArray(res)) {
        this.houses = res
        this.total = res.length
      }
      return res
    },
    async fetchHouseDetail(id) {
      const res = await getHouseDetail(id)
      this.currentHouse = res
      return res
    },
    async searchHouses(keyword) {
      const res = await searchHousesApi({ keyword })
      if (res && res.list !== undefined) {
        this.houses = res.list
        this.total = res.total
      } else if (Array.isArray(res)) {
        this.houses = res
        this.total = res.length
      }
      return res
    },
    setFilters(filters) {
      this.filters = { ...this.filters, ...filters }
    }
  }
})
