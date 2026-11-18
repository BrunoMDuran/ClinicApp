package gui;

import conexion.ConexionSQLServer;
import modelo.Especialidad;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO; 

public class ABMEspecialidadesForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;
    private DefaultTableModel tablaEspecialidadesModel;
    
    
    public ABMEspecialidadesForm() {
        initComponents();
    }
    
     public ABMEspecialidadesForm(ConexionSQLServer conexion) {
        this.conexion = conexion;
        initComponents();
        setTitle("ABM Especialidades");
        setSize(600, 400);
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
        inicializarModeloTabla();
        setLocationRelativeTo(null);
        cargarEspecialidadesEnTabla();
    }

    private void inicializarModeloTabla() {
        tablaEspecialidadesModel = new DefaultTableModel(
                new Object[]{"ID", "Nombre"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblEspecialidades.setModel(tablaEspecialidadesModel); 
        tblEspecialidades.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblEspecialidades.getSelectedRow() != -1) {
                mostrarEspecialidadSeleccionada();
            }
        });
    }
    
    private void limpiarCampos() {
        txtIdEspecialidad.setText("");
        txtNombreEspecialidad.setText("");
        tblEspecialidades.clearSelection();
    }

    private void guardarEspecialidad() {
        try {
            String nombre = txtNombreEspecialidad.getText();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la especialidad es obligatorio.", "Campo Vacio", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Especialidad especialidad = new Especialidad(nombre);

            Connection conn = null; 
            try {
                conn = conexion.getConexion();
                if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
                conn.setAutoCommit(false);

                if (txtIdEspecialidad.getText().isEmpty()) {
                    if (especialidadExisteEnBD(nombre)) {
                         JOptionPane.showMessageDialog(this, "La especialidad '" + nombre + "' ya existe.", "Duplicado", JOptionPane.WARNING_MESSAGE);
                         return;
                    }
                    String insertSQL = "INSERT INTO Especialidades (NombreEspecialidad) VALUES (?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setString(1, especialidad.getNombre());
                        conexion.ejecutarUpdate(pstmt);
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                especialidad.setId(rs.getInt(1));
                            }
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Especialidad registrada con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    especialidad.setId(Integer.parseInt(txtIdEspecialidad.getText()));
                    String updateSQL = "UPDATE Especialidades SET NombreEspecialidad = ? WHERE IdEspecialidad = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                        pstmt.setString(1, especialidad.getNombre());
                        pstmt.setInt(2, especialidad.getId());
                        conexion.ejecutarUpdate(pstmt);
                    }
                    JOptionPane.showMessageDialog(this, "Especialidad actualizada con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                }
                conn.commit();
                cargarEspecialidadesEnTabla();
                limpiarCampos();
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al guardar especialidad en BD: " + e.getMessage());
                try {
                    if (conn != null && !conn.isClosed()) { try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR: Fallo en rollback: " + ex.getMessage()); } }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMEspecialidadesForm.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(this, "Error al guardar especialidad: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) { try { conn.setAutoCommit(true); } catch (SQLException ex) { System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage()); } }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMEspecialidadesForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID de especialidad invalido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarEspecialidad() {
        int selectedRow = tblEspecialidades.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una especialidad de la tabla para eliminar.", "Sin Seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idEspecialidad = (Integer) tablaEspecialidadesModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "¿Esta seguro de eliminar esta especialidad? Esto puede afectar a medicos asociados.", "Confirmar Eliminacion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = null;
            try {
                conn = conexion.getConexion();
                if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
                conn.setAutoCommit(false);

                String updateMedicosSQL = "UPDATE Medicos SET IdEspecialidad = NULL WHERE IdEspecialidad = ?"; 
                try (PreparedStatement pstmt = conn.prepareStatement(updateMedicosSQL)) {
                    pstmt.setInt(1, idEspecialidad);
                    pstmt.executeUpdate();
                }

                String deleteSQL = "DELETE FROM Especialidades WHERE IdEspecialidad = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
                    pstmt.setInt(1, idEspecialidad);
                    pstmt.executeUpdate();
                }
                conn.commit();
                JOptionPane.showMessageDialog(this, "Especialidad eliminada con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                cargarEspecialidadesEnTabla();
                limpiarCampos();
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al eliminar especialidad en BD: " + e.getMessage());
                try {
                    if (conn != null && !conn.isClosed()) { try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR: Fallo en rollback: " + ex.getMessage()); } }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMEspecialidadesForm.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(this, "Error al eliminar especialidad: " + e.getMessage() + "\nAsegurese que no haya medicos asociados que impidan la eliminacion.", "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) { try { conn.setAutoCommit(true); } catch (SQLException ex) { System.err.println("ERROR: Fallo al restaurar auto-commit: " + ex.getMessage()); } }
                } catch (SQLException ex) {
                    Logger.getLogger(ABMEspecialidadesForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void cargarEspecialidadesEnTabla() {
        tablaEspecialidadesModel.setRowCount(0);
        String query = "SELECT IdEspecialidad, NombreEspecialidad FROM Especialidades ORDER BY NombreEspecialidad";
        try (Connection conn = conexion.getConexion();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery(query)) {
            while (rs.next()) {
                tablaEspecialidadesModel.addRow(new Object[]{
                    rs.getInt("IdEspecialidad"),
                    rs.getString("NombreEspecialidad")
                });
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al cargar especialidades en tabla: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al cargar especialidades: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void mostrarEspecialidadSeleccionada() {
        int selectedRow = tblEspecialidades.getSelectedRow();
        if (selectedRow != -1) {
            int idEspecialidad = (Integer) tablaEspecialidadesModel.getValueAt(selectedRow, 0);
            String query = "SELECT IdEspecialidad, NombreEspecialidad FROM Especialidades WHERE IdEspecialidad = ?";
            Connection conn = null; // Declarar conn fuera del try-with-resources
            PreparedStatement pstmt = null; // Declarar pstmt fuera del try-with-resources
            ResultSet rs = null; // Declarar rs fuera del try-with-resources
            try {
                conn = conexion.getConexion();
                if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
                pstmt = conn.prepareStatement(query); 
                
                pstmt.setInt(1, idEspecialidad); // <<-- AQUI LA CORRECCION: Setea el parametro 1
                rs = conexion.ejecutarQuery(pstmt); 
                
                if (rs.next()) {
                    txtIdEspecialidad.setText(String.valueOf(rs.getInt("IdEspecialidad")));
                    txtNombreEspecialidad.setText(rs.getString("NombreEspecialidad"));
                }
            } catch (SQLException e) {
                System.err.println("ERROR: Fallo al mostrar especialidad seleccionada: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error al cargar detalles de la especialidad: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                } catch (SQLException ex) {
                    System.err.println("ERROR: Fallo al cerrar recursos JDBC: " + ex.getMessage());
                }
            }
        }
    }

    private boolean especialidadExisteEnBD(String nombre) {
        String query = "SELECT COUNT(*) FROM Especialidades WHERE NombreEspecialidad = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getConexion();
            if (conn == null) throw new SQLException("No hay conexion a la base de datos.");
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, nombre); 
            rs = conexion.ejecutarQuery(pstmt);
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar especialidad duplicada: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error de BD al verificar especialidad.", "Error DB", JOptionPane.ERROR_MESSAGE);
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
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtIdEspecialidad = new javax.swing.JTextField();
        txtNombreEspecialidad = new javax.swing.JTextField();
        sP = new javax.swing.JScrollPane();
        tblEspecialidades = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        btnNuevo.setText("Nuevo");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        jLabel1.setText("ID Especialidad:");

        jLabel2.setText("Nombre:");

        txtIdEspecialidad.setEditable(false);
        txtIdEspecialidad.setEnabled(false);

        txtNombreEspecialidad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNombreEspecialidadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtNombreEspecialidad, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(41, 41, 41)
                        .addComponent(txtIdEspecialidad, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnNuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtIdEspecialidad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnNuevo))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtNombreEspecialidad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addComponent(btnEliminar)
                .addContainerGap())
        );

        tblEspecialidades.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "ID", "Nombre"
            }
        ));
        sP.setViewportView(tblEspecialidades);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sP, javax.swing.GroupLayout.PREFERRED_SIZE, 573, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(9, 9, 9))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sP, javax.swing.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        limpiarCampos();
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardarEspecialidad();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        eliminarEspecialidad();
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void txtNombreEspecialidadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNombreEspecialidadActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNombreEspecialidadActionPerformed


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
            java.util.logging.Logger.getLogger(ABMEspecialidadesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ABMEspecialidadesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ABMEspecialidadesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ABMEspecialidadesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ABMEspecialidadesForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane sP;
    private javax.swing.JTable tblEspecialidades;
    private javax.swing.JTextField txtIdEspecialidad;
    private javax.swing.JTextField txtNombreEspecialidad;
    // End of variables declaration//GEN-END:variables
}
