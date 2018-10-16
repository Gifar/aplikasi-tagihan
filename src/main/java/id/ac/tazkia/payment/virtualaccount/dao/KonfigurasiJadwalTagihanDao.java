package id.ac.tazkia.payment.virtualaccount.dao;

import id.ac.tazkia.payment.virtualaccount.entity.KonfigurasiJadwalTagihan;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface KonfigurasiJadwalTagihanDao extends PagingAndSortingRepository<KonfigurasiJadwalTagihan,String> {
    KonfigurasiJadwalTagihan findByNama(String nama);
}
