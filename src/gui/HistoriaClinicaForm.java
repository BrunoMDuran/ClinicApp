package gui;

import conexion.ConexionSQLServer;
import modelo.Paciente;
import modelo.Medico;
import modelo.HistoriaClinica;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.awt.Image;
import java.io.IOException; 
import javax.imageio.ImageIO; 
import javax.swing.JFrame;

public class HistoriaClinicaForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;
    private Paciente pacienteActual;
    private Medico medicoActual;
    private DefaultTableModel tablaHistorialModel;
    
    public HistoriaClinicaForm() {
        initComponents();
    }

    public HistoriaClinicaForm(ConexionSQLServer conexion, Paciente paciente, Medico medico) {
        this.conexion = conexion;
        this.pacienteActual = paciente;
        this.medicoActual = medico;
        initComponents();
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(null);
        try {
            Image icon = ImageIO.read(getClass().getResource("/resources/logo.png"));
            this.setIconImage(icon);
        } catch (IOException e) {
            System.err.println("ERROR: No se pudo cargar el logo: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: La ruta del logo es invalida o el archivo no existe: " + e.getMessage());
        }
        setLocationRelativeTo(null);
        setTitle("Historia Clinica de " + paciente.getNombre() + " " + paciente.getApellido());
        lblPacienteInfo.setText("Paciente: " + paciente.getNombre() + " " + paciente.getApellido() + " (DNI: " + paciente.getDni() + ")");
        inicializarModeloTabla();
        cargarHistorialEnTabla();
        limpiarCamposEntrada();
    }
    
    private void inicializarModeloTabla() {
        tablaHistorialModel = new DefaultTableModel(new Object[]{"ID", "Fecha", "Diagnostico", "Tratamiento", "Notas"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblHistorial.setModel(tablaHistorialModel);
    }
    
    private void limpiarCamposEntrada() {
        txtFechaAtencion.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        txtDiagnostico.setText("");
        txtTratamiento.setText("");
        txtNotas.setText("");
    }

    private void guardarEntradaHistoriaClinica() {
        String diagnostico = txtDiagnostico.getText().trim();
        String tratamiento = txtTratamiento.getText().trim();
        String notas = txtNotas.getText().trim();

        if (diagnostico.isEmpty() && tratamiento.isEmpty() && notas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar al menos un diagnostico, tratamiento o nota.", "Campos Vacios", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        HistoriaClinica nuevaEntrada = new HistoriaClinica(pacienteActual, LocalDateTime.now(), diagnostico, tratamiento, notas);

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rsGeneratedKeys = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
            conn.setAutoCommit(false);

            String insertSQL = "INSERT INTO HistoriasClinicas (IdPaciente, FechaAtencion, Diagnostico, Tratamiento, Notas) VALUES (?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, nuevaEntrada.getPaciente().getIdPaciente());
            pstmt.setObject(2, nuevaEntrada.getFechaAtencion());
            pstmt.setString(3, nuevaEntrada.getDiagnostico());
            pstmt.setString(4, nuevaEntrada.getTratamiento());
            pstmt.setString(5, nuevaEntrada.getNotas());
            conexion.ejecutarUpdate(pstmt);
            rsGeneratedKeys = pstmt.getGeneratedKeys();
            if (rsGeneratedKeys.next()) {
                nuevaEntrada.setIdHistoria(rsGeneratedKeys.getInt(1));
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Entrada de historia clinica guardada con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            cargarHistorialEnTabla();
            limpiarCamposEntrada();
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al guardar entrada HC en BD: " + e.getMessage());
            try {
                if (conn != null && !conn.isClosed()) { try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR: Fallo en rollback: " + ex.getMessage()); } }
            } catch (SQLException ex) {
                Logger.getLogger(HistoriaClinicaForm.class.getName()).log(Level.SEVERE, null, ex);
            }
            JOptionPane.showMessageDialog(this, "Error al guardar entrada de historia clinica: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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
                Logger.getLogger(HistoriaClinicaForm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void cargarHistorialEnTabla() {
        tablaHistorialModel.setRowCount(0);
        String query = "SELECT IdHistoria, FechaAtencion, Diagnostico, Tratamiento, Notas FROM HistoriasClinicas WHERE IdPaciente = ? ORDER BY FechaAtencion DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
            pstmt = conn.prepareStatement(query);
            
            pstmt.setInt(1, pacienteActual.getIdPaciente());
            rs = conexion.ejecutarQuery(pstmt);
            while (rs.next()) {
                tablaHistorialModel.addRow(new Object[]{
                    rs.getInt("IdHistoria"),
                    rs.getTimestamp("FechaAtencion").toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    rs.getString("Diagnostico"),
                    rs.getString("Tratamiento"),
                    rs.getString("Notas")
                });
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar historial en tabla: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar historial clinico: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException ex) {
                System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblPacienteInfo = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtTratamiento = new javax.swing.JTextArea();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtDiagnostico = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtNotas = new javax.swing.JTextArea();
        txtFechaAtencion = new javax.swing.JTextField();
        btnGuardarEntrada = new javax.swing.JButton();
        btnNuevaEntrada = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblHistorial = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLabel2.setText("Nueva entrada:");

        jLabel3.setText("Fecha y hora:");

        jLabel4.setText("Diagnostico:");

        txtTratamiento.setColumns(20);
        txtTratamiento.setRows(5);
        jScrollPane1.setViewportView(txtTratamiento);

        jLabel5.setText("Tratamiento:");

        txtDiagnostico.setColumns(20);
        txtDiagnostico.setRows(5);
        jScrollPane2.setViewportView(txtDiagnostico);

        jLabel6.setText("Notas:");

        txtNotas.setColumns(20);
        txtNotas.setRows(5);
        jScrollPane3.setViewportView(txtNotas);

        btnGuardarEntrada.setText("Guardar Entrada");
        btnGuardarEntrada.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarEntradaActionPerformed(evt);
            }
        });

        btnNuevaEntrada.setText("Limpiar Campos");
        btnNuevaEntrada.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevaEntradaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblPacienteInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(46, 46, 46)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                    .addComponent(txtFechaAtencion))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 162, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnGuardarEntrada, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnNuevaEntrada, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(116, 116, 116))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(lblPacienteInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtFechaAtencion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnNuevaEntrada))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(91, 91, 91)
                        .addComponent(jLabel5))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnGuardarEntrada))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(7, Short.MAX_VALUE))
        );

        tblHistorial.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane4.setViewportView(tblHistorial);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jScrollPane4)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGuardarEntradaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarEntradaActionPerformed
        guardarEntradaHistoriaClinica();
    }//GEN-LAST:event_btnGuardarEntradaActionPerformed

    private void btnNuevaEntradaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevaEntradaActionPerformed
        limpiarCamposEntrada();
    }//GEN-LAST:event_btnNuevaEntradaActionPerformed

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
            java.util.logging.Logger.getLogger(HistoriaClinicaForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(HistoriaClinicaForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(HistoriaClinicaForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(HistoriaClinicaForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new HistoriaClinicaForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGuardarEntrada;
    private javax.swing.JButton btnNuevaEntrada;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lblPacienteInfo;
    private javax.swing.JTable tblHistorial;
    private javax.swing.JTextArea txtDiagnostico;
    private javax.swing.JTextField txtFechaAtencion;
    private javax.swing.JTextArea txtNotas;
    private javax.swing.JTextArea txtTratamiento;
    // End of variables declaration//GEN-END:variables
}
