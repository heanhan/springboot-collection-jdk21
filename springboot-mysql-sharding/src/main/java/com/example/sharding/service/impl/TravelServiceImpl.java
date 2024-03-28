package com.example.sharding.service.impl;

import com.example.sharding.dao.TravelDao;
import com.example.sharding.service.TravelService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TravelServiceImpl implements TravelService {

    @Resource
    private TravelDao travelDao;


}
