package com.leyou.item.service;

import com.leyou.item.model.SpecGroup;
import com.leyou.item.model.SpecParam;

import java.util.List;

public interface SpecService {

    List<SpecGroup> querySpecGroupByCid(Long cid);

    List<SpecParam> queryParam(Long gid, Long cid, Boolean searching, Boolean generic);

    void saveSpecGroup(SpecGroup specGroup);

    void updateSpecGroup(SpecGroup specGroup);

    void saveSpecParam(SpecParam specParam);

    void deleteSpecGroup(Long id);

    void deleteSpecParam(Long id);

    void updateSpecParam(SpecParam specParam);

    List<SpecGroup> querySpecsByCid(Long cid);
}
