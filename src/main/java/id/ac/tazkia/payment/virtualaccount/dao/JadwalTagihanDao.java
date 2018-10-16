package id.ac.tazkia.payment.virtualaccount.dao;

import id.ac.tazkia.payment.virtualaccount.entity.JadwalTagihan;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface JadwalTagihanDao extends PagingAndSortingRepository<JadwalTagihan,String> {
    Iterable<JadwalTagihan> findByKonfigurasiJadwalTagihanTanggalPenagihan(Integer tanggalHariIni);
}
