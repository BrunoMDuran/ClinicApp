package gui;

import conexion.ConexionSQLServer;
import modelo.Usuario;
import hilos.CitaProcessor_Thread;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon; 
import java.io.IOException;
import javax.imageio.ImageIO;

public class AdminMainForm extends javax.swing.JFrame {
    private ConexionSQLServer conexion;
    private Usuario usuarioLogeado;
    
    private LoginForm loginform;
    
    public AdminMainForm() {
        initComponents();
    
    }
    
    public AdminMainForm(ConexionSQLServer conexion, Usuario usuario) {
        this.conexion = conexion;
        this.usuarioLogeado = usuario;
        
        initComponents();
        setLocationRelativeTo(null);
        setTitle("Panel de Administracion - " + usuario.getNombreUsuario());
        
        setSize(500, 450);
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
    }

    private void procesarCitasAntiguas() {
        String fechaStr = JOptionPane.showInputDialog(this, "Marcar como 'No Asistida' citas ANTES de (AAAA-MM-DD):", LocalDate.now().toString());
        if (fechaStr == null || fechaStr.isEmpty()) {
            return;
        }
        
        try {
            LocalDate fechaLimite = LocalDate.parse(fechaStr);
            new CitaProcessor_Thread(conexion, fechaLimite).start(); 
            JOptionPane.showMessageDialog(this, "Procesamiento de citas antiguas iniciado en segundo plano.", "Informacion", JOptionPane.INFORMATION_MESSAGE);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Formato de fecha invalido. Use AAAA-MM-DD.", "Error de Fecha", JOptionPane.ERROR_MESSAGE);
        }
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

        lblBienvenida = new javax.swing.JLabel();
        btnGestionUsuarios = new javax.swing.JButton();
        btnGestionMedicos = new javax.swing.JButton();
        btnGestionEspecialidades = new javax.swing.JButton();
        btnProcesarCitasAntiguas = new javax.swing.JButton();
        btnGestionHorarios = new javax.swing.JButton();
        btnLogout = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        btnGestionUsuarios.setText("Gestion de Usuarios");
        btnGestionUsuarios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionUsuariosActionPerformed(evt);
            }
        });

        btnGestionMedicos.setText("Gestion de Medicos");
        btnGestionMedicos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionMedicosActionPerformed(evt);
            }
        });

        btnGestionEspecialidades.setText("Gestion de Especialidades");
        btnGestionEspecialidades.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionEspecialidadesActionPerformed(evt);
            }
        });

        btnProcesarCitasAntiguas.setText("Procesar Citas Antiguas");
        btnProcesarCitasAntiguas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProcesarCitasAntiguasActionPerformed(evt);
            }
        });

        btnGestionHorarios.setText("Gestion de Horarios Medicos");
        btnGestionHorarios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionHorariosActionPerformed(evt);
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
                .addGap(150, 150, 150)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblBienvenida, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnGestionUsuarios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnGestionMedicos, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(btnGestionEspecialidades, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(btnProcesarCitasAntiguas, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(btnGestionHorarios, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(btnLogout, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                .addContainerGap(150, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(lblBienvenida)
                .addGap(30, 30, 30)
                .addComponent(btnGestionUsuarios)
                .addGap(18, 18, 18)
                .addComponent(btnGestionMedicos)
                .addGap(18, 18, 18)
                .addComponent(btnGestionEspecialidades)
                .addGap(18, 18, 18)
                .addComponent(btnGestionHorarios)
                .addGap(18, 18, 18)
                .addComponent(btnProcesarCitasAntiguas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 91, Short.MAX_VALUE)
                .addComponent(btnLogout)
                .addGap(63, 63, 63))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGestionUsuariosActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnGestionUsuariosActionPerformed
        new ABMUsuariosForm(conexion).setVisible(true);
    }//GEN-LAST:event_btnGestionUsuariosActionPerformed

    private void btnProcesarCitasAntiguasActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnProcesarCitasAntiguasActionPerformed
        procesarCitasAntiguas();
    }//GEN-LAST:event_btnProcesarCitasAntiguasActionPerformed

    private void btnLogoutActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        cerrarSesion();
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void btnGestionMedicosActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnGestionMedicosActionPerformed
        new ABMMedicosForm(conexion).setVisible(true);
    }//GEN-LAST:event_btnGestionMedicosActionPerformed

    private void btnGestionEspecialidadesActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnGestionEspecialidadesActionPerformed
         new ABMEspecialidadesForm(conexion).setVisible(true);
    }//GEN-LAST:event_btnGestionEspecialidadesActionPerformed

    private void btnGestionHorariosActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnGestionHorariosActionPerformed
        new GestionHorariosMedicoForm(conexion).setVisible(true);
    }//GEN-LAST:event_btnGestionHorariosActionPerformed

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
            java.util.logging.Logger.getLogger(AdminMainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AdminMainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AdminMainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AdminMainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AdminMainForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGestionEspecialidades;
    private javax.swing.JButton btnGestionHorarios;
    private javax.swing.JButton btnGestionMedicos;
    private javax.swing.JButton btnGestionUsuarios;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnProcesarCitasAntiguas;
    private javax.swing.JLabel lblBienvenida;
    // End of variables declaration//GEN-END:variables
}
