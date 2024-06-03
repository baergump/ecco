package at.jku.isse.ecco.adapter.cpp;

import org.eclipse.cdt.core.parser.AbstractParserLogService;

public class MyParserLogService extends AbstractParserLogService {
    @Override
    public void traceLog(String message) {
        System.out.println("Trace: " + message);
    }

    @Override
    public boolean isTracing() {
        return true;  // Enable tracing
    }
}
