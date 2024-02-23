package TestClass;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.example.Main;
import org.example.entity.Shop;
import org.example.mapper.ShopMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author 3590
 * @Date 2024/2/20 23:33
 * @Description
 */
@SpringBootTest(classes = {Main.class})
public class MyTest {
    @Autowired
    ShopMapper shopMapper;

    @Test
    public void test() {
        IPage<Shop> page = new Page<>(1, 3);
        shopMapper.queryClosestShops(page, 1L, 120.149192, 30.316078);
    }
}
