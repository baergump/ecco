package result.persister;

import result.Result;

import java.sql.*;

public class ResultDatabasePersister implements ResultPersister{

    private final String DB_PATH = this.getClass().getResource("/database/results.db").toString();
    private final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    public static void main(String[] args) {
        ResultDatabasePersister obj = new ResultDatabasePersister();
        obj.createResultTable();
    }

    @Override
    public void persist(Result result) {

    }

    private void createResultTable(){
        /*String sql1 = "CREATE TABLE IF NOT EXISTS results ("
                + "	ID INTEGER PRIMARY KEY,"
                + "	Repository TEXT NOT NULL,"
                + "	NumberOfVariants INTEGER NOT NULL,"
                + "	VariantConfigurations TEXT NOT NULL,"
                + "	NumberOfSampledFeatures INTEGERS NOT NULL,"
                + "	SampledFeatures TEXT NOT NULL,"
                + "	FeatureTracePercentage INTEGER NOT NULL,"
                + "	MistakePercentage INTEGER NOT NULL,"
                + "	EvaluationStrategy TEXT NOT NULL,"
                + "	MistakeType TEXT NOT NULL"
                + ");";*/

        String sql1 = "CREATE TABLE warehouses (name TEXT PRIMARY KEY, capacity INTEGER NOT NULL);";

        String sql2 = "INSERT INTO warehouses(name,capacity) VALUES(?,?)";

        try (Connection conn = DriverManager.getConnection(this.DB_URL);
            PreparedStatement pstmt = conn.prepareStatement(sql2)){

            Statement stmt = conn.createStatement();
            stmt.execute(sql1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Connection conn = DriverManager.getConnection(this.DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql2)){

            pstmt.setString(1, "name1");
            pstmt.setDouble(2, 99);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
