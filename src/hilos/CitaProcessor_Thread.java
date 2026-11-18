package hilos;

import modelo.Cita;
import conexion.ConexionSQLServer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane; 

public class CitaProcessor_Thread extends Thread {
    private ConexionSQLServer conexion;
    private LocalDate fechaLimite;
    private int citasProcesadas = 0; 
    private int citasError = 0; 

    public CitaProcessor_Thread(ConexionSQLServer conexion, LocalDate fechaLimite) {
        this.conexion = conexion;
        this.fechaLimite = fechaLimite;
    }

    @Override
    public void run() {
        System.out.println("Hilo Procesador de Citas: Iniciando procesamiento de citas anteriores al " + fechaLimite.toString());
        JOptionPane.showMessageDialog(null, "Iniciando procesamiento de citas antiguas en segundo plano...", "Procesando Citas", JOptionPane.INFORMATION_MESSAGE);

        Connection conn = null; 
        PreparedStatement pstmtSelect = null;
        PreparedStatement pstmtUpdate = null;
        ResultSet rs = null;

        try {
            conn = conexion.getConexion(); 
            if (conn == null) {
                System.err.println("ERROR: No hay conexion a la base de datos para procesamiento de citas.");
                JOptionPane.showMessageDialog(null, "Error de conexion a BD. No se pudo procesar citas antiguas.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            conn.setAutoCommit(false); 

            String queryCitas = "SELECT IdCita FROM Citas WHERE FechaCita < ? AND Estado IN ('Agendada', 'En Proceso')";
            String updateSQL = "UPDATE Citas SET Estado = 'No Asistida' WHERE IdCita = ?"; 

            pstmtSelect = conn.prepareStatement(queryCitas);
            pstmtUpdate = conn.prepareStatement(updateSQL);
                
            pstmtSelect.setDate(1, java.sql.Date.valueOf(fechaLimite));
            
            rs = conexion.ejecutarQuery(pstmtSelect);
            while (rs.next()) {
                int idCita = rs.getInt("IdCita");
                try {
                    pstmtUpdate.setInt(1, idCita); 
                    pstmtUpdate.addBatch(); 
                    citasProcesadas++;
                } catch (SQLException ex) {
                    System.err.println("ERROR: Fallo al anadir actualizacion para cita ID " + idCita + " al lote: " + ex.getMessage());
                    citasError++;
                }
            }
            
            if (citasProcesadas > 0) {
                int[] results = pstmtUpdate.executeBatch(); 
                conn.commit();
                System.out.println("Citas antiguas procesadas: " + citasProcesadas + " marcadas como 'No Asistida'. Errores en lote: " + citasError);
                JOptionPane.showMessageDialog(null, "Procesamiento de citas antiguas finalizado.\nCitas actualizadas: " + citasProcesadas + "\nErrores: " + citasError, "Procesamiento Completo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                System.out.println("No se encontraron citas 'Agendada' o 'En Proceso' anteriores al " + fechaLimite.toString() + " para procesar.");
                JOptionPane.showMessageDialog(null, "No se encontraron citas antiguas pendientes de procesamiento.", "Procesamiento Completo", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            System.err.println("ERROR: Fallo grave en el procesamiento de citas antiguas: " + e.getMessage());
            try {
                if (conn != null && !conn.isClosed()) {
                    try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR: Fallo en rollback: " + ex.getMessage()); }
                }
            } catch (SQLException ex) {
                Logger.getLogger(CitaProcessor_Thread.class.getName()).log(Level.SEVERE, null, ex);
            }
            JOptionPane.showMessageDialog(null, "Error grave al procesar citas antiguas: " + e.getMessage(), "Error Fatal", JOptionPane.ERROR_MESSAGE);
        } finally {
            try { 
                if (rs != null) rs.close();
                if (pstmtSelect != null) pstmtSelect.close();
                if (pstmtUpdate != null) pstmtUpdate.close();
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC en hilo: " + ex.getMessage());
            }
            try {
                if (conn != null && !conn.isClosed()) {
                    try { conn.setAutoCommit(true); } catch (SQLException ex) { System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage()); }
                }
            } catch (SQLException ex) {
                Logger.getLogger(CitaProcessor_Thread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Hilo CitaProcessor: Finalizado.");
    }
}