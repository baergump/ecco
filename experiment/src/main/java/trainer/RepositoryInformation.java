package trainer;

import java.util.List;

public class RepositoryInformation {

    public String repositoryName;
    public String[] sampledFeatures;
    public List<String> variantConfigurations;
    public String sampleName;

    public RepositoryInformation(String repositoryName,
                                 String[] sampledFeatures,
                                 List<String> variantConfigurations,
                                 String sampleName){
        this.repositoryName = repositoryName;
        this.sampledFeatures = sampledFeatures;
        this.variantConfigurations = variantConfigurations;
        this.sampleName = sampleName;
    }

    public RepositoryInformation(){
    }
}
