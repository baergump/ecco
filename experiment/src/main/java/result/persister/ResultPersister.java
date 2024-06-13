package result.persister;

import result.Result;
import result.ResultContext;

public interface ResultPersister {
    void persist(Result result);

    void setResultContext(ResultContext resultContext);
    ResultContext getResultContext();
}
