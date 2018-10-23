package id.ac.tazkia.payment.virtualaccount.controller;

import id.ac.tazkia.payment.virtualaccount.dao.*;
import id.ac.tazkia.payment.virtualaccount.dto.UploadError;
import id.ac.tazkia.payment.virtualaccount.entity.Debitur;
import id.ac.tazkia.payment.virtualaccount.entity.JadwalTagihan;
import id.ac.tazkia.payment.virtualaccount.entity.KonfigurasiJadwalTagihan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/jadwal/tagihan")
public class TagihanTerjadwalController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagihanTerjadwalController.class);


    @Value("classpath:sample/jadwal.csv")
    private Resource contohFileJadwal;

    @Autowired
    private JenisTagihanDao jenisTagihanDao;
    @Autowired
    private KodeBiayaDao kodeBiayaDao;
    @Autowired
    private DebiturDao debiturDao;
    @Autowired
    private JadwalTagihanDao jadwalTagihanDao;
    @Autowired
    private KonfigurasiJadwalTagihanDao konfigurasiJadwalTagihanDao;

    @ModelAttribute("listKonfig")
    public Iterable<KonfigurasiJadwalTagihan> daftarKonfig() {
        return konfigurasiJadwalTagihanDao.findAll();
    }

    @GetMapping("/konfigurasi/form")
    public void displayFormKonfigurasiJadwalTagihan(Model model){
        model.addAttribute("jenisTagihan", jenisTagihanDao.findByAktifOrderByKode(Boolean.TRUE));
        model.addAttribute("kodeBiaya", kodeBiayaDao.findAll());

    }

    @PostMapping("/konfigurasi/form")
    public String prosesFormKonfigurasiJadwalTagihan(@ModelAttribute @Valid KonfigurasiJadwalTagihan konfigurasiJadwalTagihan,@RequestParam(required = false) Boolean otomatisAkumulasi,BindingResult errors){

        if (errors.hasErrors()) {
            return "/jadwal/tagihan/konfigurasi/form";
        }

        if (otomatisAkumulasi == null){
            konfigurasiJadwalTagihan.setOtomatisAkumulasi(Boolean.FALSE);
        }

        konfigurasiJadwalTagihanDao.save(konfigurasiJadwalTagihan);

        return "redirect:/jadwal/tagihan/list";
    }


    @GetMapping("/upload")
    public void prosesFormUploadJadwalTagihan(){

    }

    @PostMapping("/upload")
    public String displayFormUploadJadwalTagihan(@RequestParam KonfigurasiJadwalTagihan konfigurasiJadwalTagihan,
                                               @RequestParam(required = false) Boolean pakaiHeader,
                                               MultipartFile fileJadwal,
                                               RedirectAttributes redirectAttrs){

        List<UploadError> errors = new ArrayList<>();
        Integer baris = 0;

        if(konfigurasiJadwalTagihan == null){
            errors.add(new UploadError(baris, "Jenis tagihan harus diisi", ""));
            redirectAttrs
                    .addFlashAttribute("jumlahBaris", 0)
                    .addFlashAttribute("jumlahSukses", 0)
                    .addFlashAttribute("jumlahError", errors.size())
                    .addFlashAttribute("errors", errors);
            return "redirect:hasil";
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileJadwal.getInputStream()));
            String content;

            if((pakaiHeader != null && pakaiHeader)){
                content = reader.readLine(); //NOSONAR
            }

            while ((content = reader.readLine()) != null) {
                baris++;
                String[] data = content.split(",");
                if (data.length != 2) {
                    errors.add(new UploadError(baris, "Format data salah", content));
                    continue;
                }


                Debitur d = debiturDao.findByNomorDebitur(data[0]);
                if (d == null) {
                    errors.add(new UploadError(baris, "Debitur "+data[1]+" tidak terdaftar", content));
                    continue;
                }


                JadwalTagihan jadwalTagihan = new JadwalTagihan();
                jadwalTagihan.setDebitur(d);
                jadwalTagihan.setKonfigurasiJadwalTagihan(konfigurasiJadwalTagihan);
                jadwalTagihan.setNilai(new BigDecimal(data[1]));
                jadwalTagihanDao.save(jadwalTagihan);


            }
        } catch (IOException err){
            LOGGER.warn(err.getMessage(), err);
            errors.add(new UploadError(0, "Format file salah", ""));
        }

        redirectAttrs
                .addFlashAttribute("jumlahBaris", baris)
                .addFlashAttribute("jumlahSukses", baris - errors.size())
                .addFlashAttribute("jumlahError", errors.size())
                .addFlashAttribute("errors", errors);
        return "redirect:hasil";

    }



    @GetMapping("/list")
    public void daftarJadwalTagihan(){

    }

    @GetMapping("/hasil")
    public void hasil(){

    }

    @GetMapping("/contoh/jadwal")
    public void downloadContohFileTagihan(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=contoh-jadwal.csv");
        FileCopyUtils.copy(contohFileJadwal.getInputStream(), response.getOutputStream());
        response.getOutputStream().flush();
    }

    @GetMapping("/konfigurasi/list")
    public ModelMap listKonfigurasi(@PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        return new ModelMap().addAttribute("konfigurasiTagihan",konfigurasiJadwalTagihanDao.findAll(pageable));
    }
}