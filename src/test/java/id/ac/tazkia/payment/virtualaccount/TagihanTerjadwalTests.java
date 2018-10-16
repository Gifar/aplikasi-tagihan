package id.ac.tazkia.payment.virtualaccount;

import id.ac.tazkia.payment.virtualaccount.service.TagihanService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TagihanTerjadwalTests {
    @Autowired private TagihanService tagihanService;

    @Test
    public void testGenerateTagihanTerjadwal() {
        tagihanService.prosesTagihanTerjadwal();
    }
}
