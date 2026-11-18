package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class ConexionSQLServer {
    private String serverName;
    private String databaseName;
    private String user;
    private String password;
    private String connectionUrl;
    
    private Connection conn = null;

    public ConexionSQLServer(String serverName, String databaseName, String user, String password) {
        this.serverName = serverName;
        this.databaseName = databaseName;
        this.user = user;
        this.password = password;
        this.connectionUrl = "jdbc:sqlserver://" + serverName + ";databaseName=" + databaseName + ";user=" + user + ";password=" + password + ";encrypt=false;trustServerCertificate=true;";
        
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: No se pudo cargar el driver JDBC de SQL Server. Asegurese de que el archivo .jar este en el classpath.");
            System.err.println("Detalles: " + e.getMessage());
        }
    }

    public ConexionSQLServer() {
        this(env("CLINICA_DB_HOST", "localhost:1433"),
             env("CLINICA_DB_NAME", "ClinicaDB"),
             env("CLINICA_DB_USER", "sa"),
             env("CLINICA_DB_PASSWORD", ""));
    }

    private static String env(String clave, String porDefecto) {
        String valor = System.getenv(clave);
        return (valor == null || valor.isEmpty()) ? porDefecto : valor;
    }

    public Connection getConexion() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(connectionUrl);
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al conectar a la base de datos: " + e.getMessage());
            conn = null; 
        } 
        return conn;
    }

    public void cerrarConexion() {
        try {
            if (conn != null && !conn.isClosed()) { 
                conn.close();
                conn = null;
                System.out.println("Conexion a la base de datos cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cerrar la conexion: " + e.getMessage());
        }
    }

    public ResultSet consultar(String query) {
        try {
            Connection currentConn = getConexion();
            if (currentConn != null) {
                Statement stm = currentConn.createStatement();
                ResultSet rs = stm.executeQuery(query);
                return rs;
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al ejecutar consulta: " + e.getMessage());
        }
        return null;
    }
    
    public int ejecutarUpdate(PreparedStatement pstmt) throws SQLException {
        return pstmt.executeUpdate();
    }
    
    public ResultSet ejecutarQuery(PreparedStatement pstmt) throws SQLException {
        return pstmt.executeQuery();
    }
}