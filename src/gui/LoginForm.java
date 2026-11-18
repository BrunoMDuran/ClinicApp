package gui;

import conexion.ConexionSQLServer;
import modelo.Usuario;
import modelo.Medico;
import modelo.Especialidad;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import java.awt.Image;
import javax.swing.ImageIcon;
import java.io.IOException;
import javax.imageio.ImageIO;

public class LoginForm extends javax.swing.JFrame {

    private ConexionSQLServer conexion;

    public LoginForm() {
        initComponents();
    }

    public LoginForm(ConexionSQLServer conexion) {
        initComponents();
        this.conexion = conexion;
        setLocationRelativeTo(null);
        try {
            Image icon = ImageIO.read(getClass().getResource("/resources/logo.png"));
            this.setIconImage(icon);
        } catch (IOException e) {
            System.err.println("ERROR: No se pudo cargar el logo: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: La ruta del logo es invalida o el archivo no existe: " + e.getMessage());
        }
    }

    private void autenticarUsuario() {
        String usuario = txtUsuario.getText();
        String contrasena = new String(txtContrasena.getPassword());

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese usuario y contrasena.", "Login Fallido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Usuario usuarioAutenticado = verificarCredenciales(usuario, contrasena);

        if (usuarioAutenticado != null) {
            JOptionPane.showMessageDialog(this, "Bienvenido, " + usuarioAutenticado.getNombreUsuario() + "!", "Login Exitoso", JOptionPane.INFORMATION_MESSAGE);
            abrirMainForm(usuarioAutenticado);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Usuario o contrasena incorrectos.", "Login Fallido", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Usuario verificarCredenciales(String nombreUsuario, String contrasena) {
        String query = "SELECT IdUsuario, NombreUsuario, ContrasenaHash, Rol, IdMedicoAsociado FROM Usuarios WHERE NombreUsuario = ? AND ContrasenaHash = ?";
        try (Connection conn = conexion.getConexion(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, nombreUsuario);
            pstmt.setString(2, contrasena);

            try (ResultSet rs = conexion.ejecutarQuery(pstmt)) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("IdUsuario"),
                            rs.getString("NombreUsuario"),
                            rs.getString("ContrasenaHash"),
                            rs.getString("Rol"),
                            rs.getObject("IdMedicoAsociado") != null ? rs.getInt("IdMedicoAsociado") : null
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al verificar credenciales: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error de base de datos al verificar credenciales.", "Error DB", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void abrirMainForm(Usuario usuario) {
        switch (usuario.getRol()) {
            case "Administrador":
                new AdminMainForm(conexion, usuario).setVisible(true);
                break;
            case "Secretario_Admin":
                new SecretarioMainForm(conexion, usuario).setVisible(true);
                break;
            case "Medico":
                Medico medico = obtenerMedicoPorId(usuario.getIdMedicoAsociado());
                if (medico != null) {
                    new MedicoMainForm(conexion, usuario, medico).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Error: Medico asociado no encontrado para el usuario.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                break;
            default:
                JOptionPane.showMessageDialog(this, "Rol de usuario desconocido.", "Error", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }

    private Medico obtenerMedicoPorId(Integer idMedico) {
        if (idMedico == null) {
            return null;
        }
        String query = "SELECT m.IdMedico, m.Nombre, m.Apellido, m.NumeroMatricula, m.Telefono, e.IdEspecialidad, e.NombreEspecialidad "
                + "FROM Medicos m JOIN Especialidades e ON m.IdEspecialidad = e.IdEspecialidad WHERE m.IdMedico = ?";
        try (Connection conn = conexion.getConexion(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, idMedico);
            try (ResultSet rs = conexion.ejecutarQuery(pstmt)) {
                if (rs.next()) {
                    Especialidad especialidad = new Especialidad(rs.getInt("IdEspecialidad"), rs.getString("NombreEspecialidad"));
                    return new Medico(
                            rs.getInt("IdMedico"),
                            rs.getString("Nombre"),
                            rs.getString("Apellido"),
                            rs.getString("NumeroMatricula"),
                            especialidad,
                            rs.getString("Telefono")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Fallo al obtener medico por ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        btnLogin = new javax.swing.JButton();
        txtContrasena = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Login");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setResizable(false);
        setSize(new java.awt.Dimension(350, 200));

        jLabel1.setText("Usuario:");

        jLabel2.setText("Contrasena:");

        btnLogin.setText("Ingresar");
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });
        btnLogin.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnLoginKeyPressed(evt);
            }
        });

        txtContrasena.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtContrasenaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnLogin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addGap(30, 30, 30)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtUsuario)
                            .addComponent(txtContrasena, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE))))
                .addGap(50, 50, 50))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtContrasena, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(32, 32, 32)
                .addComponent(btnLogin)
                .addGap(32, 32, 32))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        autenticarUsuario();
    }//GEN-LAST:event_btnLoginActionPerformed

    private void txtContrasenaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtContrasenaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtContrasenaActionPerformed

    private void btnLoginKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btnLoginKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnLoginKeyPressed

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
            java.util.logging.Logger.getLogger(LoginForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LoginForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LoginForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LoginForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LoginForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPasswordField txtContrasena;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables
}
