package TestClass;

import org.example.Main;
import org.example.entity.Shop;
import org.example.mapper.ShopMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Author 3590
 * @Date 2024/2/20 23:33
 * @Description 如果 mapper 无法注入，请使用下列代码，以在测试框架中启动 SpringBoot
 */
@SpringBootTest(classes = {Main.class})
@RunWith(SpringRunner.class)
public class MyTest {
    @Autowired
    ShopMapper shopMapper;

    @Test
    public void test() {
        Shop shop = shopMapper.selectById(2);
        System.out.println(shop);
    }
}
