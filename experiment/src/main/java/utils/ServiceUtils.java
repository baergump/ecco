package utils;

import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;

import java.nio.file.Path;

public class ServiceUtils {

    public static Repository.Op openEccoRepository(Path repositoryPath){
        EccoService eccoService = new EccoService();
        eccoService.setRepositoryDir(repositoryPath.resolve(".ecco"));
        eccoService.open();
        return (Repository.Op) eccoService.getRepository();
    }

    public static EccoService createEccoService(Path repositoryPath){
        EccoService eccoService = new EccoService();
        eccoService.setRepositoryDir(repositoryPath.resolve(".ecco").toAbsolutePath());
        eccoService.init();
        return eccoService;
    }
}
