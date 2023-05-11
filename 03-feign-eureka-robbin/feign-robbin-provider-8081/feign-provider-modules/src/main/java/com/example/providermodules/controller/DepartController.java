package com.example.providermodules.controller;


import com.example.vo.DepartVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RequestMapping("/provider/depart")
@RestController
public class DepartController {

    //方便后面讲负载均衡，查看ip，此处获取配置中的端口号和ip
    @Value("${server.port}")
    private String port;
    @Value("${spring.cloud.client.ip-address}")
    private String ip;


    @PostMapping("/save")
    public boolean saveHandle(@RequestBody DepartVO DepartVO) {
        System.out.println("DepartVO = " + DepartVO);
        return true;
    }

    @DeleteMapping("/del/{id}")
    public boolean deleteHandle(@PathVariable("id") int id) {
        System.out.println("id = " + id);
        return true;
    }

    @PutMapping("/update")
    public boolean updateHandle(@RequestBody DepartVO DepartVO) {
        System.out.println("DepartVO = " + DepartVO);
        return true;
    }

    @GetMapping("/get/{id}")
    public DepartVO getHandle(@PathVariable("id") int id) {
        DepartVO departVO = new DepartVO();
        departVO.setName("当前访问服务地址：" + ip + ":" + port+"  "+"查询商品订单，订单号："+id);

        return departVO;
    }

    @GetMapping("/list")
    public List<DepartVO> listHandle() {
        return new ArrayList<>();
    }


}
