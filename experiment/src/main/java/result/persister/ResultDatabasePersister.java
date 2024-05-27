package result.persister;

import result.Result;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ResultDatabasePersister implements ResultPersister{

    private final String DB_URL = this.getClass().getResource("/database/results.db").toString();

    public static void main(String[] args) {
        ResultDatabasePersister obj = new ResultDatabasePersister();
    }

    @Override
    public void persist(Result result) {

    }

    private void createTable(){
        var sql = "CREATE TABLE IF NOT EXISTS results ("
                + "	id INTEGER PRIMARY KEY,"
                + "	name text NOT NULL,"
                + "	capacity REAL"
                + ");";
    }

    private void connect() {
        Connection conn = null;
        try {
            conn =  DriverManager.getConnection(this.DB_URL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
