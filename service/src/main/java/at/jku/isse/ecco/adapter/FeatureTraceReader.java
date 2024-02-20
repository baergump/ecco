package at.jku.isse.ecco.adapter;

import at.jku.isse.ecco.repository.Repository;

// TODO: find better design solution for feature trace extension?
public interface FeatureTraceReader<I, O> {
    O read(I base, I[] input, Repository.Op repository);

    O read(I[] input, Repository.Op repository);
}
