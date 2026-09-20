package za.ac.cput.controller;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.domain.Promotion;
import za.ac.cput.domain.enums.DiscountType;
import za.ac.cput.service.IPromotionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final IPromotionService service;

    public PromotionController(IPromotionService service) {
        this.service = service;
    }

    public record PromotionRequest(String code, String description, DiscountType discountType,
                                   BigDecimal discountValue, LocalDate startDate,
                                   LocalDate endDate, int usageLimit) {}

    @PostMapping
    public Promotion create(@RequestBody PromotionRequest request) {
        return service.register(request.code(), request.description(), request.discountType(),
                request.discountValue(), request.startDate(), request.endDate(), request.usageLimit());
    }

    @GetMapping("/{id}")
    public Promotion read(@PathVariable String id) {
        return service.read(id);
    }

    @GetMapping
    public List<Promotion> getAll() {
        return service.getAll();
    }

    @GetMapping("/validate/{code}")
    public Promotion validate(@PathVariable String code) {
        return service.validate(code);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }


}

