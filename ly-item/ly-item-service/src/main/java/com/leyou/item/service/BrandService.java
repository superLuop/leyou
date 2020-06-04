package com.leyou.item.service;

import com.leyou.common.vo.PageResult;
import com.leyou.item.model.Brand;
import com.leyou.item.vo.BrandVo;
import com.leyou.item.model.Category;

import java.util.List;

public interface BrandService {
    public PageResult<Brand> queryBrandByPage(Integer pageNum, Integer pageSize, String sortBy, Boolean desc, String key);

    public void saveBrand(Brand brand, List<Long> cids);

    List<Category> queryCategoryByBid(Long bid);

    void updateBrand(BrandVo brandVo);

    void deleteBrand(Long bid);

    List<Brand> queryBrandByCid(Long cid);

    Brand queryBrandByBid(Long id);

    List<Brand> queryBrandByIds(List<Long> ids);
}
