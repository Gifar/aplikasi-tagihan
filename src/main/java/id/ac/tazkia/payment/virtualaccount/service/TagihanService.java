package id.ac.tazkia.payment.virtualaccount.service;

import id.ac.tazkia.payment.virtualaccount.dao.*;
import id.ac.tazkia.payment.virtualaccount.dto.TagihanResponse;
import id.ac.tazkia.payment.virtualaccount.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service @Transactional
public class TagihanService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String TIMEZONE = "GMT+07:00";
    private static final Logger LOGGER = LoggerFactory.getLogger(TagihanService.class);

    @Autowired private RunningNumberService runningNumberService;
    @Autowired private TagihanDao tagihanDao;
    @Autowired private VirtualAccountDao virtualAccountDao;
    @Autowired private PeriksaStatusTagihanDao periksaStatusTagihanDao;
    @Autowired private KafkaSenderService kafkaSenderService;
    @Autowired private JadwalTagihanDao jadwalTagihanDao;
    @Autowired private TagihanTerjadwalDao tagihanTerjadwalDao;

    public void saveTagihan(Tagihan t) {
        t.setNilaiTagihan(t.getNilaiTagihan().setScale(0, RoundingMode.DOWN));

        // tagihan baru
        if (t.getId() == null) {
            generateNomorTagihan(t);
            tagihanDao.save(t);
            for (Bank b : t.getJenisTagihan().getDaftarBank()) {
                VirtualAccount va = new VirtualAccount();
                va.setBank(b);
                va.setTagihan(t);
                virtualAccountDao.save(va);
            }
        } else {
            for (VirtualAccount va : virtualAccountDao.findByTagihan(t)) {
                va.setVaStatus(StatusTagihan.AKTIF.equals(t.getStatusTagihan()) ? VaStatus.UPDATE : VaStatus.DELETE);
                virtualAccountDao.save(va);
            }
            tagihanDao.save(t);
        }

        TagihanResponse response = new TagihanResponse();
        response.setDebitur(t.getDebitur().getNomorDebitur());
        response.setJenisTagihan(t.getJenisTagihan().getId());
        response.setKodeBiaya(t.getKodeBiaya().getId());
        response.setKeterangan(t.getKeterangan());
        response.setNilaiTagihan(t.getNilaiTagihan());
        response.setSukses(true);
        response.setNomorTagihan(t.getNomor());
        response.setTanggalTagihan(t.getTanggalTagihan());
        response.setTanggalJatuhTempo(t.getTanggalJatuhTempo());
        kafkaSenderService.sendTagihanResponse(response);
    }

    private void generateNomorTagihan(Tagihan t) {
        String datePrefix = DATE_FORMAT.format(LocalDateTime.now(ZoneId.of(TIMEZONE)));
        Long runningNumber = runningNumberService.getNumber(datePrefix);
        LOGGER.debug("Tagihan : {}", t);
        t.setNomor(datePrefix + t.getJenisTagihan().getKode() + String.format("%06d", runningNumber));
    }

    public void periksaStatus(Tagihan tagihan) {
        for (VirtualAccount va : virtualAccountDao.findByTagihan(tagihan)) {
            PeriksaStatusTagihan p = new PeriksaStatusTagihan();
            p.setVirtualAccount(va);
            p.setWaktuPeriksa(LocalDateTime.now());
            p.setStatusPemeriksaanTagihan(StatusPemeriksaanTagihan.BARU);
            periksaStatusTagihanDao.save(p);

            va.setVaStatus(VaStatus.INQUIRY);
            virtualAccountDao.save(va);
        }
    }

    @Scheduled(cron = "${jadwal.generate.tagihan}")
    public void prosesTagihanTerjadwal(){
        Integer tanggalHariIni = LocalDate.now().getDayOfMonth();
        LOGGER.debug("Memproses tagihan terjadwal untuk tanggal : {}", tanggalHariIni);
        Iterable<JadwalTagihan> daftarTagihanHariIni = jadwalTagihanDao.findByKonfigurasiJadwalTagihanTanggalPenagihan(tanggalHariIni);


        daftarTagihanHariIni.forEach(jadwalTagihan -> {

            Tagihan t = new Tagihan();
            t.setKodeBiaya(jadwalTagihan.getKonfigurasiJadwalTagihan().getKodeBiaya());
            t.setNilaiTagihan(jadwalTagihan.getNilai());
            t.setTanggalJatuhTempo(LocalDate.now().minusMonths(jadwalTagihan.getKonfigurasiJadwalTagihan().getJatuhTempoBulan()));
            t.setDebitur(jadwalTagihan.getDebitur());
            t.setJenisTagihan(jadwalTagihan.getKonfigurasiJadwalTagihan().getJenisTagihan());
            t.setKeterangan("Tagihan terjadwal "+t.getJenisTagihan().getNama()+" "
            + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            generateNomorTagihan(t);
            tagihanDao.save(t);

            LOGGER.debug("Tagihan no {} atas nama {}-{} telah di-generate", t.getNomor(), t.getDebitur().getNomorDebitur(), t.getDebitur().getNama());

            TagihanTerjadwal tagihanTerjadwal = new TagihanTerjadwal();
            tagihanTerjadwal.setJadwalTagihan(jadwalTagihan);
            tagihanTerjadwal.setTagihan(t);
            tagihanTerjadwalDao.save(tagihanTerjadwal);

        });

    }
}
