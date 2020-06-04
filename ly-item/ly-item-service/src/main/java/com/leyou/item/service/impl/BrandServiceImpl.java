package com.leyou.item.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.model.Brand;
import com.leyou.item.model.Category;
import com.leyou.item.service.BrandService;
import com.leyou.item.vo.BrandVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Collections;
import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandMapper brandMapper;

    /**
     * 分页查询品牌
     * @param pageNum
     * @param pageSize
     * @param sortBy
     * @param desc
     * @param key
     * @return
     */
    @Override
    public PageResult<Brand> queryBrandByPage(Integer pageNum, Integer pageSize, String sortBy, Boolean desc, String key) {
        //使用分页助手开启分页
        PageHelper.startPage(pageNum, pageSize);
        //过滤
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)){
            example.createCriteria().orLike("name", "%" + key + "%").orEqualTo("letter", key);
        }
        if (StringUtils.isNotBlank(sortBy)){
            String sortByClause = sortBy + (desc ? " DESC" : " ASC");
            example.setOrderByClause(sortByClause);
        }
        List<Brand> brands = brandMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        PageInfo<Brand> brandPageInfo = new PageInfo<>(brands);
        return new PageResult<>(brandPageInfo.getTotal(), brands);
    }

    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    @Transactional
    @Override
    public void saveBrand(Brand brand, List<Long> cids) {
        brand.setId(null);
        int resultNum = brandMapper.insert(brand);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.BRAND_CREATE_FAILED);
        }
        for (Long cid : cids) {
            resultNum = brandMapper.save_category_brand(cid, brand.getId());
            if (resultNum != 1){
                throw new LyException(ExceptionEnum.BRAND_CREATE_FAILED);
            }
        }
    }

    @Override
    public List<Category> queryCategoryByBid(Long bid) {
        return brandMapper.queryCategoryByBid(bid);
    }

    /**
     * 更新品牌
     * @param brandVo
     */
    @Transactional
    @Override
    public void updateBrand(BrandVo brandVo) {
        Brand brand = new Brand();
        brand.setId(brandVo.getId());
        brand.setName(brandVo.getName());
        brand.setImage(brandVo.getImage());
        brand.setLetter(brandVo.getLetter());
        //更新
        int resultNum = brandMapper.updateByPrimaryKey(brand);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.UPDATE_BRAND_FAILED);
        }
        List<Long> cids = brandVo.getCids();

        //删除原来的数据
        brandMapper.deleteCategoryBrandByBid(brandVo.getId());

        for (Long cid : cids) {
            resultNum = brandMapper.save_category_brand(cid, brandVo.getId());
            if (resultNum != 1){
                throw new LyException(ExceptionEnum.UPDATE_BRAND_FAILED);
            }
        }
    }

    /**
     * 删除品牌
     * @param bid
     */
    @Transactional
    @Override
    public void deleteBrand(Long bid) {
        int resultNum = brandMapper.deleteByPrimaryKey(bid);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.DELETE_BRAND_EXCEPTION);
        }
        //删除中间表
        resultNum = brandMapper.deleteCategoryBrandByBid(bid);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.DELETE_BRAND_EXCEPTION);
        }
    }

    /**
     * 通过分类ID查询品牌
     * @param cid
     * @return
     */
    @Override
    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> brandList = brandMapper.queryBrandByCid(cid);
        if (CollectionUtils.isEmpty(brandList)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brandList;
    }

    /**
     * 通过品牌ID查询品牌
     * @param id
     * @return
     */
    @Override
    public Brand queryBrandByBid(Long id) {
        Brand brand = new Brand();
        brand.setId(id);
        Brand brand1 = brandMapper.selectByPrimaryKey(brand);
        if (brand1 == null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand1;
    }

    /**
     * 通过Ids查询品牌
     * @param ids
     * @return
     */
    @Override
    public List<Brand> queryBrandByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
