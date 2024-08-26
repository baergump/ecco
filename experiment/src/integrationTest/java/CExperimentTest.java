
import at.jku.isse.ecco.experiment.utils.ServiceUtils;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


public class CExperimentTest {

    @Test
    public void noEndlessLoop1(){
        Path variantBasePath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\tmp\\TestRepo\\test\\variants");
        List<Path> variantPaths = new LinkedList<>();
        Path variant1 = variantBasePath.resolve("Variant1");
        Path variant23 = variantBasePath.resolve("Variant23");
        Path variant35 = variantBasePath.resolve("Variant35");
        variantPaths.add(variant1);
        variantPaths.add(variant23);
        variantPaths.add(variant35);

        Path repoPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\tmp\\TestRepo\\test\\repo");
        EccoService eccoService = ServiceUtils.createEccoService(repoPath);

        for (Path variantPath : variantPaths){
            eccoService.setBaseDir(variantPath.toAbsolutePath());
            eccoService.commit();
        }
    }

    @Test
    public void noEndlessLoop2(){
        Path variantBasePath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\tmp\\TestRepo2\\test\\variants");
        List<Path> variantPaths = new LinkedList<>();
        Path variant30 = variantBasePath.resolve("Variant30");
        Path variant16 = variantBasePath.resolve("Variant16");
        Path variant39 = variantBasePath.resolve("Variant39");
        variantPaths.add(variant30);
        variantPaths.add(variant16);
        variantPaths.add(variant39);

        Path repoPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\tmp\\TestRepo2\\test\\repo");
        EccoService eccoService = ServiceUtils.createEccoService(repoPath);

        for (Path variantPath : variantPaths){
            eccoService.setBaseDir(variantPath.toAbsolutePath());
            eccoService.commit();
        }
    }
}
