package com.example.consumer8080.controller;

import com.example.vo.DepartVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xlwang55
 * @date 2023/5/15 9:30
 */
@RestController
@RequestMapping("/test/depart")
public class TestController {
    @GetMapping("/get/{id}")
    public DepartVO getHandle(@PathVariable("id") int id) {
        DepartVO departVO = new DepartVO();
        departVO.setId(id);
        departVO.setName("测试名称");
        return departVO;
    }

}
