package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.repository.Repository;

import java.util.Collection;

public interface MistakeStrategy {

    boolean createMistake(FeatureTrace trace);

    void init(Repository.Op repository);

    default <E> E getRandom(Collection<E> e) {
        return e.stream()
                .skip((int) (e.size() * Math.random()))
                .findFirst().get();
    }

}
