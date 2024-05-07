package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.repository.Repository;

public class NoMistaker implements MistakeStrategy{
    @Override
    public boolean createMistake(FeatureTrace trace) {
        return true;
    }

    @Override
    public void init(Repository.Op repository) {

    }
}
