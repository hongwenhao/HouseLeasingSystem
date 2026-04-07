package com.houseleasing.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 分页查询结果封装类
 *
 * @author hongwenhao
 * @description 用于封装分页查询的结果，包含总记录数、当前页数据、页码和页大小
 * @param <T> 分页数据的类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    /** 总记录数 */
    private long total;
    /** 当前页的数据列表 */
    private List<T> records;
    /** 当前页码（从1开始） */
    private int page;
    /** 每页显示条数 */
    private int size;

    /**
     * 静态工厂方法，快速创建分页结果对象
     *
     * @param <T>     数据类型
     * @param total   总记录数
     * @param records 当前页的数据列表
     * @param page    当前页码
     * @param size    每页大小
     * @return 封装好的分页结果对象
     */
    public static <T> PageResult<T> of(long total, List<T> records, int page, int size) {
        return new PageResult<>(total, records, page, size);
    }
}
