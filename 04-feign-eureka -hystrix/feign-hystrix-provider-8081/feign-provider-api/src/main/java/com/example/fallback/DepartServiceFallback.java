package com.example.fallback;

import com.example.providerapi.DepartService;
import com.example.vo.DepartVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: DepartServiceFallback
 * @Description: 失败回调类
 * @Author: wang xiao le
 * @Date: 2023/05/11 21:56
 **/
@Component
public class DepartServiceFallback implements DepartService {
    @Override
    public boolean saveDepart(DepartVO depart) {
        return false;
    }

    @Override
    public boolean removeDepartById(int id) {
        return false;
    }

    @Override
    public boolean modifyDepart(DepartVO depart) {
        return false;
    }

    @Override
    public DepartVO getDepartById(int id) {
        DepartVO departVO = new DepartVO();
        departVO.setId(id);
        departVO.setName("查询异常");
        return departVO;
    }

    @Override
    public List<DepartVO> listAllDeparts() {
        return null;
    }
}
