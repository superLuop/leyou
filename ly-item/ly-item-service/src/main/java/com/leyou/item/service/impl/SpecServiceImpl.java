package com.leyou.item.service.impl;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.model.SpecGroup;
import com.leyou.item.model.SpecParam;
import com.leyou.item.service.SpecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Transactional
@Service
public class SpecServiceImpl implements SpecService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    @Override
    public List<SpecGroup> querySpecGroupByCid(Long cid) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        List<SpecGroup> list = specGroupMapper.select(specGroup);
        if (CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return list;
    }

    /**
     * 添加规格组
     * @param specGroup
     */
    @Override
    public void saveSpecGroup(SpecGroup specGroup) {
        int resultNum = specGroupMapper.insert(specGroup);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.SPEC_GROUP_CREATE_FAILED);
        }
    }

    /**
     * 更新规格组
     * @param specGroup
     */
    @Override
    public void updateSpecGroup(SpecGroup specGroup) {
        int resultNum = specGroupMapper.updateByPrimaryKey(specGroup);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.UPDATE_SPEC_GROUP_FAILED);
        }
    }

    /**
     * 删除规格组
     * @param id
     */
    @Override
    public void deleteSpecGroup(Long id) {
        if (id == null){
            throw new LyException(ExceptionEnum.INVALID_PARAM);
        }
        SpecGroup specGroup = new SpecGroup();
        specGroup.setId(id);
        int resultNum = specGroupMapper.deleteByPrimaryKey(specGroup);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.DELETE_SPEC_GROUP_FAILED);
        }
    }

    /**
     * 查询规格参数
     * @param gid
     * @param cid
     * @param searching
     * @param generic
     * @return
     */
    @Override
    public List<SpecParam> queryParam(Long gid, Long cid, Boolean searching, Boolean generic) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setSearching(searching);
        specParam.setGeneric(generic);
        List<SpecParam> paramList = specParamMapper.select(specParam);
        if (CollectionUtils.isEmpty(paramList)){
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        return paramList;
    }

    /**
     * 新增规格参数
     * @param specParam
     */
    @Override
    public void saveSpecParam(SpecParam specParam) {
        int resultNum = specParamMapper.insert(specParam);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.SPEC_PARAM_CREATE_FAILED);
        }
    }

    /**
     * 删除规格参数
     * @param id
     */
    @Override
    public void deleteSpecParam(Long id) {
        if (id == null){
            throw new LyException(ExceptionEnum.INVALID_PARAM);
        }
        int resultNum = specParamMapper.deleteByPrimaryKey(id);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.DELETE_SPEC_PARAM_FAILED);
        }
    }

    /**
     * 更新规格参数
     * @param specParam
     */
    @Override
    public void updateSpecParam(SpecParam specParam) {
        int resultNum = specParamMapper.updateByPrimaryKeySelective(specParam);
        if (resultNum != 1){
            throw new LyException(ExceptionEnum.UPDATE_SPEC_PARAM_FAILED);
        }
    }

    /**
     * 通过分类id查询规格组
     * @param cid
     * @return
     */
    @Override
    public List<SpecGroup> querySpecsByCid(Long cid) {
        List<SpecGroup> specGroups = querySpecGroupByCid(cid);
        List<SpecParam> specParams = queryParam(null, cid, null, null);
        Map<Long, List<SpecParam>> map = new HashMap<>();
        for (SpecParam param : specParams) {
            Long groupId = param.getGroupId();
            if (!map.containsKey(groupId)){
                map.put(groupId, new ArrayList<>());
            }
            map.get(groupId).add(param);
        }
        for (SpecGroup specGroup : specGroups) {
            specGroup.setParams(map.get(specGroup.getId()));
        }
        return specGroups;
    }
}
