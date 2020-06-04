package com.leyou.item.service;

import com.leyou.item.model.Category;

import java.util.List;

public interface CategoryService {

    public List<Category> queryCategoryListByPid(Long pid);

    List<Category> queryCategoryByIds(List<Long> ids);

    List<Category> queryAllByCid3(Long id);
}
