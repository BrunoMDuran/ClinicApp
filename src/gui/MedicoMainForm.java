package gui;

import conexion.ConexionSQLServer;
import modelo.Usuario;
import modelo.Medico;
import modelo.Cita;
import modelo.Paciente; 
import modelo.Especialidad; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MedicoMainForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;
    private Usuario usuarioLogeado;
    private Medico medicoLogeado; 
    private DefaultTableModel tablaAgendaModel;

    public MedicoMainForm() {
        initComponents();
    }
       
    public MedicoMainForm(ConexionSQLServer conexion, Usuario usuario, Medico medico) {
        this.conexion = conexion;
        this.usuarioLogeado = usuario;
        this.medicoLogeado = medico;
        initComponents();
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(null);
        try {
            Image icon = ImageIO.read(getClass().getResource("/resources/logo.png"));
            this.setIconImage(icon); // 'this' se refiere al JFrame actual
        } catch (IOException e) {
            System.err.println("ERROR: No se pudo cargar el logo: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: La ruta del logo es invalida o el archivo no existe: " + e.getMessage());
        }

        setLocationRelativeTo(null);
        inicializarModeloTabla();
        setTitle("Panel Medico - Dr/a. " + medicoLogeado.getApellido());
        cargarAgendaHoy(); 
    }
    
     private void inicializarModeloTabla() {
        tablaAgendaModel = new DefaultTableModel(new Object[]{"ID Cita", "Hora", "Paciente (DNI)", "Motivo", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblAgenda.setModel(tablaAgendaModel); 
    }
     
    private void cargarAgendaHoy() {
        tablaAgendaModel.setRowCount(0);
        LocalDate hoy = LocalDate.now();
        lblAgendaFecha.setText("Agenda para hoy: " + hoy.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String query = "SELECT c.IdCita, c.HoraCita, p.IdPaciente, p.Dni, p.Nombre, p.Apellido, c.Motivo, c.Estado " +
                       "FROM Citas c JOIN Pacientes p ON c.IdPaciente = p.IdPaciente " +
                       "WHERE c.IdMedico = ? AND c.FechaCita = ? " +
                       "ORDER BY c.HoraCita ASC";
        
        try (Connection conn = conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, medicoLogeado.getIdMedico());
            pstmt.setDate(2, java.sql.Date.valueOf(hoy));

            try (ResultSet rs = conexion.ejecutarQuery(pstmt)) {
                while (rs.next()) {
                    Paciente paciente = new Paciente(rs.getInt("IdPaciente"), rs.getInt("Dni"), rs.getString("Nombre"), rs.getString("Apellido"), null, null, null, null);
                    tablaAgendaModel.addRow(new Object[]{
                        rs.getInt("IdCita"),
                        rs.getTime("HoraCita").toLocalTime(),
                        paciente.getNombre() + " " + paciente.getApellido() + " (" + paciente.getDni() + ")",
                        rs.getString("Motivo"),
                        rs.getString("Estado")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar agenda: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar agenda: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void gestionarHistoriaClinica() {
        int selectedRow = tblAgenda.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita de la tabla para gestionar su historia clinica.", "Sin Seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idCita = (Integer) tablaAgendaModel.getValueAt(selectedRow, 0);
        Cita citaSeleccionada = obtenerCitaCompleta(idCita);
        if (citaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Error al cargar detalles de la cita seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new HistoriaClinicaForm(conexion, citaSeleccionada.getPaciente(), medicoLogeado).setVisible(true);
    }
    
    private Cita obtenerCitaCompleta(int idCita) {
        String query = "SELECT c.IdCita, c.FechaCita, c.HoraCita, c.Motivo, c.Estado, " +
                       "p.IdPaciente, p.Dni, p.Nombre, p.Apellido, p.FechaNacimiento, p.Telefono, p.CorreoElectronico, p.Direccion, " +
                       "m.IdMedico, m.Nombre AS MedicoNombre, m.Apellido AS MedicoApellido, m.NumeroMatricula, m.Telefono AS MedicoTelefono, " +
                       "e.IdEspecialidad, e.NombreEspecialidad " +
                       "FROM Citas c " +
                       "JOIN Pacientes p ON c.IdPaciente = p.IdPaciente " +
                       "JOIN Medicos m ON c.IdMedico = m.IdMedico " +
                       "JOIN Especialidades e ON m.IdEspecialidad = e.IdEspecialidad " +
                       "WHERE c.IdCita = ?";
        try (Connection conn = conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, idCita); // <<-- AQUI LA CORRECCION: Setea el parametro 1
            try (ResultSet rs = conexion.ejecutarQuery(pstmt)) {
                if (rs.next()) {
                    Paciente paciente = new Paciente(
                        rs.getInt("IdPaciente"),
                        rs.getInt("Dni"),
                        rs.getString("Nombre"),
                        rs.getString("Apellido"),
                        rs.getDate("FechaNacimiento") != null ? rs.getDate("FechaNacimiento") : null,
                        rs.getString("Telefono"),
                        rs.getString("CorreoElectronico"),
                        rs.getString("Direccion")
                    );
                    Especialidad especialidad = new Especialidad(rs.getInt("IdEspecialidad"), rs.getString("NombreEspecialidad"));
                    Medico medico = new Medico(
                        rs.getInt("IdMedico"),
                        rs.getString("MedicoNombre"),
                        rs.getString("MedicoApellido"),
                        rs.getString("NumeroMatricula"),
                        especialidad,
                        rs.getString("MedicoTelefono")
                    );
                    return new Cita(
                        rs.getInt("IdCita"),
                        paciente,
                        medico,
                        rs.getDate("FechaCita").toLocalDate(),
                        rs.getTime("HoraCita").toLocalTime(),
                        rs.getString("Motivo"),
                        rs.getString("Estado")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al obtener cita completa: " + e.getMessage());
        }
        return null;
    }

    private void cerrarSesion() {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Desea cerrar sesion?", "Cerrar Sesion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new LoginForm(conexion).setVisible(true);
            this.dispose();
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblAgenda = new javax.swing.JLabel();
        lblAgendaFecha = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblAgenda = new javax.swing.JTable();
        btnVerAgenda = new javax.swing.JButton();
        btnGestionHistoriaClinica = new javax.swing.JButton();
        btnLogout = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        lblAgenda.setText("Agenda de " + medicoLogeado.getNombre() + " " + medicoLogeado.getApellido());

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(lblAgendaFecha, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblAgenda, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE))
                .addGap(277, 277, 277))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblAgenda)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblAgendaFecha, javax.swing.GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE)
                .addContainerGap())
        );

        tblAgenda.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblAgenda);

        btnVerAgenda.setText("Refrescar Agenda");
        btnVerAgenda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerAgendaActionPerformed(evt);
            }
        });

        btnGestionHistoriaClinica.setText("Gestionar Historia Clinica de Cita seleccionada");
        btnGestionHistoriaClinica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionHistoriaClinicaActionPerformed(evt);
            }
        });

        btnLogout.setText("Cerrar Sesion");
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 735, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(btnVerAgenda, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                                .addGap(49, 49, 49)
                                .addComponent(btnGestionHistoriaClinica, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                                .addGap(63, 63, 63)
                                .addComponent(btnLogout, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)))
                        .addGap(31, 31, 31)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnVerAgenda)
                    .addComponent(btnGestionHistoriaClinica)
                    .addComponent(btnLogout))
                .addGap(32, 32, 32))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnVerAgendaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerAgendaActionPerformed
        cargarAgendaHoy();
    }//GEN-LAST:event_btnVerAgendaActionPerformed

    private void btnGestionHistoriaClinicaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGestionHistoriaClinicaActionPerformed
        gestionarHistoriaClinica();
    }//GEN-LAST:event_btnGestionHistoriaClinicaActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        cerrarSesion();
    }//GEN-LAST:event_btnLogoutActionPerformed

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MedicoMainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MedicoMainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MedicoMainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MedicoMainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MedicoMainForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGestionHistoriaClinica;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnVerAgenda;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblAgenda;
    private javax.swing.JLabel lblAgendaFecha;
    private javax.swing.JTable tblAgenda;
    // End of variables declaration//GEN-END:variables
}
