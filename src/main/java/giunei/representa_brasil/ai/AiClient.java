package giunei.representa_brasil.ai;

public interface AiClient {

	boolean isEnabled();

	String summarize(String tipo, String texto, java.util.List<String> fontes);
}
