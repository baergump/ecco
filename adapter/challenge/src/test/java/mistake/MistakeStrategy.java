package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;

import java.util.Collection;

public interface MistakeStrategy {

    boolean createMistake(FeatureTrace trace);

    default <E> E getRandom(Collection<E> e) {
        return e.stream()
                .skip((int) (e.size() * Math.random()))
                .findFirst().get();
    }

}
