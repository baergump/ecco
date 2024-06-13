package result.persister;

import result.Result;
import result.ResultContext;

import java.nio.file.Paths;
import java.sql.*;
import java.util.stream.Collectors;

public class ResultDatabasePersister implements ResultPersister{

    // TODO: make relative to project
    //private final String DB_PATH = this.getClass().getResource("/database/results.db").toString();
    private final String DB_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ecco\\experiment\\src\\main\\resources\\database\\results.db").toString();
    private final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    private ResultContext resultContext;

    public static void main(String[] args) {
        ResultDatabasePersister obj = new ResultDatabasePersister();
        obj.createDatabase();
        obj.createResultTable();
        //obj.printDBPath();
    }

    private void printDBPath(){
        System.out.println(DB_URL);
    }

    @Override
    public void persist(Result result) {
        String sql = "INSERT INTO results (repository, numberOfVariants, variantConfigurations, " +
                "numberOfSampledFeatures, sampledFeatures, featureTracePercentage, mistakePercentage, " +
                "evaluationStrategy, mistakeType, tp, fp, tn, fn, precision, recall, f1) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        String variantConfigurations = String.join("; ",
                this.resultContext.getRepositoryInformation().variantConfigurations);

        try (Connection conn = DriverManager.getConnection(this.DB_URL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, this.resultContext.getRepositoryInformation().repositoryName);
            pstmt.setInt(2, this.resultContext.getRepositoryInformation().variantConfigurations.size());
            pstmt.setString(3, variantConfigurations);
            pstmt.setInt(4, this.resultContext.getRepositoryInformation().sampledFeatures.length);
            pstmt.setString(5, String.join(", ", this.resultContext.getRepositoryInformation().sampledFeatures));
            pstmt.setInt(6, this.resultContext.getFeatureTracePercentage());
            pstmt.setInt(7, this.resultContext.getMistakePercentage());
            pstmt.setString(8, this.resultContext.getEvaluationStrategy());
            pstmt.setString(9, this.resultContext.getMistakeType());
            pstmt.setInt(10, result.getTp());
            pstmt.setInt(11, result.getFp());
            pstmt.setInt(12, result.getTn());
            pstmt.setInt(13, result.getFn());
            pstmt.setDouble(14, result.getPrecision());
            pstmt.setDouble(15, result.getRecall());
            pstmt.setDouble(16, result.getF1());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setResultContext(ResultContext resultContext) {
        this.resultContext = resultContext;
    }

    @Override
    public ResultContext getResultContext() {
        return this.resultContext;
    }

    private void createResultTable(){
        String sql = "CREATE TABLE IF NOT EXISTS results ("
                + "	id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "	repository TEXT NOT NULL,"
                + "	numberOfVariants INTEGER NOT NULL,"
                + "	variantConfigurations TEXT NOT NULL,"
                + "	numberOfSampledFeatures INTEGERS NOT NULL,"
                + "	sampledFeatures TEXT NOT NULL,"
                + "	featureTracePercentage INTEGER NOT NULL,"
                + "	mistakePercentage INTEGER NOT NULL,"
                + "	evaluationStrategy TEXT NOT NULL,"
                + "	mistakeType TEXT NOT NULL,"
                + "	tp INTEGER NOT NULL,"
                + "	fp INTEGER NOT NULL,"
                + "	tn INTEGER NOT NULL,"
                + " fn INTEGER NOT NULL,"
                + "	precision DOUBLE NOT NULL,"
                + "	recall DOUBLE NOT NULL,"
                + "	f1 DOUBLE NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(this.DB_URL)){
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDatabase() {
        try (var conn = DriverManager.getConnection(this.DB_URL)) {
            if (conn != null) {
                var meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
