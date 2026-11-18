package gui;

import conexion.ConexionSQLServer;
import modelo.Medico;
import modelo.HorarioMedico;
import modelo.Especialidad; 

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.awt.Image; 
import java.io.IOException; 
import javax.imageio.ImageIO;

public class GestionHorariosMedicoForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;
    private List<Medico> medicosDisponibles;
    private DefaultTableModel tablaHorariosModel;
    
    public GestionHorariosMedicoForm() {
        initComponents();
    }
    
    public GestionHorariosMedicoForm(ConexionSQLServer conexion) {
        this.conexion = conexion;
        this.medicosDisponibles = new ArrayList<>();
        initComponents();
        setTitle("Gestion de Horarios Medicos");
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
        inicializarModeloTabla();
        setLocationRelativeTo(null);
        cargarDatosIniciales(); 
        cargarHorariosEnTabla();
    }

    private void inicializarModeloTabla() {
        tablaHorariosModel = new DefaultTableModel(new Object[]{"ID", "Medico", "Dia", "Hora Inicio", "Hora Fin"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblHorarios.setModel(tablaHorariosModel); 
    }
    
    private void cargarDatosIniciales() {
        cmbMedico.addItem("Seleccionar Medico");
        String queryMedicos = "SELECT m.IdMedico, m.Nombre, m.Apellido, m.NumeroMatricula, m.Telefono, e.IdEspecialidad, e.NombreEspecialidad FROM Medicos m JOIN Especialidades e ON m.IdEspecialidad = e.IdEspecialidad ORDER BY m.Apellido, m.Nombre";
        Connection conn = null; 
        Statement stm = null; 
        ResultSet rs = null; 
        try {
            conn = conexion.getConexion();
            if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
            stm = conn.createStatement();
            rs = stm.executeQuery(queryMedicos);
            while (rs.next()) {
                Especialidad esp = new Especialidad(rs.getInt("IdEspecialidad"), rs.getString("NombreEspecialidad"));
                Medico m = new Medico(rs.getInt("IdMedico"), rs.getString("Nombre"), rs.getString("Apellido"), rs.getString("NumeroMatricula"), esp, rs.getString("Telefono")); 
                medicosDisponibles.add(m);
                cmbMedico.addItem("Dr/a. " + m.getApellido() + " (" + m.getEspecialidad().getNombre() + ")");
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar medicos para horarios: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar medicos.", "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally { 
            try {
                if (rs != null) rs.close();
                if (stm != null) stm.close();
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }
    }

    private void agregarHorario() {
        int selectedMedicoIndex = cmbMedico.getSelectedIndex();
        if (selectedMedicoIndex <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un medico.", "Medico No Seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Medico medico = medicosDisponibles.get(selectedMedicoIndex - 1);
        
        String diaSemana = (String) cmbDiaSemana.getSelectedItem();
        
        LocalTime horaInicio;
        LocalTime horaFin;
        try {
            horaInicio = LocalTime.parse(txtHoraInicio.getText());
            horaFin = LocalTime.parse(txtHoraFin.getText());
            if (horaInicio.isAfter(horaFin) || horaInicio.equals(horaFin)) {
                JOptionPane.showMessageDialog(this, "La hora de inicio debe ser anterior a la hora de fin.", "Horario Invalido", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Formato de hora invalido. Use HH:MM.", "Error de Hora", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (horarioExisteOSolapa(medico.getIdMedico(), diaSemana, horaInicio, horaFin, 0)) {
            JOptionPane.showMessageDialog(this, "Ya existe un horario para este medico y dia que se solapa con el ingresado.", "Horario Duplicado/Solapado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        HorarioMedico nuevoHorario = new HorarioMedico(medico, diaSemana, horaInicio, horaFin);

        Connection conn = null; 
        PreparedStatement pstmt = null;
        ResultSet rsGeneratedKeys = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
            conn.setAutoCommit(false);

            String insertSQL = "INSERT INTO HorariosMedicos (IdMedico, DiaSemana, HoraInicio, HoraFin) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, nuevoHorario.getMedico().getIdMedico());
            pstmt.setString(2, nuevoHorario.getDiaSemana());
            pstmt.setObject(3, java.sql.Time.valueOf(nuevoHorario.getHoraInicio()), java.sql.Types.TIME); 
            pstmt.setObject(4, java.sql.Time.valueOf(nuevoHorario.getHoraFin()), java.sql.Types.TIME); 
            conexion.ejecutarUpdate(pstmt);
            rsGeneratedKeys = pstmt.getGeneratedKeys();
            if (rsGeneratedKeys.next()) {
                nuevoHorario.setIdHorario(rsGeneratedKeys.getInt(1));
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Horario agregado con exito. ID: " + nuevoHorario.getIdHorario(), "Exito", JOptionPane.INFORMATION_MESSAGE);
            cargarHorariosEnTabla();
            limpiarCampos();
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al agregar horario en BD: " + e.getMessage());
            try {
                if (conn != null && !conn.isClosed()) { try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR: Fallo en rollback: " + ex.getMessage()); } }
            } catch (SQLException ex) {
                Logger.getLogger(GestionHorariosMedicoForm.class.getName()).log(Level.SEVERE, null, ex);
            }
            JOptionPane.showMessageDialog(this, "Error al agregar horario: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        } finally {
            try { 
                if (rsGeneratedKeys != null) rsGeneratedKeys.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
            try {
                if (conn != null && !conn.isClosed()) { try { conn.setAutoCommit(true); } catch (SQLException ex) { System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage()); } }
            } catch (SQLException ex) {
                Logger.getLogger(GestionHorariosMedicoForm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void eliminarHorario() {
        int selectedRow = tblHorarios.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un horario de la tabla para eliminar.", "Sin Seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idHorario = (Integer) tablaHorariosModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "¿Esta seguro de eliminar este horario?", "Confirmar Eliminacion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
                conn.setAutoCommit(false);

                String deleteSQL = "DELETE FROM HorariosMedicos WHERE IdHorario = ?";
                pstmt = conn.prepareStatement(deleteSQL);
                pstmt.setInt(1, idHorario);
                conexion.ejecutarUpdate(pstmt);
                conn.commit();
                JOptionPane.showMessageDialog(this, "Horario eliminado con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                cargarHorariosEnTabla();
                limpiarCampos();
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al eliminar horario en BD: " + e.getMessage());
                try {
                    if (conn != null && !conn.isClosed()) { try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR: Fallo en rollback: " + ex.getMessage()); } }
                } catch (SQLException ex) {
                    Logger.getLogger(GestionHorariosMedicoForm.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(this, "Error al eliminar horario: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (pstmt != null) pstmt.close();
                } catch (SQLException ex) {
                    System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
                }
                try {
                    if (conn != null && !conn.isClosed()) { try { conn.setAutoCommit(true); } catch (SQLException ex) { System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage()); } }
                } catch (SQLException ex) {
                    Logger.getLogger(GestionHorariosMedicoForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void cargarHorariosEnTabla() {
        tablaHorariosModel.setRowCount(0);
        String query = "SELECT hm.IdHorario, hm.DiaSemana, hm.HoraInicio, hm.HoraFin, m.Nombre, m.Apellido, e.NombreEspecialidad FROM HorariosMedicos hm JOIN Medicos m ON hm.IdMedico = m.IdMedico JOIN Especialidades e ON m.IdEspecialidad = e.IdEspecialidad ORDER BY m.Apellido, hm.DiaSemana, hm.HoraInicio";
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
            stm = conn.createStatement();
            rs = stm.executeQuery(query);
            while (rs.next()) {
                tablaHorariosModel.addRow(new Object[]{
                    rs.getInt("IdHorario"),
                    "Dr/a. " + rs.getString("Nombre") + " " + rs.getString("Apellido") + " (" + rs.getString("NombreEspecialidad") + ")",
                    rs.getString("DiaSemana"),
                    rs.getTime("HoraInicio").toLocalTime(),
                    rs.getTime("HoraFin").toLocalTime()
                });
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar horarios en tabla: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar horarios: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stm != null) stm.close();
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }
    }
    
    private void limpiarCampos() {
        cmbMedico.setSelectedIndex(0);
        cmbDiaSemana.setSelectedIndex(0);
        txtHoraInicio.setText("");
        txtHoraFin.setText("");
        tblHorarios.clearSelection();
    }

    private boolean horarioExisteOSolapa(int idMedico, String diaSemana, LocalTime horaInicio, LocalTime horaFin, int idHorarioExcluir) {
        String query = "SELECT COUNT(*) FROM HorariosMedicos WHERE IdMedico = ? AND DiaSemana = ? AND NOT (CAST(HoraFin AS TIME) <= CAST(? AS TIME) OR CAST(HoraInicio AS TIME) >= CAST(? AS TIME))";
        if (idHorarioExcluir > 0) {
            query += " AND IdHorario != " + idHorarioExcluir;
        }
        Connection conn = null; 
        PreparedStatement pstmt = null; 
        ResultSet rs = null; 
        try {
            conn = conexion.getConexion();
            if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, idMedico);
            pstmt.setString(2, diaSemana);
            pstmt.setObject(3, java.sql.Time.valueOf(horaInicio), java.sql.Types.TIME); 
            pstmt.setObject(4, java.sql.Time.valueOf(horaFin), java.sql.Types.TIME); 
            rs = conexion.ejecutarQuery(pstmt);
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar solapamiento de horario: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error de BD al verificar solapamiento de horario.", "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally { 
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtHoraInicio = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtHoraFin = new javax.swing.JTextField();
        cmbMedico = new javax.swing.JComboBox<>();
        cmbDiaSemana = new javax.swing.JComboBox<>();
        btnAgregarHorario = new javax.swing.JButton();
        btnEliminarHorario = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblHorarios = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLabel1.setText("Medico:");

        jLabel2.setText("Dia:");

        jLabel3.setText("Hora Inicio (hh:mm):");

        txtHoraInicio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtHoraInicioActionPerformed(evt);
            }
        });

        jLabel4.setText("Hora fin (hh:mm):");

        txtHoraFin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtHoraFinActionPerformed(evt);
            }
        });

        cmbDiaSemana.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo" }));
        cmbDiaSemana.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbDiaSemanaActionPerformed(evt);
            }
        });

        btnAgregarHorario.setText("Agregar Horario");
        btnAgregarHorario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarHorarioActionPerformed(evt);
            }
        });

        btnEliminarHorario.setText("Eliminar Horario Seleccionado");
        btnEliminarHorario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarHorarioActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(cmbMedico, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cmbDiaSemana, 0, 174, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(txtHoraInicio, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtHoraFin, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnEliminarHorario, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAgregarHorario, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(cmbMedico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAgregarHorario, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(cmbDiaSemana, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtHoraInicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4)
                            .addComponent(txtHoraFin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(btnEliminarHorario, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tblHorarios.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblHorarios);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 254, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(27, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtHoraFinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtHoraFinActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtHoraFinActionPerformed

    private void btnAgregarHorarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarHorarioActionPerformed
        agregarHorario();
    }//GEN-LAST:event_btnAgregarHorarioActionPerformed

    private void btnEliminarHorarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarHorarioActionPerformed
        eliminarHorario();
    }//GEN-LAST:event_btnEliminarHorarioActionPerformed

    private void cmbDiaSemanaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbDiaSemanaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbDiaSemanaActionPerformed

    private void txtHoraInicioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtHoraInicioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtHoraInicioActionPerformed

    /**
     * @param args the command line arguments
     */
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
            java.util.logging.Logger.getLogger(GestionHorariosMedicoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GestionHorariosMedicoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GestionHorariosMedicoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GestionHorariosMedicoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GestionHorariosMedicoForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregarHorario;
    private javax.swing.JButton btnEliminarHorario;
    private javax.swing.JComboBox<String> cmbDiaSemana;
    private javax.swing.JComboBox<String> cmbMedico;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblHorarios;
    private javax.swing.JTextField txtHoraFin;
    private javax.swing.JTextField txtHoraInicio;
    // End of variables declaration//GEN-END:variables
}
