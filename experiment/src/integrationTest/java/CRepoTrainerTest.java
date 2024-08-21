import at.jku.isse.ecco.experiment.config.ExperimentRunConfiguration;
import at.jku.isse.ecco.experiment.trainer.EccoCRepoTrainer;
import at.jku.isse.ecco.experiment.utils.DirUtils;
import at.jku.isse.ecco.experiment.utils.ResourceUtils;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import config.DummyConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class CRepoTrainerTest {

    @AfterEach
    public void deleteRepo(){
        Path repoPath = ResourceUtils.getResourceFolderPath("repo");
        DirUtils.deleteDir(repoPath);
        DirUtils.createDir(repoPath);
    }

    @Test
    public void trainingAllVariantsTest(){
        DummyConfiguration dummyConfig = new DummyConfiguration();
        ExperimentRunConfiguration config = dummyConfig.createRunConfiguration();
        EccoCRepoTrainer trainer = new EccoCRepoTrainer(config);
        trainer.train();
        assertNotNull(trainer.getRepository());
        // TODO: check feature traces
        // TODO: check associations
    }

    @Test
    public void trainingFeatureATest(){
        DummyConfiguration dummyConfig = new DummyConfiguration();
        dummyConfig.setVariantsDir(ResourceUtils.getResourceFolderPath("Sampling_Base_2/C_SPL/Sample1/SingleCommit"));
        dummyConfig.setNumberOfVariants(1);
        ExperimentRunConfiguration config = dummyConfig.createRunConfiguration();
        EccoCRepoTrainer trainer = new EccoCRepoTrainer(config);
        trainer.train();
        assertNotNull(trainer.getRepository());
        // TODO: check feature traces
        // TODO: check associations
    }

    @Test
    public void trainingFeatureABTest(){
        DummyConfiguration dummyConfig = new DummyConfiguration();
        dummyConfig.setVariantsDir(ResourceUtils.getResourceFolderPath("Sampling_Base_3/C_SPL/Sample1/SingleCommit"));
        dummyConfig.setNumberOfVariants(1);
        ExperimentRunConfiguration config = dummyConfig.createRunConfiguration();
        EccoCRepoTrainer trainer = new EccoCRepoTrainer(config);
        trainer.train();
        assertNotNull(trainer.getRepository());
        // TODO: check feature traces
        // TODO: check associations
    }

    @Test
    public void trainingFeatureBTest(){
        DummyConfiguration dummyConfig = new DummyConfiguration();
        dummyConfig.setVariantsDir(ResourceUtils.getResourceFolderPath("Sampling_Base_4/C_SPL/Sample1/SingleCommit"));
        dummyConfig.setNumberOfVariants(1);
        ExperimentRunConfiguration config = dummyConfig.createRunConfiguration();
        EccoCRepoTrainer trainer = new EccoCRepoTrainer(config);
        trainer.train();
        assertNotNull(trainer.getRepository());
        // TODO: check feature traces
        // TODO: check associations
    }

    @Test
    public void trainingFeatureNullTest(){
        DummyConfiguration dummyConfig = new DummyConfiguration();
        dummyConfig.setVariantsDir(ResourceUtils.getResourceFolderPath("Sampling_Base_5/C_SPL/Sample1/SingleCommit"));
        dummyConfig.setNumberOfVariants(1);
        ExperimentRunConfiguration config = dummyConfig.createRunConfiguration();
        EccoCRepoTrainer trainer = new EccoCRepoTrainer(config);
        trainer.train();
        assertNotNull(trainer.getRepository());
        // TODO: check feature traces
        // TODO: check associations
    }
}
