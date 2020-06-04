package com.leyou.item.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.CartDto;
import com.leyou.item.mapper.*;
import com.leyou.item.model.*;
import com.leyou.item.service.GoodsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 分页查询Spu
     * @param page
     * @param rows
     * @param key
     * @param saleable
     * @return
     */
    @Override
    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, String key, Boolean saleable) {
        //分页
        PageHelper.startPage(page, rows);

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();

        if (StringUtils.isNoneBlank(key)){
            criteria.andLike("title", "%"+ key +"%");
        }
        if (saleable != null){
            criteria.orEqualTo("saleable", saleable);
        }
        //默认以上一次更新时间排序
        example.setOrderByClause("last_update_time desc");
        //只查询未删除商品
        criteria.andEqualTo("valid", 1);
        //查询
        List<Spu> spus = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(spus)){
            throw new LyException(ExceptionEnum.SPU_NOT_FOUND);
        }
        //对查询结果中的品牌名和分类名处理
        handleCategoryAndBrand(spus);
        PageInfo<Spu> pageInfo = new PageInfo<>(spus);

        return new PageResult<>(pageInfo.getTotal(), spus);
    }

    /**
     * 处理商品品牌名和分类名
     * @param spus
     */
    private void handleCategoryAndBrand(List<Spu> spus) {
        for (Spu spu : spus) {
            //根据spu中的分类ids查询分类名
            List<String> names = categoryMapper.selectByIdList(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            //对分类名处理
            spu.setCname(StringUtils.join(names, "/"));

            //查询品牌
            spu.setBname(brandMapper.selectByPrimaryKey(spu.getBrandId()).getName());
        }
    }

    /**
     * 根据SpuId查询SpuDetail
     * @param spuId
     * @return
     */
    @Override
    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null){
            throw new LyException(ExceptionEnum.SPU_NOT_FOUND);
        }
        return spuDetail;
    }

    /**
     * 根据SpuId查询Sku
     * @param spuId
     * @return
     */
    @Override
    public List<Sku> querySkuBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.SKU_NOT_FOUND);
        }
        //查库存
        for (Sku sku1 : skuList) {
            sku1.setStock(stockMapper.selectByPrimaryKey(sku1.getId()).getStock());
        }
        return skuList;
    }

    /**
     * 通过ids查询Skus
     * @param ids
     * @return
     */
    @Override
    public List<Sku> querySkusByIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skus)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //填充库存
        fillStock(ids, skus);
        return skus;
    }

    private void fillStock(List<Long> ids, List<Sku> skus) {
        //批量查询库存
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stocks)){
            throw new LyException(ExceptionEnum.STOCK_NOT_FOUND);
        }
        //先将库存转换为map, key为sku的id
        Map<Long, Integer> map = stocks.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        //遍历skus,并填充库存
        for (Sku sku : skus) {
            sku.setStock(map.get(sku.getId()));
        }
    }

    /**
     * 通过spuId删除商品
     * @param spuId
     */
    @Transactional
    @Override
    public void deleteGoodsBySpuId(Long spuId) {
        if (spuId == null){
            throw new LyException(ExceptionEnum.INVALID_PARAM);
        }
        //删除spu,把spu中的valid字段设置为false
        Spu spu = new Spu();
        spu.setId(spuId);
        spu.setValid(false);
        int resultNum = spuMapper.updateByPrimaryKeySelective(spu);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.DELETE_GOODS_ERROR);
        }
        //发送消息
        sendMessage(spuId, "delete");
    }

    /**
     * 封装发送到消息队列
     * @param id
     * @param type
     */
    private void sendMessage(Long id, String type) {
        try {
            amqpTemplate.convertAndSend("item." + type, id);
        }catch (Exception e){
            log.error("{}商品消息发送异常, 商品ID: {}", type, id, e);
        }
    }

    /**
     * 添加商品
     * @param spu
     */
    @Transactional
    @Override
    public void addGoods(Spu spu) {
        //添加商品要添加四个表spu、spuDetail、sku、stock
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        //插入数据
        int insert = spuMapper.insert(spu);
        if (insert != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        //插入spuDetail数据
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        insert = spuDetailMapper.insert(spuDetail);
        if (insert != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        //插入sku和库存
        saveSkuAndStock(spu);

        //发送消息
        sendMessage(spu.getId(), "insert");
    }

    /**
     * 保存sku和库存
     * @param spu
     */
    private void saveSkuAndStock(Spu spu) {
        List<Sku> skuList = spu.getSkus();
        List<Stock> stocks = new ArrayList<>();
        for (Sku sku : skuList) {
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            int insert = skuMapper.insert(sku);
            if (insert != 1){
                throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
            }

            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stocks.add(stock);
        }
        //批量插入库存
        int resultNum = stockMapper.insertList(stocks);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    /**
     * 更新商品
     * @param spu
     */
    @Transactional
    @Override
    public void updateGoods(Spu spu) {
        if (spu.getId() == 0){
            throw new LyException(ExceptionEnum.INVALID_PARAM);
        }
        //查询sku
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)){
            //删除所有sku
            skuMapper.delete(sku);
            //删除库存
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        //更新数据库 spu spuDetail
        spu.setLastUpdateTime(new Date());
        int update = spuMapper.updateByPrimaryKeySelective(spu);
        if (update != 1){
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }

        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        int update1 = spuDetailMapper.updateByPrimaryKeySelective(spuDetail);
        if (update1 != 1){
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        //更新sku和stock
        saveSkuAndStock(spu);
        //发送消息
        sendMessage(spu.getId(), "update");
    }

    @Override
    public void handleSaleable(Spu spu) {
        spu.setSaleable(!spu.getSaleable());
        int update = spuMapper.updateByPrimaryKeySelective(spu);
        if (update != 1){
            throw new LyException(ExceptionEnum.UPDATE_SALEABLE_ERROR);
        }
    }

    /**
     * 通过spuId查询spu
     * @param spuId
     * @return
     */
    @Override
    public Spu querySpuBySpuId(Long spuId) {
        //根据spuId查询spu
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        //查询spuDetail
        SpuDetail spuDetail = querySpuDetailBySpuId(spuId);
        //查询skus
        List<Sku> skus = querySkuBySpuId(spuId);
        spu.setSpuDetail(spuDetail);
        spu.setSkus(skus);
        return spu;
    }

    /**
     *
     * @param cartDtos
     */
    @Transactional
    @Override
    public void decreaseStock(List<CartDto> cartDtos) {
        for (CartDto cartDto : cartDtos) {
            int resultNum = stockMapper.decreaseStock(cartDto.getSkuId(), cartDto.getNum());
            if (resultNum != 1){
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
