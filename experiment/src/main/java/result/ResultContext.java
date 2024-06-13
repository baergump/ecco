package result;

import trainer.RepositoryInformation;

public class ResultContext {

    private RepositoryInformation repositoryInformation;
    private int featureTracePercentage;
    private int mistakePercentage;
    private String evaluationStrategy;
    private String mistakeType;

    public ResultContext(RepositoryInformation repositoryInformation,
                         int featureTracePercentage,
                         int mistakePercentage,
                         String evaluationStrategy,
                         String mistakeType){
        this.repositoryInformation = repositoryInformation;
        this.featureTracePercentage = featureTracePercentage;
        this.mistakePercentage = mistakePercentage;
        this.evaluationStrategy = evaluationStrategy;
        this.mistakeType = mistakeType;
    }

    public RepositoryInformation getRepositoryInformation() {
        return repositoryInformation;
    }

    public int getFeatureTracePercentage() {
        return featureTracePercentage;
    }

    public int getMistakePercentage() {
        return mistakePercentage;
    }

    public String getEvaluationStrategy() {
        return evaluationStrategy;
    }

    public String getMistakeType() {
        return mistakeType;
    }
}
