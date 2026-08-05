package giunei.representa_brasil.bill;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import giunei.representa_brasil.bill.dto.BillComplementaryData;
import giunei.representa_brasil.bill.dto.BillOfficialData;
import giunei.representa_brasil.bill.dto.BillProcessedData;
import giunei.representa_brasil.bill.dto.BillSearchItem;
import giunei.representa_brasil.shared.response.GroupedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/bills")
@Tag(name = "Projetos")
public class BillController {

	private final BillService billService;

	public BillController(BillService billService) {
		this.billService = billService;
	}

	@GetMapping
	@Operation(summary = "Buscar projetos de lei, PECs e matérias")
	public List<BillSearchItem> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String tipo,
			@RequestParam(required = false) Integer ano,
			@RequestParam(required = false) String origem) {
		return billService.search(q, tipo, ano, origem);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detalhe agrupado de um projeto")
	public GroupedResponse<BillOfficialData, BillProcessedData, BillComplementaryData> detail(
			@PathVariable String id) {
		return billService.getById(id);
	}
}
