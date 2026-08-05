package giunei.representa_brasil.integration.planalto;

import java.util.List;

public interface PlanaltoClient {

	boolean isEnabled();

	List<PlanaltoAct> listActs(String query);
}
