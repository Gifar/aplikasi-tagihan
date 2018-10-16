package id.ac.tazkia.payment.virtualaccount.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity @Data
@Table(uniqueConstraints={@UniqueConstraint(columnNames={"nama"},name = "nama")})
public class KonfigurasiJadwalTagihan {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @NotNull
    @Column(unique = true,name = "nama")
    private String nama;

    @ManyToOne
    @JoinColumn(name = "id_jenis_tagihan")
    private JenisTagihan jenisTagihan;

    @ManyToOne
    @JoinColumn(name = "id_kode_biaya")
    private KodeBiaya kodeBiaya;

    @NotNull
    @Min(1) @Max(28)
    private Integer tanggalPenagihan;

    @NotNull
    @Min(1)
    private Integer jumlahPenagihan;

    @NotNull
    @Column(columnDefinition = "DATE")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tanggalMulai;

    @NotNull @Min(1)
    private Integer jatuhTempoBulan = 12;

    @NotNull
    private Boolean otomatisAkumulasi = true;
}
