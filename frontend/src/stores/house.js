/**
 * stores/house.js —— 房源状态管理 Store（Pinia）
 *
 * 统一管理房源列表、当前房源详情、分页信息以及筛选条件。
 * 提供以下 actions：
 *   - fetchHouses()      分页获取房源列表（带筛选/排序条件）
 *   - fetchHouseDetail() 获取指定 ID 的房源详情
 *   - searchHouses()     按关键词搜索房源
 *   - setFilters()       更新筛选条件（不立即发起请求）
 */

import { defineStore } from 'pinia'
import { getHouses, getHouseDetail, searchHouses as searchHousesApi } from '../api/house.js'

export const useHouseStore = defineStore('house', {
  state: () => ({
    houses: [],          // 当前页房源列表
    currentHouse: null,  // 当前查看的房源详情
    total: 0,            // 符合条件的房源总数（用于分页）
    page: 1,             // 当前页码
    pageSize: 12,        // 每页显示数量
    /** 筛选条件对象，对应接口的查询参数 */
    filters: {
      city: '',          // 城市筛选
      district: '',      // 区域筛选
      minPrice: null,    // 最低月租金（元）
      maxPrice: null,    // 最高月租金（元）
      rooms: '',         // 房间数筛选
      ownerType: '',     // 房东类型：OWNER / SUBLEASE / AGENT
      decoration: '',    // 装修情况：FINE / SIMPLE / ROUGH
      keyword: ''        // 关键词模糊搜索
    }
  }),
  actions: {
    /**
     * 获取房源列表（支持分页和多条件筛选）
     * @param {Object} params - 额外覆盖参数（可选）
     * 将当前 page、pageSize、filters 合并后发起请求
     * 兼容后端返回分页对象 { list, total } 和直接返回数组两种格式
     */
    async fetchHouses(params = {}) {
      const query = {
        page: this.page,
        pageSize: this.pageSize,
        ...this.filters, // 合并筛选条件
        ...params        // 允许外部覆盖特定参数
      }
      const res = await getHouses(query)
      if (res && res.records !== undefined) {
        this.houses = res.records
        this.total = res.total ?? res.records.length
      } else if (res && res.list !== undefined) {
        // 后端返回分页包装格式
        this.houses = res.list
        this.total = res.total
      } else if (Array.isArray(res)) {
        // 后端直接返回数组格式
        this.houses = res
        this.total = res.length
      }
      return res
    },

    /**
     * 获取单个房源的完整详情（含图片、费用配置、房东信息等）
     * @param {number|string} id - 房源 ID
     */
    async fetchHouseDetail(id) {
      const res = await getHouseDetail(id)
      this.currentHouse = res
      return res
    },

    /**
     * 按关键词搜索房源
     * @param {string} keyword - 搜索关键词（匹配标题、描述、地址）
     */
    async searchHouses(keyword) {
      const res = await searchHousesApi({ keyword })
      if (res && res.records !== undefined) {
        this.houses = res.records
        this.total = res.total ?? res.records.length
      } else if (res && res.list !== undefined) {
        this.houses = res.list
        this.total = res.total
      } else if (Array.isArray(res)) {
        this.houses = res
        this.total = res.length
      }
      return res
    },

    /**
     * 更新筛选条件（合并更新，不覆盖未传入的字段）
     * @param {Object} filters - 需要修改的筛选字段
     */
    setFilters(filters) {
      this.filters = { ...this.filters, ...filters }
    }
  }
})
