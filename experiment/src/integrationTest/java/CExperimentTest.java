
import at.jku.isse.ecco.experiment.utils.ServiceUtils;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


public class CExperimentTest {

    @Test
    public void noEndlessLoop(){
        Path variantBasePath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\TestRepo\\test\\variants");
        List<Path> variantPaths = new LinkedList<>();
        Path variant1 = variantBasePath.resolve("Variant1");
        Path variant23 = variantBasePath.resolve("Variant23");
        Path variant35 = variantBasePath.resolve("Variant35");
        variantPaths.add(variant1);
        variantPaths.add(variant23);
        variantPaths.add(variant35);

        Path repoPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\TestRepo\\test\\repo");
        EccoService eccoService = ServiceUtils.createEccoService(repoPath);

        for (Path variantPath : variantPaths){
            eccoService.setBaseDir(variantPath.toAbsolutePath());
            eccoService.commit();
        }
    }
}
