package main;

import conexion.ConexionSQLServer;
import gui.LoginForm;
import modelo.Paciente;
import modelo.Especialidad;
import modelo.HorarioMedico;
import modelo.Medico;
import modelo.Usuario;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

public class MainApp {

    private static ConexionSQLServer conexion;
    private static final CountDownLatch guiCloseLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        System.out.println("Iniciando Sistema de Gestion de Clinica...");

        conexion = new ConexionSQLServer();
        
        if (conexion.getConexion() == null) {
            System.err.println("No se pudo conectar a la base de datos. Verifique configuracion y que SQL Server este corriendo.");
            JOptionPane.showMessageDialog(null, "No se pudo conectar a la base de datos. Verifique SQL Server.", "Error de Conexion", JOptionPane.ERROR_MESSAGE);
            return;
        } else {
            System.out.println("Conexion a la base de datos establecida.");
        }
        
        verificarYCargarDatosInicialesBD();
        
        System.out.println("\nAbriendo formulario de Login...");

        SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm(conexion);
            loginForm.setVisible(true);

            loginForm.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    boolean anyJFrameOpen = false;

                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (window instanceof JFrame && window.isShowing()) {
                            anyJFrameOpen = true;
                            break;
                        }
                    }
                    if (!anyJFrameOpen) {
                        guiCloseLatch.countDown();
                    }
                }
            });
        });

        System.out.println("\nEsperando que la GUI se cierre para finalizar la aplicacion...");
        try {
            guiCloseLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ERROR: Hilo principal interrumpido esperando cierre de GUI.");
        }

        System.out.println("\nGUI cerrada. Cerrando conexion de BD.");
        conexion.cerrarConexion();
        System.out.println("Sistema de Gestion de Clinica finalizado.");
        System.exit(0);
    }

    private static void verificarYCargarDatosInicialesBD() {
        String checkUsersQuery = "SELECT COUNT(*) FROM Usuarios";
        try (Connection conn = conexion.getConexion(); Statement stmtCheck = conn.createStatement(); ResultSet rs = stmtCheck.executeQuery(checkUsersQuery)) {
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Base de datos de usuarios vacia. Insertando datos de ejemplo.");
                try (Statement stmtInsert = conn.createStatement()) {
                    stmtInsert.executeUpdate("INSERT INTO Especialidades (NombreEspecialidad) VALUES ('Cardiologia'), ('Dermatologia'), ('Pediatria'), ('Traumatologia'), ('Oftalmologia')");
                    stmtInsert.executeUpdate("INSERT INTO Medicos (Nombre, Apellido, NumeroMatricula, IdEspecialidad, Telefono) VALUES "
                            + "('Dr. Juan', 'Perez', 'MP12345', (SELECT IdEspecialidad FROM Especialidades WHERE NombreEspecialidad = 'Cardiologia'), '11-2222-3333'),"
                            + "('Dra. Maria', 'Gomez', 'MP67890', (SELECT IdEspecialidad FROM Especialidades WHERE NombreEspecialidad = 'Pediatria'), '11-4444-5555'),"
                            + "('Dr. Carlos', 'Ruiz', 'MP11223', (SELECT IdEspecialidad FROM Especialidades WHERE NombreEspecialidad = 'Dermatologia'), '11-6666-7777')");
                    stmtInsert.executeUpdate("INSERT INTO HorariosMedicos (IdMedico, DiaSemana, HoraInicio, HoraFin) VALUES "
                            + "((SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP12345'), 'Lunes', '09:00', '13:00'),"
                            + "((SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP12345'), 'Miercoles', '14:00', '18:00'),"
                            + "((SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP67890'), 'Martes', '10:00', '14:00'),"
                            + "((SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP67890'), 'Jueves', '15:00', '19:00'),"
                            + "((SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP11223'), 'Viernes', '09:00', '17:00')");
                    stmtInsert.executeUpdate("INSERT INTO Pacientes (Dni, Nombre, Apellido, FechaNacimiento, Telefono, CorreoElectronico, Direccion) VALUES "
                            + "(12345678, 'Laura', 'Lopez', '1990-05-15', '11-9876-5432', 'laura.lo@email.com', 'Calle Falsa 123'),"
                            + "(87654321, 'Pedro', 'Martinez', '1985-11-20', '11-1234-5678', 'pedro.m@email.com', 'Av. Siempre Viva 742'),"
                            + "(11223344, 'Sofia', 'Garcia', '2000-01-01', '11-5555-0000', 'sofia.g@email.com', 'Ruta 66 Km 1')");
                    stmtInsert.executeUpdate("INSERT INTO Citas (IdPaciente, IdMedico, FechaCita, HoraCita, Motivo, Estado) VALUES "
                            + "((SELECT IdPaciente FROM Pacientes WHERE Dni = 12345678), (SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP12345'), '2025-07-01', '10:00:00', 'Agendada'),"
                            + "((SELECT IdPaciente FROM Pacientes WHERE Dni = 87654321), (SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP67890'), '2025-07-01', '11:00:00', 'Agendada'),"
                            + "((SELECT IdPaciente FROM Pacientes WHERE Dni = 11223344), (SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP11223'), '2025-07-02', '14:30:00', 'Agendada')");
                    stmtInsert.executeUpdate("INSERT INTO Usuarios (NombreUsuario, ContrasenaHash, Rol, IdMedicoAsociado) VALUES "
                            + "('admin', 'admin123', 'Administrador', NULL),"
                            + "('secretaria', 'sec123', 'Secretario_Admin', NULL),"
                            + "('jperez', 'med123', 'Medico', (SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP12345')),"
                            + "('mgomez', 'med123', 'Medico', (SELECT IdMedico FROM Medicos WHERE NumeroMatricula = 'MP67890'))");
                    System.out.println("Datos de ejemplo insertados en la base de datos.");
                } catch (SQLException e) {
                    System.err.println("ERROR: Fallo al insertar datos de ejemplo: " + e.getMessage());
                }
            } else {
                System.out.println("Base de datos ya contiene datos de usuarios. No se insertaran datos de ejemplo.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al verificar si la BD esta vacia: " + e.getMessage());
        }
    }
}
