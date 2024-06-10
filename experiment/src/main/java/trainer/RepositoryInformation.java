package trainer;

import java.util.List;

public class RepositoryInformation {

    public String repositoryName;
    public String[] sampledFeatures;
    public List<String> variantConfigurations;

    public RepositoryInformation(String repositoryName,
                                 String[] sampledFeatures,
                                 List<String> variantConfigurations){
        this.repositoryName = repositoryName;
        this.sampledFeatures = sampledFeatures;
        this.variantConfigurations = variantConfigurations;
    }
}
